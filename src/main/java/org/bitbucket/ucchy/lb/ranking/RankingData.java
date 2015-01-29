/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb.ranking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.bitbucket.ucchy.lb.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * ランキングデータ
 * @author ucchy
 */
public class RankingData {

    private HashMap<String, RankingScoreData> datas;
    private ArrayList<RankingScoreData> ranking;
    private File file;

    /**
     * コンストラクタ
     */
    private RankingData(File file) {
        this.file = file;
        datas = new HashMap<String, RankingScoreData>();
        ranking = new ArrayList<RankingScoreData>();
    }

    /**
     * データを更新する
     * @param data データ
     * @return 更新したかどうか、
     * 既にランキング登録されているスコアより低くて更新されなかった場合はfalse
     */
    public boolean updateData(RankingScoreData data) {

        String uuid = data.getUuid();
        if ( datas.containsKey(uuid) ) {

            if ( data.getScore() <= datas.get(uuid).getScore() ) {
                // ランキングに登録されているデータの方がスコアが高い
                return false;
            }
        }

        // データ挿入
        datas.put(uuid, data);

        // ランキングを更新する
        updateRanking();

        // 保存
        save();

        return true;
    }

    /**
     * ランキングを更新する
     */
    private void updateRanking() {

        ranking = new ArrayList<RankingScoreData>(datas.values());
        Collections.sort(ranking, new Comparator<RankingScoreData>() {
            public int compare(RankingScoreData o1, RankingScoreData o2) {
                return o2.getScore() - o1.getScore();
            }
        });
    }

    /**
     * ファイルからランキングデータをロードする
     * @param file ファイル
     * @return ランキングデータ
     */
    public static RankingData loadFromFile(File file) {

        RankingData ranking = new RankingData(file);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for ( String key : config.getKeys(false) ) {

            String uuid = key;
            if ( !isUUID(key) && Utility.isCB178orLater() ) {
                OfflinePlayer player = Utility.getOfflinePlayer(key);
                if ( player != null && player.hasPlayedBefore() ) {
                    // UUIDへアップデートする
                    uuid = player.getUniqueId().toString();
                }
            }

            RankingScoreData data =
                    RankingScoreData.loadFromSection(config.getConfigurationSection(key));
            data.setUuid(uuid);

            ranking.datas.put(key, data);
        }

        ranking.updateRanking();

        return ranking;
    }

    /**
     * ファイルにデータを保存する
     */
    private void save() {

        YamlConfiguration config = new YamlConfiguration();

        for ( String uuid : datas.keySet() ) {
            ConfigurationSection section = config.createSection(uuid);
            datas.get(uuid).saveToSection(section);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文字列がUUIDかどうかを判定する
     * @param source 文字列
     * @return UUIDかどうか
     */
    private static boolean isUUID(String source) {
        return source.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * @return ranking
     */
    public ArrayList<RankingScoreData> getRanking() {
        return ranking;
    }

    /**
     * 指定されたプレイヤーの順位を取得する
     * @param player プレイヤー
     * @return 順位
     */
    public int getRankingNum(Player player) {

        if ( !datas.containsKey(player.getUniqueId()) ) {
            return 99999;
        }

        for ( int index = 0; index < ranking.size(); index++ ) {
            if ( ranking.get(index).getUuid().equals(player.getUniqueId()) ) {
                return index + 1;
            }
        }
        return 99999;
    }

    /**
     * 指定されたプレイヤーのスコアを取得する
     * @param player プレイヤー
     * @return スコア
     */
    public int getScore(Player player) {

        if ( !datas.containsKey(player.getUniqueId()) ) {
            return 0;
        }
        return datas.get(player.getUniqueId()).getScore();
    }
}
