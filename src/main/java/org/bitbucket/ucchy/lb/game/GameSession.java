/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.game;

import org.bitbucket.ucchy.lb.Difficulty;
import org.bitbucket.ucchy.lb.LandmineBusters;
import org.bitbucket.ucchy.lb.Messages;
import org.bitbucket.ucchy.lb.Utility;
import org.bitbucket.ucchy.lb.ranking.RankingDataManager;
import org.bitbucket.ucchy.lb.ranking.RankingScoreData;
import org.bukkit.Bukkit;
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

    private LandmineBusters parent;
    private Inventory tempInventory;
    private Inventory tempArmors;
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
     * @param parent プラグインのインスタンス
     * @param player プレイヤー
     * @param size マップサイズ
     * @param minenum 埋める地雷の個数
     * @param difficulty 難易度
     */
    public GameSession(LandmineBusters parent, Player player,
            int size, int mine, Difficulty difficulty) {

        this.parent = parent;
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

        player.sendMessage(Messages.get("InformationPreparing"));

        // グリッドをマネージャから取得する
        int[] grid = parent.getGameSessionManager().getOpenGrid();
        this.grid_x = grid[0];
        this.grid_z = grid[1];

        // グリッドにゲーム用フィールドを生成する
        Location origin = new Location(
                parent.getWorld(), grid_x * 64, 65, grid_z * 64);
        RankingScoreData top = parent.getRankingManager().getTopData(difficulty);
        this.field = new FieldData(size, mine, origin, difficulty, top);
        startLoc = field.applyField();

        // 不正防止用のテレポート遅延をする
        int delay = parent.getLBConfig().getStartDelay();
        if ( delay == 0 ) {
            // そのままIN_GAMEフェイズに移行する
            runInGame();

        } else {

            player.sendMessage(Messages.get(
                    "InformationPreparingDelay", "%delay", delay));

            // 指定の秒数後にゲームを開始する
            delayTimer = new BukkitRunnable() {
                public void run() {
                    runInGame();
                }
            };
            delayTimer.runTaskLater(parent, delay * 20);
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
        player.sendMessage(Messages.get("InformationStartGame1"));
        player.sendMessage(Messages.get("InformationStartGame2",
                "%num", field.getRemainCount()));
        player.sendMessage(Messages.get("InformationStartGame3"));

        // アナウンスを流す
        if ( parent.getLBConfig().isAnnounce() ) {
            String message = Messages.get("AnnouncePrefix")
                    + Messages.get("AnnounceStartGame",
                            new String[]{"%name", "%difficulty"},
                            new String[]{player.getName(), difficulty.getName()});
            Bukkit.broadcastMessage(message);
        }
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
        parent.getGameSessionManager().removeSession(player);

        // メッセージを流す
        player.sendMessage(Messages.get("InformationEndGameWin"));

        // リザルトを表示する
        RankingScoreData score = sendResult(true);

        // アナウンスを流す
        if ( parent.getLBConfig().isAnnounce() ) {
            String pre = Messages.get("AnnouncePrefix");
            Bukkit.broadcastMessage(
                    pre + Messages.get("AnnounceEndGameWin", "%name", player.getName()));
            Bukkit.broadcastMessage(
                    pre + Messages.get("AnnounceResult",
                            new String[]{"%difficulty", "%score"},
                            new String[]{difficulty.getName(), score.getScore() + ""}));
        }
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
        parent.getGameSessionManager().removeSession(player);

        // リザルトを表示する
        RankingScoreData score = sendResult(false);

        // アナウンスを流す
        if ( parent.getLBConfig().isAnnounce() ) {
            String pre = Messages.get("AnnouncePrefix");
            Bukkit.broadcastMessage(
                    pre + Messages.get("AnnounceEndGameLose", "%name", player.getName()));
            Bukkit.broadcastMessage(
                    pre + Messages.get("AnnounceResult",
                            new String[]{"%difficulty", "%score"},
                            new String[]{difficulty.getName(), score.getScore() + ""}));
        }

        // リスポーン地点を返す（プレイヤー死亡中はテレポートできないため）
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
        parent.getGameSessionManager().removeSession(player);

        // メッセージを流す
        player.sendMessage(Messages.get("InformationGameCancelled"));
    }

    /**
     * インベントリや経験値を、テンポラリ領域に保存する
     */
    private void moveToTempInventory() {

        // インベントリの保存
        tempInventory = Bukkit.createInventory(player, 5 * 9);
        for ( ItemStack item : player.getInventory().getContents() ) {
            if ( item != null ) {
                tempInventory.addItem(item);
            }
        }

        // 防具の保存
        tempArmors = Bukkit.createInventory(player, 9);
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = player.getInventory().getArmorContents()[index];
            if ( armor != null ) {
                tempArmors.setItem(index, armor);
            }
        }

        // インベントリの消去とアップデート
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });
        updateInventory(player);

        // レベルと経験値の保存と消去
        tempLevel = player.getLevel();
        tempExp = player.getExp();
        player.setLevel(0);
        player.setExp(0);
    }

    /**
     * テンポラリ領域に保存していたインベントリを復帰する
     */
    private void restoreInventory() {

        // インベントリの消去
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });

        // インベントリと防具の復帰、更新
        for ( ItemStack item : tempInventory.getContents() ) {
            if ( item != null ) {
                player.getInventory().addItem(item);
            }
        }
        ItemStack[] armorCont = new ItemStack[4];
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = tempArmors.getItem(index);
            if ( armor != null ) {
                armorCont[index] = armor;
            } else {
                armorCont[index] = new ItemStack(Material.AIR);
            }
            player.getInventory().setArmorContents(armorCont);
        }
        updateInventory(player);

        // テンポラリ領域の消去（念のため）
        tempInventory.clear();
        tempInventory = null;
        tempArmors.clear();
        tempArmors = null;

        // レベルと経験値の復帰
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

        // 地雷除去ポイントを加算
        int deactive = field.getDeactiveCount();
        int deactivePoint = deactive * 10;
        point += deactivePoint;

        player.sendMessage(Messages.get("InformationResultTitle"));
        if ( isClear ) {
            player.sendMessage(Messages.get(
                    "InformationResultWin", "%difficulty", difficulty.getName()));
        } else {
            player.sendMessage(Messages.get(
                    "InformationResultLose", "%difficulty", difficulty.getName()));
        }
        player.sendMessage(Messages.get("InformationResultTime",
                new String[]{"%second", "%point"},
                new String[]{time + "", timePoint + ""}));
        player.sendMessage(Messages.get("InformationResultMine",
                new String[]{"%mine", "%point"},
                new String[]{deactive + "", deactivePoint + ""}));
        player.sendMessage(Messages.get(
                "InformationResultTotal", "%pointP", point));

        // スコアデータを作成する
        RankingScoreData data = new RankingScoreData();
        data.setName(player.getName());
        data.setScore(point);
        if ( Utility.isCB178orLater() ) {
            data.setUuid(player.getUniqueId().toString());
        } else {
            data.setUuid(player.getName());
        }

        // ランキングを更新する
        RankingDataManager manager = parent.getRankingManager();
        if ( manager.getData(difficulty).updateData(data) ) {
            player.sendMessage(Messages.get("InformationResultUpdated"));
        }
        int num = manager.getRankingNum(player, difficulty);
        int best = manager.getScore(player, difficulty);
        player.sendMessage(Messages.get("InformationResultYourRank",
                new String[]{"%rank", "%point"},
                new String[]{num + "", best + ""}));

        player.sendMessage(Messages.get("InformationResultLastLine"));

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

    /**
     * インベントリのアップデートを行う
     * @param player 更新対象のプレイヤー
     */
    @SuppressWarnings("deprecation")
    private void updateInventory(Player player) {
        player.updateInventory();
    }
}
