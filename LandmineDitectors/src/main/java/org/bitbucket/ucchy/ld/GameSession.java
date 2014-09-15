/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
    private GameSessionPhase phase;

    private int grid_x;
    private int grid_z;
    private FieldData field;

    private long startTime;

    /**
     * コンストラクタ
     * @param player プレイヤー
     * @param size マップサイズ
     * @param minenum 埋める地雷の個数
     */
    public GameSession(Player player, int size, int mine) {

        this.player = player;
        this.size = size;
        this.mine = mine;

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
        Location startLoc = field.applyField();

        // 元いた場所を記憶する
        tempLoc = player.getLocation();

        // TODO 不正防止用のテレポート遅延をする

        // 何かに乗っている、何かを乗せているなら強制パージする
        player.leaveVehicle();
        if ( player.getPassenger() != null ) {
            player.getPassenger().leaveVehicle();
        }

        // スタート地点に送る
        player.teleport(startLoc, TeleportCause.PLUGIN);

        // プレイヤーの身ぐるみを剥がす
        moveToTempInventory();

        // アイテムを持たせる
        player.getInventory().addItem(new ItemStack(Material.REDSTONE_TORCH_ON, mine));

        // そのままIN_GAMEフェイズに移行する
        runInGame();
    }

    /**
     * ゲームのIN_GAMEフェーズを実行する
     */
    public void runInGame() {

        phase = GameSessionPhase.IN_GAME;

        // 開始時刻を記録する
        startTime = System.currentTimeMillis();

        // メッセージを流す
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
     */
    public void runLose() {

        phase = GameSessionPhase.LOSE;

        // インベントリを復帰する TODO リスポーン後にする必要があるかも
        restoreInventory();

        // もといた場所に戻す TODO リスポーン後にする必要があるかも
        player.teleport(tempLoc, TeleportCause.PLUGIN);

        // セッションマネージャから登録を削除する
        LandmineBusters.getInstance().getGameSessionManager().removeSession(player);

        // メッセージを流す
        player.sendMessage("地雷を踏んでしまった・・・");

        // リザルトを表示する
        sendResult(false);
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runCancel() {

        phase = GameSessionPhase.LOSE;

        // インベントリを復帰する
        restoreInventory();

        // もといた場所に戻す
        player.teleport(tempLoc, TeleportCause.PLUGIN);

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

    private int sendResult(boolean isClear) {

        int point = 0;

        // 残りタイムポイントを加算（クリア時のみ）
        int time = (int)((System.currentTimeMillis() - startTime) / 1000);
        int timePoint = mine * 20 - time;
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
            player.sendMessage(ChatColor.RED + "クリア！！");
        } else {
            player.sendMessage(ChatColor.BLUE + "失敗。。。");
        }
        player.sendMessage(String.format(
                "タイム: %d秒 " + ChatColor.GREEN + "(+%dP)", time, timePoint));
//        player.sendMessage(String.format(
//                "踏破率: %.1f％ " + ChatColor.GREEN + "(+%dP)", stepOnPercent*100, stepOnPoint));
        player.sendMessage(String.format(
                "除去した地雷: %d個 " + ChatColor.GREEN + "(+%dP)", deactive, deactivePoint));
        player.sendMessage(ChatColor.GOLD + "トータルスコア: " + point + "P");
        player.sendMessage("==========================");

        return point;
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
