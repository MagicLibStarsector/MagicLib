package org.magiclib.bounty.ui

import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import org.lwjgl.input.Mouse

open class InteractiveUIPanelPlugin : BaseUIPanelPlugin() {
    var highlightedButton: UIComponentAPI? = null
    val buttons: MutableMap<ButtonAPI, ButtonHandler> = hashMapOf()
    val clickables: MutableMap<UIComponentAPI, ButtonHandler> = hashMapOf()
    private val checkBoxes: MutableList<CheckboxData> = mutableListOf()
    var eatAllClicks = false

    open fun advancePanel(amount: Float) {
    }

    final override fun advance(amount: Float) {
        var checkedButton: ButtonAPI? = null
        buttons.forEach { (button, _) ->
            if (button.isChecked && button.isEnabled) {
                checkedButton = button
                return@forEach
            }
        }

        checkedButton?.let {
            buttons[it]?.onClicked()
        }

        checkBoxes.forEach {
            it.checkButton()
        }

        advancePanel(amount)
    }

    override fun processInput(events: List<InputEventAPI>) {
        if (highlightedButton != null) {
            checkHighlightedButton(highlightedButton!!, events)
        } else {
            checkButtons(events)
            checkClickables(events)
        }

        if (eatAllClicks) {
            events.forEach {
                if ((it.isMouseDownEvent || it.isMouseUpEvent)
                    && it.x.toFloat() in pos.x..(pos.x + pos.width)
                    && it.y.toFloat() in pos.y..(pos.y + pos.height)
                    && !it.isConsumed
                ) {
                    it.consume()
                }
            }
        }
    }

    fun checkClickables(events: List<InputEventAPI>) {
        clickables.forEach { (uiComp, listener) ->
            val containedEvents: List<InputEventAPI> = events
                .filter { !it.isConsumed }
                .filter { uiComp.position.containsEvent(it) }

            if (containedEvents.isNotEmpty() && highlightedButton == null) {
                listener.onHighlighted()
                highlightedButton = uiComp
            }
        }
    }

    fun checkButtons(events: List<InputEventAPI>) {
        buttons.forEach { (button, listener) ->
            val containedEvents: List<InputEventAPI> = events
                .filter { !it.isConsumed }
                .filter { button.position.containsEvent(it) }

            if (containedEvents.isNotEmpty()) {
                if (highlightedButton == null) {
                    listener.onHighlighted()
                    highlightedButton = button
                }
            }
        }
    }

    fun checkHighlightedButton(uiComp: UIComponentAPI, events: List<InputEventAPI>) {
        if (highlightedButton !is ButtonAPI) {
            val containedEvents: List<InputEventAPI> = events
                .filter { !it.isConsumed }
                .filter { uiComp.position.containsEvent(it) }

            if (containedEvents.isEmpty()) {
                clickables[uiComp]?.onUnhighlighted()
                highlightedButton = null
            }

            containedEvents
                .filter { it.isLMBUpEvent && !it.isConsumed }
                .forEach {
                    clickables[uiComp]?.onClicked()
                    it.consume()
                }
        } else {
            val containedEvents: List<InputEventAPI> = events
                .filter { !it.isConsumed }
                .filter { uiComp.position.containsEvent(it) }

            if (containedEvents.isEmpty() && !Mouse.isButtonDown(0)) {
                buttons[uiComp]?.onUnhighlighted()
                highlightedButton = null
            }
        }
    }

    fun addCheckbox(button: ButtonAPI, handler: CheckboxHandler) {
        checkBoxes.add(CheckboxData(button, handler))
    }

    fun addCheckbox(button: ButtonAPI, handler: (Boolean) -> Unit) {
        checkBoxes.add(CheckboxData(button, FunctionalCheckboxHandler(handler)))
    }

    fun addButton(button: ButtonAPI, handler: () -> Unit) {
        buttons[button] = FunctionalButtonHandler(handler)
    }
}

private class CheckboxData(val button: ButtonAPI, val handler: CheckboxHandler) {
    var wasChecked = false

    /**
     * Every frame, check state of button. If different than last frame, call click handler.
     */
    fun checkButton() {
        if (!button.isEnabled) return

        if (button.isChecked != wasChecked) {
            wasChecked = button.isChecked
            handler.onClicked(wasChecked)
        }
    }
}