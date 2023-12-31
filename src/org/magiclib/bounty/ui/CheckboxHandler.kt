package org.magiclib.bounty.ui

open class CheckboxHandler {
    open fun onHighlighted() {}

    open fun onUnhighlighted() {}

    open fun onClicked(checked: Boolean) {}
}

open class FunctionalCheckboxHandler(val handler: (Boolean) -> Unit): CheckboxHandler() {
    override fun onClicked(checked: Boolean) {
        handler.invoke(checked)
    }
}