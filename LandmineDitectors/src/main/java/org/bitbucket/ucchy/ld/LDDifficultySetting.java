/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.ld;

import org.bukkit.configuration.ConfigurationSection;

/**
 *
 * @author ucchy
 */
public class LDDifficultySetting {

    private int size;
    private int mine;

    protected LDDifficultySetting(int size, int mine) {
        this.size = size;
        this.mine = mine;
    }

    protected static LDDifficultySetting loadFromSection(
            ConfigurationSection section, int defSize, int defMine) {

        int size = section.getInt("size", defSize);
        int mine = section.getInt("mine", defMine);
        return new LDDifficultySetting(size, mine);
    }

    /**
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return mine
     */
    public int getMine() {
        return mine;
    }

    /**
     * @param size size
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @param mine mine
     */
    public void setMine(int mine) {
        this.mine = mine;
    }
}
