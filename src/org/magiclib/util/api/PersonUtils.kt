package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin

object PersonUtils {
    /**
     * Returns the maximum level for an officer.
     *
     * If the person is the player, returns the maximum level from the levelup plugin.
     * If the person is not an AICore, returns the maximum level from the officer levelup plugin.
     * Otherwise, returns the current level.
     */
    @JvmStatic
    fun getMaxOfficerLevel(person: PersonAPI): Int {
        if (person.isPlayer) {
            val levelUpPlugin = Global.getSettings().levelupPlugin
            return levelUpPlugin.maxLevel
            //return Global.getSettings().getInt("playerMaxLevel")
        } else if (!person.isAICore) {
            val plugin = Global.getSettings().getPlugin("officerLevelUp") as OfficerLevelupPlugin
            return plugin.getMaxLevel(person)
        }
        return person.stats.level
    }

    /**
     * Returns the maximum number of elite skills for an officer.
     *
     * If the person is not an AICore, returns the maximum number of elite skills from the officer levelup plugin.
     * Otherwise, returns the current amount of combat skills.
     */
    @JvmStatic
    fun getMaxOfficerEliteSkills(person: PersonAPI): Int {
        if (!person.isAICore) {
            val plugin = Global.getSettings().getPlugin("officerLevelUp") as OfficerLevelupPlugin
            return plugin.getMaxEliteSkills(person)
        }
        return person.stats.skillsCopy.count { it.level > 0 && it.skill.isCombatOfficerSkill }
    }
}