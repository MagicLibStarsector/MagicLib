package org.magiclib.paintjobs

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

class MagicSkinSwapHullMod : BaseHullMod() {
    companion object {
        const val ID = "ML_skinSwap"
        const val PAINTJOB_TAG_PREFIX = "ML_paintjob-"
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship ?: return
        id ?: return

        // TODO remove this part
        val randomPaintjob = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.hullId).firstOrNull { it.hullId == ship.hullSpec.hullId }
        if (randomPaintjob != null) {
            MagicPaintjobManager.applyPaintjob(null, ship, randomPaintjob)
        }

        val tag = ship.tags.firstOrNull { it.startsWith(PAINTJOB_TAG_PREFIX) }
        val paintjob = if (tag != null) {
            val paintjobId = tag.removePrefix(PAINTJOB_TAG_PREFIX)
            MagicPaintjobManager.getPaintjob(paintjobId) ?: return
        } else return

        MagicPaintjobManager.applyPaintjob(null, ship, paintjob)
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)
        ship ?: return

        val skins = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.hullId)
        skins.forEach { skin ->
            tooltip?.addPara(skin.name, 10f)
        }
    }
}