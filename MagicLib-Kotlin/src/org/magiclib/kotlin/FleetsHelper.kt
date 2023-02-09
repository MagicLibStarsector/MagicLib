package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.Faction
import org.lwjgl.util.vector.Vector2f
import java.util.*

/**
 * @author SafariJohn
 */
// Not ready for primetime yet, I haven't looked it over
private object FleetsHelper {
    enum class Category {
        COMBAT,
        FREIGHTER,
        TANKER,
        TRANSPORT,
        LINER,
        UTILITY
    }

    enum class Weight {
        HIGH,
        MID,
        LOW,
        NONE
    }

    /**
     * Creates a [FleetParamsV3] using the given [Weight]s for different [Category]s.
     */
    fun createWeightedFleetParams(
        random: Random,
        points: Map<Category, Weight>,
        fleetPoints: Float,
        qualityMod: Float,
        qualityOverride: Float?,
        source: MarketAPI?,
        locInHyper: Vector2f?,
        factionId: String?,
        fleetType: String?
    ): FleetParamsV3 {
        val results = FleetParamsV3(
            source,
            locInHyper,
            factionId,
            qualityOverride,
            fleetType,
            getWeight(points[Category.COMBAT], random),
            getWeight(points[Category.FREIGHTER], random),
            getWeight(points[Category.TANKER], random),
            getWeight(points[Category.TRANSPORT], random),
            getWeight(points[Category.LINER], random),
            getWeight(points[Category.UTILITY], random),
            qualityMod
        )
        convertFpToPercent(results, fleetPoints)
        return results
    }

    /**
     * Creates a fleet with multiple factions included.
     */
    fun createMultifactionFleet(
        params: FleetParamsV3,
        primaryFaction: Faction,
        vararg otherFactions: Faction
    ): CampaignFleetAPI {
        val factionsSet = HashSet<Faction>()
        factionsSet.add(primaryFaction)
        factionsSet.addAll(otherFactions.distinctBy { it.id })

        val allParams = splitParams(params, primaryFaction.id, factionsSet.map { it.id }.toSet())
        val fleet = FleetFactoryV3.createFleet(allParams[0])
        for (p in allParams) {
            if (p.factionId == primaryFaction.id) continue

            val tempFleet = FleetFactoryV3.createFleet(p)

            if (tempFleet != null && !tempFleet.isEmpty) {
                for (member in tempFleet.membersWithFightersCopy) {
                    if (member.isFighterWing) continue
                    member.captain = null
                    if (member.isFlagship) member.isFlagship = false
                    fleet.fleetData.addFleetMember(member)
                }
            }
        }
        FleetFactoryV3.addCommanderAndOfficers(fleet, params, params.random ?: Misc.random ?: Random())
        fleet.fleetData.sort()
//        Roider_Misc.sortFleetByShipSize(fleet)
        return fleet
    }

    private fun getWeight(weight: Weight?, random: Random): Float {
        if (weight == null) return 0f
        return when (weight) {
            Weight.HIGH -> 40f + 20f * random.nextFloat()
            Weight.MID -> 20f + 10f * random.nextFloat()
            Weight.LOW -> 5f + 10f * random.nextFloat()
            Weight.NONE -> 0f
        }
    }

    /**
     * Multiplies each category's points by targetFP and flattens it so the total FP matches targetFP.
     */
    private fun convertFpToPercent(
        params: FleetParamsV3,
        targetFP: Float
    ) {
        val divisor = params.combatPts + params.freighterPts + params.tankerPts
        +params.transportPts + params.linerPts + params.utilityPts
        params.combatPts *= targetFP / divisor
        params.freighterPts *= targetFP / divisor
        params.tankerPts *= targetFP / divisor
        params.transportPts *= targetFP / divisor
        params.linerPts *= targetFP / divisor
        params.utilityPts *= targetFP / divisor
    }

    private fun splitParams(params: FleetParamsV3, primaryFaction: String, factions: Set<String>): List<FleetParamsV3> {
        val results = ArrayList<FleetParamsV3>()
        val divisor = factions.size.toFloat() + 1
        val primary = FleetParamsV3(
            params.source,
            params.locInHyper,
            primaryFaction,
            params.qualityOverride,
            params.fleetType,
            params.combatPts / divisor,
            params.freighterPts / divisor,
            params.tankerPts / divisor,
            params.transportPts / divisor,
            params.linerPts / divisor,
            params.utilityPts / divisor,
            params.qualityMod
        )
        copyParamsExtra(params, primary, true, divisor)
        results.add(primary)

        for (faction in factions) {
            if (faction.isEmpty() || faction == primaryFaction) continue

            val secondary = FleetParamsV3(
                params.source,
                params.locInHyper,
                faction,
                params.qualityOverride,
                params.fleetType,
                params.combatPts / divisor,
                params.freighterPts / divisor,
                params.tankerPts / divisor,
                params.transportPts / divisor,
                params.linerPts / divisor,
                params.utilityPts / divisor,
                params.qualityMod
            )
            copyParamsExtra(params, primary, false, divisor)
            results.add(secondary)
        }

        return results
    }

    private fun copyParamsExtra(
        sourceParams: FleetParamsV3,
        destParams: FleetParamsV3,
        isPrimary: Boolean,
        divisor: Float
    ) {
        destParams.fleetType = sourceParams.fleetType ?: null
        destParams.aiCores = sourceParams.aiCores ?: null
        destParams.allWeapons = sourceParams.allWeapons ?: null
        destParams.averageSMods = sourceParams.averageSMods ?: null
        destParams.doNotAddShipsBeforePruning = sourceParams.doNotAddShipsBeforePruning ?: null
        destParams.doNotPrune = sourceParams.doNotPrune ?: null
        destParams.doctrineOverride = sourceParams.doctrineOverride ?: null
        destParams.doNotIntegrateAICores = sourceParams.doNotIntegrateAICores
        destParams.flagshipVariant = sourceParams.flagshipVariant ?: null
        destParams.flagshipVariantId = sourceParams.flagshipVariantId ?: null
        destParams.forceAllowPhaseShipsEtc = sourceParams.forceAllowPhaseShipsEtc ?: null
        destParams.ignoreMarketFleetSizeMult = sourceParams.ignoreMarketFleetSizeMult ?: null
        destParams.modeOverride = sourceParams.modeOverride ?: null
        destParams.maxNumShips =
            if (sourceParams.maxNumShips != null)
                sourceParams.maxNumShips / divisor.toInt()
            else null
        destParams.maxShipSize = sourceParams.maxShipSize
        destParams.minShipSize = sourceParams.minShipSize
        destParams.officerLevelBonus = sourceParams.officerLevelBonus
        destParams.officerLevelLimit = sourceParams.officerLevelLimit
        destParams.officerNumberBonus = sourceParams.officerNumberBonus
        destParams.officerNumberMult = sourceParams.officerNumberMult
        destParams.onlyApplyFleetSizeToCombatShips = sourceParams.onlyApplyFleetSizeToCombatShips ?: null
        destParams.onlyRetainFlagship = sourceParams.onlyRetainFlagship ?: null
        destParams.random = sourceParams.random ?: null
        destParams.timestamp = sourceParams.timestamp ?: null
        destParams.treatCombatFreighterSettingAsFraction = sourceParams.treatCombatFreighterSettingAsFraction ?: null
        destParams.withOfficers = sourceParams.withOfficers

        if (isPrimary) {
            destParams.commander = sourceParams.commander ?: null
            destParams.commanderLevelLimit = sourceParams.commanderLevelLimit
            destParams.noCommanderSkills = sourceParams.noCommanderSkills ?: null
        }
    }

}