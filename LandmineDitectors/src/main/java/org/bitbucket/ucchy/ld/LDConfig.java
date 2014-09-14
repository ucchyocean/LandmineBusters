/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import java.io.File;
import java.util.HashMap;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * LandmineDetectorsのコンフィグクラス
 * @author ucchy
 */
public class LDConfig {

    private LandmineDetectors parent;
    private HashMap<String, LDDifficultySetting> difficulty;
    private int startDelay;

    /**
     * コンストラクタ
     * @param parent
     */
    public LDConfig(LandmineDetectors parent) {
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

        difficulty = new HashMap<String, LDDifficultySetting>();

        if ( conf.contains("difficulty.easy") ) {
            difficulty.put("easy", LDDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.easy"), 10, 5));
        } else {
            difficulty.put("easy", new LDDifficultySetting(10, 5));
        }

        if ( conf.contains("difficulty.normal") ) {
            difficulty.put("normal", LDDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.normal"), 12, 12));
        } else {
            difficulty.put("normal", new LDDifficultySetting(12, 12));
        }

        if ( conf.contains("difficulty.hard") ) {
            difficulty.put("hard", LDDifficultySetting.loadFromSection(
                    conf.getConfigurationSection("difficulty.hard"), 15, 30));
        } else {
            difficulty.put("hard", new LDDifficultySetting(15, 30));
        }

        startDelay = conf.getInt("startDelay", 5);

        // 値のチェック
        for ( String dif : new String[]{"easy", "normal", "hard"} ) {
            LDDifficultySetting setting = difficulty.get(dif);
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
    public HashMap<String, LDDifficultySetting> getDifficulty() {
        return difficulty;
    }

    /**
     * @return startDelay
     */
    public int getStartDelay() {
        return startDelay;
    }
}
