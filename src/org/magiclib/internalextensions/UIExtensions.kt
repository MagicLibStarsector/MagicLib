package org.magiclib.internalextensions


import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.ui.*
import com.fs.graphics.util.Fader
import org.magiclib.ReflectionUtils
import java.awt.Color

internal var UIComponentAPI.fader: Fader?
    get() = ReflectionUtils.invoke("getFader", this) as Fader?
    set(fader) {
        ReflectionUtils.invoke("setFader", this, fader)
    }

internal var UIComponentAPI.opacity: Float
    get() = ReflectionUtils.invoke("getOpacity", this) as Float
    set(alpha) {
        ReflectionUtils.invoke("setOpacity", this, alpha)
    }

internal var UIComponentAPI.parent: UIPanelAPI?
    get() = ReflectionUtils.invoke("getParent", this) as UIPanelAPI?
    set(parent) {
        ReflectionUtils.invoke("setParent", this, parent)
    }

internal fun UIComponentAPI.setMouseOverPad(pad1: Float, pad2: Float, pad3: Float, pad4: Float) {
    ReflectionUtils.invoke("setMouseOverPad", this, pad1, pad2, pad3, pad4)
}

internal val UIComponentAPI.mouseoverHighlightFader: Fader?
    get() = ReflectionUtils.invoke("getMouseoverHighlightFader", this) as Fader?

internal val UIComponentAPI.topAncestor: UIPanelAPI?
    get() = ReflectionUtils.invoke("findTopAncestor", this) as UIPanelAPI?

internal fun UIComponentAPI.setTooltipOffsetFromCenter(xPad: Float, yPad: Float){
    ReflectionUtils.invoke("setTooltipOffsetFromCenter", this, xPad, yPad)
}

internal fun UIComponentAPI.setTooltipPositionRelativeToAnchor(xPad: Float, yPad: Float, anchor: UIComponentAPI){
    ReflectionUtils.invoke("setTooltipPositionRelativeToAnchor", this, xPad, yPad, anchor)
}

internal fun UIComponentAPI.setSlideData(xOffset: Float, yOffset: Float, durationIn: Float, durationOut: Float){
    ReflectionUtils.invoke("setSlideData", this, xOffset, yOffset, durationIn, durationOut)
}

internal fun UIComponentAPI.slideIn(){
    ReflectionUtils.invoke("slideIn", this)
}

internal fun UIComponentAPI.slideOut(){
    ReflectionUtils.invoke("slideOut", this)
}

internal fun UIComponentAPI.forceSlideIn(){
    ReflectionUtils.invoke("forceSlideIn", this)
}

internal fun UIComponentAPI.forceSlideOut(){
    ReflectionUtils.invoke("forceSlideOut", this)
}

internal val UIComponentAPI.sliding: Boolean
    get() = ReflectionUtils.invoke("isSliding", this) as Boolean

internal val UIComponentAPI.slidIn: Boolean
    get() = ReflectionUtils.invoke("isSlidIn", this) as Boolean

internal val UIComponentAPI.slidOut: Boolean
    get() = ReflectionUtils.invoke("isSlidOut", this) as Boolean

internal val UIComponentAPI.slidingIn: Boolean
    get() = ReflectionUtils.invoke("isSlidingIn", this) as Boolean

internal var UIComponentAPI.enabled: Boolean
    get() = ReflectionUtils.invoke("isEnabled", this) as Boolean
    set(enabled) {
        ReflectionUtils.invoke("setEnabled", this, enabled)
    }

internal var UIComponentAPI.width
    get() = this.position.width
    set(width) { this.position.setSize(width, this.position.height) }

internal var UIComponentAPI.height
    get() = this.position.height
    set(height) { this.position.setSize(this.position.width, height) }

internal fun UIComponentAPI.setSize(width: Float, height: Float){
    this.position.setSize(width, height)
}

internal val UIComponentAPI.x
    get() = this.position.x

internal val UIComponentAPI.y
    get() = this.position.y

internal val UIComponentAPI.left
    get() = this.x

internal val UIComponentAPI.bottom
    get() = this.y

internal val UIComponentAPI.top
    get() = this.y + this.height

internal val UIComponentAPI.right
    get() = this.x + this.width

internal fun UIComponentAPI.setLocation(x: Float, y: Float){
    this.position.setLocation(x, y)
}

internal val UIComponentAPI.centerX
    get() = this.position.centerX

internal val UIComponentAPI.centerY
    get() = this.position.centerY

internal var UIComponentAPI.xAlignOffset: Float
    get() = ReflectionUtils.invoke("getXAlignOffset", this.position) as Float
    set(xOffset) { this.position.setXAlignOffset(xOffset) }

internal var UIComponentAPI.yAlignOffset: Float
    get() = ReflectionUtils.invoke("getYAlignOffset", this.position) as Float
    set(yOffset) { this.position.setYAlignOffset(yOffset) }

internal fun UIPanelAPI.getChildrenCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
}

internal fun UIPanelAPI.getChildrenNonCopy(): List<UIComponentAPI> {
    return ReflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
}

internal fun UIPanelAPI.findChildWithMethod(methodName: String): UIComponentAPI? {
    return getChildrenCopy().find { ReflectionUtils.hasMethodOfName(methodName, it) }
}

internal fun UIPanelAPI.clearChildren() {
    ReflectionUtils.invoke("clearChildren", this)
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

internal enum class Font {
    VICTOR_10,
    VICTOR_14,
    ORBITRON_20,
    ORBITRON_20_BOLD,
    ORBITRON_24,
    ORBITRON_24_BOLD
}

internal fun UIPanelAPI.addButton(
    text: String, data: Any?, baseColor: Color, bgColor: Color, align: Alignment, style: CutStyle,
    font: Font?, width: Float, height: Float): ButtonAPI{
    // make a button in a temp panel/element
    val tempPanel = Global.getSettings().createCustom(width, height, null)
    val tempTMAPI = tempPanel.createUIElement(width, height, false)
    when(font){
        Font.VICTOR_10 -> tempTMAPI.setButtonFontVictor10()
        Font.VICTOR_14 -> tempTMAPI.setButtonFontVictor14()
        Font.ORBITRON_20 -> tempTMAPI.setButtonFontOrbitron20()
        Font.ORBITRON_20_BOLD -> tempTMAPI.setButtonFontOrbitron20Bold()
        Font.ORBITRON_24 -> tempTMAPI.setButtonFontOrbitron24()
        Font.ORBITRON_24_BOLD -> tempTMAPI.setButtonFontOrbitron24Bold()
        null -> tempTMAPI.setButtonFontDefault()
    }
    val button = tempTMAPI.addButton(text, data, baseColor, bgColor, align, style, width, height, 0f)

    // hijack button and move it to UIPanel
    this.addComponent(button)
    button.xAlignOffset = 0f
    button.yAlignOffset = 0f
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

internal var CustomPanelAPI.plugin: CustomUIPanelPlugin?
    get() = ReflectionUtils.getFieldsOfType(this, CustomUIPanelPlugin::class.java).firstOrNull()?.let {
        ReflectionUtils.get(it, this)
    } as CustomUIPanelPlugin?
    set(plugin) = ReflectionUtils.getFieldsOfType(this, CustomUIPanelPlugin::class.java).first().let {
        ReflectionUtils.set(it, this, plugin)
    }
