/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.game;

import java.util.ArrayList;

import org.bitbucket.ucchy.lb.Difficulty;
import org.bitbucket.ucchy.lb.ranking.RankingScoreData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;

/**
 * フィールドデータ
 * @author ucchy
 */
public class FieldData {

    private int size;
    private ArrayList<Location> mines;
    private ArrayList<Location> actives;
    private Location origin;
    private Difficulty difficulty;
    private RankingScoreData top;

    private int stepOnCount;

    /**
     * コンストラクタ
     * @param size 1辺のサイズ
     * @param minenum 埋まっている地雷の数
     * @param origin フィールドの基点
     * @param difficulty ゲームの難易度
     * @param top 該当難易度のトッププレイヤーのデータ
     */
    public FieldData(int size, int minenum, Location origin,
            Difficulty difficulty, RankingScoreData top) {

        this.size = size;
        this.origin = origin;
        this.difficulty = difficulty;
        this.top = top;
        this.stepOnCount = 0;

        mines = new ArrayList<Location>();

        for ( int i=0; i<minenum; i++ ) {
            Location location;
            do {
                int x = (int)(Math.random() * size);
                int z = (int)(Math.random() * size);
                location = new Location(
                        origin.getWorld(), origin.getBlockX() + x,
                        origin.getBlockY(), origin.getBlockZ() + z);
            } while ( containsSameLocation(mines, location) );
            mines.add(location);
        }

        // クローンを作る
        actives = new ArrayList<Location>();
        actives.addAll(mines);
    }

    /**
     * ワールドに、ゲームフィールドを生成する
     * @return ゲームフィールドのスタート地点
     */
    protected Location applyField() {

        int startx = origin.getBlockX();
        int startz = origin.getBlockZ();
        World world = origin.getWorld();

        // 領域を全クリア
        for ( int x=startx; x<startx+64; x++ ) {
            for ( int z=startz; z<startz+64; z++ ) {
                for ( int y=255; y>=0; y-- ) {
                    if ( world.getBlockAt(x, y, z).getType() != Material.AIR ) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
        for ( Entity entity : world.getEntities() ) {
            Location loc = entity.getLocation();
            if ( startx <= loc.getBlockX() && loc.getBlockX() <= (startx + 64) &&
                    startz <= loc.getBlockZ() && loc.getBlockZ() <= (startz + 64) ) {
                entity.remove();
            }
        }

        // 草ブロックを生成
        for ( int x = startx; x < startx + size; x++ ) {
            for ( int z = startz; z < startz + size; z++ ) {
                Material material = (x + z) % 2 == 0 ? Material.GRASS : Material.MYCEL;
                world.getBlockAt(x, origin.getBlockY(), z).setType(material);
            }
        }

        // スタート地点を生成
        world.getBlockAt((startx+size), origin.getBlockY(), (startz+size-2)).setType(Material.STONE);
        world.getBlockAt((startx+size), origin.getBlockY(), (startz+size-1)).setType(Material.STONE);
        world.getBlockAt((startx+size), origin.getBlockY(), (startz+size)).setType(Material.STONE);
        world.getBlockAt((startx+size-1), origin.getBlockY(), (startz+size)).setType(Material.STONE);
        world.getBlockAt((startx+size-2), origin.getBlockY(), (startz+size)).setType(Material.STONE);

        // 情報看板
        Block signBlock = world.getBlockAt((startx+size), origin.getBlockY()+1, (startz+size-2));
        signBlock.setType(Material.SIGN_POST);
        Sign sign = (Sign)signBlock.getState();
        sign.setLine(0, "難易度: " + difficulty.getName());
        if ( top != null ) {
            sign.setLine(1, "現在のトップ:");
            sign.setLine(2, top.getName());
            sign.setLine(3, top.getScore() + "P");
        }
        setBlockData(signBlock, (byte)0);
        sign.update();

        // スタート地点を返す
        Location startLoc = new Location(
                world, startx+size+0.5, origin.getBlockY() + 1, startz+size+0.5 );
        startLoc.setDirection(origin.subtract(startLoc).toVector().setY(0).normalize());
        return startLoc;
    }

    /**
     * 一番近い地雷との距離を返す
     * @param location 現在位置
     * @return 距離
     */
    public double getNearestMineDistance(Location location) {

        double least = Double.MAX_VALUE;
        for ( Location mine : mines ) {
            Location temp = mine.clone().add(0.5, 1.0, 0.5);
            if ( least > temp.distanceSquared(location) ) {
                least = temp.distanceSquared(location);
            }
        }
        return least;
    }

    /**
     * 周囲1マス範囲の地雷の個数を返す
     * @param location 現在位置
     * @return 1マス範囲内の地雷の個数
     */
    public int getLandmineCountAround(Location location) {

        int count = 0;
        for ( Location loc : mines ) {
            int dx = location.getBlockX() - loc.getBlockX();
            int dz = location.getBlockZ() - loc.getBlockZ();
            if ( -1 <= dx && dx <= 1 && -1 <= dz && dz <= 1 ) {
                count++;
            }
        }
        return count;
    }

    /**
     * 現在位置に地雷が埋まっているかどうかを返す
     * @param location 現在位置
     * @return 地雷が埋まっているかどうか
     */
    public boolean isLandmineExist(Location location) {
        return containsSameLocation(mines, location);
    }

    /**
     * 現在位置にアクティブな地雷が埋まっているかどうかを返す
     * @param location 現在位置
     * @return 地雷が埋まっているかどうか
     */
    public boolean isActiveExist(Location location) {
        return containsSameLocation(actives, location);
    }

    /**
     * 指定された場所に地雷がうまっているなら、無効にする
     * @param location 場所
     * @return 残りの地雷の個数
     */
    public int tryDeactiveMine(Location location) {

        Location candidate = null;
        for ( Location loc : actives ) {
            if ( loc.getBlockX() == location.getBlockX() &&
                    loc.getBlockZ() == location.getBlockZ() ) {
                candidate = loc;
            }
        }
        if ( candidate != null ) {
            actives.remove(candidate);
        }
        return actives.size();
    }

    /**
     * 指定された場所に地雷が埋まっているなら、有効にする
     * @param location 場所
     * @return 残りの地雷の個数
     */
    public int tryActiveMine(Location location) {

        Location candidate = null;
        for ( Location loc : mines ) {
            if ( loc.getBlockX() == location.getBlockX() &&
                    loc.getBlockZ() == location.getBlockZ() ) {
                candidate = loc;
            }
        }
        if ( candidate != null && !actives.contains(candidate) ) {
            actives.add(candidate);
        }
        return actives.size();
    }

    /**
     * 踏破率を1つ加算する
     */
    public void increaseStepOn() {
        stepOnCount++;
    }

    /**
     * 踏破率を返す
     * @return 踏破率
     */
    public double getStepOnPercentage() {
        int total = (size * size) - mines.size();
        return (double)stepOnCount / (double)total;
    }

    /**
     * 無効化した地雷の個数を返す
     * @return 無効化した地雷の個数
     */
    public int deactiveCount() {
        return mines.size() - actives.size();
    }

    /**
     * 指定されたArrayListに、同じ場所を示すLocationが含まれていいるかどうかを返す
     * @param locations Locationの配列
     * @param location 評価するLocation
     * @return 含まれているかどうか
     */
    private boolean containsSameLocation(ArrayList<Location> locations, Location location) {

        for ( Location loc : locations ) {
            if ( loc.getBlockX() == location.getBlockX() &&
                    loc.getBlockZ() == location.getBlockZ() ) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private void setBlockData(Block block, byte data) {
        block.setData(data);
    }

//    private void debugPrint() {
//
//        for ( int x=0; x<size; x++ ) {
//            for ( int y=0; y<size; y++ ) {
//                if ( !mine[x][y] )
//                    System.out.print("□");
//                else
//                    System.out.print("■");
//            }
//            System.out.println();
//        }
//    }
}
