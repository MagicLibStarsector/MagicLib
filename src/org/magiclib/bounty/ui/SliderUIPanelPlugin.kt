package org.magiclib.bounty.ui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.ui.LazyFont
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.absoluteValue

open class SliderUIPanelPlugin(private val min: Float,
                               private val max: Float,
                               var value: Float = min + (max - min).absoluteValue / 2f,
                               var title: String? = null) : InteractiveUIPanelPlugin() {
    companion object {
        private val lazyFont: LazyFont = LazyFont.loadFont("graphics/fonts/insignia15LTaa.fnt")
    }
    private val minLabel: LazyFont.DrawableString = lazyFont.createText(Misc.getRoundedValue(min))
    private val maxLabel: LazyFont.DrawableString = lazyFont.createText(Misc.getRoundedValue(max))
    private val currLabel: LazyFont.DrawableString = lazyFont.createText(Misc.getRoundedValue(value))
    private val titleLabel: LazyFont.DrawableString = lazyFont.createText(Misc.getRoundedValue(value))

    init {
        minLabel.fontSize = 14 * Global.getSettings().screenScaleMult
        minLabel.baseColor = Color.RED
        minLabel.alignment = LazyFont.TextAlignment.LEFT
        maxLabel.alignment = LazyFont.TextAlignment.RIGHT
        currLabel.alignment = LazyFont.TextAlignment.CENTER
        titleLabel.alignment = LazyFont.TextAlignment.CENTER
    }

    private val listeners: MutableList<SliderValueListener> = mutableListOf()
    private var baseBarColor = Misc.getBasePlayerColor().darker()
    private var hoveredBarColor = Misc.getBrightPlayerColor()
    private var disabledBarColor = baseBarColor
    private var barColor = baseBarColor
    var barHeight = 16f * Global.getSettings().screenScaleMult
        set(value) { field = value * Global.getSettings().screenScaleMult}
    var enabled = true
    var panel: CustomPanelAPI? = null

    fun layoutPanel(rootPanel: CustomPanelAPI): CustomPanelAPI {
        val panelLocal: CustomPanelAPI = rootPanel.createCustomPanel(panelWidth, panelHeight, this)
        rootPanel.addComponent(panelLocal)
        panel = panelLocal
        return panelLocal
    }

    override fun processInput(events: List<InputEventAPI>) {
        if (!enabled) {
            barColor = disabledBarColor
            return
        }

        var isHovered = false
        var oldValue = value
        events
            .filter { !it.isConsumed }
            .forEach {
                if (it.x.toFloat() in pos.x..(pos.x + pos.width)
                    && it.y.toFloat() in pos.y..(pos.y + panelHeight - barHeight)
                ) {
                    if (it.isMouseDownEvent || it.isMouseUpEvent || it.isLMBEvent || it.isLMBDownEvent) {
                        val fillRatio = (it.x.toFloat() - pos.x) / ((pos.x + pos.width) - pos.x)
                        value = min + (max - min) * fillRatio
                        it.consume()
                    } else if (it.isMouseMoveEvent) {
                        isHovered = true
                    }
                }
            }

        if (isHovered) {
            barColor = hoveredBarColor
        } else {
            barColor = baseBarColor
        }

        if (value != oldValue) {
            currLabel.text = Misc.getRoundedValue(value)
            listeners.forEach { it.valueChanged(oldValue, value) }
        }
    }

    fun updateLabel() {
        currLabel.text = Misc.getRoundedValue(value)
    }

    override fun renderBelow(alphaMult: Float) {
        val c = if (bgColor.alpha > 0) bgColor
        else Color(0, 0, 0)

        // bar background
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * 1f)
        )
        GL11.glRectf(pos.x, pos.y, pos.x + pos.width, pos.y + pos.height)

        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    override fun render(alphaMult: Float) {
        val x = pos.x
        val y = pos.y
        val width = pos.width
        val height = pos.height
        var c = Misc.getDarkPlayerColor()

        GL11.glPushMatrix()
        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        //outside border
        GL11.glColor4f(
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * 1f)
        )

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        GL11.glVertex2f(x, y + panelHeight - barHeight)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x + width, y)
        GL11.glVertex2f(x + width, y + panelHeight - barHeight)
        GL11.glVertex2f(x, y + panelHeight - barHeight)

        GL11.glEnd()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glPopMatrix()

        //filling box
        c = barColor
        val fillRatio = (value - min) / (max - min)
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (alphaMult * 1f)
        )
        GL11.glRectf(x + 1, y + 1, x + (width - 1) * fillRatio, y + (panelHeight - barHeight - 1))

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()

        minLabel.draw(x, y + height)
        maxLabel.draw(x + width - maxLabel.width, y + height)
        currLabel.drawOutlined(x + width / 2f - currLabel.width, y + height / 2f)

        title?.let {
            titleLabel.text = it
            titleLabel.drawOutlined(x + width / 2f - titleLabel.width / 2f, y + height)
        }
    }

    fun addListener(listener: SliderValueListener) {
        listeners.add(listener)
    }
}

fun interface SliderValueListener {
    fun valueChanged(oldValue: Float, newValue: Float)
}

fun LazyFont.DrawableString.drawOutlined(x: Float, y: Float, outlineColor: Color = Color.black) {
    val oldColor = this.baseColor
    this.baseColor = Color.black
    this.draw(x - 1, y - 1)
    this.draw(x + 1, y - 1)
    this.draw(x - 1, y + 1)
    this.draw(x + 1, y + 1)
    this.baseColor = oldColor
    this.draw(x, y)
}