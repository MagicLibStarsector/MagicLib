package org.magiclib.bounty.intel

import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.intel.filters.LocationParam
import org.magiclib.bounty.ui.lists.filtered.Filterable
import org.magiclib.bounty.ui.lists.filtered.FilterableParam

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
}