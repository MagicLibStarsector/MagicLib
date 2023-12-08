package org.magiclib.bounty.intel

import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.intel.filters.LocationParam
import org.magiclib.bounty.ui.lists.filtered.FilterableParam
import org.magiclib.bounty.ui.lists.filtered.Filterable

interface BountyInfo : Filterable<BountyInfo> {
    fun getBountyId(): String
    fun getBountyName(): String
    fun getBountyType(): String
    fun getBountyPayout(): Int
    fun getJobIcon(): String?
    fun getLocationIfBountyIsActive(): LocationAPI?

    fun shouldShow(): Boolean {
        return true
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