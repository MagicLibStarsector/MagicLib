package org.magiclib.paintjobs

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicTxt

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
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship ?: return
        id ?: return
        if (!MagicPaintjobManager.isEnabled) return
        val paintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.variant) ?: return

        MagicPaintjobManager.applyPaintjob(ship, paintjob)

        if (paintjob.engineSpec == null) ship.setCustomData(MAGIC_PJ_CACHE_KEY, paintjob)
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if (!MagicPaintjobManager.isEnabled) return
        val paintjob = ship.customData[MAGIC_PJ_CACHE_KEY] as? MagicPaintjobSpec
            ?: run {
                val resolved = MagicPaintjobManager.getCurrentShipPaintjob(ship.variant) ?: return
                // If the paintjob sets engines, delay until the engines exist
                if (ship.engineController.shipEngines.isNotEmpty() || resolved.engineSpec == null)
                    MagicPaintjobManager.applyPaintjob(ship, resolved)
                else
                    return

                ship.setCustomData(MAGIC_PJ_CACHE_KEY, resolved)
                resolved
            }

        // Apply each frame because of shields. (Shields can reset and thus need to be applied each frame)
        MagicPaintjobManager.applyPaintjobToShield(ship, paintjob)

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