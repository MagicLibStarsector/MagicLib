package org.magiclib.bounty.ui.lists

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.ui.BaseUIPanelPlugin

abstract class ListItemUIPanelPlugin<T>(var item: T): BaseUIPanelPlugin() {
    var panel: CustomPanelAPI? = null
    var disabled: Boolean = false

    fun setUpItem(tooltip: TooltipMakerAPI): CustomPanelAPI {
        panel = layoutPanel(tooltip)
        return panel!!
    }

    abstract fun layoutPanel(tooltip: TooltipMakerAPI): CustomPanelAPI
}