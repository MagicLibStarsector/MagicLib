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


class MagicPaintjobShinyAdder : EveryFrameScript {
    companion object {
        @Deprecated("Functionality moved to work on a per mod basis, setting this no longer does anything. See shiny_settings.json to change probability of your mod's shiny paintjobs.")
        @JvmStatic
        var probability = 25

        private val defaultProbability = 25 // 1 in 25 chance of spawning a shiny

        const val SHINY_APPLIED_FLEET_KEY = "\$ML_shinyAppliedTo"
    }

    private lateinit var defaultProbabilityForModID: Map<String, Int>

    private var isDoneInternal = false
    override fun isDone() = isDoneInternal
    override fun runWhilePaused() = false

    private val interval = IntervalUtil(2f, 3f)

    override fun advance(amount: Float) {
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        if (!MagicPaintjobManager.isEnabled) return

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
        val paintjobs: List<MagicPaintjobSpec>
    )

    data class ShinyAppliedTo(
        val paintjobID: String,
        val memberID: String
    )

    private fun MemoryAPI.getShinyAppliedTo(): ShinyAppliedTo? {
        if (!contains(SHINY_APPLIED_FLEET_KEY)) return null

        val value = get(SHINY_APPLIED_FLEET_KEY)

        return when (value) {
            is List<*> -> {
                val paintjobID = value.getOrNull(0) as? String
                val memberID = value.getOrNull(1) as? String
                if (paintjobID != null && memberID != null)
                    ShinyAppliedTo(paintjobID, memberID)
                else null
            }
            is ShinyAppliedTo -> value // This is for backwards compatibility. This can be freely removed on version 0.98.5a and greater.
            else -> null
        }
    }

    private fun MemoryAPI.setShinyAppliedTo(paintjobID: String, memberID: String) {
        set(SHINY_APPLIED_FLEET_KEY, listOf(paintjobID, memberID))
    }

    fun checkAndApplyShiniesToAllFleetsInPlayerLocation(
        allShinyPaintjobs: List<MagicPaintjobSpec> =
            MagicPaintjobManager.getPaintjobs(includeShiny = true).filter { it.isShiny }
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

            // Reapply existing shiny if needed
            val applied = memory.getShinyAppliedTo()

            if (applied != null) {
                if (applied.paintjobID.isEmpty() || applied.memberID.isEmpty()) continue

                val pj = MagicPaintjobManager.getPaintjob(applied.paintjobID) ?: continue

                val member = fleet.fleetData.membersListCopy.find { it.id == applied.memberID } ?: continue
                if (!MagicPaintjobManager.hasPaintjob(member)) {
                    applyShinyPaintjob(fleet, member, pj)
                }
                continue
            }

            memory.setShinyAppliedTo("","")

            val members = fleet.fleetData.membersListCopy
            if (members.isEmpty()) continue

            if (members.any {
                    MagicPaintjobManager.getCurrentShipPaintjob(it)?.isShiny == true
                }) continue

            // Build entries
            val entries = members.mapNotNull { ship ->
                val pjs = paintjobsByHull[ship.hullId] ?: return@mapNotNull null

                ShipEntry(ship, pjs)
            }

            if (entries.isEmpty()) continue

            val baseSeed = fleet.getSalvageSeed()
            val modRng = Random(baseSeed xor 0xABCDEF)

            val modsToPaintjobs = entries
                .flatMap { it.paintjobs }
                .groupBy { it.modId }

            if (modsToPaintjobs.isEmpty()) continue

            fun weightFromProbability(prob: Int): Float {
                return if (prob <= 0) 0f else 1f / prob.toFloat()
            }

            setupDefaultProbabilityForMods(allShinyPaintjobs)

            // Build weights
            val modWeights = modsToPaintjobs.keys.associateWith { modId ->
                weightFromProbability(defaultProbabilityForModID[modId] ?: defaultProbability)
            }

            // STEP 1: roll if ANY shiny happens
            val totalWeight = modWeights.values.sum()
            if (totalWeight <= 0f) continue

            if (modRng.nextFloat() >= totalWeight) {
                continue
            }

            // STEP 2: pick WHICH mod
            val chosenMod = weightedPick(modWeights, modRng) ?: continue

            // STEP 3: build ship -> paintjobs map FOR THIS MOD ONLY
            val shipsToPaintjobs = entries
                .mapNotNull { entry ->
                    val valid = entry.paintjobs.filter { it.modId == chosenMod }
                    if (valid.isEmpty()) null else entry.ship to valid
                }

            if (shipsToPaintjobs.isEmpty()) continue

            val pjRng = Random(baseSeed xor 0x123456)

            // STEP 4: pick ship first
            val (chosenShip, shipPaintjobs) = shipsToPaintjobs[pjRng.nextInt(shipsToPaintjobs.size)]

            // STEP 5: pick paintjob from that ship
            val chosenPaintjob = shipPaintjobs[pjRng.nextInt(shipPaintjobs.size)]

            applyShinyPaintjob(fleet, chosenShip, chosenPaintjob)
        }
    }

    private fun setupDefaultProbabilityForMods(allShinyPaintjobs: List<MagicPaintjobSpec>) {
        val settings = Global.getSettings()
        if (!::defaultProbabilityForModID.isInitialized) {
            defaultProbabilityForModID = allShinyPaintjobs
                .asSequence()
                .map { it.modId }
                .distinct()
                .associateWith { modId ->
                    runCatching {
                        settings
                            .loadJSON("data/config/paintjobs/shiny_settings.json", modId)
                            .optInt("probability", defaultProbability)
                    }.getOrDefault(defaultProbability)
                }
        }
    }

    private fun <T> weightedPick(weights: Map<T, Float>, rng: Random): T? {
        val totalWeight = weights.values.sum()
        if (totalWeight <= 0f) return null

        var roll = rng.nextFloat() * totalWeight

        for ((item, weight) in weights) {
            roll -= weight
            if (roll <= 0f) return item
        }

        return weights.keys.firstOrNull()
    }

    private fun applyShinyPaintjob(
        fleet: CampaignFleetAPI,
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

        fleet.memoryWithoutUpdate.setShinyAppliedTo(paintjob.id, ship.id)
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