package org.magiclib.achievements


import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * From LunaLib.
 */
internal class ProgressBarInternal(
    private var value: Float,
    private var minvalue: Float,
    private var maxValue: Float,
    textColor: Color,
    private val tooltip: TooltipMakerAPI,
    private var width: Float,
    private var height: Float,
    private val displayText: Boolean
) : CustomUIPanelPlugin {

    // LunaElement
    private var parentElement: TooltipMakerAPI = tooltip
    private var elementPanel: CustomPanelAPI
    private var innerElement: TooltipMakerAPI
    private var paragraph: LabelAPI? = null

    private var x: Float
    private var y: Float

    private var isHeld = false

    private var isHovering = false
    private var hoverEnter = false

    private var position: PositionAPI

    private var backgroundColor = Misc.getDarkPlayerColor().darker()
    private var borderColor = Misc.getDarkPlayerColor()

    private var renderBorder = true

    private var enableTransparency = false
    private var borderAlpha = 1f
    private var backgroundAlpha = 1f

    private var selectionGroup = ""

    private var customData: HashMap<String, Any?> = HashMap()

    private var positionFunc: MutableList<(position: PositionAPI) -> Unit> = ArrayList()
    private var advanceFunc: MutableList<(amount: Float) -> Unit> = ArrayList()
    private var renderFunc: MutableList<(alphaMult: Float) -> Unit> = ArrayList()
    private var renderBGFunc: MutableList<(alphaMult: Float) -> Unit> = ArrayList()
    private var inputFunc: MutableList<(inputs: MutableList<InputEventAPI>) -> Unit> = ArrayList()

    private var onClickFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onClickOutsideFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()

    private var onHeldFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()

    private var onHoverFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onHoverEnterFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onHoverExitFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()

    init {
        this.parentElement = tooltip
        this.elementPanel = Global.getSettings().createCustom(width, height, this)
        val comp = tooltip.addCustom(elementPanel, 0f)
        this.position = comp.position

        this.x = position.x
        this.y = position.y

        this.innerElement = elementPanel.createUIElement(width, height, false)
        elementPanel.addUIElement(innerElement)

        selectionGroup = "all"
    }

    private fun addText(
        text: String,
        baseColor: Color = Misc.getTextColor(),
        highlightColor: Color = Misc.getHighlightColor(),
        highlights: List<String> = listOf()
    ) {
        paragraph = innerElement.addPara(text, 0f, baseColor, highlightColor, *highlights.toTypedArray())
    }

    private fun centerText() {
        if (paragraph == null) {
            addText("")
        }
        paragraph!!.position.inTL(
            width / 2 - paragraph!!.computeTextWidth(paragraph!!.text) / 2,
            height / 2 - paragraph!!.computeTextHeight(paragraph!!.text) / 2
        )
    }

    override fun positionChanged(position: PositionAPI?) {
        if (position != null) {
            width = position.width
            height = position.height
            x = position.x
            y = position.y

            positionFunc.forEach {
                it(position)
            }
        }
    }

    override fun render(alphaMult: Float) {

        if (renderBorder) {
            val c = borderColor
            GL11.glPushMatrix()

            GL11.glTranslatef(0f, 0f, 0f)
            GL11.glRotatef(0f, 0f, 0f, 1f)

            GL11.glDisable(GL11.GL_TEXTURE_2D)

            if (enableTransparency) {
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            } else {
                GL11.glDisable(GL11.GL_BLEND)
            }

            GL11.glColor4f(
                c.red / 255f,
                c.green / 255f,
                c.blue / 255f,
                c.alpha / 255f * (alphaMult * borderAlpha)
            )

            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            GL11.glVertex2f(x, y)
            GL11.glVertex2f(x, y + height)
            GL11.glVertex2f(x + width, y + height)
            GL11.glVertex2f(x + width, y)
            GL11.glVertex2f(x, y)

            GL11.glEnd()
            GL11.glPopMatrix()
        }

        renderFunc.forEach {
            it(alphaMult * borderAlpha)
        }
    }

    override fun advance(amount: Float) {
        advanceFunc.forEach {
            it(amount)
        }
    }

    override fun processInput(events: MutableList<InputEventAPI>?) {
        if (events == null) return

        for (event in events) {
            if (event.isMouseEvent) {
                if (event.x.toFloat() in x..(x + width) && event.y.toFloat() in y..(y + height)) {
                    if (!hoverEnter) {
                        hoverEnter = true
                        isHovering = true
                        for (onHoverEnter in onHoverEnterFunctions) {
                            onHoverEnter(event)
                        }
                        onHoverEnter(event)
                    }
                    for (onHover in onHoverFunctions) {
                        isHovering = true
                        onHover(event)
                    }
                    onHover(event)
                } else {
                    if (isHovering) {
                        for (onHoverExit in onHoverExitFunctions) {
                            onHoverExit(event)
                        }
                        hoverEnter = false
                        isHovering = false
                        onHoverExit(event)
                    }
                }
            }

            if (event.isMouseDownEvent) {
                if (event.x.toFloat() in x..(x + width) && event.y.toFloat() in y..(y + height)) {
                    isHeld = true
                    for (onClick in onClickFunctions) {
                        onClick(event)
                    }
                    onClick(event)
                    event.consume()
                } else {
                    for (onClickOutside in onClickOutsideFunctions) {
                        onClickOutside(event)
                    }
                    onClickOutside(event)
                }
            } else if (event.isMouseUpEvent) {
                /*for (onNotHeld in onNotHeldFunctions)
                {
                    onNotHeld(event)
                }*/
                isHeld = false
            } else if (event.isMouseEvent && isHeld) {
                for (onHeld in onHeldFunctions) {
                    onHeld(event)
                }
                event.consume()
            }
        }

        inputFunc.forEach {
            it(events)
        }
    }

    private fun onClick(input: InputEventAPI) {}

    private fun onClickOutside(input: InputEventAPI) {}

    private fun onHover(input: InputEventAPI) {}

    private fun onHoverEnter(input: InputEventAPI) {}

    private fun onHoverExit(input: InputEventAPI) {}

    private fun advance(function: (Float) -> Unit) {
        advanceFunc.add(function)
    }

    private fun render(function: (Float) -> Unit) {
        renderFunc.add(function)
    }

    private fun onClick(function: (InputEventAPI) -> Unit) {
        onClickFunctions.add(function)
    }

    private fun onHover(function: (InputEventAPI) -> Unit) {
        onHoverFunctions.add(function)
    }

    override fun buttonPressed(buttonId: Any?) {

    }

    // LunaProgressBar

    init {
        value = MathUtils.clamp(value, minvalue, maxValue)

        if (displayText) {
            addText(
                "${value.toString().trimEnd('0').trimEnd('.')} / ${maxValue.toString().trimEnd('0').trimEnd('.')}",
                baseColor = textColor
            )
            centerText()
        }

        renderBorder = true
    }

    private fun getValue() = value

    override fun renderBelow(alphaMult: Float) {
        // LunaElement
        renderBGFunc.forEach {
            it(alphaMult * backgroundAlpha)
        }

        // LunaProgressBar
        val level = (value - minvalue) / (maxValue - minvalue)

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        if (enableTransparency) {
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        } else {
            GL11.glDisable(GL11.GL_BLEND)
        }

        var curPos = x
        val segments = 30
        val colorPerSegment = 1f / segments
        var curColor = 1f

        GL11.glBegin(GL11.GL_QUAD_STRIP)

        for (segment in 0..segments) {

            val bc = backgroundColor
            val c = Color((bc.red * curColor).toInt(), (bc.green * curColor).toInt(), (bc.blue * curColor).toInt())
            curColor -= colorPerSegment / 3

            GL11.glColor4f(
                c.red / 255f,
                c.green / 255f,
                c.blue / 255f,
                c.alpha / 255f * (alphaMult * backgroundAlpha)
            )

            GL11.glVertex2f(curPos, y)
            GL11.glVertex2f(curPos, y + height)
            curPos += width / segments * level
        }

        GL11.glEnd()
        GL11.glPopMatrix()
    }
}