@file:JvmName("MemberUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.Misc

/**
 * Delegate to [ShipHullSpecAPI.getActualHull]
 */
fun FleetMemberAPI.getActualHull(): ShipHullSpecAPI {
    return hullSpec.getActualHull()
}

/**
 * Delegate to [ShipHullSpecAPI.getActualHullId]
 */
fun FleetMemberAPI.getActualHullId(): String {
    return hullSpec.getActualHullId()
}

/**
 * Clones this member's variant and assigns the clone back via [FleetMemberAPI.setVariant], so it
 * can be edited without affecting other members or shared variants.
 *
 * The clone gets a new [ShipVariantAPI.getHullVariantId], [ShipVariantAPI.getSource] set to
 * [VariantSource.REFIT], and (if [clearOriginalVariant] is true) its original variant cleared.
 *
 * ### Why
 * [FleetMemberAPI.getVariant] may return an instance shared with other members or the game's
 * variant registry. Editing it in place can silently affect those other holders and may not
 * persist correctly on save/load. Route edits through this function instead.
 *
 * @param clearOriginalVariant If true, clears the clone's original-variant reference.
 * @return The newly cloned, member-owned variant now assigned to this member.
 */
@JvmOverloads
fun FleetMemberAPI.cloneVariantForEdit(clearOriginalVariant: Boolean = false): ShipVariantAPI {
    return variant.clone().apply {
        hullVariantId = "${hullId}_${Misc.genUID()}"
        source = VariantSource.REFIT
        if (clearOriginalVariant) setOriginalVariant(null)
    }.also { setVariant(it, false, false) }
}