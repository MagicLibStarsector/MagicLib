@file:JvmName("HullUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.magiclib.kotlin.getErrorVariantID
import org.magiclib.util.MagicLookup


/**
 * Returns all built in wings on this hull.
 *
 * The map key is the wing index, and the value is the [FighterWingSpecAPI]
 * of the wing installed in that slot.
 */
fun ShipHullSpecAPI.getBuiltInWingSpecs(): Map<Int, FighterWingSpecAPI> {
    val wings = mutableMapOf<Int, FighterWingSpecAPI>()
    this.builtInWings.forEachIndexed { index, wingId ->
        if(wingId.isNullOrEmpty()) return@forEachIndexed
        val wing = MagicLookup.getFighterWingSpec(wingId) ?: return@forEachIndexed
        wings[index] = wing
    }
    return wings
}

/**
 * Returns all built-in weapons on this hull.
 *
 * The map key is the weapon slot ID, and the value is the [WeaponSpecAPI]
 * of the weapon installed in that slot.
 */
fun ShipHullSpecAPI.getBuiltInWeaponSpecs(): Map<String, WeaponSpecAPI> {
    val weapons = mutableMapOf<String, WeaponSpecAPI>()
    this.builtInWeapons.forEach { (slot, weaponId) ->
        val weapon = MagicLookup.getWeaponSpec(weaponId) ?: return@forEach
        weapons[slot] = weapon
    }
    return weapons
}

/**
 * Returns true if the hull is a module.
 */
fun ShipHullSpecAPI.isModule(): Boolean =
    hints.contains(ShipHullSpecAPI.ShipTypeHints.MODULE)

/**
 * Returns true if the hull is a fighter wing.
 */
fun ShipHullSpecAPI.isFighterWing(): Boolean =
    hullSize == HullSize.FIGHTER

/**
 * Returns a set of all the built-in D-Mods for this hull.
 */
fun ShipHullSpecAPI.getBuiltInDMods(): Set<String> =
    builtInMods.filter { MagicLookup.isDMod(it) }.toSet()

/**
 * Returns a list of weapon slot IDs which are [WeaponAPI.WeaponType.STATION_MODULE].
 *
 * These are the points on the hull which modules are intended to attach too.
 *
 * @return A list of weapon slot IDs that are station modules.
 */
fun ShipHullSpecAPI.getSlotsForModules(): List<String> {
    return this.allWeaponSlotsCopy.mapNotNull {
        if (it.weaponType == WeaponAPI.WeaponType.STATION_MODULE)
            it.id
        else
            null
    }
}

/**
 * Returns the "effective" hull for a hull spec.
 *
 * In Starsector, hull specs may represent:
 * - A base hull (Normal ship hull made straight from the .ship file)
 * - A default D-Hull (Ship Hull/Skin with _default_D placed at the end to be annoying. Has no seemingly other meaningful changes aside from ruining hullID comparisons)
 * - A skin derived from another hull (Normal ship skin made straight from the .skin file)
 * - A D-Modded hull skin (Ship skin made straight from the .skin file. Notably has DMods as built in mods, sometimes missing mounts and a restoreToBaseHull)
 *
 * This function resolves the input hull to the most appropriate "base-like" hull provided the hull is compatible with its base.
 *
 * @return The effective hull spec.
 */
fun ShipHullSpecAPI.getEffectiveHull(): ShipHullSpecAPI {
    val hull = this.getActualHull()
    return if (hull.isCompatibleWithBase) {
        /*if (hull.dParentHull != null) {
            val dParent = hull.dParentHull
            if (dParent.isCompatibleWithBase)
                dParent.baseHull ?: dParent
            else
                dParent
        } else */
        hull.baseHull ?: hull
    } else {
        hull
    }
}

/**
 * Returns the effective hull ID of the [ShipHullSpecAPI]. See [getEffectiveHull].
 */
fun ShipHullSpecAPI.getEffectiveHullId(): String =
    this.getEffectiveHull().hullId

/**
 * Returns the HullSpec from its source file (.ship or .skin), without any extra modifications such as default D-Hull variations.
 *
 * @return The actual hull spec.
 */
fun ShipHullSpecAPI.getActualHull(): ShipHullSpecAPI {
    return this.dParentHull ?: this
    /*return when {
        !this.hullId.endsWith(Misc.D_HULL_SUFFIX) -> this
        else -> this.dParentHull ?: this
    }*/
}

/**
 * Returns the HullSpec id from its source file (.ship or .skin), without any extra modifications such as default D-Hull variations.
 */
fun ShipHullSpecAPI.getActualHullId(): String =
    this.getActualHull().hullId

/**
 * A skin is a hull that is a variation of another hull. Skins are made from .skin files.
 *
 * @return True if the hull is a skin, false otherwise.
 */
fun ShipHullSpecAPI.isSkin(): Boolean {
    val hull = this.getActualHull()
    return hull.baseHullId != hull.hullId
}

/**
 * Creates a ShipVariantAPI for a given ShipHullSpecAPI.
 *
 * This exists because createEmptyVariant does not create modules.
 *
 * @return A variant for the given hull spec.
 */
fun ShipHullSpecAPI.createHullVariant(): ShipVariantAPI {
    // This function is incredibly overbuilt.
    // Simply getting the variant with the actual hull_id with _Hull appended to the end should work in most cases, but I really want to avoid having any issues here.
    return run {
        val variants = MagicLookup.getVariantsForEffectiveHullSpecRaw(this)

        variants.filter { it.source == VariantSource.HULL } // Filter out non hull variants
            .takeIf { it.isNotEmpty() }
            ?.let { hullVariants ->
                hullVariants.find { it.hullSpec.hullId == this.hullId }                             // Exact match
                    ?: hullVariants.find { it.hullSpec.hullId == this.getActualHullId() }           // Actual match
                    //?: hullVariants.find { it.hullSpec.hullId == hull.getCompatibleDLessHullId() }// D-less match
                    ?: hullVariants.find { it.hullSpec.hullId == this.getEffectiveHullId() }        // Effective match
                    ?: run {
                        Global.getLogger(javaClass).warn("Could not find ideal match when getting hull variant with hullId '${this.hullId}' and effectiveId '${this.getEffectiveHullId()}'")
                        hullVariants.firstOrNull() // Cannot find a good enough match, just go for whatever
                    }
            }?.clone()?.apply { source = null }
    } ?: runCatching {
        val emptyVariant = Global.getSettings().createEmptyVariant(this.hullId, this)
        Global.getLogger(javaClass).warn(
            "Failed to find HULL variant for '${this.hullId}' and fell back to createEmptyVariant. This can usually be ignored." +
                    "\nHowever, ships with modules may spawn without modules which can crash the game in certain circumstances"
        )
        emptyVariant
    }.getOrNull() ?: run {
        Global.getLogger(javaClass).error("Failed to find HULL variant for '${this.hullId}'")
        Global.getSettings().getVariant(Global.getSettings().getErrorVariantID())
    }
}