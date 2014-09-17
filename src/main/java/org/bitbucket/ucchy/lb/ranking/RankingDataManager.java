/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.ranking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.lb.Difficulty;
import org.bukkit.entity.Player;

/**
 * ランキングデータマネージャ
 * @author ucchy
 */
public class RankingDataManager {

    private HashMap<Difficulty, RankingData> datas;

    /**
     * コンストラクタ
     * @param dataFolder プラグインのデータフォルダ
     */
    public RankingDataManager(File dataFolder) {

        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        datas = new HashMap<Difficulty, RankingData>();
        for ( Difficulty difficulty : Difficulty.values() ) {
            datas.put(difficulty, loadData(dataFolder, difficulty));
        }
    }

    /**
     * ランキングデータをロードする
     * @param dataFolder プラグインのデータフォルダ
     * @param difficulty 難易度
     * @return ロードされたランキングデータ
     */
    private RankingData loadData(File dataFolder, Difficulty difficulty) {

        File file = new File(dataFolder, "ranking_" + difficulty.getName() + ".yml");

        if ( !file.exists() ) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // do nothing.
            }
        }

        return RankingData.loadFromFile(file);
    }

    /**
     * 指定した難易度のランキングデータを取得する
     * @param difficulty 難易度
     * @return ランキングデータ
     */
    public RankingData getData(Difficulty difficulty) {
        return datas.get(difficulty);
    }

    /**
     * 指定されたプレイヤーの、指定された難易度における順位を取得する
     * @param player プレイヤー
     * @param difficulty 難易度
     * @return 順位
     */
    public int getRankingNum(Player player, Difficulty difficulty) {
        return datas.get(difficulty).getRankingNum(player);
    }

    /**
     * 指定されたプレイヤーの、指定された難易度におけるスコアを取得する
     * @param player プレイヤー
     * @param difficulty 難易度
     * @return スコア
     */
    public int getScore(Player player, Difficulty difficulty) {
        return datas.get(difficulty).getScore(player);
    }

    /**
     * 指定した難易度のトッププレイヤーのデータを取得する
     * @param difficulty 難易度
     * @return トッププレイヤーのデータ（プレイ人数が0人ならnull）
     */
    public RankingScoreData getTopData(Difficulty difficulty) {

        RankingData data = getData(difficulty);
        ArrayList<RankingScoreData> ranking = data.getRanking();
        if ( ranking.size() <= 0 ) {
            return null;
        }
        return ranking.get(0);
    }
}
