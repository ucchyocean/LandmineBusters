/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.entity.Player;

/**
 * ランキングデータマネージャ
 * @author ucchy
 */
public class RankingDataManager {

    private HashMap<Difficulty, RankingData> datas;

    public RankingDataManager(File dataFolder) {

        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        datas = new HashMap<Difficulty, RankingData>();
        for ( Difficulty difficulty : Difficulty.values() ) {
            datas.put(difficulty, loadData(dataFolder, difficulty.getName()));
        }
    }

    private RankingData loadData(File dataFolder, String rank) {

        File file = new File(dataFolder, "ranking_" + rank + ".yml");

        if ( !file.exists() ) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // do nothing.
            }
        }

        return RankingData.loadFromFile(file);
    }

    public RankingData getData(Difficulty difficulty) {
        return datas.get(difficulty);
    }

    public int getRankingNum(Player player, Difficulty difficulty) {
        return datas.get(difficulty).getRankingNum(player);
    }

    public int getScore(Player player, Difficulty difficulty) {
        return datas.get(difficulty).getScore(player);
    }
}
