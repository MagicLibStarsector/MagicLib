package org.magiclib.achievements

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import kotlin.random.Random

internal class MagicAchievementRunner : EveryFrameScript {
    init {
        Global.getSector().registerPlugin(object : BaseCampaignPlugin() {
            // Choose random id each run just in case, though since it's not saved it shouldn't matter.
            private val id = "MagicAchievements_CampaignPlugin_${Random.nextInt()}"

            override fun getId(): String = id

            // No need to add to saves
            override fun isTransient(): Boolean = true

            /**
             * When the player interacts with a dialog, override the default interaction with a
             * mod-specific one if necessary.
             */
            override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
                return this.pickInteractionDialogPlugin(interactionTarget)
            }
        })
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        MagicAchievementManager.getInstance().achievements.forEach { achievement ->
            if (!achievement.isComplete) {
                achievement.advanceInternal(amount)
            }
        }
    }
}