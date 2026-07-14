package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.magiclib.util.MagicLookup
import org.magiclib.util.api.VariantUtils

/**
 * Delegate to [ShipHullSpecAPI.getActualHull]
 */
fun ShipVariantAPI.getActualHull(): ShipHullSpecAPI =
    hullSpec.getActualHull()

/**
 * Delegate to [ShipHullSpecAPI.getActualHullId]
 */
fun ShipVariantAPI.getActualHullId(): String =
    hullSpec.getActualHullId()

/**
 * Returns a map of all modules attached to this variant.
 *
 * Any slots with null variants are filtered out. Use [getModulesAllowNull] if you want to include null variants.
 *
 * The map key is the module slot ID, and the value is the corresponding [ShipVariantAPI] for that module.
 *
 * This delegates to [VariantUtils.getModules]
 */
fun ShipVariantAPI.getModules(): Map<String, ShipVariantAPI> {
    return VariantUtils.getModules(this)
}

/**
 * Returns a map of all modules attached to this variant.
 *
 * The map key is the module slot ID, and the value is the corresponding [ShipVariantAPI] for that module.
 *
 * This delegates to [VariantUtils.getModulesAllowNull]
 */
fun ShipVariantAPI.getModulesAllowNull(): Map<String, ShipVariantAPI?> {
    return VariantUtils.getModulesAllowNull(this)
}

/**
 * Returns all fitted weapons on this variant.
 *
 * This includes built in weapons, it does not include decorative weapons.
 *
 * The map key is the weapon slot ID, and the value is the [WeaponSpecAPI]
 * of the weapon installed in that slot.
 */
fun ShipVariantAPI.getFittedWeapons(): Map<String, WeaponSpecAPI> {
    val weapons = mutableMapOf<String, WeaponSpecAPI>()
    fittedWeaponSlots.forEach { slot ->
        val weapon = getWeaponSpec(slot) ?: return@forEach
        weapons[slot] = weapon
    }
    return weapons
}

/**
 * Returns all non-built-in weapons on this variant.
 *
 * This only excludes built in weapons
 *
 * The map key is the weapon slot ID, and the value is the [WeaponSpecAPI]
 * of the weapon installed in that slot.
 */
fun ShipVariantAPI.getNonBuiltInWeapons(): Map<String, WeaponSpecAPI> {
    val weapons = mutableMapOf<String, WeaponSpecAPI>()
    nonBuiltInWeaponSlots.forEach { slot ->
        val weapon = getWeaponSpec(slot) ?: return@forEach
        weapons[slot] = weapon
    }
    return weapons
}

/**
 * Completely removes a mod from the variant. This includes removing it from sMods, sModdedBuiltIns, permaMods, and hullMods.
 *
 * Optionally removes built-in mods from the variant. Not allowed by default.
 *
 * @param modId The ID of the mod to be removed.
 */
fun ShipVariantAPI.removeModFull(modId: String, removeBuiltIns: Boolean = false) {
    sModdedBuiltIns.remove(modId)
    removePermaMod(modId)

    if (!hullSpec.builtInMods.contains(modId)) {
        suppressedMods.remove(modId)
        removeMod(modId)
    } else if (removeBuiltIns) {
        addSuppressedMod(modId)
    }
}

/**
 * Returns a set of all DMods in the variant.
 */
fun ShipVariantAPI.allDMods(): Set<String> =
    hullMods.filter { MagicLookup.isDMod(it) }.toSet()

/**
 * Returns a set of all SMods in the variant. This includes both SMods and SModdedBuiltIns.
 */
fun ShipVariantAPI.allSMods(): Set<String> {
    val outputSMods = mutableSetOf<String>()
    for (mod in sMods) {
        outputSMods.add(mod)
    }
    for (mod in sModdedBuiltIns) {
        outputSMods.add(mod)
    }
    return outputSMods
}

/**
 * Gets all hullmods that are not sMods, perma mods, suppressed mods, or built-in mods. Simply the ordinary hullmods only.
 */
fun ShipVariantAPI.allRegularHullMods(): Set<String> {
    return hullMods
        .filter { !sModdedBuiltIns.contains(it) && !sMods.contains(it) && !permaMods.contains(it) && !suppressedMods.contains(it) && !hullSpec.builtInMods.contains(it) }
        .toSet()
}


fun ShipVariantAPI.createFleetMember(): FleetMemberAPI =
    Global.getSettings().createFleetMember(if (isFighter) FleetMemberType.FIGHTER_WING else FleetMemberType.SHIP, this)
