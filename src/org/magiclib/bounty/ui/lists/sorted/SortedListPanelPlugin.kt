package org.magiclib.bounty.ui.lists.sorted

import com.fs.starfarer.api.ui.*
import org.magiclib.bounty.ui.ButtonHandler
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.bounty.ui.lists.ListUIPanelPlugin
import org.magiclib.util.MagicTxt

abstract class SortedListPanelPlugin<T : Sortable<T>>(parentPanel: CustomPanelAPI) :
    ListUIPanelPlugin<T>(parentPanel) {
    var sorterButton: ButtonAPI? = null
    var sorterContainerPanel: CustomPanelAPI? = null
    var sortersForItems: List<ListSorter<T, *>> = getApplicableSorters()

    protected abstract fun getApplicableSorters(): List<ListSorter<T, *>>

    override fun layoutPanels(members: List<T>): CustomPanelAPI {
        if (outerPanel != null) {
            outerTooltip!!.removeComponent(innerPanel)
            outerPanel!!.removeComponent(outerTooltip)
            clearItems()
        }

        val outerPanelLocal = outerPanel ?: parentPanel.createCustomPanel(panelWidth, panelHeight, this)
        outerPanel = outerPanelLocal

        sortersForItems.forEach { it.loadFromPersistentData(members) }
        var validMembers = members.filter { shouldMakePanelForItem(it) }
        lastMembers = validMembers
        //validMembers = sortMembers(validMembers)
        validMembers = validMembers.sortedBy { it.getSortIndex() }

        val outerTooltipLocal = outerPanelLocal.createUIElement(panelWidth, panelHeight, false)
        outerTooltip = outerTooltipLocal

        createListHeader(outerTooltipLocal)

        val buttonHeight = 20f
        val sorterButtonLocal = outerTooltipLocal.addButton(
            sorterButtonText(),
            null,
            panelWidth - 4f,
            buttonHeight,
            2f
        )
        sorterButton = sorterButtonLocal
        this.buttons[sorterButtonLocal] = SorterButtonHandler()
        sorterButtonLocal.position.inTMid(22f)

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
        outerTooltipLocal.addCustom(holdingPanel, 0f).position.belowMid(sorterButtonLocal, 2f)
        outerPanelLocal.addUIElement(outerTooltipLocal).inTL(0f, 0f)
        this.parentPanel.addComponent(outerPanelLocal).inTL(0f, 0f)
        scroller = scrollerTooltip.externalScroller

        return outerPanelLocal
    }

    protected fun createSorterPanel() {
        val sorterContainerPanelPlugin = InteractiveUIPanelPlugin()
        sorterContainerPanelPlugin.renderBackground = true
        sorterContainerPanelPlugin.eatAllClicks = true

        val sorterContainerPanelLocal =
            outerPanel!!.createCustomPanel(panelWidth, panelHeight * 0.33f, sorterContainerPanelPlugin)
        sorterContainerPanel = sorterContainerPanelLocal

        val sorterContainerTooltip = sorterContainerPanelLocal.createUIElement(panelWidth, panelHeight * 0.33f, true)
        sorterContainerTooltip.addSpacer(panelHeight * 0.33f) // sorterContainerTooltip's height is always forced to 0f for some reason which makes all contents invisible. Calling this function allows the contents to be seen and keeps track of height.

        var lastItem: UIComponentAPI? = null

        sortersForItems.forEach {
            val sorterPanel = it.createPanel(sorterContainerTooltip, panelWidth - 4f, lastMembers!!)
            if (lastItem != null) {
                sorterPanel.position.belowMid(lastItem, 4f).setXAlignOffset(-3f)
            } else {
                sorterPanel.position.inTMid(4f).setXAlignOffset(-3f)
            }
            lastItem = sorterPanel
        }

        sorterContainerPanelLocal.addUIElement(sorterContainerTooltip).inBMid(4f)

        outerPanel!!.addComponent(sorterContainerPanelLocal).inTMid(46f)
    }

    fun closeSorterPanel() {
        sortersForItems.forEach { it.saveToPersistentData() }
        outerPanel!!.removeComponent(sorterContainerPanel)
        sorterContainerPanel = null

        layoutPanels()
    }

    protected abstract fun getSortersFromItem(item: T): List<String>

    inner class SorterButtonHandler : ButtonHandler() {
        override fun onClicked() {
            sorterButton!!.isChecked = false
            if (this@SortedListPanelPlugin.sorterContainerPanel == null) {
                sorterButton!!.text = MagicTxt.getString("mb_confirm")
                createSorterPanel()
            } else {
                sorterButton!!.text =
                    sorterButtonText()
                closeSorterPanel()
            }
        }
    }

    private fun sorterButtonText() =
        MagicTxt.getString("mb_sort")
}