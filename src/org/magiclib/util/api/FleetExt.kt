@file:JvmName("FleetUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.RepairTrackerAPI
import org.magiclib.kotlin.isMercenary
import kotlin.math.max

/**
 * Returns a list of all officers in the fleet that are not assigned to any ship.
 */
@JvmOverloads
fun FleetDataAPI.getUnassignedOfficers(includeMercenaries: Boolean = true): List<PersonAPI> {
    return this.officersCopy.map { it.person }.filter {
        this.getMemberWithCaptain(it) == null && (includeMercenaries || !it.isMercenary())
    }
}


/**
 * Returns a list of all officers in the fleet that are assigned to a ship.
 */
@JvmOverloads
fun FleetDataAPI.getAssignedOfficers(includeMercenaries: Boolean = true): List<PersonAPI> {
    return this.officersCopy.map { it.person }.filter {
        this.getMemberWithCaptain(it) != null && (includeMercenaries || !it.isMercenary())
    }
}

/**
 * Repairs all ships in the fleet and restores their CR to maximum
 */
fun FleetDataAPI.repairAndRestoreCR() {
    this.membersListCopy.forEach { member ->
        member.status.repairFully()

        val repairs: RepairTrackerAPI = member.repairTracker
        repairs.cr = max(repairs.cr, repairs.maxCR)
        member.setStatUpdateNeeded(true)
    }
}