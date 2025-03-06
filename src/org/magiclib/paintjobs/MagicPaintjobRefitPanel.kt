package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.loading.specs.HullVariantSpec
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

internal fun createMagicPaintjobRefitPanel(variant : HullVariantSpec): CustomPanelAPI {
    class MagicPaintjobRefitPanelPlugin : BaseCustomUIPanelPlugin() {
        lateinit var paintjobPanel: CustomPanelAPI
        val backgroundAlpha = 0.65f

        override fun renderBelow(alphaMult: Float) {
            val p = paintjobPanel.position

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

            val panelColor = Color(8, 23, 27, 255)
            GL11.glColor4f(
                panelColor.red / 255f, panelColor.green / 255f, panelColor.blue / 255f,
                panelColor.alpha * alphaMult / 255f
            )
            GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)

            GL11.glPopMatrix()
        }

        override fun render(alphaMult: Float) {
            val p = paintjobPanel.position
            val borderColor = Misc.getDarkPlayerColor()
            val darkerBorderColor = borderColor.darker()
            GL11.glPushMatrix()

            GL11.glTranslatef(0f, 0f, 0f)
            GL11.glRotatef(0f, 0f, 0f, 1f)

            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)

            GL11.glColor4f(darkerBorderColor.red / 255f, darkerBorderColor.green / 255f,
                darkerBorderColor.blue / 255f, darkerBorderColor.alpha * alphaMult  / 255f
            )
            drawBorder(p.x,p.y,p.x+p.width,p.y+p.height)
            GL11.glColor4f(borderColor.red / 255f, borderColor.green / 255f,
                borderColor.blue / 255f, borderColor.alpha * alphaMult  / 255f
            )
            drawBorder(p.x-210,p.y+p.height+7-850,p.x-210+1250,p.y+p.height+7)
            GL11.glPopMatrix()
        }

        fun drawBorder(x1: Float, y1: Float, x2: Float, y2: Float){
            GL11.glRectf(x1, y1, x2+1, y1-1)
            GL11.glRectf(x2, y1, x2+1, y2+1)
            GL11.glRectf(x1, y2, x1-1, y1-1)
            GL11.glRectf(x2, y2, x1-1, y2+1)
        }

        override fun processInput(events: MutableList<InputEventAPI>?) {
            events!!.forEach { event ->
                if (!event.isConsumed && event.isKeyboardEvent && event.eventValue == Keyboard.KEY_ESCAPE) {
                    paintjobPanel.getParent()!!.removeComponent(paintjobPanel)
                    event.consume()
                } else if (!event.isConsumed && (event.isKeyboardEvent || event.isMouseMoveEvent ||
                            event.isMouseDownEvent || event.isMouseScrollEvent)) {
                    event.consume()
                }
            }
        }

        override fun advance(amount: Float) {
        }
    }

    val plugin = MagicPaintjobRefitPanelPlugin()
    val paintjobPanel = Global.getSettings().createCustom(700f, 800f, plugin)
    plugin.paintjobPanel = paintjobPanel

    val shipPreviews = makeShipPreviews(variant)
    var prev: UIPanelAPI? = null
    shipPreviews.forEach { preview ->
        paintjobPanel.addComponent(preview).let { pos ->
            if (prev == null) pos.inTL(0f, 10f) else pos.rightOfTop(prev, 3f)
            prev = preview
        }
    }

    return paintjobPanel
}

private fun makeShipPreviews(hullVariantSpec: HullVariantSpec): List<UIPanelAPI> {
    val baseHullPaintjobs = MagicPaintjobManager.getPaintjobsForHull(
        (hullVariantSpec as ShipVariantAPI).hullSpec.baseHullId,
        includeShiny = false
    )
    val shipPreviews = mutableListOf<UIPanelAPI>()
    (listOf(null) + baseHullPaintjobs).forEach { baseHullPaintjob ->
        val shipPreview = ReflectionUtils.instantiate(MagicPaintjobRefitPanelCreator.SHIP_PREVIEW_CLASS!!)!!

        ReflectionUtils.invoke("setVariant", shipPreview, hullVariantSpec)
        ReflectionUtils.invoke("overrideVariant", shipPreview, hullVariantSpec)
        ReflectionUtils.invoke("setShowBorder", shipPreview, false)
        ReflectionUtils.invoke("setScaleDownSmallerShipsMagnitude", shipPreview, 1f)
        ReflectionUtils.invoke("adjustOverlay", shipPreview, 0f, 0f)
        (shipPreview as UIPanelAPI).position.setSize(200f, 200f)

        // make the ship list so the ships exist when we try and get them
        ReflectionUtils.invoke("prepareShip", shipPreview)
        val ships = ReflectionUtils.get(MagicPaintjobRefitPanelCreator.SHIPS_FIELD!!, shipPreview) as Array<ShipAPI>

        // if the paintjob exists, replace the sprites
        if (baseHullPaintjob != null) {
            ships.forEach { ship ->
                val paintjobs = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId, includeShiny = false)
                paintjobs.forEach { paintjob ->
                    if (paintjob.id.contains(baseHullPaintjob.id, true)) {
                        MagicPaintjobManager.applyPaintjob(null, ship, paintjob)
                    }
                }
            }
        }
        shipPreviews.add(shipPreview)

    }
    return shipPreviews
}


