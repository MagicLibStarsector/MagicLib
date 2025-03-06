package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.*
import java.awt.Color

internal fun UIPanelAPI.getChildrenCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

internal fun UIPanelAPI.getChildrenNonCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

internal fun UIPanelAPI.getWidth(): Float {
    return ReflectionUtils.invoke("getWidth", this) as Float
}

internal fun UIPanelAPI.getHeight(): Float {
    return ReflectionUtils.invoke("getHeight", this) as Float
}

internal fun UIPanelAPI.clearChildren() {
    ReflectionUtils.invoke("clearChildren", this)
}

internal fun UIComponentAPI.getParent(): UIPanelAPI? {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}

internal fun TooltipMakerAPI.getParentWidget(): UIComponentAPI? {
    return ReflectionUtils.invoke("getParentWidget", this) as UIPanelAPI
}

internal fun UIComponentAPI.setOpacity(alpha: Float) {
    ReflectionUtils.invoke("setOpacity", this, alpha)
}

internal fun TooltipMakerAPI.addTooltip(
    to: UIComponentAPI,
    location: TooltipMakerAPI.TooltipLocation,
    width: Float,
    lambda: (TooltipMakerAPI) -> Unit
) {
    this.addTooltipTo(object : TooltipMakerAPI.TooltipCreator {
        override fun isTooltipExpandable(tooltipParam: Any?): Boolean {
            return false
        }

        override fun getTooltipWidth(tooltipParam: Any?): Float {
            return width
        }

        override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
            lambda(tooltip!!)
        }

    }, to, location)
}

internal fun UIPanelAPI.addButton(text: String, baseColor: Color, bgColor: Color, align: Alignment, style: CutStyle,
                                  width: Float, height: Float): ButtonAPI{
    // make a button in a temp panel/element
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    tempTMAPI.setButtonFontOrbitron20()
    val button = tempTMAPI.addButton(text, null, baseColor, bgColor, align, style, width, height, 0f)

    // hijack button and move it to UIPanel
    this.addComponent(button)
    button.position.setXAlignOffset(0f)
    button.position.setYAlignOffset(0f)
    return button
}

// CustomPanelAPI implements the same Listener that a ButtonAPI requires,
// A CustomPanel then happens to trigger its CustomUIPanelPlugin buttonPressed() method
// thus we can map our functions into a CustomUIPanelPlugin, and have them be triggered
private class ButtonListener(button: ButtonAPI) : BaseCustomUIPanelPlugin() {
    private val onClickFunctions = mutableListOf<() -> Unit>()

    init {
        val buttonListener = Global.getSettings().createCustom(0f, 0f, this)
        val setListenerMethod = ReflectionUtils.getMethodsOfName("setListener", button)[0]
        ReflectionUtils.rawInvoke(setListenerMethod, button, buttonListener)
    }

    override fun buttonPressed(buttonId: Any?) {
        onClickFunctions.forEach { it() }
    }

    fun addOnClick(function: () -> Unit) {
        onClickFunctions.add(function)
    }
}

// Extension function for ButtonAPI
internal fun ButtonAPI.onClick(function: () -> Unit) {
    // Use reflection to check if this button already has a listener
    val existingListener = ReflectionUtils.invoke("getListener", this)
    if (existingListener is ButtonListener) {
        existingListener.addOnClick(function)
    } else {
        // if not, make one
        val listener = ButtonListener(this)
        listener.addOnClick(function)
    }
}
