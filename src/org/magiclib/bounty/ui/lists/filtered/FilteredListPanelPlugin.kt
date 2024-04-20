package org.magiclib.bounty.ui.lists.filtered

import com.fs.starfarer.api.ui.*
import org.magiclib.bounty.ui.ButtonHandler
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.bounty.ui.lists.ListUIPanelPlugin
import org.magiclib.util.MagicTxt

abstract class FilteredListPanelPlugin<T : Filterable<T>>(parentPanel: CustomPanelAPI) :
    ListUIPanelPlugin<T>(parentPanel) {
    var filterButton: ButtonAPI? = null
    var filterContainerPanel: CustomPanelAPI? = null
    var filtersForItems: List<ListFilter<T, *>> = getApplicableFilters()

    protected abstract fun getApplicableFilters(): List<ListFilter<T, *>>

    override fun layoutPanels(members: List<T>): CustomPanelAPI {
        if (outerPanel != null) {
            outerTooltip!!.removeComponent(innerPanel)
            outerPanel!!.removeComponent(outerTooltip)
            clearItems()
        }

        val outerPanelLocal = outerPanel ?: parentPanel.createCustomPanel(panelWidth, panelHeight, this)
        outerPanel = outerPanelLocal

        filtersForItems.forEach { it.loadFromPersistentData() }
        var validMembers = members.filter { shouldMakePanelForItem(it) }
        lastMembers = validMembers
        validMembers = validMembers.filter { shouldFilterItem(it) }
        validMembers = sortMembers(validMembers)

        val outerTooltipLocal = outerPanelLocal.createUIElement(panelWidth, panelHeight, false)
        outerTooltip = outerTooltipLocal

        createListHeader(outerTooltipLocal)

        val buttonHeight = 20f
        val filterButtonLocal = outerTooltipLocal.addButton(
            filterButtonText(),
            null,
            panelWidth - 4f,
            buttonHeight,
            2f
        )
        filterButton = filterButtonLocal
        this.buttons[filterButtonLocal] = FilterButtonHandler()
        filterButtonLocal.position.inTMid(22f)

        val listHeight = panelHeight - buttonHeight - 22f
        val holdingPanel = outerPanelLocal.createCustomPanel(panelWidth, listHeight, null)
        innerPanel = holdingPanel

        val scrollerTooltip: TooltipMakerAPI = holdingPanel.createUIElement(panelWidth, listHeight, true)
        val scrollingPanel: CustomPanelAPI =
            holdingPanel.createCustomPanel(panelWidth, getListHeight(validMembers.size) + buttonHeight + 22f, null)
        val tooltip: TooltipMakerAPI =
            scrollingPanel.createUIElement(panelWidth, getListHeight(validMembers.size) + buttonHeight + 22f, false)

        var lastItem: UIPanelAPI? = null
        validMembers
            .map { it to createPanelForItem(tooltip, it) }
            .filter { (_, rowPlugin) -> rowPlugin != null }
            .forEach { (item, rowPlugin) ->
                lastItem = placeItem(tooltip, rowPlugin!!, lastItem)
            }

        scrollingPanel.addUIElement(tooltip).inTL(0f, 0f)
        scrollerTooltip.addCustom(scrollingPanel, 0f).position.inTL(0f, 0f)
        holdingPanel.addUIElement(scrollerTooltip).inTL(0f, 0f)
        outerTooltipLocal.addCustom(holdingPanel, 0f).position.belowMid(filterButtonLocal, 2f)
        outerPanelLocal.addUIElement(outerTooltipLocal).inTL(0f, 0f)
        this.parentPanel.addComponent(outerPanelLocal).inTL(0f, 0f)
        scroller = scrollerTooltip.externalScroller

        return outerPanelLocal
    }

    protected fun createFilterPanel() {
        val filterContainerPanelPlugin = InteractiveUIPanelPlugin()
        filterContainerPanelPlugin.renderBackground = true
        filterContainerPanelPlugin.eatAllClicks = true

        val filterContainerPanelLocal =
            outerPanel!!.createCustomPanel(panelWidth, panelHeight * 0.33f, filterContainerPanelPlugin)
        filterContainerPanel = filterContainerPanelLocal

        val filterContainerTooltip = filterContainerPanelLocal.createUIElement(panelWidth, panelHeight * 0.33f, true)
        var lastItem: UIComponentAPI? = null
        filtersForItems.forEach {
            val filterPanel = it.createPanel(filterContainerTooltip, panelWidth - 4f, lastMembers!!)
            if (lastItem != null) {
                filterPanel.position.belowMid(lastItem, 4f).setXAlignOffset(-3f)
            } else {
                filterPanel.position.inTMid(4f).setXAlignOffset(-3f)
            }
            lastItem = filterPanel
        }
        filterContainerPanelLocal.addUIElement(filterContainerTooltip).inBMid(4f)

        outerPanel!!.addComponent(filterContainerPanelLocal).inTMid(46f)
    }

    fun closeFilterPanel() {
        filtersForItems.forEach { it.saveToPersistentData() }
        outerPanel!!.removeComponent(filterContainerPanel)
        filterContainerPanel = null

        layoutPanels()
    }

    protected abstract fun getFiltersFromItem(item: T): List<String>

    private fun shouldFilterItem(item: T): Boolean {
        if (filtersForItems.isEmpty()) return true
        return filtersForItems
            .all { it.matches(item.getFilterData()) }
    }

    inner class FilterButtonHandler : ButtonHandler() {
        override fun onClicked() {
            filterButton!!.isChecked = false
            if (this@FilteredListPanelPlugin.filterContainerPanel == null) {
                filterButton!!.text = MagicTxt.getString("mb_confirm")
                createFilterPanel()
            } else {
                filterButton!!.text =
                    filterButtonText()
                closeFilterPanel()
            }
        }
    }

    private fun filterButtonText() =
        MagicTxt.getString("mb_filters") + if (filtersForItems.any { it.isActive() }) " (${filtersForItems.count { it.isActive() }})" else ""
}