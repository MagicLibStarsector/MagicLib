package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.magiclib.util.api.kotlin.HullExt.getActualHull
import org.magiclib.util.api.kotlin.HullExt.getActualHullId

object MemberExt {
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
}