@file:JvmName("PersonUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin

/**
 * Returns the maximum level for an officer.
 *
 * If the person is the player, returns the maximum level from the levelup plugin.
 * If the person is not an AICore, returns the maximum level from the officer levelup plugin.
 * Otherwise, returns the current level.
 */
fun PersonAPI.getMaxOfficerLevel(): Int {
    if (this.isPlayer) {
        val levelUpPlugin = Global.getSettings().levelupPlugin
        return levelUpPlugin.maxLevel
        //return Global.getSettings().getInt("playerMaxLevel")
    } else if (!this.isAICore) {
        val plugin = Global.getSettings().getPlugin("officerLevelUp") as OfficerLevelupPlugin
        return plugin.getMaxLevel(this)
    }
    return this.stats.level
}

/**
 * Returns the maximum number of elite skills for an officer.
 *
 * If the person is not an AICore, returns the maximum number of elite skills from the officer levelup plugin.
 * Otherwise, returns the current amount of combat skills.
 */
fun PersonAPI.getMaxOfficerEliteSkills(): Int {
    if (!this.isAICore) {
        val plugin = Global.getSettings().getPlugin("officerLevelUp") as OfficerLevelupPlugin
        return plugin.getMaxEliteSkills(this)
    }
    return this.stats.skillsCopy.count { it.level > 0 && it.skill.isCombatOfficerSkill }
}