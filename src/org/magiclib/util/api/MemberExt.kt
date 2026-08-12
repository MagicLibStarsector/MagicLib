@file:JvmName("MemberUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI

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
