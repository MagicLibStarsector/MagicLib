package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

internal fun createMagicPaintjobRefitPanel(): CustomPanelAPI {
    class MagicPaintjobRefitPanelPlugin : BaseCustomUIPanelPlugin() {
        lateinit var refitPanel: CustomPanelAPI
        val backgroundAlpha = 0.7f

        override fun renderBelow(alphaMult: Float) {
            val p = refitPanel.position

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)

            GL11.glDisable(GL11.GL_BLEND)

            val panelColor = Global.getSettings().darkPlayerColor
            GL11.glColor4f(
                panelColor.red / 255f, panelColor.green / 255f, panelColor.blue / 255f,
                panelColor.alpha * alphaMult / 255f
            )

            GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)

            GL11.glPopMatrix()

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            val bgColor = Color.BLACK
            GL11.glColor4f(
                bgColor.red / 255f, bgColor.green / 255f, bgColor.blue / 255f,
                backgroundAlpha * bgColor.alpha * alphaMult / 255f
            )

            GL11.glRectf(0f, 0f, Global.getSettings().screenWidth, Global.getSettings().screenHeight)

            GL11.glPopMatrix()
        }

        override fun processInput(events: MutableList<InputEventAPI>?) {
            events!!.forEach { event ->
                if (!event.isConsumed && event.isKeyboardEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                    refitPanel.getParent()!!.removeComponent(refitPanel)
                    event.consume()
                } else if (!event.isConsumed && (event.isKeyboardEvent || event.isMouseMoveEvent || event.isMouseDownEvent)) {
                    event.consume()
                }
            }
        }

        override fun advance(amount: Float) {
        }
    }

    val plugin = MagicPaintjobRefitPanelPlugin()
    val panel = Global.getSettings().createCustom(1000f, 500f, plugin)
    plugin.refitPanel = panel
    return panel
}


