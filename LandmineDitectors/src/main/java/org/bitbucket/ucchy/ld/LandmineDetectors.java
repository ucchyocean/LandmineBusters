/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

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

    private static final String WORLD_NAME = "LandmineDetectors";

    private static LandmineDetectors instance;
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

        // ワールドのロード
        world = getServer().getWorld(WORLD_NAME);
        if ( world == null ) {
            world = createWorld();
        }

        // マネージャの生成
        manager = new GameSessionManager();

        // コマンドの準備
        command = new LDCommand();
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

        return getServer().createWorld(creator);
    }

    /**
     * プラグイン用のワールドを取得する
     * @return プラグイン用のワールド
     */
    public static World getWorld() {
        return instance.world;
    }

    /**
     * ゲームセッションマネージャを取得する
     * @return ゲームセッションマネージャ
     */
    public static GameSessionManager getGameSessionManager() {
        return instance.manager;
    }
}
