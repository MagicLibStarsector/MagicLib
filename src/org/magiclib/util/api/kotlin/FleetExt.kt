package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import org.magiclib.util.api.FleetUtils

/**
 * Returns a list of all officers in the fleet that are not assigned to any ship.
 *
 * This function is a delegate to [FleetUtils.getUnassignedOfficers].
 */
fun FleetDataAPI.getUnassignedOfficers(includeMercenaries: Boolean = true): List<PersonAPI> =
    FleetUtils.getUnassignedOfficers(this, includeMercenaries = includeMercenaries)

/**
 * Returns a list of all officers in the fleet that are assigned to a ship.
 *
 * This function is a delegate to [FleetUtils.getAssignedOfficers].
 */
fun FleetDataAPI.getAssignedOfficers(includeMercenaries: Boolean = true): List<PersonAPI> =
    FleetUtils.getAssignedOfficers(this, includeMercenaries = includeMercenaries)

/**
 * Repairs all ships in the fleet and restores their CR to maximum
 *
 * This function is a delegate to [FleetUtils.repairAndRestoreCR].
 */
fun FleetDataAPI.repairAndRestoreCR() =
    FleetUtils.repairAndRestoreCR(this)