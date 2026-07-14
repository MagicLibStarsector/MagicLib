package org.magiclib.util.api

import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.RepairTrackerAPI
import org.magiclib.kotlin.isMercenary
import kotlin.math.max

object FleetUtils {

    /**
     * Returns a list of all officers in the fleet that are not assigned to any ship.
     */
    @JvmStatic
    @JvmOverloads
    fun getUnassignedOfficers(fleet: FleetDataAPI, includeMercenaries: Boolean = true): List<PersonAPI> {
        return fleet.officersCopy.map { it.person }.filter {
            fleet.getMemberWithCaptain(it) == null && (includeMercenaries || !it.isMercenary())
        }
    }

    /**
     * Returns a list of all officers in the fleet that are assigned to a ship.
     */
    @JvmStatic
    @JvmOverloads
    fun getAssignedOfficers(fleet: FleetDataAPI, includeMercenaries: Boolean = true): List<PersonAPI> {
        return fleet.officersCopy.map { it.person }.filter {
            fleet.getMemberWithCaptain(it) != null && (includeMercenaries || !it.isMercenary())
        }
    }

    /**
     * Repairs all ships in the fleet and restores their CR to maximum
     */
    @JvmStatic
    fun repairAndRestoreCR(fleet: FleetDataAPI) {
        fleet.membersListCopy.forEach { member ->
            member.status.repairFully()

            val repairs: RepairTrackerAPI = member.repairTracker
            repairs.cr = max(repairs.cr, repairs.maxCR)
            member.setStatUpdateNeeded(true)
        }
    }
}