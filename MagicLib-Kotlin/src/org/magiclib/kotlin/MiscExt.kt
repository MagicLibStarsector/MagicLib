package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.events.CampaignEventTarget
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidSource
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.FleetFilter
import org.json.JSONObject
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import java.nio.Buffer
import java.util.*


val Vector2f.ZERO
    get() = Misc.ZERO

fun String.ucFirst() = Misc.ucFirst(this)
fun String.lcFirst() = Misc.lcFirst(this)

fun String.replaceTokensFromMemory(memoryMap: Map<String, MemoryAPI>) = Misc.replaceTokensFromMemory(this, memoryMap)

fun SectorEntityToken.getDistance(to: SectorEntityToken) = Misc.getDistance(this, to)

fun SectorEntityToken.getDistanceLY(to: SectorEntityToken) = Misc.getDistanceLY(this, to)

fun Vector2f.getDistanceSq(to: Vector2f) = Misc.getDistanceSq(this, to)

fun Vector2f.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

fun SectorEntityToken.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

fun Vector2f.getDistanceLY(to: Vector2f) = Misc.getDistanceLY(this, to)

fun Float.getRounded() = Misc.getRounded(this)

fun Float.getRoundedValue() = Misc.getRoundedValue(this)

fun Float.getRoundedValueFloat() = Misc.getRoundedValueFloat(this)

fun Float.getRoundedValueMaxOneAfterDecimal() =
    Misc.getRoundedValueMaxOneAfterDecimal(this)

fun Float.logOfBase(num: Float) = Misc.logOfBase(this, num)

fun Vector2f.getPointAtRadius(r: Float) = Misc.getPointAtRadius(this, r)

fun Vector2f.getPointAtRadius(r: Float, random: Random) =
    Misc.getPointAtRadius(this, r, random)

fun Vector2f.getPointWithinRadius(r: Float, random: Random = Misc.random) =
    Misc.getPointWithinRadius(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(r: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(minR: Float, maxR: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, minR, maxR, random)

fun CampaignFleetAPI.getSnapshotFPLost() = Misc.getSnapshotFPLost(this)

fun CampaignFleetAPI.getSnapshotMembersLost() = Misc.getSnapshotMembersLost(this)

fun CampaignEventTarget.startEvent(eventId: String, params: Any) =
    Misc.startEvent(this, eventId, params)

fun String.getAndJoined(strings: List<String>) = Misc.getAndJoined(strings)

fun String.getAndJoined(vararg strings: String) = Misc.getAndJoined(*strings)

fun String.getJoined(joiner: String, strings: List<String>) =
    Misc.getJoined(joiner, strings)

fun String.getJoined(joiner: String, vararg strings: String) =
    Misc.getJoined(joiner, *strings)

fun SectorEntityToken.findNearbyFleets(maxRange: Float, filter: FleetFilter) =
    Misc.findNearbyFleets(this, maxRange, filter)

fun StarSystemAPI.getFleetsInOrNearSystem() = Misc.getFleetsInOrNearSystem(this)

fun LocationAPI.getMarketsInLocation(factionId: String? = null) =
    if (factionId == null)
        Misc.getMarketsInLocation(this)
    else
        Misc.getMarketsInLocation(this, factionId)

fun LocationAPI.getBiggestMarketInLocation() = Misc.getBiggestMarketInLocation(this)

fun FactionAPI.getFactionMarkets(econGroup: String? = null) =
    if (econGroup == null)
        Misc.getFactionMarkets(this)
    else
        Misc.getFactionMarkets(this, econGroup)

fun Vector2f.getNearbyMarkets(distLY: Float) = Misc.getNearbyMarkets(this, distLY)

fun FactionAPI.getNumHostileMarkets(from: SectorEntityToken, maxDist: Float) =
    Misc.getNumHostileMarkets(this, from, maxDist)

fun SectorEntityToken.getNearbyStarSystems(maxRangeLY: Float) =
    Misc.getNearbyStarSystems(this, maxRangeLY)

fun SectorEntityToken.getNearbyStarSystem(maxRangeLY: Float) =
    Misc.getNearbyStarSystem(this, maxRangeLY)

fun SectorEntityToken.getNearestStarSystem() = Misc.getNearestStarSystem(this)

fun SectorEntityToken.getNearbyStarSystem() = Misc.getNearbyStarSystem(this)

fun SectorEntityToken.showRuleDialog(initialTrigger: String) =
    Misc.showRuleDialog(this, initialTrigger)

fun Vector2f.getAngleInDegreesStrict() = Misc.getAngleInDegreesStrict(this)

fun Vector2f.getAngleInDegreesStrict(to: Vector2f) = Misc.getAngleInDegreesStrict(this, to)

fun Vector2f.getAngleInDegrees() = Misc.getAngleInDegrees(this)

fun Vector2f.getAngleInDegrees(to: Vector2f) = Misc.getAngleInDegrees(this, to)

fun Vector2f.normalise() = Misc.normalise(this)

/**
 * Normalizes an angle given in degrees.
 */
fun Float.normalizeAngle() = Misc.normalizeAngle(this)

fun SectorEntityToken.findNearestLocalMarket(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearestLocalMarket(this, maxDist, filter)

fun SectorEntityToken.findNearbyLocalMarkets(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearbyLocalMarkets(this, maxDist, filter)

fun SectorEntityToken.findNearestLocalMarketWithSameFaction(maxDist: Float) =
    Misc.findNearestLocalMarketWithSameFaction(this, maxDist)

fun Vector2f.getUnitVector(to: Vector2f) = Misc.getUnitVector(this, to)

/**
 * Called on an angle given in degrees.
 */
fun Float.getUnitVectorAtDegreeAngle() = Misc.getUnitVectorAtDegreeAngle(this)

fun Vector2f.rotateAroundOrigin(angle: Float) = Misc.rotateAroundOrigin(this, angle)

fun Vector2f.rotateAroundOrigin(angle: Float, origin: Vector2f) =
    Misc.rotateAroundOrigin(this, angle, origin)

/**
 * Angles.
 */
fun Float.isBetween(two: Float, check: Float) = Misc.isBetween(this, two, check)

fun CampaignFleetAPI.getShieldedCargoFraction() = Misc.getShieldedCargoFraction(this)

fun Color.interpolateColor(to: Color, progress: Float) = Misc.interpolateColor(this, to, progress)

fun Vector2f.interpolateVector(to: Vector2f, progress: Float) = Misc.interpolateVector(this, to, progress)

fun Float.interpolate(to: Float, progress: Float) = Misc.interpolate(this, to, progress)

fun Color.scaleColor(factor: Float) = Misc.scaleColor(this, factor)

fun Color.scaleColorOnly(factor: Float) = Misc.scaleColorOnly(this, factor)

fun Color.scaleAlpha(factor: Float) = Misc.scaleAlpha(this, factor)

fun Color.setAlpha(alpha: Int) = Misc.setAlpha(this, alpha)

fun ShipAPI.HullSize.getSizeNum() = Misc.getSizeNum(this)

fun MemoryAPI.unsetAll(prefix: String, memKey: String) = Misc.unsetAll(prefix, memKey, this)

fun CombatEntityAPI.getTargetingRadius(from: Vector2f, considerShield: Boolean) =
    Misc.getTargetingRadius(from, this, considerShield)

// getClosingSpeed skipped, doesn't make sense to convert.

fun Float.getWithDGS() = Misc.getWithDGS(this)

fun Float.getDGSCredits() = Misc.getDGSCredits(this)

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
fun MemoryAPI.setFlagWithReason(flagKey: String, reason: String?, value: Boolean, expire: Float) =
    Misc.setFlagWithReason(this, flagKey, reason, value, expire)

fun MemoryAPI.flagHasReason(flagKey: String, reason: String?) = Misc.flagHasReason(this, flagKey, reason)

fun MemoryAPI.clearFlag(flagKey: String) = Misc.clearFlag(this, flagKey)

fun CampaignFleetAPI.makeLowRepImpact(reason: String?) = Misc.makeLowRepImpact(this, reason)

fun CampaignFleetAPI.makeNoRepImpact(reason: String?) = Misc.makeNoRepImpact(this, reason)

fun CampaignFleetAPI.makeHostile() = Misc.makeHostile(this)

fun CampaignFleetAPI.makeNotLowRepImpact(reason: String?) = Misc.makeNotLowRepImpact(this, reason)

fun Long.getAgoStringForTimestamp() = Misc.getAgoStringForTimestamp(this)

fun Long.getDetailedAgoString() = Misc.getDetailedAgoString(this)

fun Int.getAtLeastStringForDays() = Misc.getAtLeastStringForDays(this)

fun Int.getStringForDays() = Misc.getStringForDays(this)

fun Float.getBurnLevelForSpeed() = Misc.getBurnLevelForSpeed(this)

fun Float.getFractionalBurnLevelForSpeed() = Misc.getFractionalBurnLevelForSpeed(this)

fun Float.getSpeedForBurnLevel() = Misc.getSpeedForBurnLevel(this)

fun CampaignFleetAPI.getFuelPerDay(burnLevel: Float) = Misc.getFuelPerDay(this, burnLevel)

fun CampaignFleetAPI.getFuelPerDayAtSpeed(speed: Float) = Misc.getFuelPerDayAtSpeed(this, speed)

fun CampaignFleetAPI.getLYPerDayAtBurn(burnLevel: Float) = Misc.getLYPerDayAtBurn(this, burnLevel)

fun CampaignFleetAPI.getLYPerDayAtSpeed(speed: Float) = Misc.getLYPerDayAtSpeed(this, speed)

fun Vector3f.getDistance(other: Vector3f) = Misc.getDistance(this, other)
fun Float.getAngleDiff(to: Float) = Misc.getAngleDiff(this, to)

// Skipping isInArc, doesn't seem to make sense for an extension.
// Skipping isInArc, doesn't seem to make sense for an extension.
// Skipping addNebulaFromPNG, doesn't seem to make sense for an extension.
// Skipping renderQuad, doesn't seem to make sense for an extension.

/**
 * Shortest distance from line to a point.
 */
fun Vector2f.distanceFromLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.distanceFromLineToPoint(lineStart, lineEnd, this)

fun Vector2f.closestPointOnLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnLineToPoint(lineStart, lineEnd, this)

fun Vector2f.closestPointOnSegmentToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnSegmentToPoint(lineStart, lineEnd, this)

fun Vector2f.isPointInBounds(bounds: List<Vector2f>) = Misc.isPointInBounds(this, bounds)

// Skipping intersectSegments, doesn't seem to make sense for an extension.
// Skipping intersectLines, doesn't seem to make sense for an extension.
// Skipping intersectSegmentAndCircle, doesn't seem to make sense for an extension.
// Skipping areSegmentsCoincident, doesn't seem to make sense for an extension.

fun Vector2f.getPerp() = Misc.getPerp(this)

// Skipping getClosestTurnDirection, doesn't seem to make sense for an extension.

fun Vector2f.getClosestTurnDirection(other: Vector2f) = Misc.getClosestTurnDirection(this, other)

fun Vector2f.getDiff(other: Vector2f) = Misc.getDiff(this, other)

fun CampaignFleetAPI.getSourceMarket() = Misc.getSourceMarket(this)

// Skipping getSpawnChanceMult, doesn't seem to make sense for an extension.
// Skipping pickHyperLocationNotNearPlayer, doesn't seem to make sense for an extension.
// Skipping pickLocationNotNearPlayer, doesn't seem to make sense for an extension.

fun Vector2f.wiggle(max: Float) = Misc.wiggle(this, max)

fun CampaignFleetAPI.isPlayerOrCombinedPlayerPrimary() = Misc.isPlayerOrCombinedPlayerPrimary(this)

fun CampaignFleetAPI.isPlayerOrCombinedContainingPlayer() = Misc.isPlayerOrCombinedContainingPlayer(this)

/**
 * Call on an asteroid.
 */
fun SectorEntityToken.getAsteroidSource() = Misc.getAsteroidSource(this)

/**
 * Call on an asteroid.
 */
fun SectorEntityToken.setAsteroidSource(source: AsteroidSource?) =
    Misc.setAsteroidSource(this, source)

/**
 * Call on an asteroid.
 */
fun SectorEntityToken.clearAsteroidSource() = Misc.clearAsteroidSource(this)

/**
 * Call on a star.
 */
fun PlanetAPI.getCoronaFor() = Misc.getCoronaFor(this)

/**
 * Call on a star.
 */
fun PlanetAPI.getPulsarFor() = Misc.getPulsarFor(this)

fun StarSystemAPI?.hasPulsar() = Misc.hasPulsar(this)

fun CampaignFleetAPI.caresAboutPlayerTransponder() =
    Misc.caresAboutPlayerTransponder(this)

fun ShipAPI?.findClosestShipEnemyOf(
    locFromForSorting: Vector2f,
    smallestToNote: ShipAPI.HullSize,
    maxRange: Float,
    considerShipRadius: Boolean,
    filter: Misc.FindShipFilter? = null
) = Misc.findClosestShipEnemyOf(this, locFromForSorting, smallestToNote, maxRange, considerShipRadius, filter)

fun <T : Enum<T>> JSONObject.mapToEnum(key: String, enumType: Class<T>, defaultOption: T, required: Boolean = true) =
    Misc.mapToEnum(this, key, enumType, defaultOption, required)

fun JSONObject.getColor(key: String) = Misc.getColor(this, key)
fun JSONObject.optColor(key: String, defaultValue: Color?) = Misc.optColor(this, key, defaultValue)

// Skipping normalizeNoise, doesn't seem to make sense for an extension.
// Skipping initNoise, doesn't seem to make sense for an extension.
// Skipping genFractalNoise, doesn't seem to make sense for an extension.
// Skipping fill, doesn't seem to make sense for an extension.
// Skipping computeAngleSpan, doesn't seem to make sense for an extension.
// Skipping computeAngleRadius, doesn't seem to make sense for an extension.
// Skipping approach, doesn't seem to make sense for an extension.

fun Buffer.cleanBuffer() = Misc.cleanBuffer(this)

fun CampaignFleetAPI.getFleetwideTotalStat(dynamicMemberStatId: String) =
    Misc.getFleetwideTotalStat(this, dynamicMemberStatId)

fun CampaignFleetAPI.getFleetwideTotalMod(dynamicMemberStatId: String, base: Float, ship: ShipAPI? = null) =
    Misc.getFleetwideTotalMod(this, dynamicMemberStatId, base, ship)

fun PlanetAPI.getStarId() = Misc.getStarId(this)

fun StarSystemAPI.getMinSystemSurveyLevel() = Misc.getMinSystemSurveyLevel(this)

fun StarSystemAPI.hasAnySurveyDataFor() = Misc.hasAnySurveyDataFor(this)

// Skipping setAllPlanetsKnown, doesn't seem to make sense for an extension.

fun StarSystemAPI.setAllPlanetsKnown() = Misc.setAllPlanetsKnown(this)

fun StarSystemAPI.setAllPlanetsSurveyed(setRuinsExplored: Boolean) = Misc.setAllPlanetsSurveyed(this, setRuinsExplored)

fun StarSystemAPI.generatePlanetConditions(age: StarAge) = Misc.generatePlanetConditions(this, age)

fun PlanetAPI.getEstimatedOrbitIndex() = Misc.getEstimatedOrbitIndex(this)

// Skipping getRandom, doesn't seem to make sense for an extension.

fun PlanetAPI.addSurveyDataFor(text: TextPanelAPI) = Misc.addSurveyDataFor(this, text)

fun MarketAPI.setFullySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setFullySurveyed(this, text, withNotification)

fun MarketAPI.setPreliminarySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setPreliminarySurveyed(this, text, withNotification)

fun MarketAPI.setSeen(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setSeen(this, text, withNotification)

fun String.getStringWithTokenReplacement(entity: SectorEntityToken, memoryMap: Map<String, MemoryAPI>?) =
    Misc.getStringWithTokenReplacement(this, entity, memoryMap)

// Skipping renderQuadAlpha, doesn't seem to make sense for an extension.

fun SectorEntityToken.fadeAndExpire(seconds: Float = 1f) =
    Misc.fadeAndExpire(this, seconds)

fun SectorEntityToken.fadeInOutAndExpire(`in`: Float, dur: Float, out: Float) =
    Misc.fadeInOutAndExpire(this, `in`, dur, out)

// Skipping addCargoPods, doesn't seem to make sense for an extension.

fun LocationAPI.addDebrisField(params: DebrisFieldTerrainPlugin.DebrisFieldParams, random: Random?) =
    Misc.addDebrisField(this, params, random)

fun FleetMemberAPI.isUnboardable() =
    Misc.isUnboardable(this)

fun ShipHullSpecAPI.isUnboardable() =
    Misc.isUnboardable(this)

fun FleetMemberAPI.isShipRecoverable(
    recoverer: CampaignFleetAPI?,
    own: Boolean,
    useOfficerRecovery: Boolean,
    chanceMult: Float
) = Misc.isShipRecoverable(this, recoverer, own, useOfficerRecovery, chanceMult)

fun SectorEntityToken.findNearestJumpPointTo() =
    Misc.findNearestJumpPointTo(this)

fun SectorEntityToken.findNearestJumpPointThatCouldBeExitedFrom() =
    Misc.findNearestJumpPointThatCouldBeExitedFrom(this)

fun SectorEntityToken.findNearestPlanetTo(requireGasGiant: Boolean, allowStars: Boolean) =
    Misc.findNearestPlanetTo(this, requireGasGiant, allowStars)

fun LocationAPI.shouldConvertFromStub(location: Vector2f) =
    Misc.shouldConvertFromStub(this, location)

fun List<Color>.colorsToString() = Misc.colorsToString(this)

fun String.colorsFromString() = Misc.colorsFromString(this)

fun PlanetAPI.getJumpPointTo() = Misc.getJumpPointTo(this)

fun SectorEntityToken.findNearestJumpPoint() = Misc.findNearestJumpPoint(this)

fun ShipHullSpecAPI.getDHullId() = Misc.getDHullId(this)

// Skipping getMod, doesn't seem to make sense for an extension.
// Skipping getDistanceFromArc, doesn't seem to make sense for an extension.

fun PlanetAPI.initConditionMarket() = Misc.initConditionMarket(this)

fun PlanetAPI.initEconomyMarket() = Misc.initEconomyMarket(this)

fun MarketAPI.SurveyLevel.getSurveyLevelString(withBrackets: Boolean) = Misc.getSurveyLevelString(this, withBrackets)

fun SectorEntityToken.setDefenderOverride(override: DefenderDataOverride) = Misc.setDefenderOverride(this, override)

fun SectorEntityToken.setSalvageSpecial(data: Any?) = Misc.setSalvageSpecial(this, data)

fun SectorEntityToken.setPrevSalvageSpecial(data: Any?) = Misc.setPrevSalvageSpecial(this, data)

fun SectorEntityToken.getSalvageSpecial() = Misc.getSalvageSpecial(this)

fun SectorEntityToken.getPrevSalvageSpecial() = Misc.getPrevSalvageSpecial(this)

fun SectorEntityToken.getSystemsInRange(exclude: Set<StarSystemAPI>, nonEmpty: Boolean, maxRange: Float) =
    Misc.getSystemsInRange(this, exclude, nonEmpty, maxRange)

fun StarSystemAPI.getPulsarInSystem() = Misc.getPulsarInSystem(this)

fun StarSystemAPI.systemHasPlanets() = Misc.systemHasPlanets(this)

fun ShipAPI.HullSize.getCampaignShipScaleMult() = Misc.getCampaignShipScaleMult(this)

// Skipping createStringPicker, doesn't seem to make sense for an extension.

fun SectorEntityToken.setWarningBeaconGlowColor(color: Color) = Misc.setWarningBeaconGlowColor(this, color)

fun SectorEntityToken.setWarningBeaconPingColor(color: Color) = Misc.setWarningBeaconPingColor(this, color)

fun SectorEntityToken.setWarningBeaconColors(color: Color, ping: Color) = Misc.setWarningBeaconColors(this, color, ping)

fun SectorEntityToken.getNearbyFleets(maxDist: Float) = Misc.getNearbyFleets(this, maxDist)

fun SectorEntityToken.getVisibleFleets(includeSensorContacts: Boolean) =
    Misc.getVisibleFleets(this, includeSensorContacts)

fun CargoAPI.isSameCargo(other: CargoAPI) = Misc.isSameCargo(this, other)

fun StarSystemAPI.getDistressJumpPoint() = Misc.getDistressJumpPoint(this)

fun CampaignFleetAPI.clearTarget(forgetTransponder: Boolean) = Misc.clearTarget(this, forgetTransponder)

fun CampaignFleetAPI.giveStandardReturnToSourceAssignments(withClear: Boolean = true) =
    Misc.giveStandardReturnToSourceAssignments(this, withClear)

fun CampaignFleetAPI.giveStandardReturnAssignments(where: SectorEntityToken, text: String, withClear: Boolean) =
    Misc.giveStandardReturnAssignments(this, where, text, withClear)

// Skipping adjustRep, doesn't seem to make sense for an extension.

fun CampaignFleetAPI.interruptAbilitiesWithTag(tag: String) =
    Misc.interruptAbilitiesWithTag(this, tag)