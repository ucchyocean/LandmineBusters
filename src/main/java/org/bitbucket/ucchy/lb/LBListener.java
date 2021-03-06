/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.util.HashMap;
import java.util.UUID;

import org.bitbucket.ucchy.lb.game.GameSession;
import org.bitbucket.ucchy.lb.game.GameSessionManager;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * LandmineBustersのリスナークラス
 * @author ucchy
 */
public class LBListener implements Listener {

    private static final String META_KEEPEXP = "KeepExp";

    private HashMap<UUID, Location> respawnLocations;

    public LBListener() {
        respawnLocations = new HashMap<UUID, Location>();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        Location location = player.getLocation();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // 遅延開始タイマー中なら、セッションをキャンセルする
        if ( manager.isPlayerPrepare(player) ) {
            GameSession session = manager.getSession(player);
            if ( session.isDelayTimer() ) {
                session.runCancel();
            }
            return;
        }

        // ゲーム中のプレイヤーでなければ無視する
        if ( !manager.isPlayerInGame(player) ) {
            return;
        }

        GameSession session = manager.getSession(player);

        // 現在位置のブロックを取得する
        Block block = event.getTo().getBlock();

        // 現在位置に地雷があるかどうかを確認する。あったらゲームオーバー
        if ( session.getField().isLandmineExist(location) ) {

            // 爆発エフェクト
            GameoverEffect effect =
                    LandmineBusters.getInstance().getLBConfig().getGameoverEffect();
            if ( effect == GameoverEffect.BOMB ) {
                location.getWorld().createExplosion(location, 0);
            } else if ( effect == GameoverEffect.FIREWORK ) {
                final Firework firework = (Firework)location.getWorld().spawnEntity(
                        player.getEyeLocation(), EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();
                FireworkEffect feffect = FireworkEffect.builder()
                        .flicker(true)
                        .withColor(Color.WHITE)
                        .withFade(Color.RED)
                        .with(Type.BALL_LARGE)
                        .trail(true)
                        .build();
                meta.addEffect(feffect);
                meta.setPower(1);
                firework.setFireworkMeta(meta);
                new BukkitRunnable() {
                    public void run() {
                        firework.detonate();
                    }
                }.runTaskLater(LandmineBusters.getInstance(), 1);
            }

            // ダメージを与えてゲームオーバー画面にする
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setMetadata(META_KEEPEXP,
                    new FixedMetadataValue(LandmineBusters.getInstance(), true));
            player.sendMessage(Messages.get("InformationEndGameLose"));
            player.damage(30000);
            respawnLocations.put(player.getUniqueId(), session.runLose());
            return; // ゲームオーバー

        } else {

            // フィールドから落下した場合はゲームオーバー
            if ( location.getY() < 50 ) {
                player.setMetadata(META_KEEPEXP,
                        new FixedMetadataValue(LandmineBusters.getInstance(), true));
                player.sendMessage(Messages.get("InformationEndGameLoseFall"));
                player.damage(30000);
                respawnLocations.put(player.getUniqueId(), session.runLose());
                return; // ゲームオーバー
            }

            // 足元を土ブロックに変更する
            Block down = block.getRelative(BlockFace.DOWN);
            if ( block.getType() == Material.AIR &&
                    (down.getType() == Material.GRASS || down.getType() == Material.MYCEL) ) {
                setCoarseDirt(down);
                session.getField().increaseStepOn();
            }
        }

        // 一番近い地雷との距離を、経験値バーに反映する
        double value = 9.0 - session.getField().getNearestMineDistance(location);

        if ( value > 0 ) {
            player.setExp((float)(value / 9.0));
        } else {
            player.setExp(0);
        }

        // 周囲の地雷の個数を、経験値バーのレベルに反映する
        player.setLevel(session.getField().getLandmineCountAround(location));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // ゲーム中のプレイヤーでなければ無視する
        if ( !manager.isPlayerInGame(player) ) {
            return;
        }

        GameSession session = manager.getSession(player);

        // レッドストーントーチでなければ設置を拒否する
        if ( event.getBlock().getType() != Material.REDSTONE_TORCH_ON ) {
            event.setCancelled(true);
            return;
        }

        // 下部のブロックを取得する
        Block block = event.getBlock().getRelative(BlockFace.DOWN);

        // 地雷が埋まっていたら無効化する
        int remain = session.getField().tryDeactiveMine(block.getLocation());

        // 残り0個になったら勝利
        if ( remain <= 0 ) {
            session.runWin();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // ゲーム中のプレイヤーでなければ無視する
        if ( !manager.isPlayerInGame(player) ) {
            return;
        }

        GameSession session = manager.getSession(player);

        // レッドストーントーチでなければ破壊を拒否する
        if ( event.getBlock().getType() != Material.REDSTONE_TORCH_ON ) {
            event.setCancelled(true);
            return;
        }

        // 下部のブロックを取得する
        Block down = event.getBlock().getRelative(BlockFace.DOWN);

        // 地雷が埋まっていたら有効化する
        session.getField().tryActiveMine(down.getLocation());

        // 一旦イベントをキャンセルし、ブロックを削除、インベントリに直接追加する
        event.setCancelled(true);
        event.getBlock().setType(Material.AIR);
        player.getInventory().addItem(new ItemStack(Material.REDSTONE_TORCH_ON));
        updateInventory(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // ゲーム中のプレイヤーでなければ無視する
        if ( !manager.isPlayerInGame(player) ) {
            return;
        }

        GameSession session = manager.getSession(player);

        // ゲーム中プレイヤーがサーバーを退出してしまう場合は、
        // セッションをキャンセルする。
        session.runCancel();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // 遅延開始タイマー中なら、セッションをキャンセルする
        if ( manager.isPlayerPrepare(player) ) {
            GameSession session = manager.getSession(player);
            if ( session.isDelayTimer() ) {
                session.runCancel();
            }
            return;
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        if ( !(event.getEntity() instanceof Player) ) {
            return;
        }

        Player player = (Player)event.getEntity();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // 遅延開始タイマー中なら、セッションをキャンセルする
        if ( manager.isPlayerPrepare(player) ) {
            GameSession session = manager.getSession(player);
            if ( session.isDelayTimer() ) {
                session.runCancel();
            }
            return;
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        if ( !player.hasMetadata(META_KEEPEXP) ) {
            return;
        }

        player.removeMetadata(META_KEEPEXP, LandmineBusters.getInstance());
        event.setKeepLevel(true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        if ( !respawnLocations.containsKey(player.getUniqueId()) ) {
            return;
        }

        event.setRespawnLocation(respawnLocations.get(player.getUniqueId()));
        respawnLocations.remove(player.getUniqueId());
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {

        // プラグインのワールドで天候が変更した場合に阻止する。
        if ( event.getWorld().getName().equals(LandmineBusters.WORLD_NAME) ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {

        // プラグインのワールドで天候が変更した場合に阻止する。
        if ( event.getWorld().getName().equals(LandmineBusters.WORLD_NAME) ) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void setCoarseDirt(Block block) {

        block.setType(Material.DIRT);
        block.setData((byte) 1);
    }

    @SuppressWarnings("deprecation")
    private void updateInventory(Player player) {
        player.updateInventory();
    }
}
