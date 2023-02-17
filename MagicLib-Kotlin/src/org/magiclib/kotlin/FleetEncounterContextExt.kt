@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import java.util.*

/**
 * @since 0.46.1
 */
inline fun FleetMemberAPI.prepareShipForRecovery(
    retainAllHullmods: Boolean,
    retainKnownHullmods: Boolean,
    clearSMods: Boolean,
    weaponRetainProb: Float,
    wingRetainProb: Float,
    salvageRandom: Random? = null
) = FleetEncounterContext.prepareShipForRecovery(
    this,
    retainAllHullmods,
    retainKnownHullmods,
    clearSMods,
    weaponRetainProb,
    wingRetainProb,
    salvageRandom
)

/**
 * @since 0.46.1
 */
inline fun FleetMemberAPI.prepareModuleForRecovery(
    moduleSlotId: String,
    retainAllHullmods: Boolean,
    retainKnownHullmods: Boolean,
    clearSMods: Boolean,
    weaponRetainProb: Float,
    wingRetainProb: Float,
    salvageRandom: Random? = null
) = FleetEncounterContext.prepareModuleForRecovery(
    this,
    moduleSlotId,
    retainAllHullmods,
    retainKnownHullmods,
    clearSMods,
    weaponRetainProb,
    wingRetainProb,
    salvageRandom
)
