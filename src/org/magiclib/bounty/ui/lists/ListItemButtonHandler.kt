package org.magiclib.bounty.ui.lists

import org.magiclib.bounty.ui.ButtonHandler

class ListItemButtonHandler<T>(val rowPlugin: ListItemUIPanelPlugin<T>, val listPlugin: ListUIPanelPlugin<T>): ButtonHandler() {
    override fun onClicked() {
        if (rowPlugin.disabled) return
        listPlugin.itemClicked(rowPlugin.item)
    }
}