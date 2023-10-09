package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
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

        var x = 0
        var y = 0
        for (pj in pjs) {
            val isUnlocked = MagicPaintjobManager.unlockedPaintjobIds.contains(pj.id)
            val pjCell = pjMain.createUIElement(cellWidth, cellHeight, false)
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
            pjMain.addUIElement(pjCell).inTL(xPos, yPos)

//            if (!isUnlocked) {
                addDarkenCover(pjMain, cellWidth, cellHeight, xPos, yPos)
                    .also { cover -> pjMain.bringComponentToTop(cover.elementPanel) }
//            }
            addHoverHighlight(pjMain, cellWidth, cellHeight, xPos, yPos)

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
        val element = MagicLunaElementInternal()
            .apply {
                renderForeground = true
                foregroundAlpha = 0.01f
                foregroundColor = Color.yellow
                enableTransparency = true
                var alpha = foregroundAlpha
                advance {
                    if (isHovering) {
                        alpha -= 2 * it
                    } else {
                        alpha += 1 * it
                    }

                    alpha = alpha.coerceIn(0f, .01f)
                    foregroundAlpha = alpha
                }
            }
            .addTo(pjCellHover, cellWidth, cellHeight)
        pjMain.addUIElement(pjCellHover).inTL(xPos, yPos)
        return element
    }

    private fun addHoverHighlight(
        pjMain: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float
    ) {
        val pjCellHover = pjMain.createUIElement(cellWidth, cellHeight, false)
        MagicLunaElementInternal()
            .apply {
                renderBorder = true
                renderBackground = true
                backgroundAlpha = 0f
                backgroundColor = Misc.getBasePlayerColor()
                enableTransparency = true
                var alpha = backgroundAlpha
                advance {
                    if (isHovering) {
                        alpha += 2 * it
                    } else {
                        alpha -= 1 * it
                    }

                    alpha = alpha.coerceIn(0f, .1f)
                    backgroundAlpha = alpha
                    borderAlpha = alpha
                }
            }
            .addTo(pjCellHover, cellWidth, cellHeight)
        pjMain.addUIElement(pjCellHover).inTL(xPos, yPos)
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> = super.getIntelTags(map) + "Personal"
    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isEnded(): Boolean = false
    override fun isEnding(): Boolean = false
}