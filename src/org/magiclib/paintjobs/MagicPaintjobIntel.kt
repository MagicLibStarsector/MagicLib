package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import org.magiclib.MagicLunaElementInternal
import org.magiclib.util.MagicTxt
import org.magiclib.util.ui.MagicRefreshableBaseIntelPlugin
import java.awt.Color
import kotlin.math.ceil

/**
 * To anybody reading this code: I'm sorry. Please don't try to understand it or learn from it.
 *
 * Deepest apologies,
 *
 * Wisp
 */
class MagicPaintjobIntel : MagicRefreshableBaseIntelPlugin() {
    companion object {
        const val TOGGLE_VIEW_MEMKEY = "\$ML_displaybyPaintjob"
        const val TOGGLE_BUTTON_ID = "\$ML_displaybyPaintjobBtn"
    }

    @Transient
    private var scrollPos: Float? = null

    @Transient
    private var mainGridScrollPos: Float? = null

    @Transient
    var pjBeingViewed: MagicPaintjobSpec? = null

    @Transient
    var shipBeingViewed: FleetMemberAPI? = null

    @Transient
    private val logger = Global.getLogger(MagicPaintjobIntel::class.java)

    override fun getName(): String = MagicTxt.getString("ml_mp_intelName")

    override fun createLargeDescriptionImpl(panel: CustomPanelAPI, width: Float, height: Float) {
        val opad = 10f
        val pad = 3f
        val pjs = MagicPaintjobManager.paintjobs.toList()
        val mainGridTooltip = panel.createUIElement(width, height, true)
        val baseUnit = opad
        val scaleMult = 5f
        val cellWidth = baseUnit * 3f * scaleMult
        val cellHeight = baseUnit * 4f * scaleMult // 4:3 aspect ratio
        val imageSize = baseUnit * 12
        val cellsPerRow = (width / cellWidth).toInt()
        val padding = 10f

        val isViewByPaintjobs =
            Global.getSector().memoryWithoutUpdate.contains(TOGGLE_VIEW_MEMKEY) && Global.getSector().memoryWithoutUpdate.getBoolean(
                TOGGLE_VIEW_MEMKEY
            )

        mainGridTooltip.addButton(
            if (isViewByPaintjobs) "View by Ship" else "View by Paintjob",
            TOGGLE_BUTTON_ID,
            200f,
            30f,
            0f
        )
        val dumbPanel = panel.createCustomPanel(width, height - 100f, null)

        val grid = if (isViewByPaintjobs) createPaintjobGrid(
            pjs = pjs,
            cellsPerRow = cellsPerRow,
            panel = dumbPanel,
            width = width,
            height = height,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            padding = padding,
            imageSize = imageSize,
            opad = opad,
            pad = pad,
//            mainGridTooltip
        ) else
            displayShipGrid(
                cellsPerRow = cellsPerRow,
                createFromThisPanel = dumbPanel,
                width = width,
                height = height,
                cellHeight = cellHeight + 10,
                cellWidth = cellWidth,
                padding = padding,
                imageSize = imageSize,
                opad = opad,
                pad = pad
            )

        doBeforeRefresh { scrollPos = grid.externalScroller.yOffset }
        doAfterRefresh { grid.externalScroller.yOffset = scrollPos ?: 0f }

        doBeforeRefresh { mainGridScrollPos = mainGridTooltip.externalScroller.yOffset }
        doAfterRefresh { mainGridTooltip.externalScroller.yOffset = mainGridScrollPos ?: 0f }

        dumbPanel.addUIElement(grid).inTL(0f, 0f)
        mainGridTooltip.addCustom(grid, 0f)

        panel.addUIElement(mainGridTooltip).inTL(0f, 0f)
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

    private fun createPaintjobGrid(
        pjs: List<MagicPaintjobSpec>,
        cellsPerRow: Int,
        panel: CustomPanelAPI,
        width: Float,
        height: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        imageSize: Float,
        opad: Float,
        pad: Float
    ): TooltipMakerAPI {
        return createGrid(panel, cellsPerRow, width, height, cellHeight, cellWidth, padding, pjs)
        { pjCellTooltip, row, pj, _, xPosOfCellOnRow, yPosOfCellOnRow ->
            val isUnlocked = MagicPaintjobManager.unlockedPaintjobIds.contains(pj.id)
//                val pjCellTooltip = row.createUIElement(cellWidth + padding, cellHeight, false)

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
                    panel = row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    xPos = xPosOfCellOnRow,
                    yPos = yPosOfCellOnRow,
                    highlightOnHover = true
                )
            else {
                val hoverElement = addHoverHighlight(row, cellWidth, cellHeight, xPosOfCellOnRow, yPosOfCellOnRow)

                val shipSelectionViewWidth = cellWidth
                val shipSelectionViewHeight = cellHeight
                val shipSelectionCellsPerRow = 2

                // Restore UI state if refreshing
                if (pjBeingViewed != null && pjBeingViewed!!.id == pj.id) {
                    displayPaintjobApplicatorPopup(
                        row,
                        shipSelectionViewWidth,
                        shipsThatPjMayApplyTo,
                        shipSelectionCellsPerRow,
                        opad,
                        shipSelectionViewHeight,
                        pad,
                        pj,
                        padding,
                        imageSize,
                        xPosOfCellOnRow,
                        yPosOfCellOnRow
                    )
                }

                // When you click on a paintjob, it will show you the ships in your fleet that it may apply to.
                hoverElement.onClick { inputEvent ->
                    if (shipsThatPjMayApplyTo.none()) return@onClick
                    displayPaintjobApplicatorPopup(
                        row,
                        shipSelectionViewWidth,
                        shipsThatPjMayApplyTo,
                        shipSelectionCellsPerRow,
                        opad,
                        shipSelectionViewHeight,
                        pad,
                        pj,
                        padding,
                        imageSize,
                        xPosOfCellOnRow,
                        yPosOfCellOnRow
                    )
                }
            }
        }
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

        val shipGrid = createGrid(
            createFromThisPanel, cellsPerRow, width, height, cellHeight, cellWidth, padding, ships
        ) { cellTooltip, row, ship, index, xPos, yPos ->
            val paintjobsForShip = MagicPaintjobManager.getPaintjobsForHull(ship.hullId)

            val spriteName = ship.spriteOverride ?: ship.hullSpec.spriteName
            Global.getSettings().loadTexture(spriteName)
            cellTooltip.addImage(spriteName, imageSize, imageSize, opad)
            cellTooltip.addPara(ship.shipName, Misc.getHighlightColor(), opad)
            cellTooltip.addPara(ship.variant.displayName, pad)

            if (paintjobsForShip.none()) {
                addDarkenCover(
                    panel = row,
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
                panel = row,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                xPos = xPos,
                yPos = yPos
            )

            fun displayPaintjobsForShip(
            ) = displaySelectPaintjobPopup(
                panel = row,
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

            // Restore UI state if refreshing
            if (shipBeingViewed != null && shipBeingViewed!!.id == ship.id) {
                displayPaintjobsForShip()
            }

            hoverElement.onClick { inputEvent ->
                if (paintjobsForShip.none())
                    return@onClick

                displayPaintjobsForShip()
            }
        }

        return shipGrid
    }


    private fun <T> createGrid(
        rootPanel: CustomPanelAPI,
        cellsPerRow: Int,
        gridWidth: Float,
        gridHeight: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        items: List<T>,
        cellBuilder: (tooltip: TooltipMakerAPI, row: CustomPanelAPI, item: T, index: Int, xPosOfCellOnRow: Float, yPosOfCellOnRow: Float) -> Unit
    ): TooltipMakerAPI {
        @Suppress("ComplexRedundantLet")
        val numRows = (items.count() / cellsPerRow.toFloat())
            .let { ceil(it) }.toInt()
//        val height = (cellHeight * numRows) + (padding * numRows)
        val gridTooltip = rootPanel.createUIElement(gridWidth, gridHeight, true)

        for (i in 0 until numRows) {
            val row = rootPanel.createCustomPanel(gridWidth, cellHeight, null)

            for (j in 0 until cellsPerRow) {
                val index = i * cellsPerRow + j
                if (index >= items.count()) break

                val xPos = j * cellWidth
                val yPos = i * cellHeight

                val item = items[index]
                // Build cell tooltip
                val cellTooltip = row.createUIElement(cellWidth + padding, cellHeight, false)
                // Add cell tooltip to row
                row.addUIElement(cellTooltip).inTL(xPos + padding, 0f)
                // Populate cell tooltip
                cellBuilder(cellTooltip, row, item, index, xPos + padding, 0f)
            }

            // Add row to tooltip
            gridTooltip.addCustom(row, padding)
        }

        return gridTooltip
    }

    private fun displayPaintjobApplicatorPopup(
        panel: CustomPanelAPI,
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
                    panelAPI = panel,
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
//                .apply {
//                    setAlignment(Alignment.MID)
//                    position.setXAlignOffset(-(this.computeTextWidth(this.text) / 2))
//                }
            else shipInFleetTooltip.addPara("", opad)

            addHoverHighlight(
                panel = shipInFleetPanel,
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

    private fun displaySelectPaintjobPopup(
        panel: CustomPanelAPI,
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
        shipBeingViewed = ship
        val items = paintjobsForShip + null
        val paintjobSelectionCellsPerRow = 2
        val paintjobSelectionViewWidth = (cellWidth * items.count()
            .coerceAtMost(paintjobSelectionCellsPerRow)
                + opad * 6)
        val paintjobSelectionViewHeight = (cellHeight * ceil(items.count().toFloat() / paintjobSelectionCellsPerRow)
            .coerceAtLeast(1f)
                + opad * 2 // title
                + opad * 4)

        val paintjobSelectionDialog = MagicLunaElementInternal()
            .apply {
                addTo(
                    panelAPI = panel,
                    width = paintjobSelectionViewWidth,
                    height = paintjobSelectionViewHeight
                )
                {
                    it.inTL(xPos, yPos)
                }

                renderBackground = true
                renderBorder = true

                // Remove when clicking outside it.
                onClickOutside {
                    removeFromParent()
                    shipBeingViewed = null
                }
            }

        paintjobSelectionDialog.innerElement.addTitle("Select paintjob...", Misc.getBasePlayerColor())
            .position.setYAlignOffset(-pad)

        // Display paintjobs that may apply to this ship.
        val grid = createGrid(
            rootPanel = paintjobSelectionDialog.elementPanel,
            cellsPerRow = paintjobSelectionCellsPerRow,
            gridWidth = paintjobSelectionViewWidth,
            gridHeight = paintjobSelectionViewHeight,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            padding = padding,
            items = items
        ) { paintjobTooltip, row, paintjob, index, xPosOfCellOnRow, yPosOfCellOnRow ->
            val isWearingPj = MagicPaintjobManager.getCurrentShipPaintjob(ship)?.id == paintjob?.id
            val spriteName = paintjob?.spriteId ?: ship.hullSpec.spriteName

            paintjobTooltip.addPara(paintjob?.name ?: "Default", Misc.getHighlightColor(), opad)
            Global.getSettings().loadTexture(spriteName)
            paintjobTooltip.addImage(
                spriteName, imageSize, imageSize, opad
            )
            if (isWearingPj)
                paintjobTooltip.addPara("Applied", Misc.getPositiveHighlightColor(), opad)
                    .apply {
                        setAlignment(Alignment.MID)
                        position.setXAlignOffset(-(this.computeTextWidth(this.text) / 2))
                    }
            else
                paintjobTooltip.addPara("", opad)

            addHoverHighlight(
                panel = row,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                xPos = xPosOfCellOnRow,
                yPos = yPosOfCellOnRow,
                backgroundColor = if (isWearingPj) Misc.getPositiveHighlightColor() else Misc.getBasePlayerColor(),
                baseAlpha = if (isWearingPj) .1f else 0f,
                borderOnly = true
            )
                .apply {
                    onClick {
                        // Toggle paintjob.
                        if (isWearingPj || paintjob == null) MagicPaintjobManager.removePaintjobFromShip(ship)
                        else MagicPaintjobManager.applyPaintjob(ship, null, paintjob)

                        refreshPanel()
                    }
                }

        }

        paintjobSelectionDialog.elementPanel.addUIElement(grid).apply {
            inTL(0f, 30f)
        }
    }

    private fun addDarkenCover(
        panel: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float,
        highlightOnHover: Boolean
    ): MagicLunaElementInternal {
        val pjCellHover = panel.createUIElement(cellWidth, cellHeight, false)
        panel.addUIElement(pjCellHover).inTL(xPos, yPos)
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
//        pjCellHover.bringComponentToTop(element.elementPanel)
        return element
    }

    private fun addHoverHighlight(
        panel: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        xPos: Float,
        yPos: Float,
        backgroundColor: Color = Misc.getBasePlayerColor(),
        baseAlpha: Float = 0f,
        borderOnly: Boolean = false,
        onClick: ((inputEvent: Any?) -> Unit)? = null
    ): MagicLunaElementInternal {
        val pjCellHover = panel.createUIElement(cellWidth, cellHeight, false)
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
        panel.addUIElement(pjCellHover).inTL(xPos, yPos)
        return element
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> = super.getIntelTags(map) + "Personal"
    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isEnded(): Boolean = false
    override fun isEnding(): Boolean = false
}