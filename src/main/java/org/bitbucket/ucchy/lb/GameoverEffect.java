/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.lb;

/**
 * ゲームオーバー時のエフェクトの種類
 * @author ucchy
 */
public enum GameoverEffect {

    /** 爆発 */
    BOMB,

    /** 花火 */
    FIREWORK,

    /** エフェクト無し */
    NONE,
    ;

    /**
     * 指定された文字列と一致するGameoverEffectを返します。一致しない場合は、デフォルト値が返されます。
     * @param name エフェクト名
     * @param def デフォルト値
     * @return 一致するGameoverEffect
     */
    public static GameoverEffect getFromString(String name, GameoverEffect def) {
        if ( name == null ) return def;
        for ( GameoverEffect effect : values() ) {
            if ( effect.name().equals(name) ) {
                return effect;
            }
        }
        return def;
    }
}
