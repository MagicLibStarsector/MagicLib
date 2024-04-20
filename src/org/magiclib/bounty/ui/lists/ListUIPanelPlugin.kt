package org.magiclib.bounty.ui.lists

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.*
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin

abstract class ListUIPanelPlugin<T>(protected var parentPanel: CustomPanelAPI) : InteractiveUIPanelPlugin() {
    protected abstract val listHeader: String
    protected open val rowHeight = 64f
    protected open val rowWidth = 240f
    protected val pad = 3f
    protected val opad = 10f

    private var listeners: MutableList<ListListener<T>> = mutableListOf()

    override var panelWidth: Float = this.getListWidth()
    override var panelHeight: Float = Global.getSettings().screenHeight * 0.66f

    protected var lastMembers: List<T>? = null
    var outerPanel: CustomPanelAPI? = null
    protected var outerTooltip: TooltipMakerAPI? = null
    protected var innerPanel: CustomPanelAPI? = null
    @Transient
    var scroller: ScrollPanelAPI? = null

    fun getListWidth(): Float {
        return rowWidth
    }

    open fun getListHeight(rows: Int): Float {
        return opad + (rowHeight + pad) * rows
    }

    open fun createListHeader(tooltip: TooltipMakerAPI): UIComponentAPI {
        tooltip.addSectionHeading(listHeader, Alignment.MID, 0f)
        return tooltip.prev
    }

    fun layoutPanels(): CustomPanelAPI {
        return layoutPanels(lastMembers!!)
    }

    open fun layoutPanels(members: List<T>): CustomPanelAPI {
        if (outerPanel != null) {
            outerTooltip!!.removeComponent(innerPanel)
            outerPanel!!.removeComponent(outerTooltip)
            parentPanel.removeComponent(outerPanel)
            clearItems()
        }

        val outerPanelLocal = outerPanel ?: parentPanel.createCustomPanel(panelWidth, panelHeight, this)
        outerPanel = outerPanelLocal

        var validMembers = members.filter { shouldMakePanelForItem(it) }
        lastMembers = validMembers
        validMembers = sortMembers(validMembers)

        val outerTooltipLocal = outerPanelLocal.createUIElement(panelWidth, panelHeight, false)
        outerTooltip = outerTooltipLocal

        val headerComponent = createListHeader(outerTooltipLocal)

        val holdingPanel = outerPanelLocal.createCustomPanel(panelWidth, panelHeight, null)
        innerPanel = holdingPanel
        val scrollerTooltip: TooltipMakerAPI = holdingPanel.createUIElement(panelWidth, panelHeight, true)
        val scrollingPanel: CustomPanelAPI =
            holdingPanel.createCustomPanel(panelWidth, getListHeight(validMembers.size), null)
        val tooltip: TooltipMakerAPI = scrollingPanel.createUIElement(panelWidth, panelHeight, false)

        var lastItem: UIPanelAPI? = null
        validMembers
            .map { it to createPanelForItem(tooltip, it) }
            .filter { (_, rowPlugin) -> rowPlugin != null }
            .forEach { (_, rowPlugin) ->
                lastItem = placeItem(tooltip, rowPlugin!!, lastItem)
            }

        scrollingPanel.addUIElement(tooltip).inTL(0f, 0f)
        scrollerTooltip.addCustom(scrollingPanel, 0f).position.inTL(0f, 0f)
        holdingPanel.addUIElement(scrollerTooltip).inTL(0f, 0f)
        outerTooltipLocal.addCustom(holdingPanel, 0f).position.belowMid(headerComponent, 0f)
        outerPanelLocal.addUIElement(outerTooltipLocal).inTL(0f, 0f)
        this.parentPanel.addComponent(outerPanelLocal).inTL(0f, 0f)
        scroller = scrollerTooltip.externalScroller

        return outerPanelLocal
    }

    protected open fun placeItem(listTooltip: TooltipMakerAPI, rowPlugin: ListItemUIPanelPlugin<T>, lastRowPanel: UIPanelAPI?): UIPanelAPI {
        if (rowPlugin.panel == null) {
            rowPlugin.setUpItem(listTooltip)
        }
        if (lastRowPanel != null) {
            rowPlugin.panel!!.position.belowLeft(lastRowPanel, pad)
        } else {
            rowPlugin.panel!!.position.inTL(0f, pad)
        }
        clickables[rowPlugin.panel!!] = ListItemButtonHandler(rowPlugin, this)
        return rowPlugin.panel!!
    }

    open fun sortMembers(items: List<T>): List<T> {
        return items
    }

    fun clearItems() {
        clickables.clear()
    }

    protected open fun shouldMakePanelForItem(item: T): Boolean {
        return true
    }

    abstract fun createPanelForItem(tooltip: TooltipMakerAPI, item: T): ListItemUIPanelPlugin<T>?

    fun itemClicked(item: T) {
        pickedItem(item)

        listeners.forEach {
            it.pickedItem(item)
        }
    }

    open fun pickedItem(item: T) {
    }

    fun addListener(listener: ListListener<T>) {
        listeners.add(listener)
    }

    fun interface ListListener<T> {
        fun pickedItem(item: T)
    }
}