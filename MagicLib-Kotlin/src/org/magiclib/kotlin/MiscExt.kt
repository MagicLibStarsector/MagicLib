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
 * MagicLib: Normalizes an angle given in degrees.
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
 * MagicLib: Called on an angle given in degrees.
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
fun MemoryAPI.setFlagWithReason(flagKey: String, reason: String, value: Boolean, expire: Float) =
    Misc.setFlagWithReason(this, flagKey, reason, value, expire)

fun MemoryAPI.flagHasReason(flagKey: String, reason: String) = Misc.flagHasReason(this, flagKey, reason)

fun MemoryAPI.clearFlag(flagKey: String) = Misc.clearFlag(this, flagKey)

fun CampaignFleetAPI.makeLowRepImpact(reason: String) = Misc.makeLowRepImpact(this, reason)

fun CampaignFleetAPI.makeNoRepImpact(reason: String) = Misc.makeNoRepImpact(this, reason)

fun CampaignFleetAPI.makeHostile() = Misc.makeHostile(this)

fun CampaignFleetAPI.makeNotLowRepImpact(reason: String) = Misc.makeNotLowRepImpact(this, reason)

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
 * MagicLib: Call on an asteroid.
 */
fun SectorEntityToken.getAsteroidSource() = Misc.getAsteroidSource(this)

/**
 * MagicLib: Call on an asteroid.
 */
fun SectorEntityToken.setAsteroidSource(source: AsteroidSource?) =
    Misc.setAsteroidSource(this, source)

/**
 * MagicLib: Call on an asteroid.
 */
fun SectorEntityToken.clearAsteroidSource() = Misc.clearAsteroidSource(this)

/**
 * MagicLib: Call on a star.
 */
fun PlanetAPI.getCoronaFor() = Misc.getCoronaFor(this)

/**
 * MagicLib: Call on a star.
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

fun CampaignFleetAPI.interruptAbilitiesWithTag(tag: String) = Misc.interruptAbilitiesWithTag(this, tag)

fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken) = Misc.getInterceptPoint(this, other)

// Could use a default param of maxSpeedFrom = getTravelSpeed() because Alex copy/pasted the method
// but this is safer in case one of them changes in the future.
fun CampaignFleetAPI.getInterceptPoint(other: SectorEntityToken, maxSpeedFrom: Float) =
    Misc.getInterceptPoint(this, other, maxSpeedFrom)

// Skipping getListOfResources, doesn't seem to make sense for an extension.
// Skipping setColor, doesn't seem to make sense for an extension.

fun SectorEntityToken.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant() =
    Misc.doesMarketHaveMissionImportantPeopleOrIsMarketMissionImportant(this)

fun SectorEntityToken.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

fun PersonAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

fun MemoryAPI.makeImportant(reason: String, dur: Float = -1f) = Misc.makeImportant(this, reason, dur)

fun MemoryAPI.isImportantForReason(reason: String) = Misc.isImportantForReason(this, reason)

fun SectorEntityToken.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

fun PersonAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

fun MemoryAPI.makeUnimportant(reason: String) = Misc.makeUnimportant(this, reason)

fun MemoryAPI.cleanUpMissionMemory(prefix: String) = Misc.cleanUpMissionMemory(this, prefix)

// Skipping clearAreaAroundPlayer, doesn't seem to make sense for an extension.

fun SectorEntityToken.getSalvageSeed() = Misc.getSalvageSeed(this)

fun SectorEntityToken.getNameBasedSeed() = Misc.getNameBasedSeed(this)

fun CampaignFleetAPI.forgetAboutTransponder() = Misc.forgetAboutTransponder(this)

fun SectorEntityToken.setAbandonedStationMarket(marketId: String) = Misc.setAbandonedStationMarket(marketId, this)

fun CampaignFleetAPI.getDesiredMoveDir() = Misc.getDesiredMoveDir(this)

fun CampaignFleetAPI.isPermaKnowsWhoPlayerIs() = Misc.isPermaKnowsWhoPlayerIs(this)

fun MarketAPI.getImmigrationPlugin() = Misc.getImmigrationPlugin(this)

// Skipping getAICoreAdminPlugin, doesn't seem to make sense for an extension.
// Skipping getAICoreOfficerPlugin, doesn't seem to make sense for an extension.

fun MarketAPI.getAbandonMarketPlugin() = Misc.getAbandonMarketPlugin(this)

fun MarketAPI.getStabilizeMarketPlugin() = Misc.getStabilizeMarketPlugin(this)

fun CampaignFleetAPI.getInflater(params: Any) = Misc.getInflater(this, params)

fun MarketAPI.playerHasStorageAccess() = Misc.playerHasStorageAccess(this)

fun MarketAPI.getMarketSizeProgress() = Misc.getMarketSizeProgress(this)

fun MarketAPI.getStorageCostPerMonth() = Misc.getStorageCostPerMonth(this)

fun MarketAPI.getStorage() = Misc.getStorage(this)

fun MarketAPI.getLocalResources() = Misc.getLocalResources(this)

fun MarketAPI.getStorageCargo() = Misc.getStorageCargo(this)

fun MarketAPI.getLocalResourcesCargo() = Misc.getLocalResourcesCargo(this)

fun MarketAPI.getStorageTotalValue() = Misc.getStorageTotalValue(this)

fun MarketAPI.getStorageCargoValue() = Misc.getStorageCargoValue(this)

fun MarketAPI.getStorageShipValue() = Misc.getStorageShipValue(this)

/**
 * Returns true if it added anything to the tooltip.
 */
fun TooltipMakerAPI.addStorageInfo(
    color: Color,
    dark: Color,
    market: MarketAPI,
    includeLocalResources: Boolean,
    addSectionIfEmpty: Boolean
) = Misc.addStorageInfo(this, color, dark, market, includeLocalResources, addSectionIfEmpty)

fun String.getTokenReplaced(entity: SectorEntityToken) = Misc.getTokenReplaced(this, entity)

fun PersonAPI.getAdminSalary() = Misc.getAdminSalary(this)

fun PersonAPI.getOfficerSalary(mercenary: Boolean = Misc.isMercenary(this)) = Misc.getOfficerSalary(this)

/**
 * MagicLib: Call on a variant id.
 */
fun String.getHullIdForVariantId() = Misc.getHullIdForVariantId(this)

/**
 * MagicLib: Call on a variant id.
 */
fun String.getFPForVariantId() = Misc.getFPForVariantId(this)

/**
 * MagicLib: Call on fleet points.
 * MagicLib: Originally named getAdjustedStrength.
 */
fun Float.getAdjustedStrengthFromFp(market: MarketAPI) = Misc.getAdjustedStrength(this, market)

/**
 * MagicLib: Call on fleet points.
 */
fun Float.getAdjustedFP(market: MarketAPI) = Misc.getAdjustedFP(this, market)

fun MarketAPI.getShipQuality(factionId: String? = null) = Misc.getShipQuality(this, factionId)

fun MarketAPI.getShipPickMode(factionId: String? = null) = Misc.getShipPickMode(this, factionId)

fun CampaignFleetAPI.isBusy() = Misc.isBusy(this)

fun MarketAPI.getStationFleet() = Misc.getStationFleet(this)

fun SectorEntityToken.getStationFleet() = Misc.getStationFleet(this)

fun MarketAPI.getStationBaseFleet() = Misc.getStationBaseFleet(this)

fun SectorEntityToken.getStationBaseFleet() = Misc.getStationBaseFleet(this)

fun CampaignFleetAPI.getStationMarket() = Misc.getStationMarket(this)

fun MarketAPI.getStationIndustry() = Misc.getStationIndustry(this)

fun ShipVariantAPI.isActiveModule() = Misc.isActiveModule(this)

fun ShipAPI.isActiveModule() = Misc.isActiveModule(this)

// Skipping addCreditsMessage, doesn't seem to make sense for an extension.

fun JumpPointAPI.getSystemJumpPointHyperExitLocation() = Misc.getSystemJumpPointHyperExitLocation(this)

fun SectorEntityToken.isNear(hyperLoc: Vector2f) = Misc.isNear(this, hyperLoc)

/**
 * MagicLib: Call on a number in seconds.
 */
fun Float.getDays() = Misc.getDays(this)

// Skipping getProbabilityMult, doesn't seem to make sense for an extension.

fun SectorEntityToken.isHyperspaceAnchor() = Misc.isHyperspaceAnchor(this)

fun SectorEntityToken.getStarSystemForAnchor() = Misc.getStarSystemForAnchor(this)

fun TextPanelAPI.showCost(
    title: String = "Resources: consumed (available)",
    withAvailable: Boolean = true,
    widthOverride: Float = -1f,
    color: Color,
    dark: Color,
    res: Array<String>,
    quantities: IntArray,
    consumed: BooleanArray? = null
) = Misc.showCost(this, title, withAvailable, widthOverride, color, dark, res, quantities, consumed)

fun CampaignFleetAPI.isPatrol() = Misc.isPatrol(this)

fun CampaignFleetAPI.isSmuggler() = Misc.isSmuggler(this)

fun CampaignFleetAPI.isTrader() = Misc.isTrader(this)

fun CampaignFleetAPI.isPirate() = Misc.isPirate(this)

fun CampaignFleetAPI.isScavenger() = Misc.isScavenger(this)

fun CampaignFleetAPI.isRaider() = Misc.isRaider(this)

fun CampaignFleetAPI.isWarFleet() = Misc.isWarFleet(this)

/**
 * pair.one can be null if a stand-alone, non-market station is being returned in pair.two.
 */
fun CampaignFleetAPI.getNearestStationInSupportRange() = Misc.getNearestStationInSupportRange(this)

fun CampaignFleetAPI.isStationInSupportRange(station: CampaignFleetAPI) = Misc.isStationInSupportRange(this, station)

fun FleetMemberAPI.getMemberStrength(
    withHull: Boolean = true,
    withQuality: Boolean = true,
    withCaptain: Boolean = true
) = Misc.getMemberStrength(this, withHull, withQuality, withCaptain)

fun MarketAPI.increaseMarketHostileTimeout(days: Float) = Misc.increaseMarketHostileTimeout(this, days)

fun MarketAPI.removeRadioChatter() = Misc.removeRadioChatter(this)

/**
 * MagicLib: Call on a design type.
 */
fun String.getDesignTypeColor() = Misc.getDesignTypeColor(this)

/**
 * MagicLib: Call on a design type.
 */
fun String.getDesignTypeColorDim() = Misc.getDesignTypeColorDim(this)

fun TooltipMakerAPI.addDesignTypePara(design: String, pad: Float) = Misc.addDesignTypePara(this, design, pad)

fun CampaignFleetAPI.getFleetRadiusTerrainEffectMult() = Misc.getFleetRadiusTerrainEffectMult(this)

fun CampaignFleetAPI.getBurnMultForTerrain() = Misc.getBurnMultForTerrain(this)

fun LocationAPI.addHitGlow(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    dur: Float = 1f + Math.random().toFloat(),
    color: Color
) = Misc.addHitGlow(this, loc, vel, size, dur, color)

fun LocationAPI.addGlowyParticle(
    loc: Vector2f,
    vel: Vector2f,
    size: Float,
    rampUp: Float,
    dur: Float,
    color: Color
) = Misc.addGlowyParticle(this, loc, vel, size, rampUp, dur, color)

fun MarketAPI.getShippingCapacity(inFaction: Boolean) = Misc.getShippingCapacity(this, inFaction)

/**
 * MarketLib: getStrengthDesc
 */
fun Float.getStrengthDescForFP() = Misc.getStrengthDesc(this)

fun MarketAPI.isMilitary() = Misc.isMilitary(this)

fun MarketAPI.hasHeavyIndustry() = Misc.hasHeavyIndustry(this)

fun MarketAPI.hasOrbitalStation() = Misc.hasOrbitalStation(this)

fun SectorEntityToken.getClaimingFaction() = Misc.getClaimingFaction(this)

fun MarketAPI.computeTotalShutdownRefund() = Misc.computeTotalShutdownRefund(this)

fun MarketAPI.computeShutdownRefund(industry: Industry) = Misc.computeShutdownRefund(this, industry)

/**
 * MagicLib: Call on the center of the location, eg the star.
 */
fun SectorEntityToken.addWarningBeacon(gap: BaseThemeGenerator.OrbitGap, beaconTag: String) =
    Misc.addWarningBeacon(this, gap, beaconTag)

fun MemoryAPI.getTradeMode() = Misc.getTradeMode(this)

fun MarketAPI.getSpaceport() = Misc.getSpaceport(this)

fun Color.setBrightness(brightness: Int) = Misc.setBrightness(this, brightness)

fun Color.scaleColorSaturate(factor: Float) = Misc.scaleColorSaturate(this, factor)

fun CampaignFleetAPI.getMaxOfficers() = Misc.getMaxOfficers(this)

fun CampaignFleetAPI.getNumNonMercOfficers() = Misc.getNumNonMercOfficers(this)

fun CampaignFleetAPI.getMercs() = Misc.getMercs(this)

fun MarketAPI.getMaxIndustries() = Misc.getMaxIndustries(this)

fun MarketAPI.getNumIndustries() = Misc.getNumIndustries(this)

fun MarketAPI.getNumImprovedIndustries() = Misc.getNumImprovedIndustries(this)

fun StarSystemAPI.getNumStableLocations() = Misc.getNumStableLocations(this)

fun MarketAPI.getCurrentlyBeingConstructed() = Misc.getCurrentlyBeingConstructed(this)

fun Float.getRelColor() = Misc.getRelColor(this)

fun CampaignFleetAPI.getDangerLevel() = Misc.getDangerLevel(this)

// Skipping getHitGlowSize, doesn't seem to make sense for an extension.

fun PersonAPI.getNumEliteSkills() = Misc.getNumEliteSkills(this)

fun PersonAPI.isMentored() = Misc.isMentored(this)

fun PersonAPI.setMentored(mentored: Boolean) = Misc.setMentored(this, mentored)

fun PersonAPI.isMercenary() = Misc.isMercenary(this)

fun PersonAPI.setMercHiredNow() = Misc.setMercHiredNow(this)

fun PersonAPI.getMercDaysSinceHired() = Misc.getMercDaysSinceHired(this)

fun PersonAPI.setMercenary(mercenary: Boolean) = Misc.setMercenary(this, mercenary)

fun PersonAPI.isUnremovable() = Misc.isUnremovable(this)

fun PersonAPI.setUnremovable(unremovable: Boolean) = Misc.setUnremovable(this, unremovable)

fun MutableShipStatsAPI.isAutomated() = Misc.isAutomated(this)

fun FleetMemberAPI.isAutomated() = Misc.isAutomated(this)

fun ShipVariantAPI.isAutomated() = Misc.isAutomated(this)

fun ShipAPI.isAutomated() = Misc.isAutomated(this)

fun ShipAPI.getMaxPermanentMods() = Misc.getMaxPermanentMods(this)

fun FleetMemberAPI.getMaxPermanentMods(stats: MutableCharacterStatsAPI) = Misc.getMaxPermanentMods(this, stats)

fun HullModSpecAPI.getBuildInBonusXP(size: ShipAPI.HullSize) = Misc.getBuildInBonusXP(this, size)

fun HullModSpecAPI.getOPCost(size: ShipAPI.HullSize) = Misc.getOPCost(this, size)

fun ShipVariantAPI.isSpecialMod(spec: HullModSpecAPI) = Misc.isSpecialMod(this, spec)

fun ShipVariantAPI.getCurrSpecialMods() = Misc.getCurrSpecialMods(this)

fun ShipVariantAPI.getCurrSpecialModsList() = Misc.getCurrSpecialModsList(this)

fun CampaignFleetAPI.isSlowMoving() = Misc.isSlowMoving(this)

fun CampaignFleetAPI.getGoSlowBurnLevel() = Misc.getGoSlowBurnLevel(this)

fun FleetMemberAPI.applyDamage(
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

fun FleetMemberAPI.applyDamage(
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

fun FleetMemberAPI.getBonusXPForRecovering() = Misc.getBonusXPForRecovering(this)

fun FleetMemberAPI.getBonusXPForScuttling() = Misc.getBonusXPForScuttling(this)

fun CampaignFleetAPI.getSpawnFPMult() = Misc.getSpawnFPMult(this)

fun CampaignFleetAPI.setSpawnFPMult(mult: Float) = Misc.setSpawnFPMult(this, mult)

fun FactionAPI.isDecentralized() = Misc.isDecentralized(this)

fun PersonAPI.getPersonalityName() = Misc.getPersonalityName(this)

fun MarketAPI.setRaidedTimestamp() = Misc.setRaidedTimestamp(this)

fun MarketAPI.getDaysSinceLastRaided() = Misc.getDaysSinceLastRaided(this)

fun CommodityOnMarketAPI.computeEconUnitChangeFromTradeModChange(quantity: Int) =
    Misc.computeEconUnitChangeFromTradeModChange(this, quantity)

fun CommodityOnMarketAPI.affectAvailabilityWithinReason(quantity: Int) =
    Misc.affectAvailabilityWithinReason(this, quantity)

fun StarSystemAPI.isOpenlyPopulated() = Misc.isOpenlyPopulated(this)

fun Collection<String>.hasAtLeastOneOfTags(vararg other: String) = Misc.hasAtLeastOneOfTags(this, *other)

fun MarketAPI.hasUnexploredRuins() = Misc.hasUnexploredRuins(this)

fun MarketAPI.hasRuins() = Misc.hasRuins(this)

fun MarketAPI.hasFarmland() = Misc.hasFarmland(this)

fun CampaignFleetAPI.addDefeatTrigger(trigger: String) = Misc.addDefeatTrigger(this, trigger)

fun CampaignFleetAPI.removeDefeatTrigger(trigger: String) = Misc.removeDefeatTrigger(this, trigger)

fun CampaignFleetAPI.getDefeatTriggers(createIfNecessary: Boolean) = Misc.getDefeatTriggers(this, createIfNecessary)

fun CampaignFleetAPI.clearDefeatTriggersIfNeeded() = Misc.clearDefeatTriggersIfNeeded(this)

fun ShipAPI.shouldShowDamageFloaty(target: ShipAPI) = Misc.shouldShowDamageFloaty(this, target)

// Skipping playSound, doesn't seem to make sense for an extension.

fun ShipAPI.getShipWeight(adjustForNonCombat: Boolean = true) = Misc.getShipWeight(this, adjustForNonCombat)

fun ShipAPI.getIncapacitatedTime() = Misc.getIncapacitatedTime(this)

fun CampaignFleetAPI.isAvoidingPlayerHalfheartedly() = Misc.isAvoidingPlayerHalfheartedly(this)

/**
 * In vanilla, pirates and Luddic Path.
 */
fun FactionAPI.isPirateFaction() = Misc.isPirateFaction(this)

/**
 * Probably wrong sometimes...
 *
 * MagicLib: originally called getAOrAnFor.
 *
 * @return "a" or "an" for word.
 */
fun String.getAOrAnForWord() = Misc.getAOrAnFor(this)

fun PersonAPI.moveToMarket(destination: MarketAPI, alwaysAddToCommDirectory: Boolean) =
    Misc.moveToMarket(this, destination, alwaysAddToCommDirectory)

fun MarketAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

fun MemoryAPI.makeStoryCritical(reason: String) = Misc.makeStoryCritical(this, reason)

fun MarketAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

fun MemoryAPI.makeNonStoryCritical(reason: String) = Misc.makeNonStoryCritical(this, reason)

fun MarketAPI.isStoryCritical() = Misc.isStoryCritical(this)

fun MemoryAPI.isStoryCritical() = Misc.isStoryCritical(this)

/**
 * Whether it prevents salvage, surveying, etc. But NOT things that require only being
 * seen to ruin them, such as SpySat deployments.
 * @param fleet
 * @return
 */
fun CampaignFleetAPI.isInsignificant() = Misc.isInsignificant(this)

/**
 * Mainly for avoiding stuff like "pirate fleet with 4 rustbuckets will run away from the player's
 * 4 regular-quality frigates". Fleets that this evaluates to true for will avoid the player slowly.
 * @param fleet
 * @return
 */
fun CampaignFleetAPI.shouldNotWantRunFromPlayerEvenIfWeaker() = Misc.shouldNotWantRunFromPlayerEvenIfWeaker(this)

fun FloatArray.findKth(k: Int) = Misc.findKth(this, k)

fun Float.getAdjustedBaseRange(ship: ShipAPI, weapon: WeaponAPI) = Misc.getAdjustedBaseRange(this, ship, weapon)

fun Vector2f.bezier(p1: Vector2f, p2: Vector2f, t: Float) = Misc.bezier(this, p1, p2, t)

fun Vector2f.bezierCubic(p1: Vector2f, p2: Vector2f, p3: Vector2f, t: Float) = Misc.bezierCubic(this, p1, p2, p3, t)

fun Vector2f.isInsideSlipstream(radius: Float, location: LocationAPI = Global.getSector().hyperspace) =
    Misc.isInsideSlipstream(this, radius, location)

fun SectorEntityToken.isInsideSlipstream() = Misc.isInsideSlipstream(this)

fun Vector2f.isOutsideSector() = Misc.isOutsideSector(this)

fun LocationAPI.crossesAnySlipstream(from: Vector2f, to: Vector2f) = Misc.crossesAnySlipstream(this, from, to)