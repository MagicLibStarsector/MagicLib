package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.MagicLunaElementInternal
import org.magiclib.util.MagicTxt
import java.awt.Color

class MagicPaintjobIntel : BaseIntelPlugin() {
    override fun getName(): String = MagicTxt.getString("ml_mp_intelName")

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        panel ?: return
        val opad = 10f
        val pad = 3f
        val pjs = MagicPaintjobManager.paintjobs
        val pjMainContainer = panel.createUIElement(width, height, true)
        val pjMain = panel.createCustomPanel(width, height, null)
        val baseUnit = 8f
        val cellWidth = baseUnit * 18
        val cellHeight = baseUnit * 20
        val imageSize = baseUnit * 12
        val gridWidth = (width / cellWidth).toInt()
        val padding = 10f

        var x = 0
        var y = 0
        for (pj in pjs) {
            val isUnlocked = MagicPaintjobManager.unlockedPaintjobIds.contains(pj.id)
            val pjCell = pjMain.createUIElement(cellWidth + padding, cellHeight, false)
            Global.getSettings().loadTexture(pj.spriteId)
            pjCell.addImage(pj.spriteId, imageSize, imageSize, opad)
            pjCell.addPara(pj.name, Misc.getHighlightColor(), opad)
            if (pj.description.isNullOrBlank().not()) {
                pjCell.addPara(pj.description, pad)
            }

            val shipsThatPjMayApplyTo = Global.getSector().playerFleet.fleetData.membersListCopy
                .filter { it.hullId == pj.hullId }

            if (shipsThatPjMayApplyTo.any()) {
                val count = shipsThatPjMayApplyTo.count()
                pjCell.addPara("Applies to $count in fleet.", pad, Misc.getHighlightColor(), count.toString())
            }
            val xPos = x * cellWidth
            val yPos = y * cellHeight
            pjMain.addUIElement(pjCell).inTL(xPos + padding, yPos)

            if (!isUnlocked)
                addDarkenCover(pjMain, cellWidth, cellHeight, xPos, yPos)
            val hoverElement = addHoverHighlight(pjMain, cellWidth, cellHeight, xPos, yPos)

            // When you click on a paintjob, it will show you the ships in your fleet that it may apply to.
            hoverElement.onClick { inputEvent ->
                val pjApplierUIElement = MagicLunaElementInternal()
                    .apply {
                        if (shipsThatPjMayApplyTo.none())
                            addTo(pjMain, cellWidth * 2, baseUnit * 10, xPos, yPos)
                        else
                            addTo(pjMain, width, height, xPos, yPos)

                        renderBackground = true
                        renderBorder = true
                        val timer = IntervalUtil(.4f, .4f)

                        // Remove after mouse leaves it (after initial timeout has elapsed).
                        advance {
                            timer.advance(it)
                            if (timer.intervalElapsed() && !isHovering) {
                                removeFromParent()
                            }
                        }
                    }

                if (shipsThatPjMayApplyTo.none()) {
                    pjApplierUIElement.addText(
                        text = " This paintjob cannot be applied",
                        baseColor = Misc.getHighlightColor(),
                        padding = opad
                    )
                    pjApplierUIElement.addText(
                        text = " to any ships in your fleet.",
                        baseColor = Misc.getHighlightColor()
                    )
                } else {
                    // Display ships in fleet that this paintjob may apply to (and whether it's applied).
                    shipsThatPjMayApplyTo.forEach { fleetShip ->
                        val isWearingPj = MagicPaintjobManager.getCurrentShipPaintjob(fleetShip)?.id == pj.id
                        val spriteName = fleetShip.spriteOverride ?: fleetShip.hullSpec.spriteName
                        Global.getSettings().loadTexture(spriteName)
                        val sprite = Global.getSettings().getSprite(spriteName)
                        val ratio = sprite.width / sprite.height
                        val isWide = ratio > 1
                        val fleetShipWidth = (if (isWide) imageSize else imageSize * ratio) + (baseUnit * 6)
                        val fleetShipHeight = (if (isWide) imageSize / ratio else imageSize) + (baseUnit * 8)

                        val shipInFleetPanel = Global.getSettings().createCustom(fleetShipWidth, fleetShipHeight, null)
                        val shipInFleetTooltip = shipInFleetPanel.createUIElement(
                            fleetShipWidth,
                            fleetShipHeight,
                            false
                        )
                        shipInFleetPanel.addUIElement(shipInFleetTooltip).inTL(0f, 0f)
                        shipInFleetTooltip.addPara(fleetShip.shipName, Misc.getHighlightColor(), opad)
                        shipInFleetTooltip.addImage(
                            spriteName, imageSize, imageSize, opad
                        )
                        addHoverHighlight(
                            shipInFleetPanel, fleetShipWidth + opad, fleetShipHeight, -opad, 0f,
                            backgroundColor = if (isWearingPj) Misc.getPositiveHighlightColor() else Misc.getBasePlayerColor(),
                            baseAlpha = if (isWearingPj) .1f else 0f
                        )
                        pjApplierUIElement.innerElement.addCustom(shipInFleetPanel, 0f)
                    }
                }
            }

//            pjCell.addShipList(
//                5,
//                10,
//                imageSize,
//                Global.getSector().playerFaction.baseUIColor,
//                Global.getFactory().createEmptyFleet(Factions.PLAYER, pj.name, false)
//                    .apply {
//                        val variant =
//                            Global.getSettings().hullIdToVariantListMap[pj.hullId].orEmpty().firstOrNull()
//                        addShip(variant ?: "", FleetMemberType.SHIP)
//                            .apply {
//                                MagicPaintjobManager.applyPaintjob(this, null, pj)
//                            }
//                    }
//                    .fleetData.membersListCopy,
//                pad
////            )
//            pjMain.addUIElement(pjCell).inTL(xPos, yPos)
            x++
            if (x >= gridWidth) {
                x = 0
                y++
            }
        }

        pjMainContainer.addCustom(pjMain, 0f)

        panel.addUIElement(pjMainContainer).inTL(0f, 0f)
    }

    private fun addDarkenCover(
        pjMain: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float
    ): MagicLunaElementInternal {
        val pjCellHover = pjMain.createUIElement(cellWidth, cellHeight, false)
        pjMain.addUIElement(pjCellHover).inTL(xPos, yPos)
        val baselineAlpha = 0.3f
        val element = MagicLunaElementInternal()
            .addTo(pjCellHover, cellWidth, cellHeight)
            .apply {
                renderForeground = true
                foregroundAlpha = baselineAlpha
                foregroundColor = Color.black
                enableTransparency = true
                var alpha = foregroundAlpha
                advance {
                    if (isHovering) {
                        alpha -= 2 * it
                    } else {
                        alpha += 1 * it
                    }

                    alpha = alpha.coerceIn(0f, baselineAlpha)
                    foregroundAlpha = alpha
                }
            }
        pjCellHover.bringComponentToTop(element.elementPanel)
        return element
    }

    private fun addHoverHighlight(
        pjMain: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float,
        backgroundColor: Color = Misc.getBasePlayerColor(),
        baseAlpha: Float = 0f
    ): MagicLunaElementInternal {
        val pjCellHover = pjMain.createUIElement(cellWidth, cellHeight, false)
        val element = MagicLunaElementInternal()
            .addTo(pjCellHover, cellWidth, cellHeight)
            .apply {
                this.renderBorder = true
                this.renderBackground = true
                this.backgroundAlpha = baseAlpha
                this.backgroundColor = backgroundColor
                this.enableTransparency = true
                var alpha = this.backgroundAlpha
                advance {
                    if (isHovering) {
                        alpha += 2 * it
                    } else {
                        alpha -= 1 * it
                    }

                    alpha = alpha.coerceIn(baseAlpha, .1f)
                    backgroundAlpha = alpha
                    borderAlpha = alpha
                }
            }
        pjMain.addUIElement(pjCellHover).inTL(xPos, yPos)
        return element
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> = super.getIntelTags(map) + "Personal"
    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isEnded(): Boolean = false
    override fun isEnding(): Boolean = false
}