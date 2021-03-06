/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.game;

import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.lb.Difficulty;
import org.bitbucket.ucchy.lb.LandmineBusters;
import org.bukkit.entity.Player;

/**
 * ゲームセッションマネージャ
 * @author ucchy
 */
public class GameSessionManager {

    private LandmineBusters parent;
    private HashMap<String, GameSession> sessions;

    /**
     * コンストラクタ
     * @param parent プラグインのインスタンス
     */
    public GameSessionManager(LandmineBusters parent) {
        this.parent = parent;
        sessions = new HashMap<String, GameSession>();
    }

    /**
     * 指定したプレイヤーが準備中かどうかを返す
     * @param player プレイヤー
     * @return ゲーム中かどうか
     */
    public boolean isPlayerPrepare(Player player) {

        if ( sessions.containsKey(player.getName()) ) {
            return sessions.get(player.getName()).getPhase() ==
                    GameSessionPhase.PREPARE;
        }
        return false;
    }

    /**
     * 指定したプレイヤーがゲーム中かどうかを返す
     * @param player プレイヤー
     * @return ゲーム中かどうか
     */
    public boolean isPlayerInGame(Player player) {

        if ( sessions.containsKey(player.getName()) ) {
            return sessions.get(player.getName()).getPhase() ==
                    GameSessionPhase.IN_GAME;
        }
        return false;
    }

    /**
     * 指定したプレイヤーのゲームセッションを取得する
     * @param player プレイヤー
     * @return ゲームセッション
     */
    public GameSession getSession(Player player) {
        return sessions.get(player.getName());
    }

    /**
     * 指定したプレイヤーのゲームセッションを削除する
     * @param player プレイヤー
     */
    public void removeSession(Player player) {
        if ( isPlayerInGame(player) ) {
            sessions.remove(player.getName());
        }
    }

    /**
     * 新しいゲームセッションを作成する
     * @param player プレイヤー
     * @param size マップサイズ
     * @param mine 埋め込む地雷の個数
     * @param difficulty 難易度
     * @return 作成されたゲームセッション
     */
    public GameSession makeNewSession(Player player,
            int size, int mine, Difficulty difficulty) {
        GameSession session = new GameSession(parent, player, size, mine, difficulty);
        sessions.put(player.getName(), session);
        return session;
    }

    /**
     * 空き状態のグリッドを取得する
     * @return グリッド
     */
    public int[] getOpenGrid() {

        int size = 1;
        int phase = 1;
        int x = 1, z = 0;

        while ( size <= 20 ) {

            boolean isUsed = false;
            for ( GameSession session : sessions.values() ) {
                if ( session.getGrid_x() == x &&
                        session.getGrid_z() == z &&
                        !session.isEnd() ) {
                    isUsed = true;
                }
            }
            if ( !isUsed ) {
                return new int[]{x, z};
            }

            switch ( phase ) {
            case 1:
                x--;
                z++;
                if ( z == size ) {
                    phase = 2;
                }
                break;
            case 2:
                x--;
                z--;
                if ( x == -size ) {
                    phase = 3;
                }
                break;
            case 3:
                x++;
                z--;
                if ( z == -size ) {
                    phase = 4;
                }
                break;
            case 4:
                x++;
                z++;
                if ( x == size ) {
                    phase = 1;
                    size++;
                    x++;
                }
                break;
            }
        }

        return new int[]{20, 0};
        // 同時に840人が遊んでいるなら、この値が返されるが、まずそんなことはない。
    }

    /**
     * 現在のセッションを全て返す
     * @return 全てのセッション
     */
    public ArrayList<GameSession> getAllSessions() {
        return new ArrayList<GameSession>(sessions.values());
    }
}
