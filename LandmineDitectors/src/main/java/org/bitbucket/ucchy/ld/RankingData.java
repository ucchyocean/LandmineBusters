/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * ランキングデータ
 * @author ucchy
 */
public class RankingData {

    private HashMap<UUID, RankingScoreData> datas;
    private ArrayList<RankingScoreData> ranking;
    private File file;

    /**
     * コンストラクタ
     */
    private RankingData(File file) {
        this.file = file;
        datas = new HashMap<UUID, RankingScoreData>();
        ranking = new ArrayList<RankingScoreData>();
    }

    /**
     * データを更新する
     * @param data データ
     * @return 更新したかどうか、
     * 既にランキング登録されているスコアより低くて更新されなかった場合はfalse
     */
    public boolean updateData(RankingScoreData data) {

        UUID uuid = data.getUuid();
        if ( datas.containsKey(uuid) ) {

            if ( data.getScore() <= datas.get(uuid).getScore() ) {
                // ランキングに登録されているデータの方がスコアが高い
                return false;
            }
        }

        // データ挿入
        datas.put(uuid, data);

        // ランキングを更新する
        ranking = new ArrayList<RankingScoreData>(datas.values());
        Collections.sort(ranking, new Comparator<RankingScoreData>() {
            public int compare(RankingScoreData o1, RankingScoreData o2) {
                return o2.getScore() - o1.getScore();
            }
        });

        // 保存
        save();

        return true;
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

            if ( !isUUID(key) ) {
                continue;
            }

            UUID uuid = UUID.fromString(key);

            RankingScoreData data =
                    RankingScoreData.loadFromSection(config.getConfigurationSection(key));
            data.setUuid(uuid);

            ranking.datas.put(uuid, data);
        }

        return ranking;
    }

    /**
     * ファイルにデータを保存する
     */
    private void save() {

        YamlConfiguration config = new YamlConfiguration();

        for ( UUID uuid : datas.keySet() ) {
            ConfigurationSection section = config.createSection(uuid.toString());
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
}
