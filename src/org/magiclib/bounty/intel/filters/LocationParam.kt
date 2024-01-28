package org.magiclib.bounty.intel.filters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.bounty.intel.BountyInfo
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.bounty.ui.SliderUIPanelPlugin
import org.magiclib.bounty.ui.lists.filtered.Filterable
import org.magiclib.bounty.ui.lists.filtered.FilterableParam
import org.magiclib.bounty.ui.lists.filtered.ListFilter
import org.magiclib.kotlin.setAlpha
import org.magiclib.util.MagicTxt
import java.awt.Color

class LocationParam(item: BountyInfo) : FilterableParam<BountyInfo, LocationAPI>(item) {
    override fun getData(): LocationAPI? {
        return item.getLocationIfBountyIsActive()
    }
}

class LocationFilter : ListFilter<BountyInfo, LocationAPI> {
    private var rangeFilterActive = false
    private var fuelRangeOnly = false
    private var hyperspaceRange = (0f..0f)
    private var sliderPlugin: SliderUIPanelPlugin? = null
    private var enabledCheckbox: ButtonAPI? = null
    private var fuelRangeCheckbox: ButtonAPI? = null

    override fun matches(filterableParam: FilterableParam<BountyInfo, LocationAPI>): Boolean {
        if (rangeFilterActive) {
            val bountyLoc = filterableParam.getData()?.location ?: return false
            if (fuelRangeOnly) {
                hyperspaceRange = (0f..getFuelLY())
                sliderPlugin?.value = hyperspaceRange.endInclusive
            }

            if (hyperspaceRange.start < hyperspaceRange.endInclusive) {
                return hyperspaceRange.contains(Misc.getDistanceToPlayerLY(bountyLoc))
            }
        }
        return true
    }

    override fun matches(filterData: List<FilterableParam<BountyInfo, *>>): Boolean {
        if (!rangeFilterActive) return true
        return filterData
            .filterIsInstance<FilterableParam<BountyInfo, LocationAPI>>()
            .all { it.item.shouldAlwaysShow() || matches(it) }
    }

    override fun createPanel(
        tooltip: TooltipMakerAPI,
        width: Float,
        lastItems: List<Filterable<BountyInfo>>
    ): CustomPanelAPI {
        val validBounties = lastItems
            .map { it as BountyInfo }
            .filter { it.getLocationIfBountyIsActive() != null }

        val min = validBounties
            .minOfOrNull { Misc.getDistanceToPlayerLY(it.getLocationIfBountyIsActive()!!.location) }
            ?: 0f

        val max = validBounties
            .maxOfOrNull { Misc.getDistanceToPlayerLY(it.getLocationIfBountyIsActive()!!.location) }
            ?: 0f
        val currValue = hyperspaceRange.endInclusive.coerceIn(min, max)
        hyperspaceRange = min..max

        val filterPlugin = InteractiveUIPanelPlugin()
        val filterPanel = Global.getSettings().createCustom(width, 60f, filterPlugin)

        //checkbox tooltip
        val enableButtonTooltip = filterPanel.createUIElement(width, 12f, false)

        val enableCheckboxLocal = enableButtonTooltip.addCheckbox(
            16f,
            16f,
            MagicTxt.getString("mb_filters_Location_Distance"),
            null,
            ButtonAPI.UICheckboxSize.SMALL,
            0f
        )
        enabledCheckbox = enableCheckboxLocal
        enableCheckboxLocal.isChecked = rangeFilterActive
        filterPlugin.addCheckbox(enableCheckboxLocal) { checked ->
            rangeFilterActive = checked
            refreshSettings()
        }

        val fuelRangeCheckboxLocal = enableButtonTooltip.addCheckbox(
            16f,
            16f,
            MagicTxt.getString("mb_filters_Location_FuelRange"),
            null,
            ButtonAPI.UICheckboxSize.SMALL,
            4f
        )
        fuelRangeCheckbox = fuelRangeCheckboxLocal
        fuelRangeCheckboxLocal.isChecked = fuelRangeOnly
        filterPlugin.addCheckbox(fuelRangeCheckboxLocal) { checked ->
            fuelRangeOnly = checked
            refreshSettings()
        }

        filterPanel.addUIElement(enableButtonTooltip).inTMid(2f)

        //slider
        val sliderPanel = filterPanel.createCustomPanel(width, 48f, null)
        val sliderPluginLocal = SliderUIPanelPlugin(min, max, value = currValue)
        sliderPluginLocal.apply {
            title = MagicTxt.getString("mb_filters_Location_SliderTitle")
            panelWidth = width - 6f
            panelHeight = 48f
            barHeight = 20f
            bgColor = Color.RED.setAlpha(40)

            layoutPanel(sliderPanel).position.inTL(8f, 2f)

            addListener { _, newValue ->
                hyperspaceRange = (hyperspaceRange.start..newValue)
            }
        }
        sliderPlugin = sliderPluginLocal
        filterPanel.addComponent(sliderPanel).belowMid(enableButtonTooltip, 2f)
        tooltip.addCustomDoNotSetPosition(filterPanel)

        refreshSettings()
        return filterPanel
    }

    override fun saveToPersistentData() {
        Global.getSector().persistentData["MagicLib.LocationParam.SelectedRange"] = hyperspaceRange
        Global.getSector().persistentData["MagicLib.LocationParam.RangeEnabled"] = rangeFilterActive
        Global.getSector().persistentData["MagicLib.LocationParam.FuelRangeOnly"] = fuelRangeOnly
    }

    override fun loadFromPersistentData() {
        if (Global.getSector().persistentData.containsKey("MagicLib.LocationParam.SelectedRange"))
            hyperspaceRange =
                Global.getSector().persistentData["MagicLib.LocationParam.SelectedRange"] as ClosedFloatingPointRange<Float>

        if (Global.getSector().persistentData.containsKey("MagicLib.LocationParam.RangeEnabled"))
            rangeFilterActive = Global.getSector().persistentData["MagicLib.LocationParam.RangeEnabled"] as Boolean

        if (Global.getSector().persistentData.containsKey("MagicLib.LocationParam.FuelRangeOnly"))
            fuelRangeOnly = Global.getSector().persistentData["MagicLib.LocationParam.FuelRangeOnly"] as Boolean
    }

    override fun isActive(): Boolean {
        return rangeFilterActive
    }

    fun refreshSettings() {
        if (!rangeFilterActive) {
            fuelRangeCheckbox?.isChecked = false
            fuelRangeCheckbox?.isEnabled = false
            sliderPlugin?.enabled = false
        } else {
            fuelRangeCheckbox?.isEnabled = true
            fuelRangeCheckbox?.isChecked = fuelRangeOnly
            if (fuelRangeOnly) {
                hyperspaceRange = (0f..getFuelLY())
                sliderPlugin?.value = hyperspaceRange.endInclusive
                sliderPlugin?.updateLabel()
            }
            sliderPlugin?.enabled = !fuelRangeOnly
        }
    }

    /**
     * Calculates how far the player's fleet can travel, minus amount needed to jump to hyper if in-system.
     * Written by SafariJohn for Logistics Notifications mod.
     * @return distance in lightyears
     */
    private fun getFuelLY(): Float {
        // Calculate lightyears of fuel remaining
        val playerFleet = Global.getSector().playerFleet
        val fuel = playerFleet.cargo.fuel
        val fuelPerDay = playerFleet.logistics.baseFuelCostPerLightYear
        var ly: Float
        ly = if (playerFleet.isInHyperspace) {
            fuel / fuelPerDay
        } else {
            (fuel - fuelPerDay) / fuelPerDay
        }
        if (ly < 0) ly = 0f
        return ly
    }
}