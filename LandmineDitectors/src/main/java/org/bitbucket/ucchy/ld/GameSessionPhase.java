/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

/**
 * ゲームセッションの状態
 * @author ucchy
 */
public enum GameSessionPhase {

    /** ワールドの準備中 */
    PREPARE,

    /** ゲーム中 */
    IN_GAME,

    /** ゲーム終了、クリア */
    WIN,

    /** ゲーム終了、ゲームオーバー */
    LOSE,

    /** ゲーム終了、キャンセル */
    CANCEL;

    /**
     * 文字列からGameSessionPhaseを取得する
     * @param id 文字列
     * @return GameSessionPhase
     */
    public static GameSessionPhase fromString(String id) {

        if ( id == null ) return null;
        for ( GameSessionPhase mode : values() ) {
            if ( mode.toString().equals(id.toUpperCase()) ) {
                return mode;
            }
        }
        return null;
    }
}
