package org.magiclib.util

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.HullModEffect
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.set
import org.magiclib.hullmods.IncompatibleHullModEffect
import org.magiclib.hullmods.IncompatibleHullModEffect.Incompatibility

/**
 * Proposes a universal solution for hullmod incompatibilities when they can't be avoided through `isApplicableToShip`.
 */
object MagicIncompatibleHullmods {

    /**
     * Makes [hullModId] unapplicable for any ship which has [incompatibleHullModId] installed.
     *
     * Add each incompatibility only once to avoid duplicate entries. This is typically done in [BaseHullMod.init]
     */
    @JvmStatic
    fun addIncompatibility(hullModId: String, incompatibleHullModId: String) {
        val incompatibleHullMod = Global.getSettings().getHullModSpec(incompatibleHullModId) ?: return
        addIncompatibility(hullModId, incompatibleHullModId, "Incompatible with ${incompatibleHullMod.displayName}")
    }

    /**
     * Makes [hullModId] unapplicable for any ship which has [incompatibleHullModId] installed.
     *
     * Shows [incompatibleReason] as the reason.
     *
     * Add each incompatibility only once to avoid duplicate entries. This is typically done in [BaseHullMod.init]
     */
    @JvmStatic
    fun addIncompatibility(hullModId: String, incompatibleHullModId: String, incompatibleReason: String) {
        Global.getSettings().getHullModSpec(incompatibleHullModId) ?: return
        addIncompatibility(hullModId, incompatibleReason) { ship ->
            ship.variant.hasHullMod(incompatibleHullModId)
        }
    }

    /**
     * Makes [hullModId] unapplicable for any ship which [isIncompatible] returns true on.
     *
     * Shows [incompatibleReason] as the reason.
     *
     * Add each incompatibility only once to avoid duplicate entries. This is typically done in [BaseHullMod.init]
     */
    @JvmStatic
    fun addIncompatibility(hullModId: String, incompatibleReason: String, isIncompatible: (ShipAPI) -> Boolean) {
        try {
            val hullMod = Global.getSettings().getHullModSpec(hullModId)
            if (hullMod == null) {
                Global.getLogger(this::class.java).error("addIncompatibility failed for hullmod '$hullModId'")
                return
            }
            var original = hullMod.effect ?: return
            if (original !is IncompatibleHullModEffect) {
                val wrapped = IncompatibleHullModEffect(original)
                val effectField = hullMod.getFieldsMatching(type = HullModEffect::class.java)[0]
                hullMod.set(effectField, wrapped)
                original = wrapped
            }

            original.blockers.add(Incompatibility(incompatibleReason, isIncompatible))
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error("addIncompatibility failed for hullmod '$hullModId' with reason '$incompatibleReason'", e)
        }
    }


    private val INCOMPATIBILITIES: MutableMap<ShipVariantAPI, List<String>> = HashMap()
    private const val WARNING = "ML_incompatibleHullmodWarning"

    /**
     * When called, this will remove the offending hullmod, and add a "WARNING" hullmod detailing which hullmod was removed and why.
     *
     * Please note that it is best practice to remove the hullmod that was just added due to the presence of a previously installed one rather than the opposite.
     *
     * @author Tartiflette
     */
    @JvmStatic
    fun removeHullmodWithWarning(variant: ShipVariantAPI, toRemove: String, removeCause: String) {

        //cleanup of the other variants just to avoid bloat
        if (INCOMPATIBILITIES.isNotEmpty()) {
            for (v in INCOMPATIBILITIES.keys) {
                if (v !== variant && v.hullMods.contains(WARNING)) {
                    v.removeMod(WARNING)
                }
            }
        }
        INCOMPATIBILITIES.clear()

        //remove conflicting hullmod if that's not already done
        //if(variant.hullMods.contains(toRemove)){
        //    variant.removeMod(toRemove);
        //}
        if (variant.hullMods.contains(toRemove)) {
            if (variant.sMods.contains(toRemove)) {
                variant.removeMod(removeCause)
                //add the info for the tooltip
                INCOMPATIBILITIES[variant] = mutableListOf(removeCause, toRemove)
            } else {
                variant.removeMod(toRemove)
                //add the info for the tooltip
                INCOMPATIBILITIES[variant] = mutableListOf(toRemove, removeCause)
            }
        }
        //add the warning hullmod
        if (!variant.hullMods.contains(WARNING)) {
            variant.addMod(WARNING)
        }
        //add the info for the tooltip
        INCOMPATIBILITIES[variant] = mutableListOf(toRemove, removeCause)
    }

    @JvmStatic
    fun getReason(variant: ShipVariantAPI): List<String>? {
        return if (INCOMPATIBILITIES.containsKey(variant)) {
            INCOMPATIBILITIES[variant]
        } else {
            INCOMPATIBILITIES.clear()
            null
        }
    }

    @JvmStatic
    fun clearData() {
        INCOMPATIBILITIES.clear()
    }
}