package org.magiclib.bounty.ui

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.min

open class BaseUIPanelPlugin : CustomUIPanelPlugin {
    lateinit var pos: PositionAPI
    open var panelWidth: Float = 0f
    open var panelHeight: Float = 0f
    open var renderBackground: Boolean = false

    val iconSize: Float
        get() {
            return min(panelWidth, panelHeight)
        }
    open var bgColor: Color = Color(0, 0, 0, 0)

    fun setBGColor(
        red: Int = bgColor.red,
        green: Int = bgColor.green,
        blue: Int = bgColor.blue,
        alpha: Int = bgColor.alpha
    ) {
        bgColor = Color(red, green, blue, alpha)
    }

    override fun positionChanged(position: PositionAPI) {
        pos = position
    }

    override fun renderBelow(alphaMult: Float) {
        if (renderBackground || bgColor.alpha > 0) {
            var c = if (bgColor.alpha > 0) bgColor
            else Color(0, 0, 0)

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
            GL11.glRectf(pos!!.x, pos!!.y, pos!!.x + pos!!.width, pos!!.y + pos!!.height)

            GL11.glDisable(GL11.GL_BLEND)
            GL11.glPopMatrix()
        }
    }

    override fun render(alphaMult: Float) {
        if (renderBackground) {
            val x = pos.x
            val y = pos.y
            val width = pos.width
            val height = pos.height

            var c = Misc.getDarkPlayerColor()
            GL11.glPushMatrix()

            GL11.glTranslatef(0f, 0f, 0f)
            GL11.glRotatef(0f, 0f, 0f, 1f)

            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(
                c.red / 255f,
                c.green / 255f,
                c.blue / 255f,
                c.alpha / 255f * (alphaMult * 1f)
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
    }

    override fun advance(amount: Float) {
    }

    override fun processInput(events: List<InputEventAPI>) {
    }

    fun isHovered(events: List<InputEventAPI>): Boolean {
        return events
            .filter { !it.isConsumed }
            .any { pos.containsEvent(it) }
    }

    override fun buttonPressed(buttonId: Any?) {
    }
}

