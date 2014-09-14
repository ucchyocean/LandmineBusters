/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

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
 * ランドマインディテクターズ
 * @author ucchy
 */
public class LandmineDetectors extends JavaPlugin {

    protected static final String WORLD_NAME = "LandmineDetectors";

    private static LandmineDetectors instance;

    private LDConfig config;
    private World world;
    private GameSessionManager manager;
    private LDCommand command;

    /**
     * プラグインが有効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        instance = this;

        // コンフィグのロード
        config = new LDConfig(this);

        // ワールドのロード
        world = getServer().getWorld(WORLD_NAME);
        if ( world == null ) {
            world = createWorld();
        }

        // マネージャの生成
        manager = new GameSessionManager();

        // コマンドの準備
        command = new LDCommand();

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new LDListener(), this);
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
    protected LDConfig getLDConfig() {
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
    protected static LandmineDetectors getInstance() {
        return instance;
    }
}
