/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * フィールドデータ
 * @author ucchy
 */
public class FieldData {

    private boolean[][] mine;

    /**
     * コンストラクタ
     * @param size 1辺のサイズ
     * @param minenum 埋まっている地雷の数
     */
    public FieldData(int size, int minenum) {

        mine = new boolean[size][size];
        for ( int i=0; i<minenum; i++ ) {
            int x, y;
            do {
                x = (int)(Math.random() * size);
                y = (int)(Math.random() * size);
            } while ( mine[x][y] );
            mine[x][y] = true;
        }
    }

    /**
     * ワールドに、ゲームフィールドを生成する
     * @param startx 生成する基点のx座標
     * @param startz 生成する基点のz座標
     * @return ゲームフィールドのスタート地点
     */
    protected Location applyField(int startx, int startz) {

        World world = LandmineDetectors.getWorld();

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

        // 草ブロックを生成
        int size = mine.length;
        for ( int x = startx; x < startx + size; x++ ) {
            for ( int z = startz; z < startz + size; z++ ) {
                world.getBlockAt(x, 65, z).setType(Material.GRASS);
            }
        }

        // スタート地点を生成
        world.getBlockAt((startx+size), 65, (startz+size-1)).setType(Material.STONE);

        // スタート地点を返す
        Location startLoc = new Location(world, (startx+size+0.5), 65, (startz+size-0.5));
        Location originLoc = new Location(world, startx, 65, startz);
        startLoc.setDirection(originLoc.subtract(startLoc).toVector().normalize());
        return startLoc;
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
