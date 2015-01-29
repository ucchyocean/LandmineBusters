/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.ranking;

import org.bukkit.configuration.ConfigurationSection;

/**
 * ランキング掲載用のスコアデータコンポーネント
 * @author ucchy
 */
public class RankingScoreData {

    /** スコア */
    private int score;

    /** プレイヤーID */
    private String uuid;

    /** プレイヤー名 */
    private String name;

    /**
     * @return score
     */
    public int getScore() {
        return score;
    }

    /**
     * @param score score
     */
    public void setScore(int score) {
        this.score = score;
    }

    /**
     * @return uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid uuid
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * セクションからデータをロードする（ただしUUIDが含まれないことに注意）
     * @param section
     * @return
     */
    public static RankingScoreData loadFromSection(ConfigurationSection section) {

        RankingScoreData data = new RankingScoreData();
        data.setName(section.getString("name", ""));
        data.setScore(section.getInt("score", 0));
        return data;
    }

    /**
     * 指定されたセクションにデータを保存する
     * @param section
     */
    public void saveToSection(ConfigurationSection section) {
        section.set("name", name);
        section.set("score", score);
    }
}
