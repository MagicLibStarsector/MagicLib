package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import org.magiclib.achievements.MagicAchievement
import org.magiclib.kotlin.getDistance

class BlackHoleAchievement : MagicAchievement() {
    internal var plugin: Plugin? = null

    internal inner class Plugin : BaseCampaignPlugin() {
        override fun getId(): String = "magiclib_black_hole_achievement"

        override fun pickRespawnPlugin(): PluginPick<InteractionDialogPlugin>? {
            if (Global.getSector().playerFleet.containingLocation == Global.getSector().hyperspace)
                return null

            Global.getSector().starSystems
                .flatMap { it.planets }
                .filter { it.isStar }
                .filter { it.spec.isBlackHole }
                .forEach { hole ->
                    // player fleet inside black hole
                    if (hole.location.getDistance(Global.getSector().playerFleet.location) < hole.radius) {
                        completeAchievement()
                        saveChanges()
                        return null
                    }
                }

            return null
        }
    }

    override fun onSaveGameLoaded() {
        if (isComplete) return

        plugin = Plugin()
        Global.getSector().registerPlugin(plugin)
    }

    override fun onDestroyed() {
        Global.getSector().unregisterPlugin(plugin?.id)
    }
}
