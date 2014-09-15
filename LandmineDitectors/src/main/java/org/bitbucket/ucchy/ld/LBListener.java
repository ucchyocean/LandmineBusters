/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * LandmineBustersのリスナークラス
 * @author ucchy
 */
public class LBListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        Location location = player.getLocation();
        GameSessionManager manager =
                LandmineBusters.getInstance().getGameSessionManager();

        // ゲーム中のプレイヤーでなければ無視する
        if ( !manager.isPlayerInGame(player) ) {
            return;
        }

        GameSession session = manager.getSession(player);

        // 現在位置のブロックを取得する
        Block block = event.getTo().getBlock();

        // 現在位置にアクティブな地雷があるかどうかを確認する。あったらゲームオーバー
        if ( session.getField().isActiveExist(location) ) {

            // 爆発エフェクト＆爆死
            location.getWorld().createExplosion(location, 0);
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.damage(30000);
            session.runLose();
            return; // ゲームオーバー

        } else {

            // 足元を土ブロックに変更する
            Block down = block.getRelative(BlockFace.DOWN);
            if ( block.getType() == Material.AIR &&
                    down.getType() == Material.GRASS ) {
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
        Block block = event.getBlock().getRelative(BlockFace.DOWN);

        // 地雷が埋まっていたら有効化する
        session.getField().tryActiveMine(block.getLocation());
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
    public void onWeatherChange(WeatherChangeEvent event) {

        // プラグインのワールドで天候が変更した場合に阻止する。
        if ( event.getWorld().getName().equals(LandmineBusters.WORLD_NAME) ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(ThunderChangeEvent event) {

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
}
