@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.events.CampaignEventTarget
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.*
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidSource
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.FleetFilter
import org.json.JSONObject
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import java.nio.Buffer
import java.util.*


/**
 * @since 0.46.0
 */
inline val Vector2f.ZERO
    get() = Misc.ZERO

/**
 * @since 0.46.0
 */
inline fun String.ucFirst() = Misc.ucFirst(this)

/**
 * @since 0.46.0
 */
inline fun String.lcFirst() = Misc.lcFirst(this)

/**
 * @since 0.46.0
 */
inline fun String.replaceTokensFromMemory(memoryMap: Map<String, MemoryAPI>) =
    Misc.replaceTokensFromMemory(this, memoryMap)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getDistance(to: SectorEntityToken) = Misc.getDistance(this, to)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getDistanceLY(to: SectorEntityToken) = Misc.getDistanceLY(this, to)

/**
 * @since 1.3.0
 */
inline fun Vector2f.getDistance(to: Vector2f) = Misc.getDistance(this, to)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getDistanceSq(to: Vector2f) = Misc.getDistanceSq(this, to)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getDistanceLY(to: Vector2f) = Misc.getDistanceLY(this, to)

/**
 * @since 0.46.0
 */
inline fun Float.getRounded() = Misc.getRounded(this)

/**
 * @since 0.46.0
 */
inline fun Float.getRoundedValue() = Misc.getRoundedValue(this)

/**
 * @since 0.46.0
 */
inline fun Float.getRoundedValueFloat() = Misc.getRoundedValueFloat(this)

/**
 * @since 0.46.0
 */
inline fun Float.getRoundedValueMaxOneAfterDecimal() =
    Misc.getRoundedValueMaxOneAfterDecimal(this)

/**
 * @since 0.46.0
 */
inline fun Float.logOfBase(num: Float) = Misc.logOfBase(this, num)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPointAtRadius(r: Float) = Misc.getPointAtRadius(this, r)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPointAtRadius(r: Float, random: Random) =
    Misc.getPointAtRadius(this, r, random)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPointWithinRadius(r: Float, random: Random = Misc.random) =
    Misc.getPointWithinRadius(this, r, random)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPointWithinRadiusUniform(r: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, r, random)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPointWithinRadiusUniform(minR: Float, maxR: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, minR, maxR, random)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getSnapshotFPLost() = Misc.getSnapshotFPLost(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getSnapshotMembersLost() = Misc.getSnapshotMembersLost(this)

/**
 * @since 0.46.0
 */
inline fun CampaignEventTarget.startEvent(eventId: String, params: Any) =
    Misc.startEvent(this, eventId, params)

/**
 * @since 0.46.0
 */
inline fun String.getAndJoined(strings: List<String>) = Misc.getAndJoined(strings)

/**
 * @since 0.46.0
 */
inline fun String.getAndJoined(vararg strings: String) = Misc.getAndJoined(*strings)

/**
 * @since 0.46.0
 */
inline fun String.getJoined(joiner: String, strings: List<String>) =
    Misc.getJoined(joiner, strings)

/**
 * @since 0.46.0
 */
inline fun String.getJoined(joiner: String, vararg strings: String) =
    Misc.getJoined(joiner, *strings)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearbyFleets(maxRange: Float, filter: FleetFilter) =
    Misc.findNearbyFleets(this, maxRange, filter)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.getFleetsInOrNearSystem() = Misc.getFleetsInOrNearSystem(this)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.getMarketsInLocation(factionId: String? = null) =
    if (factionId == null)
        Misc.getMarketsInLocation(this)
    else
        Misc.getMarketsInLocation(this, factionId)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.getBiggestMarketInLocation() = Misc.getBiggestMarketInLocation(this)

/**
 * @since 0.46.0
 */
inline fun FactionAPI.getFactionMarkets(econGroup: String? = null) =
    if (econGroup == null)
        Misc.getFactionMarkets(this)
    else
        Misc.getFactionMarkets(this, econGroup)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getNearbyMarkets(distLY: Float) = Misc.getNearbyMarkets(this, distLY)

/**
 * @since 0.46.0
 */
inline fun FactionAPI.getNumHostileMarkets(from: SectorEntityToken, maxDist: Float) =
    Misc.getNumHostileMarkets(this, from, maxDist)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNearbyStarSystems(maxRangeLY: Float) =
    Misc.getNearbyStarSystems(this, maxRangeLY)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNearbyStarSystem(maxRangeLY: Float) =
    Misc.getNearbyStarSystem(this, maxRangeLY)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNearestStarSystem() = Misc.getNearestStarSystem(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNearbyStarSystem() = Misc.getNearbyStarSystem(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.showRuleDialog(initialTrigger: String) =
    Misc.showRuleDialog(this, initialTrigger)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getAngleInDegreesStrict() = Misc.getAngleInDegreesStrict(this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getAngleInDegreesStrict(to: Vector2f) = Misc.getAngleInDegreesStrict(this, to)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getAngleInDegrees() = Misc.getAngleInDegrees(this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getAngleInDegrees(to: Vector2f) = Misc.getAngleInDegrees(this, to)

/**
 * @since 0.46.0
 */
inline fun Vector2f.normalise() = Misc.normalise(this)

/**
 * MagicLib: Normalizes an angle given in degrees.
 */
inline fun Float.normalizeAngle() = Misc.normalizeAngle(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestLocalMarket(maxDist: Float, filter: BaseEventPlugin.MarketFilter? = null) =
    Misc.findNearestLocalMarket(this, maxDist, filter)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearbyLocalMarkets(maxDist: Float, filter: BaseEventPlugin.MarketFilter? = null) =
    Misc.findNearbyLocalMarkets(this, maxDist, filter)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestLocalMarketWithSameFaction(maxDist: Float) =
    Misc.findNearestLocalMarketWithSameFaction(this, maxDist)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getUnitVector(to: Vector2f) = Misc.getUnitVector(this, to)

/**
 * MagicLib: Called on an angle given in degrees.
 * @since 0.46.0
 */
inline fun Float.getUnitVectorAtDegreeAngle() = Misc.getUnitVectorAtDegreeAngle(this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.rotateAroundOrigin(angle: Float) = Misc.rotateAroundOrigin(this, angle)

/**
 * @since 0.46.0
 */
inline fun Vector2f.rotateAroundOrigin(angle: Float, origin: Vector2f) =
    Misc.rotateAroundOrigin(this, angle, origin)

/**
 * Angles.
 * @since 0.46.0
 */
inline fun Float.isBetween(two: Float, check: Float) = Misc.isBetween(this, two, check)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getShieldedCargoFraction() = Misc.getShieldedCargoFraction(this)

/**
 * @since 0.46.0
 */
inline fun Color.interpolateColor(to: Color, progress: Float) = Misc.interpolateColor(this, to, progress)

/**
 * @since 0.46.0
 */
inline fun Vector2f.interpolateVector(to: Vector2f, progress: Float) = Misc.interpolateVector(this, to, progress)

/**
 * @since 0.46.0
 */
inline fun Float.interpolate(to: Float, progress: Float) = Misc.interpolate(this, to, progress)

/**
 * @since 0.46.0
 */
inline fun Color.scaleColor(factor: Float) = Misc.scaleColor(this, factor)

/**
 * @since 0.46.0
 */
inline fun Color.scaleColorOnly(factor: Float) = Misc.scaleColorOnly(this, factor)

/**
 * @since 0.46.0
 */
inline fun Color.scaleAlpha(factor: Float) = Misc.scaleAlpha(this, factor)

/**
 * @since 0.46.0
 */
inline fun Color.setAlpha(alpha: Int) = Misc.setAlpha(this, alpha)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.HullSize.getSizeNum() = Misc.getSizeNum(this)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.unsetAll(prefix: String, memKey: String) = Misc.unsetAll(prefix, memKey, this)

/**
 * @since 0.46.0
 */
inline fun CombatEntityAPI.getTargetingRadius(from: Vector2f, considerShield: Boolean) =
    Misc.getTargetingRadius(from, this, considerShield)

// getClosingSpeed skipped, doesn't make sense to convert.

/**
 * @since 0.46.0
 */
inline fun Float.getWithDGS() = Misc.getWithDGS(this)

/**
 * @since 0.46.0
 */
inline fun Float.getDGSCredits() = Misc.getDGSCredits(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getInterceptPointBasic(to: SectorEntityToken) =
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
 * @since 0.46.0
 */
inline fun MemoryAPI.setFlagWithReason(flagKey: String, reason: String, value: Boolean, expire: Float) =
    Misc.setFlagWithReason(this, flagKey, reason, value, expire)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.flagHasReason(flagKey: String, reason: String) = Misc.flagHasReason(this, flagKey, reason)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.clearFlag(flagKey: String) = Misc.clearFlag(this, flagKey)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.makeLowRepImpact(reason: String) = Misc.makeLowRepImpact(this, reason)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.makeNoRepImpact(reason: String) = Misc.makeNoRepImpact(this, reason)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.makeHostile() = Misc.makeHostile(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.makeNotLowRepImpact(reason: String) = Misc.makeNotLowRepImpact(this, reason)

/**
 * @since 0.46.0
 */
inline fun Long.getAgoStringForTimestamp() = Misc.getAgoStringForTimestamp(this)

/**
 * @since 0.46.0
 */
inline fun Long.getDetailedAgoString() = Misc.getDetailedAgoString(this)

/**
 * @since 0.46.0
 */
inline fun Int.getAtLeastStringForDays() = Misc.getAtLeastStringForDays(this)

/**
 * @since 0.46.0
 */
inline fun Int.getStringForDays() = Misc.getStringForDays(this)

/**
 * @since 0.46.0
 */
inline fun Float.getBurnLevelForSpeed() = Misc.getBurnLevelForSpeed(this)

/**
 * @since 0.46.0
 */
inline fun Float.getFractionalBurnLevelForSpeed() = Misc.getFractionalBurnLevelForSpeed(this)

/**
 * @since 0.46.0
 */
inline fun Float.getSpeedForBurnLevel() = Misc.getSpeedForBurnLevel(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getFuelPerDay(burnLevel: Float) = Misc.getFuelPerDay(this, burnLevel)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getFuelPerDayAtSpeed(speed: Float) = Misc.getFuelPerDayAtSpeed(this, speed)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getLYPerDayAtBurn(burnLevel: Float) = Misc.getLYPerDayAtBurn(this, burnLevel)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getLYPerDayAtSpeed(speed: Float) = Misc.getLYPerDayAtSpeed(this, speed)

/**
 * @since 0.46.0
 */
inline fun Vector3f.getDistance(other: Vector3f) = Misc.getDistance(this, other)

/**
 * @since 0.46.0
 */
inline fun Float.getAngleDiff(to: Float) = Misc.getAngleDiff(this, to)

// Skipping isInArc, doesn't seem to make sense for an extension.
// Skipping isInArc, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun LocationAPI.addNebulaFromPNG(
    image: String,
    centerX: Float,
    centerY: Float,
    category: String,
    key: String,
    tilesWide: Int,
    tilesHigh: Int,
    terrainType: String = Terrain.NEBULA,
    age: StarAge
) = Misc.addNebulaFromPNG(image, centerX, centerY, this, category, key, tilesWide, tilesHigh, terrainType, age)

// Skipping renderQuad, doesn't seem to make sense for an extension.

/**
 * Shortest distance from line to a point.
 */
inline fun Vector2f.distanceFromLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.distanceFromLineToPoint(lineStart, lineEnd, this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.closestPointOnLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnLineToPoint(lineStart, lineEnd, this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.closestPointOnSegmentToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnSegmentToPoint(lineStart, lineEnd, this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.isPointInBounds(bounds: List<Vector2f>) = Misc.isPointInBounds(this, bounds)

// Skipping intersectSegments, doesn't seem to make sense for an extension.
// Skipping intersectLines, doesn't seem to make sense for an extension.
// Skipping intersectSegmentAndCircle, doesn't seem to make sense for an extension.
// Skipping areSegmentsCoincident, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun Vector2f.getPerp() = Misc.getPerp(this)

// Skipping getClosestTurnDirection, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun Vector2f.getClosestTurnDirection(other: Vector2f) = Misc.getClosestTurnDirection(this, other)

/**
 * @since 0.46.0
 */
inline fun Vector2f.getDiff(other: Vector2f) = Misc.getDiff(this, other)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getSourceMarket() = Misc.getSourceMarket(this)

// Skipping getSpawnChanceMult, doesn't seem to make sense for an extension.
// Skipping pickHyperLocationNotNearPlayer, doesn't seem to make sense for an extension.
// Skipping pickLocationNotNearPlayer, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun Vector2f.wiggle(max: Float) = Misc.wiggle(this, max)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isPlayerOrCombinedPlayerPrimary() = Misc.isPlayerOrCombinedPlayerPrimary(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isPlayerOrCombinedContainingPlayer() = Misc.isPlayerOrCombinedContainingPlayer(this)

/**
 * MagicLib: Call on an asteroid.
 */
inline fun SectorEntityToken.getAsteroidSource() = Misc.getAsteroidSource(this)

/**
 * MagicLib: Call on an asteroid.
 */
inline fun SectorEntityToken.setAsteroidSource(source: AsteroidSource?) =
    Misc.setAsteroidSource(this, source)

/**
 * MagicLib: Call on an asteroid.
 */
inline fun SectorEntityToken.clearAsteroidSource() = Misc.clearAsteroidSource(this)

/**
 * MagicLib: Call on a star.
 */
inline fun PlanetAPI.getCoronaFor() = Misc.getCoronaFor(this)

/**
 * MagicLib: Call on a star.
 */
inline fun PlanetAPI.getPulsarFor() = Misc.getPulsarFor(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI?.hasPulsar() = Misc.hasPulsar(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.caresAboutPlayerTransponder() =
    Misc.caresAboutPlayerTransponder(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI?.findClosestShipEnemyOf(
    locFromForSorting: Vector2f,
    smallestToNote: ShipAPI.HullSize,
    maxRange: Float,
    considerShipRadius: Boolean,
    filter: Misc.FindShipFilter? = null
) = Misc.findClosestShipEnemyOf(this, locFromForSorting, smallestToNote, maxRange, considerShipRadius, filter)

/**
 * @since 0.46.0
 */
inline fun <T : Enum<T>> JSONObject.mapToEnum(
    key: String,
    enumType: Class<T>,
    defaultOption: T,
    required: Boolean = true
) =
    Misc.mapToEnum(this, key, enumType, defaultOption, required)

/**
 * @since 0.46.0
 */
inline fun JSONObject.getColor(key: String) = Misc.getColor(this, key)

/**
 * @since 0.46.0
 */
inline fun JSONObject.optColor(key: String, defaultValue: Color?) = Misc.optColor(this, key, defaultValue)

// Skipping normalizeNoise, doesn't seem to make sense for an extension.
// Skipping initNoise, doesn't seem to make sense for an extension.
// Skipping genFractalNoise, doesn't seem to make sense for an extension.
// Skipping fill, doesn't seem to make sense for an extension.
// Skipping computeAngleSpan, doesn't seem to make sense for an extension.
// Skipping computeAngleRadius, doesn't seem to make sense for an extension.
// Skipping approach, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun Buffer.cleanBuffer() = Misc.cleanBuffer(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getFleetwideTotalStat(dynamicMemberStatId: String) =
    Misc.getFleetwideTotalStat(this, dynamicMemberStatId)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getFleetwideTotalMod(dynamicMemberStatId: String, base: Float, ship: ShipAPI? = null) =
    Misc.getFleetwideTotalMod(this, dynamicMemberStatId, base, ship)

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.getStarId() = Misc.getStarId(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.getMinSystemSurveyLevel() = Misc.getMinSystemSurveyLevel(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.hasAnySurveyDataFor() = Misc.hasAnySurveyDataFor(this)

// Skipping setAllPlanetsKnown, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.setAllPlanetsKnown() = Misc.setAllPlanetsKnown(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.setAllPlanetsSurveyed(setRuinsExplored: Boolean) =
    Misc.setAllPlanetsSurveyed(this, setRuinsExplored)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.generatePlanetConditions(age: StarAge) = Misc.generatePlanetConditions(this, age)

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.getEstimatedOrbitIndex() = Misc.getEstimatedOrbitIndex(this)

// Skipping getRandom, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.addSurveyDataFor(text: TextPanelAPI) = Misc.addSurveyDataFor(this, text)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.setFullySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setFullySurveyed(this, text, withNotification)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.setPreliminarySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setPreliminarySurveyed(this, text, withNotification)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.setSeen(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setSeen(this, text, withNotification)

/**
 * @since 0.46.0
 */
inline fun String.getStringWithTokenReplacement(entity: SectorEntityToken, memoryMap: Map<String, MemoryAPI>?) =
    Misc.getStringWithTokenReplacement(this, entity, memoryMap)

// Skipping renderQuadAlpha, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.fadeAndExpire(seconds: Float = 1f) =
    Misc.fadeAndExpire(this, seconds)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.fadeInOutAndExpire(`in`: Float, dur: Float, out: Float) =
    Misc.fadeInOutAndExpire(this, `in`, dur, out)

// Skipping addCargoPods, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun LocationAPI.addDebrisField(params: DebrisFieldTerrainPlugin.DebrisFieldParams, random: Random?) =
    Misc.addDebrisField(this, params, random)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.isUnboardable() =
    Misc.isUnboardable(this)

/**
 * @since 0.46.0
 */
inline fun ShipHullSpecAPI.isUnboardable() =
    Misc.isUnboardable(this)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.isShipRecoverable(
    recoverer: CampaignFleetAPI?,
    own: Boolean,
    useOfficerRecovery: Boolean,
    chanceMult: Float
) = Misc.isShipRecoverable(this, recoverer, own, useOfficerRecovery, chanceMult)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestJumpPointTo() =
    Misc.findNearestJumpPointTo(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestJumpPointThatCouldBeExitedFrom() =
    Misc.findNearestJumpPointThatCouldBeExitedFrom(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestPlanetTo(requireGasGiant: Boolean, allowStars: Boolean) =
    Misc.findNearestPlanetTo(this, requireGasGiant, allowStars)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.shouldConvertFromStub(location: Vector2f) =
    Misc.shouldConvertFromStub(this, location)

/**
 * @since 0.46.0
 */
inline fun List<Color>.colorsToString() = Misc.colorsToString(this)

/**
 * @since 0.46.0
 */
inline fun String.colorsFromString() = Misc.colorsFromString(this)

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.getJumpPointTo() = Misc.getJumpPointTo(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.findNearestJumpPoint() = Misc.findNearestJumpPoint(this)

/**
 * @since 0.46.0
 */
inline fun ShipHullSpecAPI.getDHullId() = Misc.getDHullId(this)

// Skipping getMod, doesn't seem to make sense for an extension.
// Skipping getDistanceFromArc, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.initConditionMarket() = Misc.initConditionMarket(this)

/**
 * @since 0.46.0
 */
inline fun PlanetAPI.initEconomyMarket() = Misc.initEconomyMarket(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.SurveyLevel.getSurveyLevelString(withBrackets: Boolean) =
    Misc.getSurveyLevelString(this, withBrackets)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setDefenderOverride(override: DefenderDataOverride) =
    Misc.setDefenderOverride(this, override)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setSalvageSpecial(data: Any?) = Misc.setSalvageSpecial(this, data)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setPrevSalvageSpecial(data: Any?) = Misc.setPrevSalvageSpecial(this, data)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getSalvageSpecial() = Misc.getSalvageSpecial(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getPrevSalvageSpecial() = Misc.getPrevSalvageSpecial(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getSystemsInRange(exclude: Set<StarSystemAPI>, nonEmpty: Boolean, maxRange: Float) =
    Misc.getSystemsInRange(this, exclude, nonEmpty, maxRange)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.getPulsarInSystem() = Misc.getPulsarInSystem(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.systemHasPlanets() = Misc.systemHasPlanets(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.HullSize.getCampaignShipScaleMult() = Misc.getCampaignShipScaleMult(this)

// Skipping createStringPicker, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setWarningBeaconGlowColor(color: Color) = Misc.setWarningBeaconGlowColor(this, color)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setWarningBeaconPingColor(color: Color) = Misc.setWarningBeaconPingColor(this, color)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setWarningBeaconColors(color: Color, ping: Color) =
    Misc.setWarningBeaconColors(this, color, ping)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNearbyFleets(maxDist: Float) = Misc.getNearbyFleets(this, maxDist)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getVisibleFleets(includeSensorContacts: Boolean) =
    Misc.getVisibleFleets(this, includeSensorContacts)

/**
 * @since 0.46.0
 */
inline fun CargoAPI.isSameCargo(other: CargoAPI) = Misc.isSameCargo(this, other)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.getDistressJumpPoint() = Misc.getDistressJumpPoint(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.clearTarget(forgetTransponder: Boolean) = Misc.clearTarget(this, forgetTransponder)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.giveStandardReturnToSourceAssignments(withClear: Boolean = true) =
    Misc.giveStandardReturnToSourceAssignments(this, withClear)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.giveStandardReturnAssignments(where: SectorEntityToken, text: String, withClear: Boolean) =
    Misc.giveStandardReturnAssignments(this, where, text, withClear)

/**
 * MagicLib: Adjust the player's reputation with the specified faction and/or person.
 *
 * MagicLib: A non-0 `repChangeWithFaction` is required.
 *
 * @since 0.46.1
 * @see Misc.adjustRep
 */
fun TextPanelAPI.adjustReputation(
    repChangeWithFaction: Float,
    limit: RepLevel?,
    factionId: String?,
    repChangeWithPerson: Float,
    personLimit: RepLevel?,
    person: PersonAPI?
) = Misc.adjustRep(repChangeWithFaction, limit, factionId, repChangeWithPerson, personLimit, person, this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.interruptAbilitiesWithTag(tag: String) = Misc.interruptAbilitiesWithTag(this, tag)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken) = Misc.getInterceptPoint(this, other)

// Could use a default param of maxSpeedFrom = getTravelSpeed() because Alex copy/pasted the method
// but this is safer in case one of them changes in the future.
inline fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken, maxSpeedFrom: Float) =
    Misc.getInterceptPoint(this, other, maxSpeedFrom)

// Skipping getListOfResources, doesn't seem to make sense for an extension.
// Skipping setColor, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant() =
    Misc.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.isImportantForReason(reason: String) = Misc.isImportantForReason(this, reason)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.cleanUpMissionMemory(prefix: String) = Misc.cleanUpMissionMemory(this, prefix)

// Skipping clearAreaAroundPlayer, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getSalvageSeed() = Misc.getSalvageSeed(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getNameBasedSeed() = Misc.getNameBasedSeed(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.forgetAboutTransponder() = Misc.forgetAboutTransponder(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.setAbandonedStationMarket(marketId: String) =
    Misc.setAbandonedStationMarket(marketId, this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getDesiredMoveDir() = Misc.getDesiredMoveDir(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isPermaKnowsWhoPlayerIs() = Misc.isPermaKnowsWhoPlayerIs(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getImmigrationPlugin() = Misc.getImmigrationPlugin(this)

// Skipping getAICoreAdminPlugin, doesn't seem to make sense for an extension.
// Skipping getAICoreOfficerPlugin, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getAbandonMarketPlugin() = Misc.getAbandonMarketPlugin(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStabilizeMarketPlugin() = Misc.getStabilizeMarketPlugin(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getInflater(params: Any) = Misc.getInflater(this, params)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.playerHasStorageAccess() = Misc.playerHasStorageAccess(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getMarketSizeProgress() = Misc.getMarketSizeProgress(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorageCostPerMonth() = Misc.getStorageCostPerMonth(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorage() = Misc.getStorage(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getLocalResources() = Misc.getLocalResources(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorageCargo() = Misc.getStorageCargo(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getLocalResourcesCargo() = Misc.getLocalResourcesCargo(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorageTotalValue() = Misc.getStorageTotalValue(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorageCargoValue() = Misc.getStorageCargoValue(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStorageShipValue() = Misc.getStorageShipValue(this)

/**
 * Returns true if it added anything to the tooltip.
 * @since 0.46.0
 */
inline fun TooltipMakerAPI.addStorageInfo(
    color: Color,
    dark: Color,
    market: MarketAPI,
    includeLocalResources: Boolean,
    addSectionIfEmpty: Boolean
) = Misc.addStorageInfo(this, color, dark, market, includeLocalResources, addSectionIfEmpty)

/**
 * @since 0.46.0
 */
inline fun String.getTokenReplaced(entity: SectorEntityToken) = Misc.getTokenReplaced(this, entity)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.getAdminSalary() = Misc.getAdminSalary(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.getOfficerSalary(mercenary: Boolean = Misc.isMercenary(this)) = Misc.getOfficerSalary(this)

/**
 * MagicLib: Call on a variant id.
 * @since 0.46.0
 */
inline fun String.getHullIdForVariantId() = Misc.getHullIdForVariantId(this)

/**
 * MagicLib: Call on a variant id.
 * @since 0.46.0
 */
inline fun String.getFPForVariantId() = Misc.getFPForVariantId(this)

/**
 * MagicLib: Call on fleet points.
 * MagicLib: Originally named getAdjustedStrength.
 */
inline fun Float.getAdjustedStrengthFromFp(market: MarketAPI) = Misc.getAdjustedStrength(this, market)

/**
 * MagicLib: Call on fleet points.
 * @since 0.46.0
 */
inline fun Float.getAdjustedFP(market: MarketAPI) = Misc.getAdjustedFP(this, market)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getShipQuality(factionId: String? = null) = Misc.getShipQuality(this, factionId)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getShipPickMode(factionId: String? = null) = Misc.getShipPickMode(this, factionId)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isBusy() = Misc.isBusy(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStationFleet() = Misc.getStationFleet(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getStationFleet() = Misc.getStationFleet(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStationBaseFleet() = Misc.getStationBaseFleet(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getStationBaseFleet() = Misc.getStationBaseFleet(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getStationMarket() = Misc.getStationMarket(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getStationIndustry() = Misc.getStationIndustry(this)

/**
 * @since 0.46.0
 */
inline fun ShipVariantAPI.isActiveModule() = Misc.isActiveModule(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.isActiveModule() = Misc.isActiveModule(this)

// Skipping addCreditsMessage, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun JumpPointAPI.getSystemJumpPointHyperExitLocation() = Misc.getSystemJumpPointHyperExitLocation(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.isNear(hyperLoc: Vector2f) = Misc.isNear(this, hyperLoc)

/**
 * MagicLib: Call on a number in seconds.
 * @since 0.46.0
 */
inline fun Float.getDays() = Misc.getDays(this)

// Skipping getProbabilityMult, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.isHyperspaceAnchor() = Misc.isHyperspaceAnchor(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getStarSystemForAnchor() = Misc.getStarSystemForAnchor(this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.showCost(
    title: String = "Resources: consumed (available)",
    withAvailable: Boolean = true,
    widthOverride: Float = -1f,
    color: Color,
    dark: Color,
    res: Array<String>,
    quantities: IntArray,
    consumed: BooleanArray? = null
) = Misc.showCost(this, title, withAvailable, widthOverride, color, dark, res, quantities, consumed)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isPatrol() = Misc.isPatrol(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isSmuggler() = Misc.isSmuggler(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isTrader() = Misc.isTrader(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isPirate() = Misc.isPirate(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isScavenger() = Misc.isScavenger(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isRaider() = Misc.isRaider(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isWarFleet() = Misc.isWarFleet(this)

/**
 * pair.one can be null if a stand-alone, non-market station is being returned in pair.two.
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getNearestStationInSupportRange() = Misc.getNearestStationInSupportRange(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isStationInSupportRange(station: CampaignFleetAPI) =
    Misc.isStationInSupportRange(this, station)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.getMemberStrength(
    withHull: Boolean = true,
    withQuality: Boolean = true,
    withCaptain: Boolean = true
) = Misc.getMemberStrength(this, withHull, withQuality, withCaptain)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.increaseMarketHostileTimeout(days: Float) = Misc.increaseMarketHostileTimeout(this, days)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.removeRadioChatter() = Misc.removeRadioChatter(this)

/**
 * MagicLib: Call on a design type.
 * @since 0.46.0
 */
// Too much spam of String intellisense
//inline fun String.getDesignTypeColor() = Misc.getDesignTypeColor(this)

/**
 * MagicLib: Call on a design type.
 * @since 0.46.0
 */
// Too much spam of String intellisense
//inline fun String.getDesignTypeColorDim() = Misc.getDesignTypeColorDim(this)

/**
 * @since 0.46.0
 */
inline fun TooltipMakerAPI.addDesignTypePara(design: String, pad: Float) = Misc.addDesignTypePara(this, design, pad)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getFleetRadiusTerrainEffectMult() = Misc.getFleetRadiusTerrainEffectMult(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getBurnMultForTerrain() = Misc.getBurnMultForTerrain(this)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.addHitGlow(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    dur: Float = 1f + Math.random().toFloat(),
    color: Color
) = Misc.addHitGlow(this, loc, vel, size, dur, color)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.addGlowyParticle(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    rampUp: Float,
    dur: Float,
    color: Color
) = Misc.addGlowyParticle(this, loc, vel, size, rampUp, dur, color)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getShippingCapacity(inFaction: Boolean) = Misc.getShippingCapacity(this, inFaction)

/**
 * MagicLib: getStrengthDesc
 * Call on FP float.
 * @since 0.46.0
 */
inline fun Float.getStrengthDescForFP() = Misc.getStrengthDesc(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.isMilitary() = Misc.isMilitary(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.hasHeavyIndustry() = Misc.hasHeavyIndustry(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.hasOrbitalStation() = Misc.hasOrbitalStation(this)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.getClaimingFaction() = Misc.getClaimingFaction(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.computeTotalShutdownRefund() = Misc.computeTotalShutdownRefund(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.computeShutdownRefund(industry: Industry) = Misc.computeShutdownRefund(this, industry)

/**
 * MagicLib: Call on the center of the location, eg the star.
 * @since 0.46.0
 */
inline fun SectorEntityToken.addWarningBeacon(gap: BaseThemeGenerator.OrbitGap, beaconTag: String) =
    Misc.addWarningBeacon(this, gap, beaconTag)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.getTradeMode() = Misc.getTradeMode(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getSpaceport() = Misc.getSpaceport(this)

/**
 * @since 0.46.0
 */
inline fun Color.setBrightness(brightness: Int) = Misc.setBrightness(this, brightness)

/**
 * @since 0.46.0
 */
inline fun Color.scaleColorSaturate(factor: Float) = Misc.scaleColorSaturate(this, factor)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getMaxOfficers() = Misc.getMaxOfficers(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getNumNonMercOfficers() = Misc.getNumNonMercOfficers(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getMercs() = Misc.getMercs(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getMaxIndustries() = Misc.getMaxIndustries(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getNumIndustries() = Misc.getNumIndustries(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getNumImprovedIndustries() = Misc.getNumImprovedIndustries(this)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.getNumStableLocations() = Misc.getNumStableLocations(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getCurrentlyBeingConstructed() = Misc.getCurrentlyBeingConstructed(this)

/**
 * @since 0.46.0
 */
inline fun Float.getRelColor() = Misc.getRelColor(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getDangerLevel() = Misc.getDangerLevel(this)

// Skipping getHitGlowSize, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun PersonAPI.getNumEliteSkills() = Misc.getNumEliteSkills(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.isMentored() = Misc.isMentored(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.setMentored(mentored: Boolean) = Misc.setMentored(this, mentored)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.isMercenary() = Misc.isMercenary(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.setMercHiredNow() = Misc.setMercHiredNow(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.getMercDaysSinceHired() = Misc.getMercDaysSinceHired(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.setMercenary(mercenary: Boolean) = Misc.setMercenary(this, mercenary)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.isUnremovable() = Misc.isUnremovable(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.setUnremovable(unremovable: Boolean) = Misc.setUnremovable(this, unremovable)

/**
 * @since 0.46.0
 */
inline fun MutableShipStatsAPI.isAutomated() = Misc.isAutomated(this)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.isAutomated() = Misc.isAutomated(this)

/**
 * @since 0.46.0
 */
inline fun ShipVariantAPI.isAutomated() = Misc.isAutomated(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.isAutomated() = Misc.isAutomated(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.getMaxPermanentMods() = Misc.getMaxPermanentMods(this)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.getMaxPermanentMods(stats: MutableCharacterStatsAPI) = Misc.getMaxPermanentMods(this, stats)

/**
 * @since 0.46.0
 */
inline fun HullModSpecAPI.getBuildInBonusXP(size: ShipAPI.HullSize) = Misc.getBuildInBonusXP(this, size)

/**
 * @since 0.46.0
 */
inline fun HullModSpecAPI.getOPCost(size: ShipAPI.HullSize) = Misc.getOPCost(this, size)

/**
 * @since 0.46.0
 */
inline fun ShipVariantAPI.isSpecialMod(spec: HullModSpecAPI) = Misc.isSpecialMod(this, spec)

/**
 * @since 0.46.0
 */
inline fun ShipVariantAPI.getCurrSpecialMods() = Misc.getCurrSpecialMods(this)

/**
 * @since 0.46.0
 */
inline fun ShipVariantAPI.getCurrSpecialModsList() = Misc.getCurrSpecialModsList(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isSlowMoving() = Misc.isSlowMoving(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getGoSlowBurnLevel() = Misc.getGoSlowBurnLevel(this)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.applyDamage(
    random: Random? = null, level: Misc.FleetMemberDamageLevel,
    withCRDamage: Boolean, crDamageId: String, crDamageReason: String,
    withMessage: Boolean, textPanel: TextPanelAPI? = null,
    messageText: String
) = Misc.applyDamage(
    this,
    random,
    level,
    withCRDamage,
    crDamageId,
    crDamageReason,
    withMessage,
    textPanel,
    messageText
)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.applyDamage(
    random: Random? = null, damageMult: Float,
    withCRDamage: Boolean, crDamageId: String, crDamageReason: String,
    withMessage: Boolean, textPanel: TextPanelAPI? = null,
    messageText: String
) = Misc.applyDamage(
    this,
    random,
    damageMult,
    withCRDamage,
    crDamageId,
    crDamageReason,
    withMessage,
    textPanel,
    messageText
)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.getBonusXPForRecovering() = Misc.getBonusXPForRecovering(this)

/**
 * @since 0.46.0
 */
inline fun FleetMemberAPI.getBonusXPForScuttling() = Misc.getBonusXPForScuttling(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getSpawnFPMult() = Misc.getSpawnFPMult(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.setSpawnFPMult(mult: Float) = Misc.setSpawnFPMult(this, mult)

/**
 * @since 0.46.0
 */
inline fun FactionAPI.isDecentralized() = Misc.isDecentralized(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.getPersonalityName() = Misc.getPersonalityName(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.setRaidedTimestamp() = Misc.setRaidedTimestamp(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.getDaysSinceLastRaided() = Misc.getDaysSinceLastRaided(this)

/**
 * @since 0.46.0
 */
inline fun CommodityOnMarketAPI.computeEconUnitChangeFromTradeModChange(quantity: Int) =
    Misc.computeEconUnitChangeFromTradeModChange(this, quantity)

/**
 * @since 0.46.0
 */
inline fun CommodityOnMarketAPI.affectAvailabilityWithinReason(quantity: Int) =
    Misc.affectAvailabilityWithinReason(this, quantity)

/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.isOpenlyPopulated() = Misc.isOpenlyPopulated(this)

/**
 * @since 0.46.0
 */
inline fun Collection<String>.hasAtLeastOneOfTags(vararg other: String) = Misc.hasAtLeastOneOfTags(this, *other)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.hasUnexploredRuins() = Misc.hasUnexploredRuins(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.hasRuins() = Misc.hasRuins(this)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.hasFarmland() = Misc.hasFarmland(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.addDefeatTrigger(trigger: String) = Misc.addDefeatTrigger(this, trigger)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.removeDefeatTrigger(trigger: String) = Misc.removeDefeatTrigger(this, trigger)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.getDefeatTriggers(createIfNecessary: Boolean) =
    Misc.getDefeatTriggers(this, createIfNecessary)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.clearDefeatTriggersIfNeeded() = Misc.clearDefeatTriggersIfNeeded(this)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.shouldShowDamageFloaty(target: ShipAPI) = Misc.shouldShowDamageFloaty(this, target)

// Skipping playSound, doesn't seem to make sense for an extension.

/**
 * @since 0.46.0
 */
inline fun ShipAPI.getShipWeight(adjustForNonCombat: Boolean = true) = Misc.getShipWeight(this, adjustForNonCombat)

/**
 * @since 0.46.0
 */
inline fun ShipAPI.getIncapacitatedTime() = Misc.getIncapacitatedTime(this)

/**
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isAvoidingPlayerHalfheartedly() = Misc.isAvoidingPlayerHalfheartedly(this)

/**
 * In vanilla, pirates and Luddic Path.
 * @since 0.46.0
 */
inline fun FactionAPI.isPirateFaction() = Misc.isPirateFaction(this)

/**
 * Probably wrong sometimes...
 *
 * MagicLib: originally called getAOrAnFor.
 *
 * @return "a" or "an" for word.
 * @since 0.46.0
 */
inline fun String.getAOrAnForWord() = Misc.getAOrAnFor(this)

/**
 * @since 0.46.0
 */
inline fun PersonAPI.moveToMarket(destination: MarketAPI, alwaysAddToCommDirectory: Boolean) =
    Misc.moveToMarket(this, destination, alwaysAddToCommDirectory)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

/**
 * @since 0.46.0
 */
inline fun MarketAPI.isStoryCritical() = Misc.isStoryCritical(this)

/**
 * @since 0.46.0
 */
inline fun MemoryAPI.isStoryCritical() = Misc.isStoryCritical(this)

/**
 * Whether it prevents salvage, surveying, etc. But NOT things that require only being
 * seen to ruin them, such as SpySat deployments.
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.isInsignificant() = Misc.isInsignificant(this)

/**
 * Mainly for avoiding stuff like "pirate fleet with 4 rustbuckets will run away from the player's
 * 4 regular-quality frigates". Fleets that this evaluates to true for will avoid the player slowly.
 * @since 0.46.0
 */
inline fun CampaignFleetAPI.shouldNotWantRunFromPlayerEvenIfWeaker() = Misc.shouldNotWantRunFromPlayerEvenIfWeaker(this)

/**
 * @since 0.46.0
 */
inline fun FloatArray.findKth(k: Int) = Misc.findKth(this, k)

/**
 * @since 0.46.0
 */
inline fun Float.getAdjustedBaseRange(ship: ShipAPI?, weapon: WeaponAPI?) = Misc.getAdjustedBaseRange(this, ship, weapon)

/**
 * @since 0.46.0
 */
inline fun Vector2f.bezier(p1: Vector2f, p2: Vector2f, t: Float) = Misc.bezier(this, p1, p2, t)

/**
 * @since 0.46.0
 */
inline fun Vector2f.bezierCubic(p1: Vector2f, p2: Vector2f, p3: Vector2f, t: Float) =
    Misc.bezierCubic(this, p1, p2, p3, t)

/**
 * @since 0.46.0
 */
inline fun Vector2f.isInsideSlipstream(radius: Float, location: LocationAPI = Global.getSector().hyperspace) =
    Misc.isInsideSlipstream(this, radius, location)

/**
 * @since 0.46.0
 */
inline fun SectorEntityToken.isInsideSlipstream() = Misc.isInsideSlipstream(this)

/**
 * @since 0.46.0
 */
inline fun Vector2f.isOutsideSector() = Misc.isOutsideSector(this)

/**
 * @since 0.46.0
 */
inline fun LocationAPI.crossesAnySlipstream(from: Vector2f, to: Vector2f) = Misc.crossesAnySlipstream(this, from, to)