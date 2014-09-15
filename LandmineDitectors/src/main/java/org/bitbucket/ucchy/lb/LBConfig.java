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
    private HashMap<String, LBDifficultySetting> difficulty;
    private int startDelay;

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

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            Utility.copyFileFromJar(
                    parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        difficulty = new HashMap<String, LBDifficultySetting>();

        if ( conf.contains("difficulty.easy") ) {
            difficulty.put("easy", LBDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.easy"), 10, 5));
        } else {
            difficulty.put("easy", new LBDifficultySetting(10, 5));
        }

        if ( conf.contains("difficulty.normal") ) {
            difficulty.put("normal", LBDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.normal"), 12, 12));
        } else {
            difficulty.put("normal", new LBDifficultySetting(12, 12));
        }

        if ( conf.contains("difficulty.hard") ) {
            difficulty.put("hard", LBDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.hard"), 15, 30));
        } else {
            difficulty.put("hard", new LBDifficultySetting(15, 30));
        }

        startDelay = conf.getInt("startDelay", 5);

        // 値のチェック
        for ( String dif : new String[]{"easy", "normal", "hard"} ) {
            LBDifficultySetting setting = difficulty.get(dif);
            if ( setting.getMine() <= 0 ) {
                setting.setMine(1);
            } else if ( setting.getMine() >= (setting.getSize() * setting.getSize()) ) {
                setting.setMine((setting.getSize() * setting.getSize()) - 1);
            }
        }
    }

    /**
     * @return difficulty
     */
    public HashMap<String, LBDifficultySetting> getDifficulty() {
        return difficulty;
    }

    /**
     * @return startDelay
     */
    public int getStartDelay() {
        return startDelay;
    }
}
