package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getSalvageSeed
import kotlin.random.Random


class MagicPaintjobShinyAdder : EveryFrameScript {
    companion object {
        @JvmStatic
        var probability = 25 // 1 in 25 chance of spawning a shiny
    }

    private var isDoneInternal = false
    override fun isDone() = isDoneInternal
    override fun runWhilePaused() = false

    private val interval = IntervalUtil(2f, 3f)
    private val fleetsCheckedIds = mutableSetOf<String>()

    override fun advance(amount: Float) {
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        val allShinyPaintjobs =
            MagicPaintjobManager.getPaintjobs(includeShiny = true).filter { it.isShiny }

        // If no shiny paintjobs exist, no point in this script.
        if (allShinyPaintjobs.isEmpty()) {
            isDoneInternal = true
            return
        }

        checkAndApplyShiniesToAllFleetsInPlayerLocation(allShinyPaintjobs)
    }

    fun checkAndApplyShiniesToAllFleetsInPlayerLocation(
        allShinyPaintjobs: List<MagicPaintjobSpec> = MagicPaintjobManager.getPaintjobs(
            includeShiny = true
        ).filter { it.isShiny }
    ) {
        for (fleet in Global.getSector().playerFleet.containingLocation?.fleets ?: emptyList()) {
            if (fleet == null || fleetsCheckedIds.contains(fleet.id) || !MagicPaintjobManager.isEnabled || fleet.isPlayerFleet
            ) {
                continue
            }

            val shipsInFleetWithAvailableShiny =
                fleet.fleetData.membersListCopy.filter { it.hullId in allShinyPaintjobs.flatMap { pj -> pj.hullIds } }
            var addedShiny = false

            for (ship in shipsInFleetWithAvailableShiny) {
                if (addedShiny) continue // Max of one per fleet

                val hullShinyPaintjobs = allShinyPaintjobs.filter { ship.hullId in it.hullIds }
                if (hullShinyPaintjobs.isEmpty()) continue

                val rng = Random(fleet.getSalvageSeed())

                val rollChance = hullShinyPaintjobs.maxOfOrNull { it.isShinyRarity }
                    ?.coerceAtLeast(1) ?: probability

                // Roll the dice once per fleet member that has an available shiny paintjob.
                if (rng.nextInt(rollChance) == 0) {
                    // If PJ already applied to ship, don't reapply.
                    if (!ship.variant.hasHullMod(MagicPaintjobHullMod.ID)) {
                        fun pickWeightedPaintjob(rng: Random): MagicPaintjobSpec {
                            val totalWeight = hullShinyPaintjobs.sumOf { it.isShinyRarity }

                            var roll = rng.nextInt(totalWeight)

                            for (pj in hullShinyPaintjobs) {
                                roll -= pj.isShinyRarity
                                if (roll < 0) return pj
                            }

                            return hullShinyPaintjobs.last()
                        }

                        val paintjob = pickWeightedPaintjob(rng)

                        applyShinyPaintjob(ship, paintjob)
                        addedShiny = true
                    }
                }
            }

            // TODO: Removed this optimization that prevents checking a fleet more than once
            // because the game keeps erasing the variant tags and therefore paintjobs
            // so we need to keep re-applying them!
            //            fleetsCheckedIds.add(fleet.id)
        }
    }

    private fun applyShinyPaintjob(
        ship: FleetMemberAPI,
        paintjob: MagicPaintjobSpec
    ) {
        setClonedVariant(ship)
        MagicPaintjobManager.applyPaintjob(
            ship,
            paintjob
        )

        if (!ship.variant.hasTag(Tags.UNRECOVERABLE)) {
            ship.variant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE)
        }
    }

    /**
     * Clones the variant and sets it as a 'custom' variant, rather than as the base, so that
     * the game doesn't replace/mess with it.
     */
    private fun setClonedVariant(member: FleetMemberAPI, setNullOrigVariant: Boolean = true): ShipVariantAPI {
        val variantClone = member.variant.clone()
        variantClone.hullVariantId = member.hullId + "_" + Misc.genUID()
        variantClone.source = VariantSource.REFIT
        if (setNullOrigVariant) variantClone.originalVariant = null
        member.setVariant(variantClone, false, false)
        return variantClone
    }
}