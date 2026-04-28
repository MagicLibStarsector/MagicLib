package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getSalvageSeed
import kotlin.random.Random


/*
    There are two systems for assigning shinies:
    - **Per fleet** (roll once for the whole fleet)
    - **Per member** (each ship rolls individually)
    These are controlled by tags.
    ### 1. **Default shiny tag**
    `MagicLib_ShinyPJ_25`
    - Per fleet behavior
    - Uses `_25` as probablity for a fleet to have shinies unless not present, then it uses  `defaultFleetProbability = 25` -> 1 in 25 fleets get shinies.
    - If probabilities with different values than`_25` exist, the probability is medianed between all of them.
    - If triggered, a random ship with a shiny paintjob will be given that paintjob
    - Lower numbers are more likely to be picked
    ### 3. **Member tag**
    `MagicLib_ShinyPJ_Member_25`
    - Per member behavior
    - Each ship individually rolls for shiny status based on the weight, which is `_25` or if not present `defaultMemberProbability = 25` 1 in 25 members get shinies
    - No fleet-wide roll involved
    - Lower numbers are more likely to be picked
    ### 4. Weight
    MagicLib_ShinyPJ_Weight_30
    - When picking between multiple paintjobs
    - In per fleet behavior when picking between multiple possible paintjobs across the fleet.
    - In per member behavior when picking between multiple possible paintjobs for the member.
    - Uses `_30` as chance, if not specified uses `defaultPaintjobWeight = 30` for the weight of the paintjob.
    - Lower numbers are more likely to be picked
    ## Priority Logic
    If both systems exist in a fleet:
    1. Fleet-level roll happens first
    2. If no shinies are produced -? fallback to per-member checks
 */

class MagicPaintjobShinyAdder : EveryFrameScript {
    companion object {
        @JvmStatic
        var defaultFleetProbability = 25 // 1 in 25 chance of spawning a shiny
        var defaultMemberProbability = 25
        var defaultPaintjobWeight = 30 // If randomly picking between multiple shiny paintjobs, this is the weight for how likely each paintjob is to be chosen.
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

    data class ShipEntry(
        val ship: FleetMemberAPI,
        val fleetPaintjobs: List<MagicPaintjobSpec>,
        val memberPaintjobs: List<MagicPaintjobSpec>
    )

    private fun computeCombinedWeight(paintjobs: List<MagicPaintjobSpec>): Double {
        var pNone = 1.0
        for (pj in paintjobs) {
            val w = 1.0 / pj.shinyWeight.coerceAtLeast(1)
            pNone *= (1.0 - w)
        }
        return 1.0 - pNone
    }

    private fun pickWeightedPaintjob(
        paintjobs: List<MagicPaintjobSpec>,
        rng: Random
    ): MagicPaintjobSpec {

        val totalWeight = paintjobs.sumOf {
            1.0 / it.shinyWeight.coerceAtLeast(1)
        }

        var roll = rng.nextDouble() * totalWeight

        for (pj in paintjobs) {
            roll -= (1.0 / pj.shinyWeight.coerceAtLeast(1))
            if (roll <= 0) return pj
        }

        return paintjobs.random(rng) // safe fallback, no bias
    }

    private fun applyAndStore(
        fleet: CampaignFleetAPI,
        ship: FleetMemberAPI,
        paintjob: MagicPaintjobSpec
    ) {
        applyShinyPaintjob(ship, paintjob)
        fleet.memoryWithoutUpdate.set("\$shinyAppliedTo", ship.id)
        fleet.memoryWithoutUpdate.set("\$shinyPaintjob", paintjob.id)
    }

    private fun MemoryAPI.getStringOrNull(key: String): String? =
        if (contains(key)) get(key) as? String else null

    private fun List<Int>.median(): Double {
        if (isEmpty()) return 0.0
        val sortedList = sorted().map { it.toDouble() }
        val size = sortedList.size
        return if (size % 2 == 0) {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2.0
        } else {
            sortedList[size / 2]
        }
    }

    fun checkAndApplyShiniesToAllFleetsInPlayerLocation(
        allShinyPaintjobs: List<MagicPaintjobSpec> = MagicPaintjobManager
            .getPaintjobs(includeShiny = true)
            .filter { it.isShiny }
    ) {
        if (!MagicPaintjobManager.isEnabled) return

        val location = Global.getSector().playerFleet.containingLocation ?: return

        // Pre-group by hull for performance
        val paintjobsByHull = allShinyPaintjobs
            .flatMap { pj -> pj.hullIds.map { it to pj } }
            .groupBy({ it.first }, { it.second })

        for (fleet in location.fleets) {
            if (fleet == null || fleet.isPlayerFleet) continue

            val memory = fleet.memoryWithoutUpdate ?: continue

            // Reapply existing shiny
            val appliedId = memory.getStringOrNull("\$shinyAppliedTo")
            if (appliedId != null) {
                if (appliedId.isEmpty()) continue

                val pjId = memory.getStringOrNull("\$shinyPaintjob") ?: continue
                val pj = MagicPaintjobManager.getPaintjob(pjId) ?: continue

                val member = fleet.fleetData.membersListCopy.find { it.id == appliedId } ?: continue
                if (!MagicPaintjobManager.hasPaintjob(member)) {
                    applyShinyPaintjob(member, pj)
                }
                continue
            }

            memory.set("\$shinyAppliedTo", "")

            val members = fleet.fleetData.membersListCopy
            if (members.isEmpty()) continue

            if (members.any {
                    MagicPaintjobManager.getCurrentShipPaintjob(it)?.isShiny == true
                }) continue

            val rng = Random(fleet.getSalvageSeed())

            // Build entries
            val entries = members.mapNotNull { ship ->
                val pjs = paintjobsByHull[ship.hullId] ?: return@mapNotNull null

                val fleetPjs = pjs.filter { it.hasShinyFleetTag }
                val memberPjs = pjs.filter { it.hasShinyMemberTag }

                ShipEntry(ship, fleetPjs, memberPjs)
            }

            if (entries.isEmpty()) continue

            val hasFleet = entries.any { it.fleetPaintjobs.isNotEmpty() }
            val hasMember = entries.any { it.memberPaintjobs.isNotEmpty() }

            // PER FLEET
            if (hasFleet) {
                val fleetProbabilities = entries
                    .flatMap { it.fleetPaintjobs }
                    .map { it.shinyFleetRarity }

                val avg = fleetProbabilities.ifEmpty { listOf(defaultFleetProbability) }.average()
                val pFleet = 1.0 / avg.coerceAtLeast(1)

                if (rng.nextDouble() < pFleet) {
                    val candidates = entries.filter { it.fleetPaintjobs.isNotEmpty() }

                    val weightedShips = candidates.mapNotNull { entry ->
                        // Use best paintjob = lowest weight = highest probability
                        val bestWeight = entry.fleetPaintjobs.minOf {
                            it.shinyWeight.coerceAtLeast(1)
                        }

                        val p = 1.0 / bestWeight
                        if (p > 0) entry to p else null
                    }

                    if (weightedShips.isEmpty()) continue

                    val total = weightedShips.sumOf { it.second }
                    var roll = rng.nextDouble() * total

                    val chosenEntry = weightedShips.firstOrNull {
                        roll -= it.second
                        roll <= 0
                    }?.first ?: weightedShips.last().first

                    val paintjob = pickWeightedPaintjob(
                        chosenEntry.fleetPaintjobs,
                        rng
                    )

                    applyAndStore(fleet, chosenEntry.ship, paintjob)
                    continue
                }
            }

            // PER MEMBER
            if (hasMember) {
                for (entry in entries) {
                    if (entry.memberPaintjobs.isEmpty()) continue

                    val probs = entry.memberPaintjobs.map {
                        it.shinyMemberRarity
                    }

                    val avg = probs.ifEmpty { listOf(defaultMemberProbability) }.min()
                    val p = 1.0 / avg.coerceAtLeast(1)

                    if (rng.nextDouble() < p) {
                        val paintjob = pickWeightedPaintjob(entry.memberPaintjobs, rng)
                        applyAndStore(fleet, entry.ship, paintjob)
                        break // only one shiny per fleet
                    }
                }
            }
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