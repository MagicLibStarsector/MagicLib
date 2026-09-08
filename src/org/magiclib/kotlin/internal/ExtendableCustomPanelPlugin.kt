package org.magiclib.kotlin.internal

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import org.magiclib.ReflectionUtils.set

internal class ExtendableCustomUIPanelPlugin(var customPanel: CustomPanelAPI) : BaseCustomUIPanelPlugin() {
    private var onClickFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onClickOutsideFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onClickReleaseFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onHoverFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onHoverEnterFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onHoverExitFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onHeldFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onKeyDownFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onKeyUpFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()

    private var renderBelowFunctions: MutableList<(Float) -> Unit> = mutableListOf()
    private var renderFunctions: MutableList<(Float) -> Unit> = mutableListOf()

    private var advanceFunctions: MutableList<(Float) -> Unit> = mutableListOf()

    var inputCaptureTopPad = 0f
    var inputCaptureBottomPad = 0f
    var inputCaptureLeftPad = 0f
    var inputCaptureRightPad = 0f

    var isHovering = false
        private set

    var hasClicked = false
        private set

    fun renderBelow(function: (alphaMult: Float) -> Unit) { renderBelowFunctions.add(function) }
    override fun renderBelow(alphaMult: Float) {
        renderBelowFunctions.forEach {
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            it(alphaMult)
            GL11.glPopMatrix()
        }
    }

    fun render(function: (alphaMult: Float) -> Unit) { renderFunctions.add(function) }
    override fun render(alphaMult: Float) {
        renderFunctions.forEach {
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            it(alphaMult)
            GL11.glPopMatrix()
        }
    }

    fun advance(function: (amount: Float) -> Unit) { advanceFunctions.add(function) }
    override fun advance(amount: Float) { advanceFunctions.forEach { it(amount) } }

    override fun processInput(events: MutableList<InputEventAPI>?) {
        events!!.filter { it.isMouseEvent }.forEach { event ->

            val inElement = event.x.toFloat() in (left-inputCaptureLeftPad)..(right+inputCaptureRightPad) &&
                    event.y.toFloat() in (bottom-inputCaptureBottomPad)..(top+inputCaptureTopPad)
            if (inElement) {
                for (onHover in onHoverFunctions) onHover(event)
                if (!isHovering) onHoverEnterFunctions.forEach { it(event) }
                isHovering = true
                if (event.isMouseDownEvent) {
                    hasClicked = true
                    onClickFunctions.forEach { it(event) }
                }
                if (event.isMouseUpEvent && hasClicked){
                    hasClicked = false
                    onClickReleaseFunctions.forEach { it(event) }
                }
                if (Mouse.isButtonDown(0)) onHeldFunctions.forEach { it(event) }
            } else {
                if (isHovering) onHoverExitFunctions.forEach { it(event) }
                isHovering = false
                if (event.isMouseDownEvent) {
                    onClickOutsideFunctions.forEach { it(event) }
                }
                if (event.isMouseUpEvent){
                    hasClicked = false
                }
            }
        }

        events.filter { it.isKeyboardEvent }.forEach { event ->
            if (event.isKeyDownEvent) onKeyDownFunctions.forEach { it(event) }
            if (event.isKeyUpEvent) onKeyUpFunctions.forEach { it(event) }
        }
    }

    fun onClick(function: (InputEventAPI) -> Unit) { onClickFunctions.add(function) }
    fun onClickRelease(function: (InputEventAPI) -> Unit) { onClickReleaseFunctions.add(function) }
    fun onClickOutside(function: (InputEventAPI) -> Unit) { onClickOutsideFunctions.add(function) }
    fun onHover(function: (InputEventAPI) -> Unit) { onHoverFunctions.add(function) }
    fun onHoverEnter(function: (InputEventAPI) -> Unit) { onHoverEnterFunctions.add(function) }
    fun onHoverExit(function: (InputEventAPI) -> Unit) { onHoverExitFunctions.add(function) }
    fun onHeld(function: (InputEventAPI) -> Unit) { onHeldFunctions.add(function) }
    fun onKeyDown(function: (InputEventAPI) -> Unit) { onKeyDownFunctions.add(function) }
    fun onKeyUp(function: (InputEventAPI) -> Unit) { onKeyUpFunctions.add(function) }


    companion object {
        internal val ExtendableCustomUIPanelPlugin.width
            get() = customPanel.width

        internal val ExtendableCustomUIPanelPlugin.height
            get() = customPanel.height

        internal val ExtendableCustomUIPanelPlugin.x
            get() = customPanel.x

        internal val ExtendableCustomUIPanelPlugin.y
            get() = customPanel.y

        internal val ExtendableCustomUIPanelPlugin.left
            get() = x

        internal val ExtendableCustomUIPanelPlugin.bottom
            get() = y

        internal val ExtendableCustomUIPanelPlugin.top
            get() = y + height

        internal val ExtendableCustomUIPanelPlugin.right
            get() = x + width

        internal val ExtendableCustomUIPanelPlugin.centerX
            get() = customPanel.centerX

        internal val ExtendableCustomUIPanelPlugin.centerY
            get() = customPanel.centerY

        internal val ExtendableCustomUIPanelPlugin.xAlignOffset
            get() = customPanel.xAlignOffset

        internal val ExtendableCustomUIPanelPlugin.yAlignOffset
            get() = customPanel.yAlignOffset

        internal fun CustomPanelAPI.setPlugin(plugin: CustomUIPanelPlugin) {
            set(value = plugin)
        }

        internal fun UIPanelAPI.CustomPanel(
            width: Float,
            height: Float,
            builder: CustomPanelAPI.(plugin: ExtendableCustomUIPanelPlugin) -> Unit = {}
        ): CustomPanelAPI {
            val panel = Global.getSettings().createCustom(width, height, null)
            val plugin = ExtendableCustomUIPanelPlugin(panel)
            panel.setPlugin(plugin)
            this.addComponent(panel)
            panel.builder(plugin)
            return panel
        }

        internal fun UIComponentAPI.anchorInBottomLeftOfParent(xPad: Float = 0f, yPad: Float = 0f) {
            this.position.inBL(xPad, yPad)
        }
    }
}