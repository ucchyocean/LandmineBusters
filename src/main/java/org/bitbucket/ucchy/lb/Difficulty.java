/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

/**
 * ゲームの難易度
 * @author ucchy
 */
public enum Difficulty {

    EASY(10, 5),

    NORMAL(12, 12),

    HARD(15, 30);

    /** デフォルトのマップサイズ */
    int size;

    /** デフォルトの地雷個数 */
    int mine;

    /**
     * コンストラクタ
     * @param size デフォルトのマップサイズ
     * @param mine デフォルトの地雷個数
     */
    private Difficulty(int size, int mine) {
        this.size = size;
        this.mine = mine;
    }

    public static Difficulty getFromString(String id, Difficulty def) {

        if ( id == null ) return def;
        for ( Difficulty dif : values() ) {
            if ( dif.name().equalsIgnoreCase(id) ) {
                return dif;
            }
        }
        return def;
    }

    public String getName() {
        return name().toLowerCase();
    }
}
