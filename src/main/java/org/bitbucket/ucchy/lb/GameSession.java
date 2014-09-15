/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ゲームセッション
 * @author ucchy
 */
public class GameSession {

    private Inventory tempInventory;
    private int tempLevel;
    private float tempExp;
    private Location tempLoc;

    private Player player;
    private int size;
    private int mine;
    private Difficulty difficulty;
    private GameSessionPhase phase;

    private int grid_x;
    private int grid_z;
    private FieldData field;
    private Location startLoc;

    private BukkitRunnable delayTimer;

    private long startTime;

    /**
     * コンストラクタ
     * @param player プレイヤー
     * @param size マップサイズ
     * @param minenum 埋める地雷の個数
     * @param difficulty 難易度
     */
    public GameSession(Player player, int size, int mine, Difficulty difficulty) {

        this.player = player;
        this.size = size;
        this.mine = mine;
        this.difficulty = difficulty;

        // そのままPREPAREフェーズに移行する
        runPrepare();
    }

    /**
     * ゲームのPREPAREフェーズを実行する
     */
    public void runPrepare() {

        phase = GameSessionPhase.PREPARE;

        // グリッドをマネージャから取得する
        int[] grid = LandmineBusters.getInstance().getGameSessionManager().getOpenGrid();
        this.grid_x = grid[0];
        this.grid_z = grid[1];

        // グリッドにゲーム用フィールドを生成する
        Location origin = new Location(
                LandmineBusters.getInstance().getWorld(), grid_x * 64, 65, grid_z * 64);
        this.field = new FieldData(size, mine, origin);
        startLoc = field.applyField();

        // 不正防止用のテレポート遅延をする
        int delay = LandmineBusters.getInstance().getLBConfig().getStartDelay();
        if ( delay == 0 ) {
            // そのままIN_GAMEフェイズに移行する
            runInGame();

        } else {

            player.sendMessage("ゲームの開始準備をしています...");
            player.sendMessage(ChatColor.RED + "" + delay +
                    ChatColor.WHITE + "秒間動かずにお待ちください...");

            // 指定の秒数後にゲームを開始する
            delayTimer = new BukkitRunnable() {
                public void run() {
                    runInGame();
                }
            };
            delayTimer.runTaskLater(LandmineBusters.getInstance(), delay * 20);
        }
    }

    /**
     * ゲームのIN_GAMEフェーズを実行する
     */
    public void runInGame() {

        // 既にゲームがキャンセルされているなら、何もしない
        if ( phase == GameSessionPhase.CANCEL ) {
            return;
        }

        phase = GameSessionPhase.IN_GAME;

        // 何かに乗っている、何かを乗せているなら強制パージする
        player.leaveVehicle();
        if ( player.getPassenger() != null ) {
            player.getPassenger().leaveVehicle();
        }

        // 元いた場所を記憶する
        tempLoc = player.getLocation();

        // スタート地点に送る
        player.teleport(startLoc, TeleportCause.PLUGIN);

        // プレイヤーの身ぐるみを剥がす
        moveToTempInventory();

        // アイテムを持たせる
        player.getInventory().addItem(new ItemStack(Material.REDSTONE_TORCH_ON, mine));

        // 開始時刻を記録する
        startTime = System.currentTimeMillis();

        // メッセージを流す
        player.sendMessage(ChatColor.GOLD +
                "Welcome to Landmine Busters !!");
        player.sendMessage(
                "フィールドに埋まっている地雷にレッドストーントーチを立てて、"
                + "全て無効化してください。");
        player.sendMessage(
                "経験値バーは一番近い地雷との距離を、"
                + "レベルは周囲のマスにある地雷の個数を示します。");
    }

    /**
     * ゲームのWINフェーズを実行する
     */
    public void runWin() {

        phase = GameSessionPhase.WIN;

        // インベントリを復帰する
        restoreInventory();

        // もといた場所に戻す
        player.teleport(tempLoc, TeleportCause.PLUGIN);

        // セッションマネージャから登録を削除する
        LandmineBusters.getInstance().getGameSessionManager().removeSession(player);

        // メッセージを流す
        player.sendMessage("ゲームに勝利しました！");

        // リザルトを表示する
        sendResult(true);
    }

    /**
     * ゲームのLOSEフェーズを実行する
     * @return リスポーン地点
     */
    public Location runLose() {

        phase = GameSessionPhase.LOSE;

        // インベントリを復帰する
        restoreInventory();

        // セッションマネージャから登録を削除する
        LandmineBusters.getInstance().getGameSessionManager().removeSession(player);

        // リザルトを表示する
        sendResult(false);

        // リスポーン地点を返す
        return tempLoc;
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runCancel() {

        phase = GameSessionPhase.CANCEL;

        if ( delayTimer != null ) {
            // 遅延開始タイマーをキャンセルする
            delayTimer.cancel();
            delayTimer = null;
        }

        if ( tempInventory != null ) {
            // インベントリを復帰する
            restoreInventory();
        }

        if ( tempLoc != null ) {
            // もといた場所に戻す
            player.teleport(tempLoc, TeleportCause.PLUGIN);
        }

        // セッションマネージャから登録を削除する
        LandmineBusters.getInstance().getGameSessionManager().removeSession(player);

        // メッセージを流す
        player.sendMessage("ゲームがキャンセルされました。");
    }

    /**
     * インベントリや経験値を、テンポラリ領域に保存する
     */
    private void moveToTempInventory() {

        tempInventory = Bukkit.createInventory(player, 6 * 9);
        for ( ItemStack item : player.getInventory().getContents() ) {
            if ( item != null ) {
                tempInventory.addItem(item);
            }
        }
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = player.getInventory().getArmorContents()[index];
            if ( armor != null ) {
                tempInventory.setItem(45 + index, armor);
            }
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });

        tempLevel = player.getLevel();
        tempExp = player.getExp();
        player.setLevel(0);
        player.setExp(0);
    }

    /**
     * テンポラリ領域に保存していたインベントリを復帰する
     */
    private void restoreInventory() {

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });

        for ( ItemStack item : tempInventory.getContents() ) {
            if ( item != null ) {
                player.getInventory().addItem(item);
            }
        }
        ItemStack[] armorCont = new ItemStack[4];
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = tempInventory.getItem(45 + index);
            if ( armor != null ) {
                armorCont[index] = armor;
            } else {
                armorCont[index] = new ItemStack(Material.AIR);
            }
            player.getInventory().setArmorContents(armorCont);
        }

        player.setLevel(tempLevel);
        player.setExp(tempExp);
    }

    /**
     * リザルトの表示と、スコアの計算、ランキングへの反映
     * @param isClear クリアか失敗か
     * @return スコアデータ
     */
    private RankingScoreData sendResult(boolean isClear) {

        int point = 0;

        // 残りタイムポイントを加算（クリア時のみ）
        int time = (int)((System.currentTimeMillis() - startTime) / 1000);
        int timePoint = (mine * 20 - time) * 2;
        if ( !isClear || timePoint < 0 ) timePoint = 0;
        point += timePoint;

        // 踏破率ポイントを加算
//        double stepOnPercent = field.getStepOnPercentage();
//        int stepOnPoint = (int)(stepOnPercent * 100);
//        point += stepOnPoint;

        // 地雷除去ポイントを加算
        int deactive = field.deactiveCount();
        int deactivePoint = deactive * 10;
        point += deactivePoint;

        player.sendMessage("==========リザルト==========");
        if ( isClear ) {
            player.sendMessage(difficulty.getName() + " " + ChatColor.RED + "クリア！！");
        } else {
            player.sendMessage(difficulty.getName() + " " + ChatColor.DARK_AQUA + "失敗。。。");
        }
        player.sendMessage(String.format(
                "タイム: %d秒 " + ChatColor.GREEN + "(+%dP)", time, timePoint));
//        player.sendMessage(String.format(
//                "踏破率: %.1f％ " + ChatColor.GREEN + "(+%dP)", stepOnPercent*100, stepOnPoint));
        player.sendMessage(String.format(
                "除去した地雷: %d個 " + ChatColor.GREEN + "(+%dP)", deactive, deactivePoint));
        player.sendMessage(ChatColor.GOLD + "今回のスコア: " + ChatColor.GREEN + point + "P");

        // スコアデータを作成する
        RankingScoreData data = new RankingScoreData();
        data.setName(player.getName());
        data.setScore(point);
        data.setUuid(player.getUniqueId());

        // ランキングを更新する
        RankingDataManager manager =
                LandmineBusters.getInstance().getRankingManager();
        if ( manager.getData(difficulty).updateData(data) ) {
            player.sendMessage(ChatColor.GOLD + "自己ベストを更新しました！");
        }
        int num = manager.getRankingNum(player, difficulty);
        int best = manager.getScore(player, difficulty);
        player.sendMessage("あなたの現在のランキング: " +
                ChatColor.LIGHT_PURPLE + num + "位" +
                ChatColor.GREEN + "(" + best + "P)");

        player.sendMessage("==========================");

        return data;
    }

    /**
     * 遅延開始タイマー中かどうかを返す
     * @return
     */
    public boolean isDelayTimer() {
        return delayTimer != null;
    }

    /**
     * @return player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return field
     */
    public FieldData getField() {
        return field;
    }

    /**
     * @return phase
     */
    public GameSessionPhase getPhase() {
        return phase;
    }

    /**
     * @return grid_x
     */
    public int getGrid_x() {
        return grid_x;
    }

    /**
     * @return grid_z
     */
    public int getGrid_z() {
        return grid_z;
    }

    /**
     * このセッションが終了しているかどうかを返す
     * @return 終了しているかどうか
     */
    public boolean isEnd() {
        return (phase == GameSessionPhase.WIN ||
                phase == GameSessionPhase.LOSE ||
                phase == GameSessionPhase.CANCEL);
    }
}