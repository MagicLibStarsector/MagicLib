package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
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
        var defaultProbability = 25 // 1 in 25 chance of spawning a shiny
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
        val hullsWithShinies = allShinyPaintjobs.flatMap { it.hullIds }.toSet()

        for (fleet in Global.getSector().playerFleet.containingLocation?.fleets ?: emptyList()) {
            if (fleet == null || fleetsCheckedIds.contains(fleet.id) || !MagicPaintjobManager.isEnabled || fleet.isPlayerFleet
            ) {
                continue
            }

            // TODO: Removed this optimization that prevents checking a fleet more than once
            // because the game keeps erasing the variant tags and therefore paintjobs
            // so we need to keep re-applying them!
            //            fleetsCheckedIds.add(fleet.id)
            //

            val shinyAppliedTo =
                if(fleet.memoryWithoutUpdate.contains("\$shinyAppliedTo"))
                    fleet.memoryWithoutUpdate.get("\$shinyAppliedTo") as? String
                else
                    null

            if(shinyAppliedTo != null) {
                if(shinyAppliedTo.isEmpty())
                    continue

                val shinyPaintjob =
                    if(fleet.memoryWithoutUpdate.contains("\$shinyPaintjob"))
                        fleet.memoryWithoutUpdate.get("\$shinyPaintjob") as? String
                    else
                        null
                if(shinyPaintjob == null)
                    continue

                val shinyPaintjobSpec = MagicPaintjobManager.getPaintjob(shinyPaintjob) ?: continue

                val shinyMember = fleet.fleetData.membersListCopy.find { it.id == shinyAppliedTo } ?: continue
                if(!MagicPaintjobManager.hasPaintjob(shinyMember)) {
                    applyShinyPaintjob(shinyMember, shinyPaintjobSpec)
                }
                continue
            }

            fleet.memoryWithoutUpdate.set("\$shinyAppliedTo", "") // This fleet has been checked

            if(fleet.fleetData.membersListCopy.any { member -> MagicPaintjobManager.getCurrentShipPaintjob(member)?.isShiny == true }) // Already has shiny?
                continue

            val seed = fleet.getSalvageSeed()
            val rng = Random(seed)

            data class ShipEntry(
                val ship: FleetMemberAPI,
                val paintjobs: List<MagicPaintjobSpec>,
                val p: Double // probability this ship produces a shiny
            )

            //
            // Fleet level probability
            //

            val entries = fleet.fleetData.membersListCopy.mapNotNull { ship ->
                if (ship.hullId !in hullsWithShinies) return@mapNotNull null
                if (MagicPaintjobManager.hasPaintjob(ship) && MagicPaintjobManager.getCurrentShipPaintjob(ship)?.isShiny != true) return@mapNotNull null

                val paintjobs = allShinyPaintjobs.filter { ship.hullId in it.hullIds }
                if (paintjobs.isEmpty()) return@mapNotNull null

                // Combine multiple paintjobs on same hull:
                // p = 1 - product(1 - 1/rarity)
                var pNone = 1.0
                for (pj in paintjobs) {
                    val r = pj.isShinyRarity.coerceAtLeast(1)
                    pNone *= (1.0 - 1.0 / r)
                }

                val p = 1.0 - pNone

                ShipEntry(ship, paintjobs, p)
            }

            if (entries.isEmpty()) continue

            var pNoneFleet = 1.0
            for (e in entries) {
                pNoneFleet *= (1.0 - e.p)
            }

            val pFleet = 1.0 - pNoneFleet

            if (rng.nextDouble() >= pFleet) continue

            //
            // Select fleet member proportionally to its contribution
            //

            // Normalize weights by p_i
            val totalP = entries.sumOf { it.p }

            var roll = rng.nextDouble() * totalP

            val chosen = run {
                for (e in entries) {
                    roll -= e.p
                    if (roll <= 0) return@run e
                }
                entries.last()
            }

            //
            // Apply paintjob to fleet member in weighted manner
            //

            //if(MagicPaintjobManager.hasPaintjob(chosen.ship)) // Do not apply if the ship already has a paintjob
            //    continue

            //if(MagicPaintjobManager.getCurrentShipPaintjob(chosen.ship)?.isShiny == true) // Do not apply if the ship already has a shiny paintjob
            //    continue

            val paintjob = pickWeightedPaintjob(chosen.paintjobs, rng)
            applyShinyPaintjob(chosen.ship, paintjob)
            fleet.memoryWithoutUpdate.set("\$shinyAppliedTo", chosen.ship.id)
            fleet.memoryWithoutUpdate.set("\$shinyPaintjob", paintjob.id)
        }
    }

    fun pickWeightedPaintjob(
        paintjobs: List<MagicPaintjobSpec>,
        rng: Random
    ): MagicPaintjobSpec {

        val weights = paintjobs.map { 1.0 / it.isShinyRarity.coerceAtLeast(1) }
        val total = weights.sum()

        var roll = rng.nextDouble() * total

        for ((i, pj) in paintjobs.withIndex()) {
            roll -= weights[i]
            if (roll <= 0) return pj
        }

        return paintjobs.last()
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