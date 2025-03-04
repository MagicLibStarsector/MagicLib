package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI

fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

fun UIPanelAPI.getChildrenNonCopy() : List<UIComponentAPI>  {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

fun UIPanelAPI.getWidth() : Float  {
    return ReflectionUtils.invoke("getWidth", this) as Float
}

fun UIPanelAPI.getHeight() : Float  {
    return ReflectionUtils.invoke("getHeight", this) as Float
}

fun UIPanelAPI.clearChildren() {
    ReflectionUtils.invoke("clearChildren", this)
}

fun UIComponentAPI.getParent() : UIPanelAPI?  {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}

fun TooltipMakerAPI.getParentWidget() : UIComponentAPI? {
    return ReflectionUtils.invoke("getParentWidget", this) as UIPanelAPI
}

fun UIComponentAPI.setOpacity(alpha: Float)
{
    ReflectionUtils.invoke("setOpacity", this, alpha)
}

fun TooltipMakerAPI.addTooltip(to: UIComponentAPI, location: TooltipMakerAPI.TooltipLocation, width: Float, lambda: (TooltipMakerAPI) -> Unit) {
    this.addTooltipTo(object: TooltipMakerAPI.TooltipCreator {
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
fun ButtonAPI.onClick(function: () -> Unit) {
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
