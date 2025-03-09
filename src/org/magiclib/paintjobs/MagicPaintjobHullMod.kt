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
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship ?: return
        id ?: return

        if (!MagicPaintjobManager.isEnabled) {
            return
        }

        val paintjob = getAppliedPaintjob(ship) ?: return

        MagicPaintjobManager.applyPaintjob(ship, paintjob)

    }

    private fun getAppliedPaintjob(ship: ShipAPI): MagicPaintjobSpec? {
        // (the tag should only be on the variant, not the ship, but I don't trust myself)
        val tag = (ship.tags + ship.variant.tags).firstOrNull { it.startsWith(PAINTJOB_TAG_PREFIX) }
        return tag?.removePrefix(PAINTJOB_TAG_PREFIX)?.let { paintjobId ->
            MagicPaintjobManager.getPaintjob(paintjobId)
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

        val skin = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId, includeShiny = true)
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