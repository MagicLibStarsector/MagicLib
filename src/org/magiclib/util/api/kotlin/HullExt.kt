package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import org.magiclib.util.MagicLookup
import org.magiclib.util.api.HullUtils

object HullExt {

    /**
     * Returns true if this hull is a module.
     */
    fun ShipHullSpecAPI.isModule(): Boolean =
        hints.contains(ShipHullSpecAPI.ShipTypeHints.MODULE)

    /**
     * Returns true if this hull is a fighter wing.
     */
    fun ShipHullSpecAPI.isFighterWing(): Boolean =
        hullSize == HullSize.FIGHTER

    /**
     * Returns a set of all the built-in D-Mods for this hull.
     */
    fun ShipHullSpecAPI.getBuiltInDMods(): Set<String> =
        builtInMods.filter { MagicLookup.isDMod(it) }.toSet()

    /**
     * Delegates to [HullUtils.getSlotsForModules]
     */
    fun ShipHullSpecAPI.getSlotsForModules(): List<String> =
        HullUtils.getSlotsForModules(this)

    /**
     * Delegates to [HullUtils.getEffectiveHull].
     */
    fun ShipHullSpecAPI.getEffectiveHull(): ShipHullSpecAPI =
        HullUtils.getEffectiveHull(this)

    /**
     * Returns the effective hull ID of this [ShipHullSpecAPI]. See [HullUtils.getEffectiveHull].
     */
    fun ShipHullSpecAPI.getEffectiveHullId(): String =
        HullUtils.getEffectiveHull(this).hullId

    /**
     * Returns the HullSpec from its source file (.ship or .skin), without any extra modifications such as default D-Hull variations.
     */
    fun ShipHullSpecAPI.getActualHull(): ShipHullSpecAPI =
        HullUtils.getActualHull(this)

    /**
     * Returns the HullSpec id from its source file (.ship or .skin), without any extra modifications such as default D-Hull variations.
     */
    fun ShipHullSpecAPI.getActualHullId(): String =
        HullUtils.getActualHull(this).hullId

    /**
     * Delegates to [HullUtils.isSkin].
     */
    fun ShipHullSpecAPI.isSkin(): Boolean =
        HullUtils.isSkin(this)

    /**
     * Delegates to [HullUtils.createHullVariant].
     */
    fun ShipHullSpecAPI.createHullVariant(): ShipVariantAPI =
        HullUtils.createHullVariant(this)
}