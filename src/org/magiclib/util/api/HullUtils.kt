package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.getErrorVariantID
import org.magiclib.util.MagicLookup
import org.magiclib.util.api.kotlin.getActualHullId
import org.magiclib.util.api.kotlin.getEffectiveHullId

object HullUtils
{
    /**
     * Returns a list of weapon slot IDs which are [WeaponAPI.WeaponType.STATION_MODULE].
     *
     * These are the points on the hull which modules are intended to attach too.
     *
     * @param hull The hull spec to query.
     * @return A list of weapon slot IDs that are station modules.
     */
    @JvmStatic
    fun getSlotsForModules(hull: ShipHullSpecAPI): List<String> {
        return hull.allWeaponSlotsCopy.mapNotNull {
            if (it.weaponType == WeaponAPI.WeaponType.STATION_MODULE)
                it.id
            else
                null
        }
    }

    /**
     * Creates a ShipVariantAPI for a given ShipHullSpecAPI.
     *
     * This function exists because createEmptyVariant does not create modules.
     *
     * @param hull The hull spec for which to create a variant
     * @return A variant for the given hull spec.
     */
    @JvmStatic
    fun createHullVariant(hull: ShipHullSpecAPI): ShipVariantAPI {
        // This function is incredibly overbuilt.
        // Simply getting the variant with the actual hull_id with _Hull appended to the end should work in most cases, but I really want to avoid having any issues here.
        return run {
            val variants = MagicLookup.getVariantsForEffectiveHullSpecRaw(hull)

            variants.filter { it.source == VariantSource.HULL } // Filter out non hull variants
                .takeIf { it.isNotEmpty() }
                ?.let { hullVariants ->
                    hullVariants.find { it.hullSpec.hullId == hull.hullId }                             // Exact match
                        ?: hullVariants.find { it.hullSpec.hullId == hull.getActualHullId() }           // Actual match
                        //?: hullVariants.find { it.hullSpec.hullId == hull.getCompatibleDLessHullId() }// D-less match
                        ?: hullVariants.find { it.hullSpec.hullId == hull.getEffectiveHullId() }        // Effective match
                        ?: run {
                            Global.getLogger(javaClass).warn("Could not find ideal match when getting hull variant with hullId '${hull.hullId}' and effectiveId '${hull.getEffectiveHullId()}'")
                            hullVariants.firstOrNull() // Cannot find a good enough match, just go for whatever
                        }
                }?.clone()?.apply { source = null }
        } ?: runCatching {
            val emptyVariant = Global.getSettings().createEmptyVariant(hull.hullId, hull)
            Global.getLogger(javaClass).warn(
                "Failed to find HULL variant for '${hull.hullId}' and fell back to createEmptyVariant. This can usually be ignored." +
                        "\nHowever, ships with modules may spawn without modules which can crash the game in certain circumstances"
            )
            emptyVariant
        }.getOrNull() ?: run {
            Global.getLogger(javaClass).error("Failed to find HULL variant for '${hull.hullId}'")
            Global.getSettings().getVariant(Global.getSettings().getErrorVariantID())
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
     * @param hull The hull spec to resolve.
     * @return The effective hull spec.
     */
    @JvmStatic
    fun getEffectiveHull(hull: ShipHullSpecAPI): ShipHullSpecAPI {
        val hull = getActualHull(hull)
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
     * Returns the HullSpec from its source file (.ship or .skin), without any extra modifications such as default D-Hull variations.
     *
     * @param hull The hull spec to resolve.
     * @return The actual hull spec.
     */
    @JvmStatic
    fun getActualHull(
        hull: ShipHullSpecAPI
    ): ShipHullSpecAPI {
        return when {
            !hull.hullId.endsWith(Misc.D_HULL_SUFFIX) -> hull
            else -> hull.dParentHull ?: hull
        }
    }

    /**
     * A skin is a hull that is a variation of another hull. Skins are made from .skin files.
     *
     * @param hull The hull to check.
     * @return True if the hull is a skin, false otherwise.
     */
    @JvmStatic
    fun isSkin(hull: ShipHullSpecAPI): Boolean {
        val hull = getActualHull(hull)
        return hull.baseHullId != hull.hullId
    }
}