/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ランドマインバスターズ
 * @author ucchy
 */
public class LandmineBusters extends JavaPlugin {

    // TODO ワールド名が旧プラグイン名のままなので、いつか直す・・・
    protected static final String WORLD_NAME = "LandmineDetectors";

    private static LandmineBusters instance;

    private LBConfig config;
    private World world;
    private GameSessionManager manager;
    private LBCommand command;
    private RankingDataManager ranking;

    /**
     * プラグインが有効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;

        // ワールドのロード
        world = getServer().getWorld(WORLD_NAME);
        if ( world == null ) {
            world = createWorld();
        }

        // コンフィグのロード
        config = new LBConfig(this);

        // マネージャの生成
        manager = new GameSessionManager();

        // ランキングデータのロード
        ranking = new RankingDataManager(getDataFolder());

        // コマンドの準備
        command = new LBCommand();

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new LBListener(), this);
    }

    /**
     * プラグインが無効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {

        // ゲーム中のセッションがある場合、全てキャンセルする
        for ( GameSession session : manager.getAllSessions() ) {
            if ( session.getPhase() == GameSessionPhase.PREPARE ||
                    session.getPhase() == GameSessionPhase.IN_GAME ) {
                session.runCancel();
            }
        }
    }

    /**
     * プラグインのコマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.command.onCommand(sender, command, label, args);
    }

    /**
     * プラグインのコマンドでTABキー補完された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, command, alias, args);
    }

    /**
     * プラグイン用のワールドを生成する
     * @return
     */
    private World createWorld() {

        WorldCreator creator = new WorldCreator(WORLD_NAME);

        // Nullチャンクジェネレータを設定し、からっぽの世界が生成されるようにする
        creator.generator(new ChunkGenerator() {
            public byte[][] generateBlockSections(
                    World world, Random r,
                    int x, int z, ChunkGenerator.BiomeGrid biomes) {
                return new byte[256 / 16][];
            }
            public Location getFixedSpawnLocation(World world, Random random) {
                return new Location(world, 0, 70, 0);
            }
        });

        World world = getServer().createWorld(creator);

        // ずっと昼にする
        world.setTime(6000);
        world.setGameRuleValue("doDaylightCycle", "false");

        // MOBが沸かないようにする
        world.setGameRuleValue("doMobSpawning", "false");

        // 天候を晴れにする
        world.setStorm(false);
        world.setThundering(false);

        return world;
    }

    /**
     * LandmineDetectorsのコンフィグデータを取得する
     * @return
     */
    protected LBConfig getLBConfig() {
        return config;
    }

    /**
     * プラグイン用のワールドを取得する
     * @return プラグイン用のワールド
     */
    protected World getWorld() {
        return world;
    }

    /**
     * ゲームセッションマネージャを取得する
     * @return ゲームセッションマネージャ
     */
    protected GameSessionManager getGameSessionManager() {
        return manager;
    }

    /**
     * ランキングデータマネージャを取得する
     * @return ランキングデータマネージャ
     */
    protected RankingDataManager getRankingManager() {
        return ranking;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * インスタンスを返す
     * @return
     */
    protected static LandmineBusters getInstance() {
        return instance;
    }
}
