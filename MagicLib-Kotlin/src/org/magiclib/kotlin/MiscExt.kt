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
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin
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


inline val Vector2f.ZERO
    get() = Misc.ZERO

inline fun String.ucFirst() = Misc.ucFirst(this)
inline fun String.lcFirst() = Misc.lcFirst(this)

inline fun String.replaceTokensFromMemory(memoryMap: Map<String, MemoryAPI>) =
    Misc.replaceTokensFromMemory(this, memoryMap)

inline fun SectorEntityToken.getDistance(to: SectorEntityToken) = Misc.getDistance(this, to)

inline fun SectorEntityToken.getDistanceLY(to: SectorEntityToken) = Misc.getDistanceLY(this, to)

inline fun Vector2f.getDistanceSq(to: Vector2f) = Misc.getDistanceSq(this, to)

inline fun Vector2f.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

inline fun SectorEntityToken.getDistanceToPlayerLY() = Misc.getDistanceToPlayerLY(this)

inline fun Vector2f.getDistanceLY(to: Vector2f) = Misc.getDistanceLY(this, to)

inline fun Float.getRounded() = Misc.getRounded(this)

inline fun Float.getRoundedValue() = Misc.getRoundedValue(this)

inline fun Float.getRoundedValueFloat() = Misc.getRoundedValueFloat(this)

inline fun Float.getRoundedValueMaxOneAfterDecimal() =
    Misc.getRoundedValueMaxOneAfterDecimal(this)

inline fun Float.logOfBase(num: Float) = Misc.logOfBase(this, num)

inline fun Vector2f.getPointAtRadius(r: Float) = Misc.getPointAtRadius(this, r)

inline fun Vector2f.getPointAtRadius(r: Float, random: Random) =
    Misc.getPointAtRadius(this, r, random)

inline fun Vector2f.getPointWithinRadius(r: Float, random: Random = Misc.random) =
    Misc.getPointWithinRadius(this, r, random)

inline fun Vector2f.getPointWithinRadiusUniform(r: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, r, random)

inline fun Vector2f.getPointWithinRadiusUniform(minR: Float, maxR: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, minR, maxR, random)

inline fun CampaignFleetAPI.getSnapshotFPLost() = Misc.getSnapshotFPLost(this)

inline fun CampaignFleetAPI.getSnapshotMembersLost() = Misc.getSnapshotMembersLost(this)

inline fun CampaignEventTarget.startEvent(eventId: String, params: Any) =
    Misc.startEvent(this, eventId, params)

inline fun String.getAndJoined(strings: List<String>) = Misc.getAndJoined(strings)

inline fun String.getAndJoined(vararg strings: String) = Misc.getAndJoined(*strings)

inline fun String.getJoined(joiner: String, strings: List<String>) =
    Misc.getJoined(joiner, strings)

inline fun String.getJoined(joiner: String, vararg strings: String) =
    Misc.getJoined(joiner, *strings)

inline fun SectorEntityToken.findNearbyFleets(maxRange: Float, filter: FleetFilter) =
    Misc.findNearbyFleets(this, maxRange, filter)

inline fun StarSystemAPI.getFleetsInOrNearSystem() = Misc.getFleetsInOrNearSystem(this)

inline fun LocationAPI.getMarketsInLocation(factionId: String? = null) =
    if (factionId == null)
        Misc.getMarketsInLocation(this)
    else
        Misc.getMarketsInLocation(this, factionId)

inline fun LocationAPI.getBiggestMarketInLocation() = Misc.getBiggestMarketInLocation(this)

inline fun FactionAPI.getFactionMarkets(econGroup: String? = null) =
    if (econGroup == null)
        Misc.getFactionMarkets(this)
    else
        Misc.getFactionMarkets(this, econGroup)

inline fun Vector2f.getNearbyMarkets(distLY: Float) = Misc.getNearbyMarkets(this, distLY)

inline fun FactionAPI.getNumHostileMarkets(from: SectorEntityToken, maxDist: Float) =
    Misc.getNumHostileMarkets(this, from, maxDist)

inline fun SectorEntityToken.getNearbyStarSystems(maxRangeLY: Float) =
    Misc.getNearbyStarSystems(this, maxRangeLY)

inline fun SectorEntityToken.getNearbyStarSystem(maxRangeLY: Float) =
    Misc.getNearbyStarSystem(this, maxRangeLY)

inline fun SectorEntityToken.getNearestStarSystem() = Misc.getNearestStarSystem(this)

inline fun SectorEntityToken.getNearbyStarSystem() = Misc.getNearbyStarSystem(this)

inline fun SectorEntityToken.showRuleDialog(initialTrigger: String) =
    Misc.showRuleDialog(this, initialTrigger)

inline fun Vector2f.getAngleInDegreesStrict() = Misc.getAngleInDegreesStrict(this)

inline fun Vector2f.getAngleInDegreesStrict(to: Vector2f) = Misc.getAngleInDegreesStrict(this, to)

inline fun Vector2f.getAngleInDegrees() = Misc.getAngleInDegrees(this)

inline fun Vector2f.getAngleInDegrees(to: Vector2f) = Misc.getAngleInDegrees(this, to)

inline fun Vector2f.normalise() = Misc.normalise(this)

/**
 * MagicLib: Normalizes an angle given in degrees.
 */
inline fun Float.normalizeAngle() = Misc.normalizeAngle(this)

inline fun SectorEntityToken.findNearestLocalMarket(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearestLocalMarket(this, maxDist, filter)

inline fun SectorEntityToken.findNearbyLocalMarkets(maxDist: Float, filter: BaseEventPlugin.MarketFilter) =
    Misc.findNearbyLocalMarkets(this, maxDist, filter)

inline fun SectorEntityToken.findNearestLocalMarketWithSameFaction(maxDist: Float) =
    Misc.findNearestLocalMarketWithSameFaction(this, maxDist)

inline fun Vector2f.getUnitVector(to: Vector2f) = Misc.getUnitVector(this, to)

/**
 * MagicLib: Called on an angle given in degrees.
 */
inline fun Float.getUnitVectorAtDegreeAngle() = Misc.getUnitVectorAtDegreeAngle(this)

inline fun Vector2f.rotateAroundOrigin(angle: Float) = Misc.rotateAroundOrigin(this, angle)

inline fun Vector2f.rotateAroundOrigin(angle: Float, origin: Vector2f) =
    Misc.rotateAroundOrigin(this, angle, origin)

/**
 * Angles.
 */
inline fun Float.isBetween(two: Float, check: Float) = Misc.isBetween(this, two, check)

inline fun CampaignFleetAPI.getShieldedCargoFraction() = Misc.getShieldedCargoFraction(this)

inline fun Color.interpolateColor(to: Color, progress: Float) = Misc.interpolateColor(this, to, progress)

inline fun Vector2f.interpolateVector(to: Vector2f, progress: Float) = Misc.interpolateVector(this, to, progress)

inline fun Float.interpolate(to: Float, progress: Float) = Misc.interpolate(this, to, progress)

inline fun Color.scaleColor(factor: Float) = Misc.scaleColor(this, factor)

inline fun Color.scaleColorOnly(factor: Float) = Misc.scaleColorOnly(this, factor)

inline fun Color.scaleAlpha(factor: Float) = Misc.scaleAlpha(this, factor)

inline fun Color.setAlpha(alpha: Int) = Misc.setAlpha(this, alpha)

inline fun ShipAPI.HullSize.getSizeNum() = Misc.getSizeNum(this)

inline fun MemoryAPI.unsetAll(prefix: String, memKey: String) = Misc.unsetAll(prefix, memKey, this)

inline fun CombatEntityAPI.getTargetingRadius(from: Vector2f, considerShield: Boolean) =
    Misc.getTargetingRadius(from, this, considerShield)

// getClosingSpeed skipped, doesn't make sense to convert.

inline fun Float.getWithDGS() = Misc.getWithDGS(this)

inline fun Float.getDGSCredits() = Misc.getDGSCredits(this)

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
 */
inline fun MemoryAPI.setFlagWithReason(flagKey: String, reason: String, value: Boolean, expire: Float) =
    Misc.setFlagWithReason(this, flagKey, reason, value, expire)

inline fun MemoryAPI.flagHasReason(flagKey: String, reason: String) = Misc.flagHasReason(this, flagKey, reason)

inline fun MemoryAPI.clearFlag(flagKey: String) = Misc.clearFlag(this, flagKey)

inline fun CampaignFleetAPI.makeLowRepImpact(reason: String) = Misc.makeLowRepImpact(this, reason)

inline fun CampaignFleetAPI.makeNoRepImpact(reason: String) = Misc.makeNoRepImpact(this, reason)

inline fun CampaignFleetAPI.makeHostile() = Misc.makeHostile(this)

inline fun CampaignFleetAPI.makeNotLowRepImpact(reason: String) = Misc.makeNotLowRepImpact(this, reason)

inline fun Long.getAgoStringForTimestamp() = Misc.getAgoStringForTimestamp(this)

inline fun Long.getDetailedAgoString() = Misc.getDetailedAgoString(this)

inline fun Int.getAtLeastStringForDays() = Misc.getAtLeastStringForDays(this)

inline fun Int.getStringForDays() = Misc.getStringForDays(this)

inline fun Float.getBurnLevelForSpeed() = Misc.getBurnLevelForSpeed(this)

inline fun Float.getFractionalBurnLevelForSpeed() = Misc.getFractionalBurnLevelForSpeed(this)

inline fun Float.getSpeedForBurnLevel() = Misc.getSpeedForBurnLevel(this)

inline fun CampaignFleetAPI.getFuelPerDay(burnLevel: Float) = Misc.getFuelPerDay(this, burnLevel)

inline fun CampaignFleetAPI.getFuelPerDayAtSpeed(speed: Float) = Misc.getFuelPerDayAtSpeed(this, speed)

inline fun CampaignFleetAPI.getLYPerDayAtBurn(burnLevel: Float) = Misc.getLYPerDayAtBurn(this, burnLevel)

inline fun CampaignFleetAPI.getLYPerDayAtSpeed(speed: Float) = Misc.getLYPerDayAtSpeed(this, speed)

inline fun Vector3f.getDistance(other: Vector3f) = Misc.getDistance(this, other)
inline fun Float.getAngleDiff(to: Float) = Misc.getAngleDiff(this, to)

// Skipping isInArc, doesn't seem to make sense for an extension.
// Skipping isInArc, doesn't seem to make sense for an extension.
// Skipping addNebulaFromPNG, doesn't seem to make sense for an extension.
// Skipping renderQuad, doesn't seem to make sense for an extension.

/**
 * Shortest distance from line to a point.
 */
inline fun Vector2f.distanceFromLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.distanceFromLineToPoint(lineStart, lineEnd, this)

inline fun Vector2f.closestPointOnLineToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnLineToPoint(lineStart, lineEnd, this)

inline fun Vector2f.closestPointOnSegmentToPoint(lineStart: Vector2f, lineEnd: Vector2f) =
    Misc.closestPointOnSegmentToPoint(lineStart, lineEnd, this)

inline fun Vector2f.isPointInBounds(bounds: List<Vector2f>) = Misc.isPointInBounds(this, bounds)

// Skipping intersectSegments, doesn't seem to make sense for an extension.
// Skipping intersectLines, doesn't seem to make sense for an extension.
// Skipping intersectSegmentAndCircle, doesn't seem to make sense for an extension.
// Skipping areSegmentsCoincident, doesn't seem to make sense for an extension.

inline fun Vector2f.getPerp() = Misc.getPerp(this)

// Skipping getClosestTurnDirection, doesn't seem to make sense for an extension.

inline fun Vector2f.getClosestTurnDirection(other: Vector2f) = Misc.getClosestTurnDirection(this, other)

inline fun Vector2f.getDiff(other: Vector2f) = Misc.getDiff(this, other)

inline fun CampaignFleetAPI.getSourceMarket() = Misc.getSourceMarket(this)

// Skipping getSpawnChanceMult, doesn't seem to make sense for an extension.
// Skipping pickHyperLocationNotNearPlayer, doesn't seem to make sense for an extension.
// Skipping pickLocationNotNearPlayer, doesn't seem to make sense for an extension.

inline fun Vector2f.wiggle(max: Float) = Misc.wiggle(this, max)

inline fun CampaignFleetAPI.isPlayerOrCombinedPlayerPrimary() = Misc.isPlayerOrCombinedPlayerPrimary(this)

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

inline fun StarSystemAPI?.hasPulsar() = Misc.hasPulsar(this)

inline fun CampaignFleetAPI.caresAboutPlayerTransponder() =
    Misc.caresAboutPlayerTransponder(this)

inline fun ShipAPI?.findClosestShipEnemyOf(
    locFromForSorting: Vector2f,
    smallestToNote: ShipAPI.HullSize,
    maxRange: Float,
    considerShipRadius: Boolean,
    filter: Misc.FindShipFilter? = null
) = Misc.findClosestShipEnemyOf(this, locFromForSorting, smallestToNote, maxRange, considerShipRadius, filter)

inline fun <T : Enum<T>> JSONObject.mapToEnum(
    key: String,
    enumType: Class<T>,
    defaultOption: T,
    required: Boolean = true
) =
    Misc.mapToEnum(this, key, enumType, defaultOption, required)

inline fun JSONObject.getColor(key: String) = Misc.getColor(this, key)
inline fun JSONObject.optColor(key: String, defaultValue: Color?) = Misc.optColor(this, key, defaultValue)

// Skipping normalizeNoise, doesn't seem to make sense for an extension.
// Skipping initNoise, doesn't seem to make sense for an extension.
// Skipping genFractalNoise, doesn't seem to make sense for an extension.
// Skipping fill, doesn't seem to make sense for an extension.
// Skipping computeAngleSpan, doesn't seem to make sense for an extension.
// Skipping computeAngleRadius, doesn't seem to make sense for an extension.
// Skipping approach, doesn't seem to make sense for an extension.

inline fun Buffer.cleanBuffer() = Misc.cleanBuffer(this)

inline fun CampaignFleetAPI.getFleetwideTotalStat(dynamicMemberStatId: String) =
    Misc.getFleetwideTotalStat(this, dynamicMemberStatId)

inline fun CampaignFleetAPI.getFleetwideTotalMod(dynamicMemberStatId: String, base: Float, ship: ShipAPI? = null) =
    Misc.getFleetwideTotalMod(this, dynamicMemberStatId, base, ship)

inline fun PlanetAPI.getStarId() = Misc.getStarId(this)

inline fun StarSystemAPI.getMinSystemSurveyLevel() = Misc.getMinSystemSurveyLevel(this)

inline fun StarSystemAPI.hasAnySurveyDataFor() = Misc.hasAnySurveyDataFor(this)

// Skipping setAllPlanetsKnown, doesn't seem to make sense for an extension.

inline fun StarSystemAPI.setAllPlanetsKnown() = Misc.setAllPlanetsKnown(this)

inline fun StarSystemAPI.setAllPlanetsSurveyed(setRuinsExplored: Boolean) =
    Misc.setAllPlanetsSurveyed(this, setRuinsExplored)

inline fun StarSystemAPI.generatePlanetConditions(age: StarAge) = Misc.generatePlanetConditions(this, age)

inline fun PlanetAPI.getEstimatedOrbitIndex() = Misc.getEstimatedOrbitIndex(this)

// Skipping getRandom, doesn't seem to make sense for an extension.

inline fun PlanetAPI.addSurveyDataFor(text: TextPanelAPI) = Misc.addSurveyDataFor(this, text)

inline fun MarketAPI.setFullySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setFullySurveyed(this, text, withNotification)

inline fun MarketAPI.setPreliminarySurveyed(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setPreliminarySurveyed(this, text, withNotification)

inline fun MarketAPI.setSeen(text: TextPanelAPI, withNotification: Boolean) =
    Misc.setSeen(this, text, withNotification)

inline fun String.getStringWithTokenReplacement(entity: SectorEntityToken, memoryMap: Map<String, MemoryAPI>?) =
    Misc.getStringWithTokenReplacement(this, entity, memoryMap)

// Skipping renderQuadAlpha, doesn't seem to make sense for an extension.

inline fun SectorEntityToken.fadeAndExpire(seconds: Float = 1f) =
    Misc.fadeAndExpire(this, seconds)

inline fun SectorEntityToken.fadeInOutAndExpire(`in`: Float, dur: Float, out: Float) =
    Misc.fadeInOutAndExpire(this, `in`, dur, out)

// Skipping addCargoPods, doesn't seem to make sense for an extension.

inline fun LocationAPI.addDebrisField(params: DebrisFieldTerrainPlugin.DebrisFieldParams, random: Random?) =
    Misc.addDebrisField(this, params, random)

inline fun FleetMemberAPI.isUnboardable() =
    Misc.isUnboardable(this)

inline fun ShipHullSpecAPI.isUnboardable() =
    Misc.isUnboardable(this)

inline fun FleetMemberAPI.isShipRecoverable(
    recoverer: CampaignFleetAPI?,
    own: Boolean,
    useOfficerRecovery: Boolean,
    chanceMult: Float
) = Misc.isShipRecoverable(this, recoverer, own, useOfficerRecovery, chanceMult)

inline fun SectorEntityToken.findNearestJumpPointTo() =
    Misc.findNearestJumpPointTo(this)

inline fun SectorEntityToken.findNearestJumpPointThatCouldBeExitedFrom() =
    Misc.findNearestJumpPointThatCouldBeExitedFrom(this)

inline fun SectorEntityToken.findNearestPlanetTo(requireGasGiant: Boolean, allowStars: Boolean) =
    Misc.findNearestPlanetTo(this, requireGasGiant, allowStars)

inline fun LocationAPI.shouldConvertFromStub(location: Vector2f) =
    Misc.shouldConvertFromStub(this, location)

inline fun List<Color>.colorsToString() = Misc.colorsToString(this)

inline fun String.colorsFromString() = Misc.colorsFromString(this)

inline fun PlanetAPI.getJumpPointTo() = Misc.getJumpPointTo(this)

inline fun SectorEntityToken.findNearestJumpPoint() = Misc.findNearestJumpPoint(this)

inline fun ShipHullSpecAPI.getDHullId() = Misc.getDHullId(this)

// Skipping getMod, doesn't seem to make sense for an extension.
// Skipping getDistanceFromArc, doesn't seem to make sense for an extension.

inline fun PlanetAPI.initConditionMarket() = Misc.initConditionMarket(this)

inline fun PlanetAPI.initEconomyMarket() = Misc.initEconomyMarket(this)

inline fun MarketAPI.SurveyLevel.getSurveyLevelString(withBrackets: Boolean) =
    Misc.getSurveyLevelString(this, withBrackets)

inline fun SectorEntityToken.setDefenderOverride(override: DefenderDataOverride) =
    Misc.setDefenderOverride(this, override)

inline fun SectorEntityToken.setSalvageSpecial(data: Any?) = Misc.setSalvageSpecial(this, data)

inline fun SectorEntityToken.setPrevSalvageSpecial(data: Any?) = Misc.setPrevSalvageSpecial(this, data)

inline fun SectorEntityToken.getSalvageSpecial() = Misc.getSalvageSpecial(this)

inline fun SectorEntityToken.getPrevSalvageSpecial() = Misc.getPrevSalvageSpecial(this)

inline fun SectorEntityToken.getSystemsInRange(exclude: Set<StarSystemAPI>, nonEmpty: Boolean, maxRange: Float) =
    Misc.getSystemsInRange(this, exclude, nonEmpty, maxRange)

inline fun StarSystemAPI.getPulsarInSystem() = Misc.getPulsarInSystem(this)

inline fun StarSystemAPI.systemHasPlanets() = Misc.systemHasPlanets(this)

inline fun ShipAPI.HullSize.getCampaignShipScaleMult() = Misc.getCampaignShipScaleMult(this)

// Skipping createStringPicker, doesn't seem to make sense for an extension.

inline fun SectorEntityToken.setWarningBeaconGlowColor(color: Color) = Misc.setWarningBeaconGlowColor(this, color)

inline fun SectorEntityToken.setWarningBeaconPingColor(color: Color) = Misc.setWarningBeaconPingColor(this, color)

inline fun SectorEntityToken.setWarningBeaconColors(color: Color, ping: Color) =
    Misc.setWarningBeaconColors(this, color, ping)

inline fun SectorEntityToken.getNearbyFleets(maxDist: Float) = Misc.getNearbyFleets(this, maxDist)

inline fun SectorEntityToken.getVisibleFleets(includeSensorContacts: Boolean) =
    Misc.getVisibleFleets(this, includeSensorContacts)

inline fun CargoAPI.isSameCargo(other: CargoAPI) = Misc.isSameCargo(this, other)

inline fun StarSystemAPI.getDistressJumpPoint() = Misc.getDistressJumpPoint(this)

inline fun CampaignFleetAPI.clearTarget(forgetTransponder: Boolean) = Misc.clearTarget(this, forgetTransponder)

inline fun CampaignFleetAPI.giveStandardReturnToSourceAssignments(withClear: Boolean = true) =
    Misc.giveStandardReturnToSourceAssignments(this, withClear)

inline fun CampaignFleetAPI.giveStandardReturnAssignments(where: SectorEntityToken, text: String, withClear: Boolean) =
    Misc.giveStandardReturnAssignments(this, where, text, withClear)

// Skipping adjustRep, doesn't seem to make sense for an extension.

inline fun CampaignFleetAPI.interruptAbilitiesWithTag(tag: String) = Misc.interruptAbilitiesWithTag(this, tag)

inline fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken) = Misc.getInterceptPoint(this, other)

// Could use a default param of maxSpeedFrom = getTravelSpeed() because Alex copy/pasted the method
// but this is safer in case one of them changes in the future.
inline fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken, maxSpeedFrom: Float) =
    Misc.getInterceptPoint(this, other, maxSpeedFrom)

// Skipping getListOfResources, doesn't seem to make sense for an extension.
// Skipping setColor, doesn't seem to make sense for an extension.

inline fun SectorEntityToken.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant() =
    Misc.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant(this)

inline fun SectorEntityToken.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

inline fun PersonAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

inline fun MemoryAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

inline fun MemoryAPI.isImportantForReason(reason: String) = Misc.isImportantForReason(this, reason)

inline fun SectorEntityToken.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

inline fun PersonAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

inline fun MemoryAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

inline fun MemoryAPI.cleanUpMissionMemory(prefix: String) = Misc.cleanUpMissionMemory(this, prefix)

// Skipping clearAreaAroundPlayer, doesn't seem to make sense for an extension.

inline fun SectorEntityToken.getSalvageSeed() = Misc.getSalvageSeed(this)

inline fun SectorEntityToken.getNameBasedSeed() = Misc.getNameBasedSeed(this)

inline fun CampaignFleetAPI.forgetAboutTransponder() = Misc.forgetAboutTransponder(this)

inline fun SectorEntityToken.setAbandonedStationMarket(marketId: String) =
    Misc.setAbandonedStationMarket(marketId, this)

inline fun CampaignFleetAPI.getDesiredMoveDir() = Misc.getDesiredMoveDir(this)

inline fun CampaignFleetAPI.isPermaKnowsWhoPlayerIs() = Misc.isPermaKnowsWhoPlayerIs(this)

inline fun MarketAPI.getImmigrationPlugin() = Misc.getImmigrationPlugin(this)

// Skipping getAICoreAdminPlugin, doesn't seem to make sense for an extension.
// Skipping getAICoreOfficerPlugin, doesn't seem to make sense for an extension.

inline fun MarketAPI.getAbandonMarketPlugin() = Misc.getAbandonMarketPlugin(this)

inline fun MarketAPI.getStabilizeMarketPlugin() = Misc.getStabilizeMarketPlugin(this)

inline fun CampaignFleetAPI.getInflater(params: Any) = Misc.getInflater(this, params)

inline fun MarketAPI.playerHasStorageAccess() = Misc.playerHasStorageAccess(this)

inline fun MarketAPI.getMarketSizeProgress() = Misc.getMarketSizeProgress(this)

inline fun MarketAPI.getStorageCostPerMonth() = Misc.getStorageCostPerMonth(this)

inline fun MarketAPI.getStorage() = Misc.getStorage(this)

inline fun MarketAPI.getLocalResources() = Misc.getLocalResources(this)

inline fun MarketAPI.getStorageCargo() = Misc.getStorageCargo(this)

inline fun MarketAPI.getLocalResourcesCargo() = Misc.getLocalResourcesCargo(this)

inline fun MarketAPI.getStorageTotalValue() = Misc.getStorageTotalValue(this)

inline fun MarketAPI.getStorageCargoValue() = Misc.getStorageCargoValue(this)

inline fun MarketAPI.getStorageShipValue() = Misc.getStorageShipValue(this)

/**
 * Returns true if it added anything to the tooltip.
 */
inline fun TooltipMakerAPI.addStorageInfo(
    color: Color,
    dark: Color,
    market: MarketAPI,
    includeLocalResources: Boolean,
    addSectionIfEmpty: Boolean
) = Misc.addStorageInfo(this, color, dark, market, includeLocalResources, addSectionIfEmpty)

inline fun String.getTokenReplaced(entity: SectorEntityToken) = Misc.getTokenReplaced(this, entity)

inline fun PersonAPI.getAdminSalary() = Misc.getAdminSalary(this)

inline fun PersonAPI.getOfficerSalary(mercenary: Boolean = Misc.isMercenary(this)) = Misc.getOfficerSalary(this)

/**
 * MagicLib: Call on a variant id.
 */
inline fun String.getHullIdForVariantId() = Misc.getHullIdForVariantId(this)

/**
 * MagicLib: Call on a variant id.
 */
inline fun String.getFPForVariantId() = Misc.getFPForVariantId(this)

/**
 * MagicLib: Call on fleet points.
 * MagicLib: Originally named getAdjustedStrength.
 */
inline fun Float.getAdjustedStrengthFromFp(market: MarketAPI) = Misc.getAdjustedStrength(this, market)

/**
 * MagicLib: Call on fleet points.
 */
inline fun Float.getAdjustedFP(market: MarketAPI) = Misc.getAdjustedFP(this, market)

inline fun MarketAPI.getShipQuality(factionId: String? = null) = Misc.getShipQuality(this, factionId)

inline fun MarketAPI.getShipPickMode(factionId: String? = null) = Misc.getShipPickMode(this, factionId)

inline fun CampaignFleetAPI.isBusy() = Misc.isBusy(this)

inline fun MarketAPI.getStationFleet() = Misc.getStationFleet(this)

inline fun SectorEntityToken.getStationFleet() = Misc.getStationFleet(this)

inline fun MarketAPI.getStationBaseFleet() = Misc.getStationBaseFleet(this)

inline fun SectorEntityToken.getStationBaseFleet() = Misc.getStationBaseFleet(this)

inline fun CampaignFleetAPI.getStationMarket() = Misc.getStationMarket(this)

inline fun MarketAPI.getStationIndustry() = Misc.getStationIndustry(this)

inline fun ShipVariantAPI.isActiveModule() = Misc.isActiveModule(this)

inline fun ShipAPI.isActiveModule() = Misc.isActiveModule(this)

// Skipping addCreditsMessage, doesn't seem to make sense for an extension.

inline fun JumpPointAPI.getSystemJumpPointHyperExitLocation() = Misc.getSystemJumpPointHyperExitLocation(this)

inline fun SectorEntityToken.isNear(hyperLoc: Vector2f) = Misc.isNear(this, hyperLoc)

/**
 * MagicLib: Call on a number in seconds.
 */
inline fun Float.getDays() = Misc.getDays(this)

// Skipping getProbabilityMult, doesn't seem to make sense for an extension.

inline fun SectorEntityToken.isHyperspaceAnchor() = Misc.isHyperspaceAnchor(this)

inline fun SectorEntityToken.getStarSystemForAnchor() = Misc.getStarSystemForAnchor(this)

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

inline fun CampaignFleetAPI.isPatrol() = Misc.isPatrol(this)

inline fun CampaignFleetAPI.isSmuggler() = Misc.isSmuggler(this)

inline fun CampaignFleetAPI.isTrader() = Misc.isTrader(this)

inline fun CampaignFleetAPI.isPirate() = Misc.isPirate(this)

inline fun CampaignFleetAPI.isScavenger() = Misc.isScavenger(this)

inline fun CampaignFleetAPI.isRaider() = Misc.isRaider(this)

inline fun CampaignFleetAPI.isWarFleet() = Misc.isWarFleet(this)

/**
 * pair.one can be null if a stand-alone, non-market station is being returned in pair.two.
 */
inline fun CampaignFleetAPI.getNearestStationInSupportRange() = Misc.getNearestStationInSupportRange(this)

inline fun CampaignFleetAPI.isStationInSupportRange(station: CampaignFleetAPI) =
    Misc.isStationInSupportRange(this, station)

inline fun FleetMemberAPI.getMemberStrength(
    withHull: Boolean = true,
    withQuality: Boolean = true,
    withCaptain: Boolean = true
) = Misc.getMemberStrength(this, withHull, withQuality, withCaptain)

inline fun MarketAPI.increaseMarketHostileTimeout(days: Float) = Misc.increaseMarketHostileTimeout(this, days)

inline fun MarketAPI.removeRadioChatter() = Misc.removeRadioChatter(this)

/**
 * MagicLib: Call on a design type.
 */
inline fun String.getDesignTypeColor() = Misc.getDesignTypeColor(this)

/**
 * MagicLib: Call on a design type.
 */
inline fun String.getDesignTypeColorDim() = Misc.getDesignTypeColorDim(this)

inline fun TooltipMakerAPI.addDesignTypePara(design: String, pad: Float) = Misc.addDesignTypePara(this, design, pad)

inline fun CampaignFleetAPI.getFleetRadiusTerrainEffectMult() = Misc.getFleetRadiusTerrainEffectMult(this)

inline fun CampaignFleetAPI.getBurnMultForTerrain() = Misc.getBurnMultForTerrain(this)

inline fun LocationAPI.addHitGlow(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    dur: Float = 1f + Math.random().toFloat(),
    color: Color
) = Misc.addHitGlow(this, loc, vel, size, dur, color)

inline fun LocationAPI.addGlowyParticle(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    rampUp: Float,
    dur: Float,
    color: Color
) = Misc.addGlowyParticle(this, loc, vel, size, rampUp, dur, color)

inline fun MarketAPI.getShippingCapacity(inFaction: Boolean) = Misc.getShippingCapacity(this, inFaction)

/**
 * MarketLib: getStrengthDesc
 */
inline fun Float.getStrengthDescForFP() = Misc.getStrengthDesc(this)

inline fun MarketAPI.isMilitary() = Misc.isMilitary(this)

inline fun MarketAPI.hasHeavyIndustry() = Misc.hasHeavyIndustry(this)

inline fun MarketAPI.hasOrbitalStation() = Misc.hasOrbitalStation(this)

inline fun SectorEntityToken.getClaimingFaction() = Misc.getClaimingFaction(this)

inline fun MarketAPI.computeTotalShutdownRefund() = Misc.computeTotalShutdownRefund(this)

inline fun MarketAPI.computeShutdownRefund(industry: Industry) = Misc.computeShutdownRefund(this, industry)

/**
 * MagicLib: Call on the center of the location, eg the star.
 */
inline fun SectorEntityToken.addWarningBeacon(gap: BaseThemeGenerator.OrbitGap, beaconTag: String) =
    Misc.addWarningBeacon(this, gap, beaconTag)

inline fun MemoryAPI.getTradeMode() = Misc.getTradeMode(this)

inline fun MarketAPI.getSpaceport() = Misc.getSpaceport(this)

inline fun Color.setBrightness(brightness: Int) = Misc.setBrightness(this, brightness)

inline fun Color.scaleColorSaturate(factor: Float) = Misc.scaleColorSaturate(this, factor)

inline fun CampaignFleetAPI.getMaxOfficers() = Misc.getMaxOfficers(this)

inline fun CampaignFleetAPI.getNumNonMercOfficers() = Misc.getNumNonMercOfficers(this)

inline fun CampaignFleetAPI.getMercs() = Misc.getMercs(this)

inline fun MarketAPI.getMaxIndustries() = Misc.getMaxIndustries(this)

inline fun MarketAPI.getNumIndustries() = Misc.getNumIndustries(this)

inline fun MarketAPI.getNumImprovedIndustries() = Misc.getNumImprovedIndustries(this)

inline fun StarSystemAPI.getNumStableLocations() = Misc.getNumStableLocations(this)

inline fun MarketAPI.getCurrentlyBeingConstructed() = Misc.getCurrentlyBeingConstructed(this)

inline fun Float.getRelColor() = Misc.getRelColor(this)

inline fun CampaignFleetAPI.getDangerLevel() = Misc.getDangerLevel(this)

// Skipping getHitGlowSize, doesn't seem to make sense for an extension.

inline fun PersonAPI.getNumEliteSkills() = Misc.getNumEliteSkills(this)

inline fun PersonAPI.isMentored() = Misc.isMentored(this)

inline fun PersonAPI.setMentored(mentored: Boolean) = Misc.setMentored(this, mentored)

inline fun PersonAPI.isMercenary() = Misc.isMercenary(this)

inline fun PersonAPI.setMercHiredNow() = Misc.setMercHiredNow(this)

inline fun PersonAPI.getMercDaysSinceHired() = Misc.getMercDaysSinceHired(this)

inline fun PersonAPI.setMercenary(mercenary: Boolean) = Misc.setMercenary(this, mercenary)

inline fun PersonAPI.isUnremovable() = Misc.isUnremovable(this)

inline fun PersonAPI.setUnremovable(unremovable: Boolean) = Misc.setUnremovable(this, unremovable)

inline fun MutableShipStatsAPI.isAutomated() = Misc.isAutomated(this)

inline fun FleetMemberAPI.isAutomated() = Misc.isAutomated(this)

inline fun ShipVariantAPI.isAutomated() = Misc.isAutomated(this)

inline fun ShipAPI.isAutomated() = Misc.isAutomated(this)

inline fun ShipAPI.getMaxPermanentMods() = Misc.getMaxPermanentMods(this)

inline fun FleetMemberAPI.getMaxPermanentMods(stats: MutableCharacterStatsAPI) = Misc.getMaxPermanentMods(this, stats)

inline fun HullModSpecAPI.getBuildInBonusXP(size: ShipAPI.HullSize) = Misc.getBuildInBonusXP(this, size)

inline fun HullModSpecAPI.getOPCost(size: ShipAPI.HullSize) = Misc.getOPCost(this, size)

inline fun ShipVariantAPI.isSpecialMod(spec: HullModSpecAPI) = Misc.isSpecialMod(this, spec)

inline fun ShipVariantAPI.getCurrSpecialMods() = Misc.getCurrSpecialMods(this)

inline fun ShipVariantAPI.getCurrSpecialModsList() = Misc.getCurrSpecialModsList(this)

inline fun CampaignFleetAPI.isSlowMoving() = Misc.isSlowMoving(this)

inline fun CampaignFleetAPI.getGoSlowBurnLevel() = Misc.getGoSlowBurnLevel(this)

inline fun FleetMemberAPI.applyDamage(
    random: Random, level: Misc.FleetMemberDamageLevel,
    withCRDamage: Boolean, crDamageId: String, crDamageReason: String,
    withMessage: Boolean, textPanel: TextPanelAPI,
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

inline fun FleetMemberAPI.applyDamage(
    random: Random, damageMult: Float,
    withCRDamage: Boolean, crDamageId: String, crDamageReason: String,
    withMessage: Boolean, textPanel: TextPanelAPI,
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

inline fun FleetMemberAPI.getBonusXPForRecovering() = Misc.getBonusXPForRecovering(this)

inline fun FleetMemberAPI.getBonusXPForScuttling() = Misc.getBonusXPForScuttling(this)

inline fun CampaignFleetAPI.getSpawnFPMult() = Misc.getSpawnFPMult(this)

inline fun CampaignFleetAPI.setSpawnFPMult(mult: Float) = Misc.setSpawnFPMult(this, mult)

inline fun FactionAPI.isDecentralized() = Misc.isDecentralized(this)

inline fun PersonAPI.getPersonalityName() = Misc.getPersonalityName(this)

inline fun MarketAPI.setRaidedTimestamp() = Misc.setRaidedTimestamp(this)

inline fun MarketAPI.getDaysSinceLastRaided() = Misc.getDaysSinceLastRaided(this)

inline fun CommodityOnMarketAPI.computeEconUnitChangeFromTradeModChange(quantity: Int) =
    Misc.computeEconUnitChangeFromTradeModChange(this, quantity)

inline fun CommodityOnMarketAPI.affectAvailabilityWithinReason(quantity: Int) =
    Misc.affectAvailabilityWithinReason(this, quantity)

inline fun StarSystemAPI.isOpenlyPopulated() = Misc.isOpenlyPopulated(this)

inline fun Collection<String>.hasAtLeastOneOfTags(vararg other: String) = Misc.hasAtLeastOneOfTags(this, *other)

inline fun MarketAPI.hasUnexploredRuins() = Misc.hasUnexploredRuins(this)

inline fun MarketAPI.hasRuins() = Misc.hasRuins(this)

inline fun MarketAPI.hasFarmland() = Misc.hasFarmland(this)

inline fun CampaignFleetAPI.addDefeatTrigger(trigger: String) = Misc.addDefeatTrigger(this, trigger)

inline fun CampaignFleetAPI.removeDefeatTrigger(trigger: String) = Misc.removeDefeatTrigger(this, trigger)

inline fun CampaignFleetAPI.getDefeatTriggers(createIfNecessary: Boolean) =
    Misc.getDefeatTriggers(this, createIfNecessary)

inline fun CampaignFleetAPI.clearDefeatTriggersIfNeeded() = Misc.clearDefeatTriggersIfNeeded(this)

inline fun ShipAPI.shouldShowDamageFloaty(target: ShipAPI) = Misc.shouldShowDamageFloaty(this, target)

// Skipping playSound, doesn't seem to make sense for an extension.

inline fun ShipAPI.getShipWeight(adjustForNonCombat: Boolean = true) = Misc.getShipWeight(this, adjustForNonCombat)

inline fun ShipAPI.getIncapacitatedTime() = Misc.getIncapacitatedTime(this)

inline fun CampaignFleetAPI.isAvoidingPlayerHalfheartedly() = Misc.isAvoidingPlayerHalfheartedly(this)

/**
 * In vanilla, pirates and Luddic Path.
 */
inline fun FactionAPI.isPirateFaction() = Misc.isPirateFaction(this)

/**
 * Probably wrong sometimes...
 *
 * MagicLib: originally called getAOrAnFor.
 *
 * @return "a" or "an" for word.
 */
inline fun String.getAOrAnForWord() = Misc.getAOrAnFor(this)

inline fun PersonAPI.moveToMarket(destination: MarketAPI, alwaysAddToCommDirectory: Boolean) =
    Misc.moveToMarket(this, destination, alwaysAddToCommDirectory)

inline fun MarketAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

inline fun MemoryAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

inline fun MarketAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

inline fun MemoryAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

inline fun MarketAPI.isStoryCritical() = Misc.isStoryCritical(this)

inline fun MemoryAPI.isStoryCritical() = Misc.isStoryCritical(this)

/**
 * Whether it prevents salvage, surveying, etc. But NOT things that require only being
 * seen to ruin them, such as SpySat deployments.
 * @param fleet
 * @return
 */
inline fun CampaignFleetAPI.isInsignificant() = Misc.isInsignificant(this)

/**
 * Mainly for avoiding stuff like "pirate fleet with 4 rustbuckets will run away from the player's
 * 4 regular-quality frigates". Fleets that this evaluates to true for will avoid the player slowly.
 * @param fleet
 * @return
 */
inline fun CampaignFleetAPI.shouldNotWantRunFromPlayerEvenIfWeaker() = Misc.shouldNotWantRunFromPlayerEvenIfWeaker(this)

inline fun FloatArray.findKth(k: Int) = Misc.findKth(this, k)

inline fun Float.getAdjustedBaseRange(ship: ShipAPI, weapon: WeaponAPI) = Misc.getAdjustedBaseRange(this, ship, weapon)

inline fun Vector2f.bezier(p1: Vector2f, p2: Vector2f, t: Float) = Misc.bezier(this, p1, p2, t)

inline fun Vector2f.bezierCubic(p1: Vector2f, p2: Vector2f, p3: Vector2f, t: Float) =
    Misc.bezierCubic(this, p1, p2, p3, t)

inline fun Vector2f.isInsideSlipstream(radius: Float, location: LocationAPI = Global.getSector().hyperspace) =
    Misc.isInsideSlipstream(this, radius, location)

inline fun SectorEntityToken.isInsideSlipstream() = Misc.isInsideSlipstream(this)

inline fun Vector2f.isOutsideSector() = Misc.isOutsideSector(this)

inline fun LocationAPI.crossesAnySlipstream(from: Vector2f, to: Vector2f) = Misc.crossesAnySlipstream(this, from, to)