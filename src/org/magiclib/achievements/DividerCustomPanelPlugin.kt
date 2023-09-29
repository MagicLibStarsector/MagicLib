package org.magiclib.achievements

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.lwjgl.opengl.GL11
import java.awt.Color

class DividerCustomPanelPlugin @JvmOverloads constructor(
    val width: Float,
    val height: Float = 1f,
    var color: Color,
) : BaseCustomUIPanelPlugin() {
    private var sideRatio: Float = 0.5f
    private var pos: PositionAPI? = null

    fun addTo(tooltip: TooltipMakerAPI) {
        val elementPanel: CustomPanelAPI = Global.getSettings().createCustom(width, height, this)
        val comp = tooltip.addCustom(elementPanel, 0f)
        pos = comp.position

        val innerElement = elementPanel.createUIElement(width, height, false)
        elementPanel.addUIElement(innerElement)
    }

    override fun render(alphaMult: Float) {
        val position = pos ?: return
        val x = position.x
        val y = position.y
        val w = position.width
        val h = height

        renderBox(x, y, w, h, alphaMult)
    }

    private fun renderBox(x: Float, y: Float, w: Float, h: Float, alphaMult: Float) {
        val lh: Float = h * sideRatio
        val lw: Float = w * sideRatio

        val points = floatArrayOf( // upper left
            0f, h - lh,
            0f, h,
            0 + lw, h,  // upper right
            w - lw, h,
            w, h,
            w, h - lh,  // lower right
            w, lh,
            w, 0f,
            w - lw, 0f,  // lower left
            lw, 0f,
            0f, 0f,
            0f, lh
        )
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.3f * alphaMult)
        for (i in 0..3) {
            GL11.glBegin(GL11.GL_LINES)
            run {
                val index = i * 6
                GL11.glVertex2f(points[index] + x, points[index + 1] + y)
                GL11.glVertex2f(points[index + 2] + x, points[index + 3] + y)
                GL11.glVertex2f(points[index + 2] + x, points[index + 3] + y)
                GL11.glVertex2f(points[index + 4] + x, points[index + 5] + y)
            }
            GL11.glEnd()
        }
        GL11.glPopMatrix()
    }
}