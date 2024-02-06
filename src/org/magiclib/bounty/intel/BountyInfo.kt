package org.magiclib.bounty.intel

import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.ActiveBounty
import org.magiclib.bounty.intel.filters.LocationParam
import org.magiclib.bounty.ui.lists.filtered.Filterable
import org.magiclib.bounty.ui.lists.filtered.FilterableParam
import org.magiclib.util.MagicTxt

interface BountyInfo : Filterable<BountyInfo> {
    fun getBountyId(): String
    fun getBountyName(): String
    fun getBountyType(): String
    fun getBountyPayout(): Int
    fun getJobIcon(): String?
    fun getLocationIfBountyIsActive(): LocationAPI?
    fun getSortIndex(): Int = 1
    fun addNotificationBulletpoints(info: TooltipMakerAPI) {
    }

    fun notifyWhenAvailable(): Boolean {
        return false
    }

    fun notifiedUserThatBountyIsAvailable() {

    }

    fun shouldShow(): Boolean {
        return true
    }

    fun shouldAlwaysShow(): Boolean {
        return false
    }

    fun decorateListItem(
        plugin: BountyListPanelPlugin.BountyItemPanelPlugin,
        tooltip: TooltipMakerAPI,
        width: Float,
        height: Float
    )

    fun layoutPanel(tooltip: TooltipMakerAPI, width: Float, height: Float)

    override fun getFilterData(): List<FilterableParam<BountyInfo, *>> {
        return mutableListOf(
            LocationParam(this)
        )
    }

    fun createLocationEstimateText(bounty: ActiveBounty): String {
        var loc = BreadcrumbSpecial.getLocatedString(bounty.fleetSpawnLocation)
        loc = MagicTxt.getString("mb_distance_they") + " " + loc + MagicTxt.getString(".")
        return loc
    }

    fun getPronoun(personAPI: PersonAPI): String {
        return when (personAPI.gender) {
            FullName.Gender.FEMALE -> MagicTxt.getString("mb_distance_she")
            FullName.Gender.MALE -> MagicTxt.getString("mb_distance_he")
            else -> MagicTxt.getString("mb_distance_they")
        }
    }

    fun createLocationPreciseText(bounty: ActiveBounty): String {
        var loc = MagicTxt.getString("mb_distance_last")
        loc = if (bounty.spec.fleet_behavior == FleetAssignment.PATROL_SYSTEM) {
            loc + MagicTxt.getString("mb_distance_roaming") + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
        } else {
            if (bounty.fleetSpawnLocation.market != null) {
                loc + MagicTxt.getString("mb_distance_near") + bounty.fleetSpawnLocation.market.name + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.PLANET)) {
                loc + MagicTxt.getString("mb_distance_near") + bounty.fleetSpawnLocation.name + MagicTxt.getString("mb_distance_in") + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.STATION)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_station") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.JUMP_POINT)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_jump") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.GATE)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_gate") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.DEBRIS_FIELD)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_debris") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.WRECK)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_wreck") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.COMM_RELAY)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_comm") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.SENSOR_ARRAY)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_sensor") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.NAV_BUOY)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_nav") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else if (bounty.fleetSpawnLocation.hasTag(Tags.STABLE_LOCATION)) {
                loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_stable") + MagicTxt.getString(
                    "mb_distance_in"
                ) + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            } else {
                loc + MagicTxt.getString("mb_distance_somewhere") + MagicTxt.getString("mb_distance_in") + bounty.fleetSpawnLocation.starSystem.nameWithLowercaseType
            }
        }
        loc += MagicTxt.getString(".")
        return loc
    }
}