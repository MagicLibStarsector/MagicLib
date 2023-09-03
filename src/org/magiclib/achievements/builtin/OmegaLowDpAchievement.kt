package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import org.magiclib.achievements.MagicAchievement

/**
 * Defeated <ultra redacted> using a maximum of 80 deployment points.
 */
class OmegaLowDpAchievement : MagicAchievement() {

    inner class OmegaLowDpAchievementListener : BaseCampaignEventListener(false) {
        override fun reportPlayerEngagement(result: EngagementResultAPI?) {
            super.reportPlayerEngagement(result)
            result ?: return

            try {
                if (result.didPlayerWin()) {
                    // Check that there's a coronal tap in the system
                    if (Global.getSector().playerFleet.containingLocation?.allEntities?.any { it.hasTag(Entities.CORONAL_TAP) } != true)
                        return

                    // Check that player fought two Omega cruisers (or capitals, in case a mod replaces the cruisers with capitals for some reason)
                    if (result.loserResult.allEverDeployedCopy.map { it.member }
                            .count { (it.isCruiser || it.isCapital) && it.fleetData.fleet.faction.id == Factions.OMEGA } < 2)
                        return

                    // get deployment points used by player
                    val playerDeployedAmount =
                        result.winnerResult.allEverDeployedCopy.sumOf {
                            (it.member?.deploymentPointsCost ?: 0f).toDouble()
                        }

                    if (playerDeployedAmount <= 80) {
                        completeAchievement()
                        saveChanges()
                        onDestroyed()
                    }
                }
            } catch (e: Exception) {
                logger.warn(e.message, e)
            }
        }
    }

    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()
        Global.getSector().removeTransientScriptsOfClass(OmegaLowDpAchievementListener::class.java)
        Global.getSector()?.addTransientListener(OmegaLowDpAchievementListener())
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().removeTransientScriptsOfClass(OmegaLowDpAchievementListener::class.java)
    }
}
