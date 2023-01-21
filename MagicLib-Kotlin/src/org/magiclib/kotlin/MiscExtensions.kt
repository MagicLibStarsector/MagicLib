package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.events.CampaignEventTarget
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.FleetFilter
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*


val Vector2f.ZERO
    get() = Misc.ZERO

fun String.ucFirst() = Misc.ucFirst(this)
fun String.lcFirst() = Misc.lcFirst(this)

fun String.replaceTokensFromMemory(memoryMap: Map<String, MemoryAPI>) =
    Misc.replaceTokensFromMemory(this, memoryMap)

fun SectorEntityToken.getDistance(to: SectorEntityToken) =
    Misc.getDistance(this, to)

fun SectorEntityToken.getDistanceLY(to: SectorEntityToken) =
    Misc.getDistanceLY(this, to)

fun Vector2f.getDistanceSq(to: Vector2f) =
    Misc.getDistanceSq(this, to)

fun Vector2f.getDistanceToPlayerLY() =
    Misc.getDistanceToPlayerLY(this)

fun SectorEntityToken.getDistanceToPlayerLY() =
    Misc.getDistanceToPlayerLY(this)

fun Vector2f.getDistanceLY(to: Vector2f) =
    Misc.getDistanceLY(this, to)

fun Float.getRounded() =
    Misc.getRounded(this)

fun Float.getRoundedValue() =
    Misc.getRoundedValue(this)

fun Float.getRoundedValueFloat() =
    Misc.getRoundedValueFloat(this)

fun Float.getRoundedValueMaxOneAfterDecimal() =
    Misc.getRoundedValueMaxOneAfterDecimal(this)

fun Float.logOfBase(num: Float) =
    Misc.logOfBase(this, num)

fun Vector2f.getPointAtRadius(r: Float) =
    Misc.getPointAtRadius(this, r)

fun Vector2f.getPointAtRadius(r: Float, random: Random) =
    Misc.getPointAtRadius(this, r, random)

fun Vector2f.getPointWithinRadius(r: Float, random: Random = Misc.random) =
    Misc.getPointWithinRadius(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(r: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(minR: Float, maxR: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, minR, maxR, random)

fun CampaignFleetAPI.getSnapshotFPLost() =
    Misc.getSnapshotFPLost(this)

fun CampaignFleetAPI.getSnapshotMembersLost() =
    Misc.getSnapshotMembersLost(this)

fun CampaignEventTarget.startEvent(eventId: String, params: Any) =
    Misc.startEvent(this, eventId, params)

fun String.getAndJoined(strings: List<String>) =
    Misc.getAndJoined(strings)

fun String.getAndJoined(vararg strings: String) =
    Misc.getAndJoined(*strings)

fun String.getJoined(joiner: String, strings: List<String>) =
    Misc.getJoined(joiner, strings)

fun String.getJoined(joiner: String, vararg strings: String) =
    Misc.getJoined(joiner, *strings)

fun SectorEntityToken.findNearbyFleets(maxRange: Float, filter: FleetFilter) =
    Misc.findNearbyFleets(this, maxRange, filter)

fun StarSystemAPI.getFleetsInOrNearSystem() =
    Misc.getFleetsInOrNearSystem(this)

fun LocationAPI.getMarketsInLocation(factionId: String? = null) =
    if (factionId == null)
        Misc.getMarketsInLocation(this)
    else
        Misc.getMarketsInLocation(this, factionId)

fun LocationAPI.getBiggestMarketInLocation() =
    Misc.getBiggestMarketInLocation(this)

fun FactionAPI.getFactionMarkets(econGroup: String? = null) =
    if (econGroup == null)
        Misc.getFactionMarkets(this)
    else
        Misc.getFactionMarkets(this, econGroup)

fun Vector2f.getNearbyMarkets(distLY: Float) =
    Misc.getNearbyMarkets(this, distLY)

fun FactionAPI.getNumHostileMarkets(from: SectorEntityToken, maxDist: Float) =
    Misc.getNumHostileMarkets(this, from, maxDist)

fun SectorEntityToken.getNearbyStarSystems(maxRangeLY: Float) =
    Misc.getNearbyStarSystems(this, maxRangeLY)

fun SectorEntityToken.getNearbyStarSystem(maxRangeLY: Float) =
    Misc.getNearbyStarSystem(this, maxRangeLY)

fun SectorEntityToken.getNearestStarSystem() =
    Misc.getNearestStarSystem(this)

fun SectorEntityToken.getNearbyStarSystem() =
    Misc.getNearbyStarSystem(this)

fun SectorEntityToken.showRuleDialog(initialTrigger: String) =
    Misc.showRuleDialog(this, initialTrigger)

fun Vector2f.getAngleInDegreesStrict() =
    Misc.getAngleInDegreesStrict(this)

fun Vector2f.getAngleInDegreesStrict(to: Vector2f) =
    Misc.getAngleInDegreesStrict(this, to)

fun Vector2f.getAngleInDegrees() =
    Misc.getAngleInDegrees(this)

fun Vector2f.getAngleInDegrees(to: Vector2f) =
    Misc.getAngleInDegrees(this, to)

fun Vector2f.normalise() =
    Misc.normalise(this)

/**
 * Normalizes an angle given in degrees.
 */
fun Float.normalizeAngle() =
    Misc.normalizeAngle(this)

fun SectorEntityToken.findNearestLocalMarket(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearestLocalMarket(this, maxDist, filter)

fun SectorEntityToken.findNearbyLocalMarkets(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearbyLocalMarkets(this, maxDist, filter)

fun SectorEntityToken.findNearestLocalMarketWithSameFaction(maxDist: Float) =
    Misc.findNearestLocalMarketWithSameFaction(this, maxDist)

fun Vector2f.getUnitVector(to: Vector2f) =
    Misc.getUnitVector(this, to)

/**
 * Called on an angle given in degrees.
 */
fun Float.getUnitVectorAtDegreeAngle() =
    Misc.getUnitVectorAtDegreeAngle(this)

fun Vector2f.rotateAroundOrigin(angle: Float) =
    Misc.rotateAroundOrigin(this, angle)

fun Vector2f.rotateAroundOrigin(angle: Float, origin: Vector2f) =
    Misc.rotateAroundOrigin(this, angle, origin)

/**
 * Angles.
 */
fun Float.isBetween(two: Float, check: Float) =
    Misc.isBetween(this, two, check)

fun CampaignFleetAPI.getShieldedCargoFraction() =
    Misc.getShieldedCargoFraction(this)

fun Color.interpolateColor(to: Color, progress: Float) =
    Misc.interpolateColor(this, to, progress)

fun Vector2f.interpolateVector(to: Vector2f, progress: Float) =
    Misc.interpolateVector(this, to, progress)

fun Float.interpolate(to: Float, progress: Float) =
    Misc.interpolate(this, to, progress)

fun Color.scaleColor(factor: Float) =
    Misc.scaleColor(this, factor)

fun Color.scaleColorOnly(factor: Float) =
    Misc.scaleColorOnly(this, factor)

fun Color.scaleAlpha(factor: Float) =
    Misc.scaleAlpha(this, factor)

fun Color.setAlpha(alpha: Int) =
    Misc.setAlpha(this, alpha)

fun ShipAPI.HullSize.getSizeNum() =
    Misc.getSizeNum(this)

fun MemoryAPI.unsetAll(prefix: String, memKey: String) =
    Misc.unsetAll(prefix, memKey, this)

fun CombatEntityAPI.getTargetingRadius(from: Vector2f, considerShield: Boolean) =
    Misc.getTargetingRadius(from, this, considerShield)

// getClosingSpeed skipped, doesn't make sense to convert.

fun Float.getWithDGS() =
    Misc.getWithDGS(this)

fun Float.getDGSCredits() =
    Misc.getDGSCredits(this)

fun SectorEntityToken.getInterceptPointBasic(to: SectorEntityToken) =
    Misc.getInterceptPointBasic(this, to)

/**
 * A flag can be set to true for several "reasons". As long as it hasn't been set
 * back to false for all of the "reasons", it will remain set to true.
 *
 * For example, a fleet may be hostile because it's responding to comm relay interference,
 * and because the player is running with the transponder off. Until both are resolved,
 * the "hostile" flag will remain set to true.
 *
 * Note: a flag can not be "set" to false. If it's set to false for all the current reasons,
 * the key is removed from memory.
 *
 * Returns whether the flag is still set after this method does its work.
 */
fun MemoryAPI.setFlagWithReason(flagKey: String, reason: String, value: Boolean, expire: Float) =
    Misc.setFlagWithReason(this, flagKey, reason, value, expire)

fun MemoryAPI.flagHasReason(flagKey: String, reason: String) =
    Misc.flagHasReason(this, flagKey, reason)

fun MemoryAPI.clearFlag(flagKey: String) =
    Misc.clearFlag(this, flagKey)

fun CampaignFleetAPI.makeLowRepImpact(reason: String) =
    Misc.makeLowRepImpact(this, reason)

// TODO add HubMissionWithTriggers static methods too
