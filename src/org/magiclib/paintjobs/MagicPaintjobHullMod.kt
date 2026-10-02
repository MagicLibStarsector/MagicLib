package org.magiclib.paintjobs

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicTxt
import org.magiclib.util.taskScheduler.CombatTaskScheduler
import org.magiclib.util.taskScheduler.SectorTaskScheduler

/**
 * This hullmod displays the paintjob itself. It determines which to display by looking for a tag on the variant.
 * The hullmod also allows the player to see in refit if a paintjob is applied and remove it.
 */
class MagicPaintjobHullMod : BaseHullMod() {
    companion object {
        const val ID = "ML_skinSwap"
        const val PAINTJOB_TAG_PREFIX = "ML_paintjob-"
        private const val MAGIC_PJ_CACHE_KEY = "MagicPaintjobCache"
        private const val MAGIC_PAINTJOB_APPLIED_KEY = "MagicPaintjobApplied"
        private const val MAGIC_PAINTJOB_ENGINES_APPLIED_KEY = "MagicPaintjobEnginesApplied"
    }

    private fun applyToEngines(ship: ShipAPI, paintjob: MagicPaintjobSpec) {
        if(!ship.hasTag(MAGIC_PAINTJOB_ENGINES_APPLIED_KEY)) {
            MagicPaintjobManager.applyPaintjobToEngines(ship, paintjob)
            ship.addTag(MAGIC_PAINTJOB_ENGINES_APPLIED_KEY)
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship ?: return
        id ?: return
        if (!MagicPaintjobManager.isEnabled) return
        val paintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.variant) ?: return

        MagicPaintjobManager.applyPaintjob(ship, paintjob)

        ship.setCustomData(MAGIC_PJ_CACHE_KEY, paintjob)

        // The way the base game handles this is very scuffed, so it has been disabled.
        // A new ShipAPI appears to be created every single frame when holding a ship in the fleet screen causing the engine color to flicker.
            // (I'm theorizing the drag and drop render is done in a static function, badly, thus needing to make a new ShipAPI to draw it every frame)
        // Engine paintjobs do not always apply in the codex depending on how it is opened. Use F2 to open the codex, and you won't get an engine paintjob. Click the codex button question mark and you get an engine paintjob.
            // (The codex opens in different places within the UI tree depending on how and where it is opened, thus causing inconsistent behavior)
        // In the fleet screen, all other engine paintjobs reset themselves after dropping a held ship until they are hovered over again.
            // (I'm not sure about this one)
        /*
        // Wait a tick for engines to load in if needed. No need to do this in combat as advanceInCombat will do that.
        if(Global.getCurrentState() == GameState.CAMPAIGN && ship.engineController.shipEngines?.isEmpty() == true) {
            SectorTaskScheduler.performLater {
                applyToEngines(ship, paintjob)
            }
        }
        */
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if (!MagicPaintjobManager.isEnabled) return
        val paintjob = ship.customData[MAGIC_PJ_CACHE_KEY] as? MagicPaintjobSpec ?: return

        // Apply each frame because of shields. (Shields can reset and thus need to be applied each frame)
        MagicPaintjobManager.applyPaintjobToShield(ship, paintjob)

        // Only applies if needed
        applyToEngines(ship, paintjob)

        // fighter wing paintjobs
        for (wing in ship.allWings) {
            for (fighter in wing.wingMembers) {
                if (MAGIC_PAINTJOB_APPLIED_KEY in fighter.customData) continue
                MagicPaintjobManager.getPaintjobsForHull(fighter.hullSpec).firstOrNull {
                    it.paintjobFamily?.equals(paintjob.paintjobFamily) == true
                }?.let { MagicPaintjobManager.applyPaintjob(fighter, it) }

                fighter.setCustomData(MAGIC_PAINTJOB_APPLIED_KEY, true)
            }
        }
    }

    override fun canBeAddedOrRemovedNow(
        ship: ShipAPI?,
        marketOrNull: MarketAPI?,
        mode: CampaignUIAPI.CoreUITradeMode?
    ): Boolean = false

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)
        ship ?: return

        val skin = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec, includeShiny = true)
            .firstOrNull { MagicPaintjobManager.getCurrentShipPaintjob(ship.fleetMember)?.id == it.id }

        if (skin != null) {
            tooltip?.addPara(
                MagicTxt.getString("ml_mp_appliedRefit"),
                10f,
                Misc.getTextColor(),
                Misc.getPositiveHighlightColor(),
                skin.name
            )

            if (skin.isShiny) {
                tooltip?.addPara(
                    MagicTxt.getString("ml_mp_shiny"),
                    3f,
                    Misc.getHighlightColor(),
                    Misc.getHighlightColor()
                )
            }

            if (skin.isPermanent) {
                tooltip?.addPara(
                    MagicTxt.getString("ml_mp_permanentTooltipRefit"),
                    10f,
                    Misc.getGrayColor(),
                    Misc.getHighlightColor()
                )
            }
        }
    }
}