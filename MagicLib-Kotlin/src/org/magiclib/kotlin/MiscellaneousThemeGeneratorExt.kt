package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import java.util.*

/**
 * Vanilla method: [com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator.getRemnantStations]
 * @since 1.3.0
 */
fun SectorAPI.getRemnantStations(includeDamaged: Boolean, onlyDamaged: Boolean) =
    MiscellaneousThemeGenerator.getRemnantStations(includeDamaged, onlyDamaged)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.addSalvageEntity]
 * @since 1.3.0
 */
fun LocationAPI.addSalvageEntity(
    random: Random? = null,
    id: String,
    faction: String,
    pluginParams: Any? = null
) = MiscellaneousThemeGenerator.addSalvageEntity(random, this, id, faction, pluginParams)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.setEntityLocation]
 * @since 1.3.0
 */
fun SectorEntityToken.setEntityLocation(loc: EntityLocation, type: String) =
    MiscellaneousThemeGenerator.setEntityLocation(this, loc, type)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.addEntity]
 * @since 1.3.0
 */
fun LocationAPI.addEntity(
    random: Random? = null,
    loc: EntityLocation,
    type: String,
    faction: String
) = MiscellaneousThemeGenerator.addEntity(random, this, loc, type, faction)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.addEntityAutoDetermineType]
 * @since 1.3.0
 */
fun LocationAPI.addEntityAutoDetermineType(
    random: Random? = null,
    loc: EntityLocation,
    type: String,
    faction: String
) = MiscellaneousThemeGenerator.addEntityAutoDetermineType(random, this, loc, type, faction)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.addNonSalvageEntity]
 * @since 1.3.0
 */
fun LocationAPI.addNonSalvageEntity(
    loc: EntityLocation,
    type: String,
    faction: String
) = MiscellaneousThemeGenerator.addNonSalvageEntity(this, loc, type, faction)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.addEntity]
 * @since 1.3.0
 */
fun StarSystemAPI.addEntity(
    random: Random? = null,
    locs: WeightedRandomPicker<EntityLocation>,
    type: String,
    faction: String
) = MiscellaneousThemeGenerator.addEntity(random, this, locs, type, faction)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.getOrbitalRadius]
 * @since 1.3.0
 */
fun SectorEntityToken.getOrbitalRadius() = MiscellaneousThemeGenerator.getOrbitalRadius(this)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.isAreaEmpty]
 * @since 1.3.0
 */
fun LocationAPI.isAreaEmpty(coords: Vector2f) = MiscellaneousThemeGenerator.isAreaEmpty(this, coords)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.getOuterRadius]
 * @since 1.3.0
 */
fun StarSystemAPI.getOuterRadius(): Float = MiscellaneousThemeGenerator.getOuterRadius(this)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickOuterEntityToSpawnNear]
 * @since 1.3.0
 */
fun StarSystemAPI.pickOuterEntityToSpawnNear(random: Random? = null) =
    MiscellaneousThemeGenerator.pickOuterEntityToSpawnNear(random, this)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.getInnerRadius]
 * @since 1.3.0
 */
fun StarSystemAPI.getInnerRadius() = MiscellaneousThemeGenerator.getInnerRadius(this)

/**
 * Call on the center entity.
 * Vanilla method: [com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.findGaps]
 *
 * @since 1.3.0
 */
fun SectorEntityToken.findGaps(minPad: Float, maxDist: Float, minGap: Float) = MiscellaneousThemeGenerator.findGaps(
    this,
    minPad,
    maxDist,
    minGap
)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.createLocationAtRandomGap]
 * @since 1.3.0
 */
fun SectorEntityToken.createLocationAtRandomGap(
    random: Random? = null,
    minGap: Float
) = MiscellaneousThemeGenerator.createLocationAtRandomGap(random, this, minGap)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.getLocations]
 * @since 1.3.0
 */
fun StarSystemAPI.getLocations(
    random: Random? = null,
    exclude: Set<SectorEntityToken>? = null,
    minGap: Float,
    weights: LinkedHashMap<BaseThemeGenerator.LocationType, Float>
) = MiscellaneousThemeGenerator.getLocations(random, this, exclude, minGap, weights)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickHiddenLocationNotNearStar]
 * @since 1.3.0
 */
fun StarSystemAPI.pickHiddenLocationNotNearStar(
    random: Random? = null,
    gap: Float,
    exclude: Set<SectorEntityToken>? = null
) = MiscellaneousThemeGenerator.pickHiddenLocationNotNearStar(random, this, gap, exclude)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickHiddenLocation]
 * @since 1.3.0
 */
fun StarSystemAPI.pickHiddenLocation(
    random: Random? = null,
    gap: Float,
    exclude: Set<SectorEntityToken>? = null
) = MiscellaneousThemeGenerator.pickHiddenLocation(random, this, gap, exclude)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickAnyLocation]
 * @since 1.3.0
 */
fun StarSystemAPI.pickAnyLocation(
    random: Random? = null,
    gap: Float,
    exclude: Set<SectorEntityToken>? = null
) = MiscellaneousThemeGenerator.pickAnyLocation(random, this, gap, exclude)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickUncommonLocation]
 * @since 1.3.0
 */
fun StarSystemAPI.pickUncommonLocation(
    random: Random? = null,
    gap: Float,
    exclude: Set<SectorEntityToken>? = null
) = MiscellaneousThemeGenerator.pickUncommonLocation(random, this, gap, exclude)

/**
 * Vanilla method: [MiscellaneousThemeGenerator.pickCommonLocation]
 * @since 1.3.0
 */
fun StarSystemAPI.pickCommonLocation(
    random: Random? = null,
    gap: Float,
    allowStarOrbit: Boolean,
    exclude: Set<SectorEntityToken>? = null
) = MiscellaneousThemeGenerator.pickCommonLocation(random, this, gap, allowStarOrbit, exclude)
