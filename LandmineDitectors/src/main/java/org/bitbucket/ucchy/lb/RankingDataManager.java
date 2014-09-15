/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.io.File;
import java.io.IOException;

/**
 * ランキングデータマネージャ
 * @author ucchy
 */
public class RankingDataManager {

    private RankingData easy;
    private RankingData normal;
    private RankingData hard;

    public RankingDataManager(File dataFolder) {

        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        easy = loadData(dataFolder, "easy");
        normal = loadData(dataFolder, "normal");
        hard = loadData(dataFolder, "hard");
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

    /**
     * @return easy
     */
    public RankingData getEasy() {
        return easy;
    }

    /**
     * @return normal
     */
    public RankingData getNormal() {
        return normal;
    }

    /**
     * @return hard
     */
    public RankingData getHard() {
        return hard;
    }
}
