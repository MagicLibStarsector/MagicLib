package org.magiclib.paintjobs

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import org.magiclib.kotlin.getSalvageSeed
import kotlin.random.Random

class MagicPaintjobShinyAdder : BaseCampaignEventListener(false) {
    private val fleetsCheckedIds = mutableSetOf<String>()

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        if (fleet == null || !MagicPaintjobManager.isEnabled || fleet.isPlayerFleet || fleetsCheckedIds.contains(fleet.id)) {
            return
        }

        val allShinyPaintjobs =
            MagicPaintjobManager.getPaintjobs(includeShiny = true).filter { it.isShiny }
        if (allShinyPaintjobs.isEmpty()) {
            return
        }

        val shipsInFleetWithAvailableShiny =
            fleet.fleetData.membersListCopy.filter { it.hullId in allShinyPaintjobs.map { pj -> pj.hullId } }
        val probability = 2 // 1 in X chance of getting a shiny
        var addedShiny = false

        for (ship in shipsInFleetWithAvailableShiny) {
            if (addedShiny) continue // Max of one per fleet
            if (Random(fleet.getSalvageSeed()).nextInt(probability) == 1) {
                MagicPaintjobManager.applyPaintjob(
                    ship,
                    null,
                    allShinyPaintjobs.filter { it.hullId == ship.hullId }.random()
                )

                if (!ship.variant.hasTag(Tags.UNRECOVERABLE)) {
                    ship.variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE)
                }

                addedShiny = true
            }
        }

        fleetsCheckedIds.add(fleet.id)
    }
}