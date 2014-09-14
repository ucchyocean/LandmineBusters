/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import org.bukkit.Bukkit;
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
    private FieldData field;
    private int size;
    private int mine;
    private GameSessionPhase phase;

    private int grid_x;
    private int grid_z;

    /**
     * コンストラクタ
     * @param player プレイヤー
     * @param size マップサイズ
     * @param minenum 埋める地雷の個数
     */
    public GameSession(Player player, int size, int mine) {

        this.player = player;
        this.field = new FieldData(size, mine);
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
        int[] grid = LandmineDetectors.getGameSessionManager().getOpenGrid();
        this.grid_x = grid[0];
        this.grid_z = grid[1];

        // グリッドにゲーム用フィールドを生成する
        Location startLoc = field.applyField(grid_x * 64, grid_z * 64);

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

        // TODO 何かのメッセージ（ゲームガイド的な）
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

        // TODO 賞金を与える

        // TODO 何かのメッセージ（またきてね！）
    }

    /**
     * ゲームのLOSEフェーズを実行する
     */
    public void runLose() {

        phase = GameSessionPhase.LOSE;

        // インベントリを復帰する
        restoreInventory();

        // もといた場所に戻す
        player.teleport(tempLoc, TeleportCause.PLUGIN);

        // TODO 何かのメッセージ（またきてね！）
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

        // TODO 何かのメッセージ（ゲームがキャンセルされました）
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
