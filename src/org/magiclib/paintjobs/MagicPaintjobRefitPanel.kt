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
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

internal fun createMagicPaintjobRefitPanel(variant : HullVariantSpec): CustomPanelAPI {
    val endPad = 7f
    val midPad = 5f
    val selectorsPerRow = 3
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

            val panelColor = Color(3, 3, 3, 255)
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
            drawBorder(p.x-210,p.y+p.height+7-850,p.x-210+1250,p.y+p.height+7)

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

    //val innerElement = paintjobPanel.createUIElement(700f, 800f, true)
    //innerElement.position.inTL(0f, 0f)

    val baseHullPaintjobs = MagicPaintjobManager.getPaintjobsForHull(
        (variant as ShipVariantAPI).hullSpec.baseHullId, false)


    var firstInRow: UIPanelAPI? = null
    var prev: UIPanelAPI? = null
    (listOf(null) + baseHullPaintjobs).forEachIndexed { index, paintjobSpec ->
        val selectorWidth = (paintjobPanel.getWidth()-(endPad*2+midPad*(selectorsPerRow-1)))/selectorsPerRow
        val selector = createPaintjobSelector(variant, paintjobSpec, selectorWidth)
        paintjobPanel.addComponent(selector).let { pos ->
            if (prev == null) {
                pos.inTL(endPad, endPad)
                firstInRow = selector
            }
            else if(index % selectorsPerRow == 0){
                pos.belowLeft(firstInRow, midPad)
                firstInRow = selector
            }
            else pos.rightOfTop(prev, midPad)
            prev = selector
        }
    }

    return paintjobPanel
}

private fun createPaintjobSelector(hullVariantSpec: HullVariantSpec, paintjobSpec: MagicPaintjobSpec?, width: Float):
        CustomPanelAPI{
    class MagicPaintjobSelectorPlugin : BaseCustomUIPanelPlugin() {
        lateinit var selectorPanel: CustomPanelAPI
        val descriptionHeight = 45f
        val unlocked = paintjobSpec == null || paintjobSpec in MagicPaintjobManager.unlockedPaintjobs
        var isHovering = false
            private set

        var isSelected = false

        var hasClicked = false
            private set

        private val highlightFader = FaderUtil(0.0F, 0.05F, 0.25F)
        private val hoverFader = FaderUtil(0.0F, 0.05F, 0.25F)
        private val clickFader = FaderUtil(0.0F, 0.05F, 0.25F)
        private var onClickFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
        private var onClickOutsideFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
        private var onClickReleaseFunctions: MutableList<(InputEventAPI) -> Unit> = ArrayList()
        private var onHoverFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
        private var onHoverEnterFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()
        private var onHoverExitFunctions: MutableList<(InputEventAPI) -> Unit> = mutableListOf()

        init{
            onClickFunctions.add {
                if(unlocked) clickFader.fadeIn()
                if(unlocked) isSelected = !isSelected
            }
            onClickReleaseFunctions.add { clickFader.fadeOut() }
            onHoverEnterFunctions.add { if(unlocked) hoverFader.fadeIn() }
            onHoverExitFunctions.add {
                clickFader.fadeOut()
                hoverFader.fadeOut()
            }
        }

        override fun renderBelow(alphaMult: Float) {
            val p = selectorPanel.position

            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_BLEND)

            val defaultColor = Color(3, 3, 3, 255)
            val highlightColor = Color(14, 44, 51, 255)
            val hoverColor = Color(17, 51, 61, 255)
            val clickedColor = Color(30, 90, 100, 255)

            var panelColor = OKLabInterpolateColor(defaultColor, highlightColor, highlightFader.brightness)
            panelColor = OKLabInterpolateColor(panelColor, hoverColor, hoverFader.brightness)
            panelColor = OKLabInterpolateColor(panelColor, clickedColor, clickFader.brightness)

            //panelColor = clickedColor
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
            if (!unlocked) {
                val lockedColor = Color( 0, 0, 0, 200)
                GL11.glColor4f(
                    lockedColor.red / 255f, lockedColor.green / 255f, lockedColor.blue / 255f,
                    lockedColor.alpha * alphaMult / 255f
                )
                GL11.glRectf(p.x, p.y, p.x + p.width, p.y + p.height)
            }
            GL11.glPopMatrix()
        }

        fun drawBorder(x1: Float, y1: Float, x2: Float, y2: Float){
            GL11.glRectf(x1, y1, x2+1, y1-1)
            GL11.glRectf(x2, y1, x2+1, y2+1)
            GL11.glRectf(x1, y2, x1-1, y1-1)
            GL11.glRectf(x2, y2, x1-1, y2+1)
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
            clickFader.advance(amount)

            if(isSelected) highlightFader.fadeIn()
            else highlightFader.fadeOut()
        }
    }

    val plugin = MagicPaintjobSelectorPlugin()
    val selectorPanel = Global.getSettings().createCustom(width, width+plugin.descriptionHeight, plugin)
    plugin.selectorPanel = selectorPanel

    val shipPreview = makeShipPreview(hullVariantSpec, paintjobSpec, width, width)
    selectorPanel.addComponent(shipPreview).inTL(0f, 5f)

    val textElement = selectorPanel.createUIElement(width, plugin.descriptionHeight, false)
    selectorPanel.addUIElement(textElement)
    with(textElement){
        position.inTL(0f, width+5f)
        setTitleOrbitronLarge()
        addTitle(paintjobSpec?.name ?: "Default")
        addPara(paintjobSpec?.description ?: "The Vanilla Paintjob", 3f)
    }

    return selectorPanel
}

private fun makeShipPreview(hullVariantSpec: HullVariantSpec, basePaintjobSpec: MagicPaintjobSpec?,
                            width: Float, height: Float): UIPanelAPI {

    val clonedVariant = hullVariantSpec.clone()
    MagicPaintjobManager.removePaintjobFromShip(clonedVariant)

    val shipPreview = ReflectionUtils.instantiate(MagicPaintjobRefitPanelCreator.SHIP_PREVIEW_CLASS!!)!!
    ReflectionUtils.invoke("setVariant", shipPreview, clonedVariant)
    ReflectionUtils.invoke("overrideVariant", shipPreview, clonedVariant)
    ReflectionUtils.invoke("setShowBorder", shipPreview, false)
    ReflectionUtils.invoke("setScaleDownSmallerShipsMagnitude", shipPreview, 1f)
    ReflectionUtils.invoke("adjustOverlay", shipPreview, 0f, 0f)
    (shipPreview as UIPanelAPI).position.setSize(width, height)

    // make the ship list so the ships exist when we try and get them
    ReflectionUtils.invoke("prepareShip", shipPreview)
    val ships = ReflectionUtils.get(MagicPaintjobRefitPanelCreator.SHIPS_FIELD!!, shipPreview) as Array<ShipAPI>

    // if the paintjob exists, replace the sprites
    basePaintjobSpec?.let { paintjob ->
        ships.forEach { ship ->
            val modulePaintjobs = MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId, false)
            modulePaintjobs.forEach { modulePaintjob ->
                if (modulePaintjob.id.contains(paintjob.id, true)) {
                    MagicPaintjobManager.applyPaintjob(null, ship, modulePaintjob)
                }
            }
        }
    }

    return shipPreview
}


