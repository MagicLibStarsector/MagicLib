package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.loading.specs.HullVariantSpec
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

internal fun createMagicPaintjobRefitPanel(refitPanel : UIPanelAPI, width: Float, height: Float,
                                           inCampaign: Boolean): CustomPanelAPI {
    val endPad = 6f
    val midPad = 5f
    val selectorsPerRow = 3
    class MagicPaintjobRefitPanelPlugin : BaseCustomUIPanelPlugin() {
        lateinit var paintjobPanel: CustomPanelAPI
        val backgroundAlpha = 0.69f

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

            val panelColor = Color.BLACK
            GL11.glColor4f(
                panelColor.red / 255f, panelColor.green / 255f, panelColor.blue / 255f,
                panelColor.alpha * alphaMult / 255f
            )
            GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)

            GL11.glDisable(GL11.GL_BLEND)

            val borderColor = Misc.getDarkPlayerColor()
            GL11.glColor4f(
                borderColor.red / 255f, borderColor.green / 255f, borderColor.blue / 255f,
                borderColor.alpha * alphaMult / 255f
            )
            val rp = refitPanel.position
            val outerBorderWidth = if(inCampaign) 1250 else 1010
            val outerBorderHeight = if(inCampaign) 850 else 734
            drawBorder(rp.x-210,rp.y-3,rp.x-210+outerBorderWidth,rp.y-3+outerBorderHeight)

            val darkerBorderColor = borderColor.darker()
            GL11.glColor4f(
                darkerBorderColor.red / 255f, darkerBorderColor.green / 255f,
                darkerBorderColor.blue / 255f, darkerBorderColor.alpha * alphaMult / 255f
            )
            drawBorder(p.x,p.y,p.x+p.width,p.y+p.height)

            GL11.glPopMatrix()
        }

        fun drawBorder(x1: Float, y1: Float, x2: Float, y2: Float){
            GL11.glRectf(x1, y1, x2+1, y1-1)
            GL11.glRectf(x2, y1, x2+1, y2+1)
            GL11.glRectf(x1, y2, x1-1, y1-1)
            GL11.glRectf(x2, y2, x1-1, y2+1)
        }

        override fun processInput(events: MutableList<InputEventAPI>?) {
            for (event in events!!) {
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

    val paintjobPlugin = MagicPaintjobRefitPanelPlugin()
    val paintjobPanel = Global.getSettings().createCustom(width, height, paintjobPlugin)
    paintjobPlugin.paintjobPanel = paintjobPanel

    // borders are drawn outside of panel, so +2 needed to lineup scrollbar with border
    val scrollerTooltip = paintjobPanel.createUIElement(width+2f, height, true)
    scrollerTooltip.position.inTL(0f, 0f)

    val shipDisplay = ReflectionUtils.invoke("getShipDisplay", refitPanel) as UIPanelAPI
    val baseVariant = ReflectionUtils.invoke("getCurrentVariant", shipDisplay) as HullVariantSpec

    val currentPaintjob = MagicPaintjobManager.getCurrentShipPaintjob(baseVariant)
    val baseHullPaintjobs = MagicPaintjobManager.getPaintjobsForHull(
        (baseVariant as ShipVariantAPI).hullSpec.baseHullId, false)

    val selectorWidth = (paintjobPanel.getWidth()-(endPad*2+midPad*(selectorsPerRow-1)))/selectorsPerRow
    var firstInRow: UIPanelAPI? = null
    var prev: UIPanelAPI? = null
    val selectorPlugins = mutableListOf<MagicPaintjobSelectorPlugin>()

    (listOf(null) + baseHullPaintjobs).forEachIndexed { index, paintjobSpec ->
        // make panel
        val (selectorPanel, selectorPlugin) = createPaintjobSelector(baseVariant, paintjobSpec, selectorWidth)
        selectorPlugins.add(selectorPlugin)
        if(currentPaintjob == paintjobSpec) {
            selectorPlugin.isSelected = true
            selectorPlugin.highlightFader.forceIn()
        }

        // add panel and position them into the grid
        scrollerTooltip.addCustomDoNotSetPosition(selectorPanel).position.let { pos ->
            if (prev == null) {
                pos.inTL(endPad, endPad)
                firstInRow = selectorPanel
            }
            else if(index % selectorsPerRow == 0){
                pos.belowLeft(firstInRow, midPad)
                firstInRow = selectorPanel
            }
            else pos.rightOfTop(prev, midPad)
            prev = selectorPanel
        }

        // add tooltip to locked paintjobs
        if(!selectorPlugin.isUnlocked && !paintjobSpec?.unlockConditions.isNullOrBlank()){
            scrollerTooltip.addTooltip(selectorPanel, TooltipMakerAPI.TooltipLocation.BELOW, 250f) { tooltip ->
                tooltip.addTitle("LOCKED")
                tooltip.addPara(paintjobSpec!!.unlockConditions, 0f)
            }
        }
    }

    // sync all the selectors, and apply the paintjob
    for (selectorPlugin in selectorPlugins) {
        selectorPlugin.onClick {
            if (selectorPlugin.isUnlocked){
                selectorPlugins.forEach { it.isSelected = false }
                selectorPlugin.isSelected = true

                if(selectorPlugin.paintjobSpec == null) MagicPaintjobManager.removePaintjobFromShip(baseVariant)
                else MagicPaintjobManager.applyPaintjob(baseVariant, selectorPlugin.paintjobSpec)

                baseVariant.moduleVariants?.values?.forEach { moduleVariant ->

                    if(selectorPlugin.paintjobSpec == null)
                        MagicPaintjobManager.removePaintjobFromShip(moduleVariant)
                    else{
                        val moduleHullID = (moduleVariant as ShipVariantAPI).hullSpec.hullId
                        MagicPaintjobManager.getPaintjobsForHull(moduleHullID).firstOrNull {
                            it.paintjobFamily == selectorPlugin.paintjobSpec.paintjobFamily
                        }?.let { MagicPaintjobManager.applyPaintjob(moduleVariant, it) }
                    }
                }
                ReflectionUtils.invoke("syncWithCurrentVariant", refitPanel)
                ReflectionUtils.invoke("updateModules", shipDisplay)
                ReflectionUtils.invoke("updateButtonPositionsToZoomLevel", shipDisplay)
            }
        }
    }

    // add scroll at end after setting heightSoFar, needed when using addCustom to the tooltip
    val rows = (baseHullPaintjobs.size/selectorsPerRow) + 1
    scrollerTooltip.heightSoFar = endPad*2 + prev!!.getHeight()*rows + midPad*(rows-1)
    paintjobPanel.addUIElement(scrollerTooltip)
    return paintjobPanel
}


