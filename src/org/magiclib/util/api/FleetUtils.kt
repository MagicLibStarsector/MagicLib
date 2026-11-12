@file:JvmName("FleetUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.RepairTrackerAPI
import org.magiclib.kotlin.isMercenary
import org.magiclib.kotlin.isOfficerActive
import kotlin.math.max

private fun FleetDataAPI.filterOfficers(
    assigned: Boolean,
    includeMercenaries: Boolean,
    includeInactive: Boolean
): List<PersonAPI> {
    return officersCopy.map { it.person }.filter {
        val isAssigned = getMemberWithCaptain(it) != null
        isAssigned == assigned &&
                (includeMercenaries || !it.isMercenary()) &&
                (includeInactive || !it.isOfficerActive())
    }
}

/**
 * Returns a list of all officers in the fleet that are not assigned to any ship.
 */
@JvmOverloads
fun FleetDataAPI.getUnassignedOfficers(includeMercenaries: Boolean = true, includeInactive: Boolean = true) =
    filterOfficers(false, includeMercenaries, includeInactive)

/**
 * Returns a list of all officers in the fleet that are assigned to a ship.
 */
@JvmOverloads
fun FleetDataAPI.getAssignedOfficers(includeMercenaries: Boolean = true, includeInactive: Boolean = true) =
    filterOfficers(true, includeMercenaries, includeInactive)

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