package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.loading.specs.HullVariantSpec
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

internal class MagicPaintjobSelectorPlugin(val paintjobSpec: MagicPaintjobSpec?) : BaseCustomUIPanelPlugin() {
    lateinit var selectorPanel: CustomPanelAPI

    val isUnlocked = paintjobSpec == null || paintjobSpec in MagicPaintjobManager.unlockedPaintjobs
    var isHovering = false
        private set

    var isSelected = false

    var hasClicked = false
        private set

    val highlightFader = FaderUtil(0.0F, 0.05F, 0.25F)
    private val hoverFader = FaderUtil(0.0F, 0.05F, 0.25F)
    private val lockedHoverFader = FaderUtil(0.0F, 0.25F, 0.5F)
    private val clickFader = FaderUtil(0.0F, 0.05F, 0.25F)
    private var onClickFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onClickOutsideFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onClickReleaseFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
    private var onHoverFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onHoverEnterFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
    private var onHoverExitFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()

    init{
        onClickFunctions.add {
            if(isUnlocked) clickFader.fadeIn()
        }
        onClickReleaseFunctions.add { clickFader.fadeOut() }
        onHoverEnterFunctions.add {
            if(isUnlocked) hoverFader.fadeIn()
            lockedHoverFader.fadeIn()
        }
        onHoverExitFunctions.add {
            clickFader.fadeOut()
            hoverFader.fadeOut()
            lockedHoverFader.fadeOut()
        }
    }

    override fun renderBelow(alphaMult: Float) {
        val p = selectorPanel.position

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)

        val defaultColor = Color.BLACK
        val clickedColor = Misc.getDarkPlayerColor()
        val hoverColor = Misc.getDarkPlayerColor().darker()
        val highlightColor = Misc.getDarkPlayerColor().darker().darker()

        var panelColor = Misc.interpolateColor(defaultColor, highlightColor, highlightFader.brightness)
        panelColor = Misc.interpolateColor(panelColor, hoverColor, hoverFader.brightness)
        panelColor = Misc.interpolateColor(panelColor, clickedColor, clickFader.brightness)

        GL11.glColor4f(
            panelColor.red / 255f, panelColor.green / 255f, panelColor.blue / 255f,
            panelColor.alpha * alphaMult / 255f
        )
        GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)

        val borderColor = Misc.getDarkPlayerColor()
        val darkerBorderColor = borderColor.darker()

        GL11.glColor4f(
            darkerBorderColor.red / 255f, darkerBorderColor.green / 255f,
            darkerBorderColor.blue / 255f, darkerBorderColor.alpha * alphaMult / 255f
        )
        drawBorder(p.x,p.y,p.x+p.width,p.y+p.height)

        GL11.glPopMatrix()
    }

    override fun render(alphaMult: Float) {
        val p = selectorPanel.position
        GL11.glPushMatrix()

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        if (!isUnlocked) {
            val lockedAlpha = Misc.interpolate(0.7f, 0f, lockedHoverFader.brightness)
            val lockedColor = Color.BLACK
            GL11.glColor4f(
                lockedColor.red / 255f, lockedColor.green / 255f, lockedColor.blue / 255f,
                lockedAlpha * lockedColor.alpha * alphaMult / 255f
            )
            GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)
            val lockedSprite = Global.getSettings().getSprite("icons", "lock")
            lockedSprite.alphaMult = Misc.interpolate(0.7f, 0f, lockedHoverFader.brightness)
            val scaleFactor = 0.5f * p.width / max(lockedSprite.width, lockedSprite.height)
            if(scaleFactor < 1)
                lockedSprite.setSize(scaleFactor*lockedSprite.width, scaleFactor*lockedSprite.height)
            lockedSprite.renderAtCenter(p.x+p.width/2, p.y+p.height-p.width/2)
        }
        GL11.glPopMatrix()
    }

    private fun drawBorder(x1: Float, y1: Float, x2: Float, y2: Float){
        GL11.glRectf(x1, y1, x2 + 1, y1 - 1)
        GL11.glRectf(x2, y1, x2 + 1, y2 + 1)
        GL11.glRectf(x1, y2, x1 - 1, y1 - 1)
        GL11.glRectf(x2, y2, x1 - 1, y2 + 1)
    }

    override fun processInput(events: MutableList<InputEventAPI>?) {
        val p = selectorPanel.position
        events!!.filter { it.isMouseEvent }.forEach { event ->

            val inElement = event.x.toFloat() in p.x..(p.x + p.width) &&
                    event.y.toFloat() in p.y..(p.y + p.height)
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
    }

    fun onClick(function: (InputEventAPI) -> Unit) { onClickFunctions.add(function) }
    fun onClickRelease(function: (InputEventAPI) -> Unit) { onClickReleaseFunctions.add(function) }
    fun onClickOutside(function: (InputEventAPI) -> Unit) { onClickOutsideFunctions.add(function) }
    fun onHover(function: (InputEventAPI) -> Unit) { onHoverFunctions.add(function) }
    fun onHoverEnter(function: (InputEventAPI) -> Unit) { onHoverEnterFunctions.add(function) }
    fun onHoverExit(function: (InputEventAPI) -> Unit) { onHoverExitFunctions.add(function) }

    override fun advance(amount: Float) {
        highlightFader.advance(amount)
        hoverFader.advance(amount)
        lockedHoverFader.advance(amount)
        clickFader.advance(amount)

        if(isSelected) highlightFader.fadeIn()
        else highlightFader.fadeOut()
    }
}

internal fun createPaintjobSelector(hullVariantSpec: HullVariantSpec, paintjobSpec: MagicPaintjobSpec?,
                                    width: Float): Pair<CustomPanelAPI, MagicPaintjobSelectorPlugin>{
    val descriptionHeight = 45f
    val topPad = 5f

    val plugin = MagicPaintjobSelectorPlugin(paintjobSpec)
    val selectorPanel = Global.getSettings().createCustom(width, width+descriptionHeight, plugin)
    plugin.selectorPanel = selectorPanel

    val shipPreview = createShipPreview(hullVariantSpec, paintjobSpec, width, width)
    selectorPanel.addComponent(shipPreview).inTL(0f, topPad)

    val textElement = selectorPanel.createUIElement(width, descriptionHeight-topPad, false)
    selectorPanel.addUIElement(textElement)
    with(textElement){
        position.inTL(0f, width+topPad)
        setTitleOrbitronLarge()
        addTitle(paintjobSpec?.name ?: "Default")
        addPara(paintjobSpec?.description ?: "The Standard Paintjob", 3f)
    }

    return Pair(selectorPanel, plugin)
}

private fun createShipPreview(hullVariantSpec: HullVariantSpec, basePaintjobSpec: MagicPaintjobSpec?,
                              width: Float, height: Float): UIPanelAPI {

    val clonedVariant = hullVariantSpec.clone()
    MagicPaintjobManager.removePaintjobFromShip(clonedVariant)
    clonedVariant.moduleVariants?.values?.forEach { moduleVariant ->
        MagicPaintjobManager.removePaintjobFromShip(moduleVariant as ShipVariantAPI)
    }

    val shipPreview = ReflectionUtils.instantiate(MagicPaintjobCombatRefitAdder.SHIP_PREVIEW_CLASS!!)!!
    ReflectionUtils.invoke("setVariant", shipPreview, clonedVariant)
    ReflectionUtils.invoke("overrideVariant", shipPreview, clonedVariant)
    ReflectionUtils.invoke("setShowBorder", shipPreview, false)
    ReflectionUtils.invoke("setScaleDownSmallerShipsMagnitude", shipPreview, 1f)
    ReflectionUtils.invoke("adjustOverlay", shipPreview, 0f, 0f)
    (shipPreview as UIPanelAPI).position.setSize(width, height)

    // make the ship list so the ships exist when we try and get them
    ReflectionUtils.invoke("prepareShip", shipPreview)

    // if the paintjob exists, replace the sprites
    basePaintjobSpec?.let { paintjob ->
        for(ship in ReflectionUtils.get(MagicPaintjobCombatRefitAdder.SHIPS_FIELD!!, shipPreview) as Array<ShipAPI>){
            MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId).firstOrNull {
                it.paintjobFamily?.equals(paintjob.paintjobFamily) == true
            }?.let { MagicPaintjobManager.applyPaintjob(ship, it) }
        }
    }

    return shipPreview
}