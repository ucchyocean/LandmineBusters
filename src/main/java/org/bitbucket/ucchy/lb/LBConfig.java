/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.lb;

import java.io.File;
import java.util.HashMap;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * LandmineBustersのコンフィグクラス
 * @author ucchy
 */
public class LBConfig {

    private LandmineBusters parent;
    private String lang;
    private HashMap<Difficulty, LBDifficultySetting> difficulty;
    private int startDelay;
    private boolean announce;

    /**
     * コンストラクタ
     * @param parent
     */
    public LBConfig(LandmineBusters parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        String releaseLang = Utility.getDefaultLocaleLanguage();
        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            if ( releaseLang.equals("ja") ) {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config_ja.yml", false);
            } else {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config.yml", false);
            }
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        lang = conf.getString("lang", releaseLang);

        difficulty = new HashMap<Difficulty, LBDifficultySetting>();
        for ( Difficulty dif : Difficulty.values() ) {
            String path = "difficulty." + dif.getName();
            if ( conf.contains(path) ) {
                difficulty.put(dif, LBDifficultySetting.loadFromSection(
                        conf.getConfigurationSection(path), dif.size, dif.mine));
            } else {
                difficulty.put(dif, new LBDifficultySetting(dif.size, dif.mine));
            }
        }

        startDelay = conf.getInt("startDelay", 5);

        announce = conf.getBoolean("announce", true);

        // 値のチェック
        for ( Difficulty dif : difficulty.keySet() ) {
            LBDifficultySetting setting = difficulty.get(dif);
            if ( setting.getMine() <= 0 ) {
                setting.setMine(1);
            } else if ( setting.getMine() >= (setting.getSize() * setting.getSize()) ) {
                setting.setMine((setting.getSize() * setting.getSize()) - 1);
            }
        }
    }

    /**
     * @return lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @return difficulty
     */
    public HashMap<Difficulty, LBDifficultySetting> getDifficulty() {
        return difficulty;
    }

    /**
     * @return startDelay
     */
    public int getStartDelay() {
        return startDelay;
    }

    /**
     * @return announce
     */
    public boolean isAnnounce() {
        return announce;
    }
}
