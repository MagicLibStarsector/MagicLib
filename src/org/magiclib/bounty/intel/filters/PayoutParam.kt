package org.magiclib.bounty.intel.filters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.bounty.intel.BountyInfo
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.bounty.ui.SliderUIPanelPlugin
import org.magiclib.bounty.ui.lists.filtered.Filterable
import org.magiclib.bounty.ui.lists.filtered.FilterableParam
import org.magiclib.bounty.ui.lists.filtered.ListFilter
import org.magiclib.kotlin.autoSizeToText
import org.magiclib.kotlin.getRoundedValue

class PayoutParam(item: BountyInfo) : FilterableParam<BountyInfo, Int>(item) {
    override fun getData(): Int? {
        return item.getBountyPayout()
    }
}


class PayoutFilter : ListFilter<BountyInfo, Int> {
    private var payoutMinimum = -1

    override fun matches(filterableParam: FilterableParam<BountyInfo, Int>): Boolean {
        if (payoutMinimum > 0) {
            val bountyPayout = filterableParam.getData()!!
            return payoutMinimum <= bountyPayout
        }
        return true
    }

    override fun matches(filterData: List<FilterableParam<BountyInfo, *>>): Boolean {
        return filterData
            .filter { it.getData() is Int }
            .all { matches(it as FilterableParam<BountyInfo, Int>) }
    }

    override fun createPanel(
        tooltip: TooltipMakerAPI,
        width: Float,
        lastItems: List<Filterable<BountyInfo>>
    ): CustomPanelAPI {
        val validBounties = lastItems
            .map { it as BountyInfo }

        val min: Int = validBounties
            .minOfOrNull { it.getBountyPayout() }?.coerceAtLeast(0)
            ?: 0

        val max: Int = validBounties
            .maxOfOrNull { it.getBountyPayout() }
            ?: 0
        val currValue = payoutMinimum.coerceIn(min, max)
        payoutMinimum = currValue

        val filterPlugin = InteractiveUIPanelPlugin()
        val filterPanel = Global.getSettings().createCustom(width, 60f, filterPlugin)

        //slider
        val sliderPanel = filterPanel.createCustomPanel(width, 48f, null)

        val sliderTooltip = sliderPanel.createUIElement(width - 8f, 16f, false)

        val sliderTextTooltip = sliderPanel.createUIElement(115f, 30f, false)
        sliderTextTooltip.addPara("Payout", Misc.getTextColor(), 0f).position.inMid()
        sliderPanel.addUIElement(sliderTextTooltip).inTMid(2f)

        val sliderPlugin = SliderUIPanelPlugin(min.toFloat(), max.toFloat(), value = currValue.toFloat())
        val sliderPluginPanel = sliderPanel.createCustomPanel(width - 8f, 16f, sliderPlugin)
        sliderTooltip.addCustomDoNotSetPosition(sliderPluginPanel).position.inBMid(0f)
        sliderPanel.addUIElement(sliderTooltip).belowMid(sliderTextTooltip, 4f)

        val sliderValueTextTooltip = sliderPanel.createUIElement(40f, 12f, false)
        val sliderValueText =
            sliderValueTextTooltip.addPara(sliderPlugin.value.getRoundedValue(), Misc.getTextColor(), 0f)
        sliderValueText.autoSizeToText(max.toString()).position.inMid()
        sliderPanel.addUIElement(sliderValueTextTooltip).belowMid(sliderTextTooltip, -12f)

        val sliderMinTextTooltip = sliderPanel.createUIElement(10f, 30f, false)
        sliderMinTextTooltip.addPara(min.toString(), Misc.getTextColor(), 0f).autoSizeToText().position.inMid()
        sliderPanel.addUIElement(sliderMinTextTooltip).inTL(2f, 2f)

        val sliderMaxTextTooltip = sliderPanel.createUIElement(25f, 30f, false)
        sliderMaxTextTooltip.addPara(max.toString(), Misc.getTextColor(), 0f).autoSizeToText().position.inMid()
        sliderPanel.addUIElement(sliderMaxTextTooltip).inTR(2f, 2f)

        sliderPlugin.addListener { _, newValue ->
            sliderValueText.text = newValue.toInt().toString()
            payoutMinimum = newValue.toInt()
        }

        filterPanel.addComponent(sliderPanel).inTMid(2f)
        tooltip.addCustomDoNotSetPosition(filterPanel)
        return filterPanel
    }

    override fun saveToPersistentData() {
        Global.getSector().persistentData["MagicLib.PayoutParam.SelectedMinimum"] = payoutMinimum
    }

    override fun loadFromPersistentData() {
        if (Global.getSector().persistentData.containsKey("MagicLib.PayoutParam.SelectedMinimum"))
            payoutMinimum = Global.getSector().persistentData["MagicLib.PayoutParam.SelectedMinimum"] as Int
    }

    override fun isActive(): Boolean = payoutMinimum > -1
}