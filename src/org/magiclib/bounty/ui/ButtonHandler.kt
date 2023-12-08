package org.magiclib.bounty.ui

open class ButtonHandler {
    open fun onHighlighted() {}

    open fun onUnhighlighted() {}

    open fun onClicked() {}
}

class FunctionalButtonHandler(val handler: () -> Unit): ButtonHandler() {
    override fun onClicked() {
        handler.invoke()
    }
}