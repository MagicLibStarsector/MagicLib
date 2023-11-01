package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import org.magiclib.MagicLunaElementInternal
import org.magiclib.util.MagicTxt
import org.magiclib.util.ui.MagicFunction
import org.magiclib.util.ui.MagicRefreshableBaseIntelPlugin
import java.awt.Color

class MagicPaintjobIntel : MagicRefreshableBaseIntelPlugin() {
    @Transient
    private var scrollPos: Float? = null

    @Transient
    var pjBeingViewed: MagicPaintjobSpec? = null

    companion object {
        const val TOGGLE_VIEW_MEMKEY = "\$ML_displaybyPaintjob"
        const val TOGGLE_BUTTON_ID = "\$ML_displaybyPaintjobBtn"
    }

    override fun getName(): String = MagicTxt.getString("ml_mp_intelName")

    override fun createLargeDescriptionImpl(panel: CustomPanelAPI, width: Float, height: Float) {
        val opad = 10f
        val pad = 3f
        val pjs = MagicPaintjobManager.paintjobs.toList()
        val panelTooltip = panel.createUIElement(width, height, true)
        doBeforeRefresh { this.scrollPos = panelTooltip?.externalScroller?.yOffset }
        doAfterRefresh { panelTooltip?.externalScroller?.yOffset = this.scrollPos ?: 0f }
        val baseUnit = opad
        val scaleMult = 5f
        val cellWidth = baseUnit * 3f * scaleMult
        val cellHeight = baseUnit * 4f * scaleMult // 4:3 aspect ratio
        val imageSize = baseUnit * 12
        val cellsPerRow = (width / cellWidth).toInt()
        val padding = 10f
        val doAfterShipCellsPlaced = mutableListOf<MagicFunction>()

        val isViewByPaintjobs =
            Global.getSector().memoryWithoutUpdate.contains(TOGGLE_VIEW_MEMKEY) && Global.getSector().memoryWithoutUpdate.getBoolean(
                TOGGLE_VIEW_MEMKEY
            )

//        panelTooltip.addButton(
//            if (isViewByPaintjobs) "View by Ship" else "View by Paintjob",
//            TOGGLE_BUTTON_ID,
//            200f,
//            30f,
//            0f
//        )

        if (isViewByPaintjobs) {
            displayPaintjobGrid(
                pjs = pjs,
                cellsPerRow = cellsPerRow,
                createFromThisPanel = panel,
                width = width,
                height = height,
                cellHeight = cellHeight,
                cellWidth = cellWidth,
                padding = padding,
                imageSize = imageSize,
                opad = opad,
                pad = pad,
                doAfterShipCellsPlaced = doAfterShipCellsPlaced,
                addToThisTooltip = panelTooltip
            )
        } else {
            val shipGrid = displayShipGrid(
                cellsPerRow = cellsPerRow,
                createFromThisPanel = panel,
                width = width,
                height = height,
                cellHeight = cellHeight + 10,
                cellWidth = cellWidth,
                padding = padding,
                imageSize = imageSize,
                opad = opad,
                pad = pad
            )
            panelTooltip.addPara("test above ship grid", pad)
            panelTooltip.addCustom(shipGrid, 0f)
            panelTooltip.addPara("test below ship grid", pad)
        }

        doAfterShipCellsPlaced.forEach(MagicFunction::doFunction)

        panel.addUIElement(panelTooltip).inTL(0f, 0f)
    }

    private fun displayShipGrid(
        cellsPerRow: Int,
        createFromThisPanel: CustomPanelAPI,
        width: Float,
        height: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        imageSize: Float,
        opad: Float,
        pad: Float
    ): TooltipMakerAPI {
        val fleet = Global.getSector().playerFleet
        val ships = fleet.fleetData.membersListCopy

        val test = createFromThisPanel.createUIElement(width, height, false)
        test.addPara("test", 0f)
        createFromThisPanel.addUIElement(test).inTL(0f, 0f)


        val shipGrid = createGrid(
            createFromThisPanel, cellsPerRow, width, cellHeight, cellWidth, padding, ships
        ) { cellTooltip, ship, index, xPos, yPos ->
            val paintjobsForShip = MagicPaintjobManager.getPaintjobsForHull(ship.hullId)

            val spriteName = ship.spriteOverride ?: ship.hullSpec.spriteName
            Global.getSettings().loadTexture(spriteName)
            cellTooltip.addImage(spriteName, imageSize, imageSize, opad)
            cellTooltip.addPara(ship.shipName, Misc.getHighlightColor(), opad)
            cellTooltip.addPara(ship.variant.displayName, pad)

            if (paintjobsForShip.none()) {
                addDarkenCover(
                    pjMain = createFromThisPanel,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    xPos = xPos,
                    yPos = yPos,
                    highlightOnHover = false
                )
            }

            val shipPaintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship)
            if (shipPaintjob != null)
                cellTooltip.addPara(
                    "%s available.\n%s",
                    pad,
                    Misc.getPositiveHighlightColor(),
                    "${paintjobsForShip.count()}",
                    shipPaintjob.name
                )
            else if (paintjobsForShip.any())
                cellTooltip.addPara(
                    "%s available.",
                    pad,
                    Misc.getPositiveHighlightColor(),
                    "${paintjobsForShip.count()}"
                )
            else
                cellTooltip.addPara("None available.", pad)


            val hoverElement = addHoverHighlight(
                pjMain = createFromThisPanel,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                xPos = xPos,
                yPos = yPos
            )

            hoverElement.onClick { inputEvent ->
                if (paintjobsForShip.none())
                    return@onClick

                displaySelectPaintjobPopup(
                    pjMain = createFromThisPanel,
                    cellWidth = cellWidth,
                    paintjobsForShip = paintjobsForShip,
                    opad = opad,
                    cellHeight = cellHeight,
                    pad = pad,
                    ship = ship,
                    padding = padding,
                    imageSize = imageSize,
                    xPos = xPos,
                    yPos = yPos
                )
            }
        }

        return shipGrid
    }

    private fun displayPaintjobGrid(
        pjs: List<MagicPaintjobSpec>,
        cellsPerRow: Int,
        createFromThisPanel: CustomPanelAPI,
        width: Float,
        height: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        imageSize: Float,
        opad: Float,
        pad: Float,
        doAfterShipCellsPlaced: MutableList<MagicFunction>,
        addToThisTooltip: TooltipMakerAPI
    ) {
        addToThisTooltip.addComponent(
            createGrid(
                createFromThisPanel, cellsPerRow, width, cellHeight, cellWidth, padding, pjs
            ) { pjCellTooltip, pj, index, xPos, yPos ->
                val pj = pjs[index]

                val isUnlocked = MagicPaintjobManager.unlockedPaintjobIds.contains(pj.id)

                Global.getSettings().loadTexture(pj.spriteId)
                pjCellTooltip.addImage(pj.spriteId, imageSize, imageSize, opad)
                pjCellTooltip.addPara(pj.name, Misc.getHighlightColor(), opad)
                if (pj.description.isNullOrBlank().not()) {
                    pjCellTooltip.addPara(pj.description, pad)
                }

                val shipsThatPjMayApplyTo = Global.getSector().playerFleet.fleetData.membersListCopy
                    .filter { it.hullId == pj.hullId }

                if (shipsThatPjMayApplyTo.any()) {
                    val count = shipsThatPjMayApplyTo.count()

                    val appliedToCount =
                        shipsThatPjMayApplyTo.count { MagicPaintjobManager.getCurrentShipPaintjob(it)?.id == pj.id }

                    if (appliedToCount > 0)
                        pjCellTooltip.addPara(
                            "Applied to $appliedToCount of $count in fleet.",
                            pad,
                            Misc.getHighlightColor(),
                            appliedToCount.toString(), count.toString()
                        )
                    else
                        pjCellTooltip.addPara(
                            "Applies to $count in fleet.",
                            pad,
                            Misc.getHighlightColor(),
                            count.toString()
                        )
                }

                if (!isUnlocked)
                    addDarkenCover(
                        pjMain = createFromThisPanel,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        xPos = xPos,
                        yPos = yPos,
                        highlightOnHover = false
                    )


                if (isUnlocked) {
                    val hoverElement = addHoverHighlight(createFromThisPanel, cellWidth, cellHeight, xPos, yPos)

                    val shipSelectionViewWidth = cellWidth
                    val shipSelectionViewHeight = cellHeight
                    val shipSelectionCellsPerRow = 2

                    doAfterShipCellsPlaced.add {
                        // Restore UI state if refreshing
                        if (pjBeingViewed != null && pjBeingViewed!!.id == pj.id) {
                            displaySelectShipPopup(
                                createFromThisPanel,
                                shipSelectionViewWidth,
                                shipsThatPjMayApplyTo,
                                shipSelectionCellsPerRow,
                                opad,
                                shipSelectionViewHeight,
                                pad,
                                pj,
                                padding,
                                imageSize,
                                xPos,
                                yPos
                            )
                        }
                    }

                    // When you click on a paintjob, it will show you the ships in your fleet that it may apply to.
                    hoverElement.onClick { inputEvent ->
                        if (shipsThatPjMayApplyTo.none()) return@onClick
                        displaySelectShipPopup(
                            createFromThisPanel,
                            shipSelectionViewWidth,
                            shipsThatPjMayApplyTo,
                            shipSelectionCellsPerRow,
                            opad,
                            shipSelectionViewHeight,
                            pad,
                            pj,
                            padding,
                            imageSize,
                            xPos,
                            yPos
                        )
                    }
                }
            })
    }

    private fun displaySelectPaintjobPopup(
        pjMain: CustomPanelAPI,
        cellWidth: Float,
        paintjobsForShip: List<MagicPaintjobSpec>,
        opad: Float,
        cellHeight: Float,
        pad: Float,
        ship: FleetMemberAPI,
        padding: Float,
        imageSize: Float,
        xPos: Float,
        yPos: Float
    ) {
        val paintjobSelectionViewWidth = cellWidth
        val paintjobSelectionViewHeight = cellHeight
        val paintjobSelectionCellsPerRow = 2

        val paintjobSelectionDialog = MagicLunaElementInternal()
            .apply {
                addTo(
                    panelAPI = pjMain,
                    width = paintjobSelectionViewWidth * paintjobsForShip.count()
                        .coerceAtMost(paintjobSelectionCellsPerRow)
                            + opad * 2, // padding
                    height = paintjobSelectionViewHeight * (paintjobsForShip.count() / paintjobSelectionCellsPerRow)
                        .coerceAtLeast(1)
                            + opad * 2 // title
                            + opad * 4 // padding
                )
                { it.inTL(xPos, yPos).setYAlignOffset(paintjobSelectionViewHeight) }

                renderBackground = true
                renderBorder = true

                // Remove when clicking outside it.
                onClickOutside {
                    removeFromParent()
                }
            }

        paintjobSelectionDialog.innerElement.addTitle("Select paintjob...", Misc.getBasePlayerColor())
            .position.setYAlignOffset(-pad)

        // Display paintjobs that may apply to this ship.
        paintjobsForShip.forEach { paintjob ->
            val isWearingPj = MagicPaintjobManager.getCurrentShipPaintjob(ship)?.id == paintjob.id
            val spriteName = paintjob.spriteId

            val paintjobPanel =
                Global.getSettings().createCustom(paintjobSelectionViewWidth, paintjobSelectionViewHeight, null)
            val paintjobTooltip = paintjobPanel.createUIElement(
                paintjobSelectionViewWidth + padding,
                paintjobSelectionViewHeight,
                false
            )
            paintjobPanel.addUIElement(paintjobTooltip).inTL(opad, 0f)
            paintjobTooltip.addPara(paintjob.name, Misc.getHighlightColor(), opad)
            paintjobTooltip.addImage(
                spriteName, imageSize, imageSize, opad
            )
            if (isWearingPj) paintjobTooltip.addPara("Applied", Misc.getPositiveHighlightColor(), opad)
                .apply {
                    setAlignment(Alignment.MID)
                    position.setXAlignOffset(-(this.computeTextWidth(this.text) / 2))
                }
            else paintjobTooltip.addPara("", opad)

            addHoverHighlight(
                pjMain = paintjobPanel,
                cellWidth = paintjobSelectionViewWidth,
                cellHeight = paintjobSelectionViewHeight,
                xPos = 0f,
                yPos = 0f,
                backgroundColor = if (isWearingPj) Misc.getPositiveHighlightColor() else Misc.getBasePlayerColor(),
                baseAlpha = if (isWearingPj) .1f else 0f,
                borderOnly = true
            )
                .apply {
                    onClick {
                        // Toggle paintjob.
                        if (isWearingPj) MagicPaintjobManager.removePaintjobFromShip(ship)
                        else MagicPaintjobManager.applyPaintjob(ship, null, paintjob)

                        refreshPanel()
                    }
                }

            paintjobSelectionDialog.innerElement.addCustom(paintjobPanel, opad)
        }
    }

    private fun displaySelectShipPopup(
        pjMain: CustomPanelAPI,
        shipSelectionViewWidth: Float,
        shipsThatPjMayApplyTo: List<FleetMemberAPI>,
        shipSelectionCellsPerRow: Int,
        opad: Float,
        shipSelectionViewHeight: Float,
        pad: Float,
        pj: MagicPaintjobSpec,
        padding: Float,
        imageSize: Float,
        xPos: Float,
        yPos: Float
    ) {
        pjBeingViewed = pj
        val paintjobApplicationDialog = MagicLunaElementInternal()
            .apply {
                addTo(
                    panelAPI = pjMain,
                    width = shipSelectionViewWidth * shipsThatPjMayApplyTo.count()
                        .coerceAtMost(shipSelectionCellsPerRow)
                            + opad * 2, // padding
                    height = shipSelectionViewHeight * (shipsThatPjMayApplyTo.count() / shipSelectionCellsPerRow)
                        .coerceAtLeast(1)
                            + opad * 2 // title
                            + opad * 4 // padding
                )
                { it.inTL(xPos, yPos).setYAlignOffset(shipSelectionViewHeight) }

                renderBackground = true
                renderBorder = true

                // Remove when clicking outside it.
                onClickOutside {
                    removeFromParent()
                    pjBeingViewed = null
                }
            }

        paintjobApplicationDialog.innerElement.addTitle("Apply paintjob to...", Misc.getBasePlayerColor())
            .position.setYAlignOffset(-pad)

        // Display ships in fleet that this paintjob may apply to (and whether it's applied).
        shipsThatPjMayApplyTo.forEach { fleetShip ->
            val isWearingPj = MagicPaintjobManager.getCurrentShipPaintjob(fleetShip)?.id == pj.id
            val spriteName = fleetShip.spriteOverride ?: fleetShip.hullSpec.spriteName

            val shipInFleetPanel =
                Global.getSettings().createCustom(shipSelectionViewWidth, shipSelectionViewHeight, null)
            val shipInFleetTooltip = shipInFleetPanel.createUIElement(
                shipSelectionViewWidth + padding,
                shipSelectionViewHeight,
                false
            )
            shipInFleetPanel.addUIElement(shipInFleetTooltip).inTL(opad, 0f)
            shipInFleetTooltip.addPara(fleetShip.shipName, Misc.getHighlightColor(), opad)
            shipInFleetTooltip.addImage(
                spriteName, imageSize, imageSize, opad
            )
            if (isWearingPj) shipInFleetTooltip.addPara("Applied", Misc.getPositiveHighlightColor(), opad)
                .apply {
                    setAlignment(Alignment.MID)
                    position.setXAlignOffset(-(this.computeTextWidth(this.text) / 2))
                }
            else shipInFleetTooltip.addPara("", opad)

            addHoverHighlight(
                pjMain = shipInFleetPanel,
                cellWidth = shipSelectionViewWidth,
                cellHeight = shipSelectionViewHeight,
                xPos = 0f,
                yPos = 0f,
                backgroundColor = if (isWearingPj) Misc.getPositiveHighlightColor() else Misc.getBasePlayerColor(),
                baseAlpha = if (isWearingPj) .1f else 0f,
                borderOnly = true
            )
                .apply {
                    onClick {
                        // Toggle paintjob.
                        if (isWearingPj) MagicPaintjobManager.removePaintjobFromShip(fleetShip)
                        else MagicPaintjobManager.applyPaintjob(fleetShip, null, pj)

                        refreshPanel()
                    }
                }

            paintjobApplicationDialog.innerElement.addCustom(shipInFleetPanel, opad)
        }
    }

    private fun <T> createGrid(
        rootPanel: CustomPanelAPI,
        cellsPerRow: Int,
        gridWidth: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        items: List<T>,
        cellBuilder: (tooltip: TooltipMakerAPI, item: T, index: Int, xPos: Float, yPos: Float) -> Unit
    ): TooltipMakerAPI {
        val numRows = (items.count() / cellsPerRow) + 1
        val height = (cellHeight * numRows) + (padding * numRows)
        val gridTooltip = rootPanel.createUIElement(gridWidth, height, false)
//        rootPanel.addUIElement(gridTooltip).inTL(0f, 0f)
        val gridPanel = rootPanel.createCustomPanel(gridWidth, height, null)
        gridTooltip.addCustom(gridPanel, 0f)

        for (i in 0 until numRows) {
            val row = gridPanel.createCustomPanel(gridWidth, cellHeight, null)

            for (j in 0 until cellsPerRow) {
                val index = i * cellsPerRow + j
                if (index >= items.count()) break

                val xPos = j * cellWidth
                val yPos = i * cellHeight

                val item = items[index]
                val cellTooltip = row.createUIElement(cellWidth + padding, cellHeight, false)
                cellBuilder(cellTooltip, item, index, xPos, yPos)
                row.addUIElement(cellTooltip).inTL(xPos + padding, yPos)
                val test = row.createUIElement(cellWidth, cellHeight, false)
                test.addPara("test", 0f)
                row.addUIElement(test).inTL(xPos, yPos)
            }

            // Add row to tooltip
            gridTooltip.addCustom(row, padding)
        }

        gridTooltip.addPara("test!", padding)
        return gridTooltip
    }

    private fun addDarkenCover(
        pjMain: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float,
        highlightOnHover: Boolean
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
                    if (!highlightOnHover) return@advance
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
        baseAlpha: Float = 0f,
        borderOnly: Boolean = false,
        onClick: ((inputEvent: Any?) -> Unit)? = null
    ): MagicLunaElementInternal {
        val pjCellHover = pjMain.createUIElement(cellWidth, cellHeight, false)
        val element = MagicLunaElementInternal()
            .addTo(pjCellHover, cellWidth, cellHeight)
            .apply {
                this.renderBorder = true
                this.renderBackground = !borderOnly
                this.backgroundAlpha = baseAlpha
                this.backgroundColor = backgroundColor
                this.borderColor = backgroundColor
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
                if (onClick != null)
                    this.onClick(onClick)
            }
        pjMain.addUIElement(pjCellHover).inTL(xPos, yPos)
        return element
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        if (buttonId == TOGGLE_BUTTON_ID) {
            Global.getSector().memoryWithoutUpdate.set(
                TOGGLE_VIEW_MEMKEY,
                !Global.getSector().memoryWithoutUpdate.getBoolean(TOGGLE_VIEW_MEMKEY)
            )
            refreshPanel()
        }
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> = super.getIntelTags(map) + "Personal"
    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isEnded(): Boolean = false
    override fun isEnding(): Boolean = false
}