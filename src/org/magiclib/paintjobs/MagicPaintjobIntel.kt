package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import org.magiclib.MagicLunaElementInternal
import org.magiclib.achievements.DividerCustomPanelPlugin
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

    /**
     * Set, notify intel, then unset. This will display "Unlocked <paintjob>" in the intel notification.
     */
    @Transient
    var tempPaintjobForIntelNotification: MagicPaintjobSpec? = null

    @Transient
    private val logger = Global.getLogger(MagicPaintjobIntel::class.java)

    override fun getName(): String =
        tempPaintjobForIntelNotification?.let { MagicTxt.getString("ml_mp_intelUnlockedNotification") }
            ?: MagicTxt.getString("ml_mp_intelName")

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode?) {
        if (tempPaintjobForIntelNotification != null) {
            info.addPara(createPaintjobName(tempPaintjobForIntelNotification!!), 3f)
        }
    }

    override fun getIcon() = Global.getSettings().getSpriteName("intel", "magicPaintjobs")

    override fun createLargeDescriptionImpl(panel: CustomPanelAPI, width: Float, height: Float) {
        val opad = 10f
        val pad = 3f
        val pjs = MagicPaintjobManager.getPaintjobs().toList()
        val mainGridTooltip = panel.createUIElement(width, height, true)
        val baseUnit = opad
        val scaleMult = 5f
        val cellWidth = baseUnit * 3f * scaleMult
        val cellHeight = baseUnit * 4f * scaleMult // 4:3 aspect ratio
        val imageSize = baseUnit * 12
//        val cellsPerRow = (width / cellWidth).toInt()
        val padding = 10f

        val isViewByPaintjobs =
            Global.getSector().memoryWithoutUpdate.contains(TOGGLE_VIEW_MEMKEY) && Global.getSector().memoryWithoutUpdate.getBoolean(
                TOGGLE_VIEW_MEMKEY
            )
        val dumbassPanelThatIsNeverUsedButSomehowThisWorks = panel.createCustomPanel(width, height - 100f, null)
        val overlayPanel = panel.createCustomPanel(width, height - 100f, null)

        val grid = if (isViewByPaintjobs)
            createPaintjobGrid(
                pjs = pjs,
                createFromThisPanel = dumbassPanelThatIsNeverUsedButSomehowThisWorks,
                placePopupsOnThisPanel = overlayPanel,
                width = width,
                height = height,
                cellHeight = cellHeight,
                cellWidth = cellWidth,
                padding = padding,
                imageSize = imageSize,
                opad = opad,
                pad = pad,
            ) else
            displayShipGrid(
                createFromThisPanel = dumbassPanelThatIsNeverUsedButSomehowThisWorks,
                placePopupsOnThisPanel = overlayPanel,
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

        // for some reason this line is important, `doAfterRefresh { grid.externalScroller.yOffset = scrollPos ?: 0f }` breaks otherwise.
        dumbassPanelThatIsNeverUsedButSomehowThisWorks.addUIElement(grid).inTL(0f, 0f)
        addPaintjobsHeader(
            isViewByPaintjobs = isViewByPaintjobs,
            panel = panel,
            width = width,
            opad = opad,
            headerComponent = mainGridTooltip
        )
        mainGridTooltip.addCustom(grid, 0f)

        panel.addUIElement(mainGridTooltip).inTL(0f, 0f)
        panel.addComponent(overlayPanel).inTL(0f, 0f)
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

    private fun addPaintjobsHeader(
        isViewByPaintjobs: Boolean,
        panel: CustomPanelAPI,
        width: Float,
        opad: Float,
        headerComponent: TooltipMakerAPI
    ) {
        val headerTextHeight = 30
        val headerStartX = width * 0.34f
        val imageHeight = 40
        val defaultImage = Global.getSettings().getSpriteName("intel", "magicPaintjobs")
        val headerText = MagicTxt.getString("ml_mp_intelName")

        val headerSubPanel = panel.createCustomPanel(width, headerTextHeight.toFloat(), null)
        val headerHolder = headerSubPanel.createUIElement(width, headerTextHeight.toFloat(), false)
        val buttonHolder = headerSubPanel.createUIElement(width, headerTextHeight.toFloat(), false)

        buttonHolder.addButton(
            MagicTxt.getString(if (isViewByPaintjobs) "ml_mp_viewFleet" else "ml_mp_viewPaintjobs"),
            TOGGLE_BUTTON_ID,
            200f,
            30f,
            15f
        )

        val label = headerHolder.beginImageWithText(defaultImage, imageHeight.toFloat())
        label.setTitleOrbitronVeryLarge()
        label.addTitle(headerText, Misc.getBasePlayerColor())
        headerHolder.addImageWithText(opad)

        headerSubPanel.addUIElement(buttonHolder).inTL(0f, 0f)
        headerSubPanel.addUIElement(headerHolder).inTL(headerStartX, 0f)
        headerComponent.addCustom(headerSubPanel, 0f)

        headerComponent.addSpacer((28).toFloat())
        DividerCustomPanelPlugin(width - 7, color = Global.getSettings().basePlayerColor).addTo(headerComponent)
    }

    private fun createPaintjobGrid(
        pjs: List<MagicPaintjobSpec>,
        createFromThisPanel: CustomPanelAPI,
        placePopupsOnThisPanel: CustomPanelAPI,
        width: Float,
        height: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        imageSize: Float,
        opad: Float,
        pad: Float
    ): TooltipMakerAPI {
        return createGrid(
            createFromThisPanel, width, height, cellHeight, cellWidth, padding, pjs
        ) { pjCellTooltip, row, pj, _, xPosOfCellOnRow, yPosOfCellOnRow, rowYPos ->
            val isUnlocked = MagicPaintjobManager.unlockedPaintjobIds.contains(pj.id)

            val cellPanel = row.createCustomPanel(cellWidth, cellHeight, null)
            val cellUnderlay = cellPanel.createUIElement(cellWidth, cellHeight, false)
            cellPanel.addUIElement(cellUnderlay).inTL(padding, 0f)
            pjCellTooltip.addCustom(cellPanel, 0f)

            Global.getSettings().loadTexture(pj.spriteId)
            cellUnderlay.addImage(pj.spriteId, imageSize, imageSize, opad)
            cellUnderlay.addPara(createPaintjobName(pj), Misc.getHighlightColor(), opad)

            addPaintjobHoverTooltipIfNeeded(pj, cellPanel, pjCellTooltip)

            if (!isUnlocked) {
                val cellOverlay = cellPanel.createUIElement(80f, 10f, false)
                cellPanel.addUIElement(cellOverlay).inTMid(imageSize / 2).setXAlignOffset(padding)
                cellOverlay.addPara(MagicTxt.getString("ml_mp_locked"), Misc.getNegativeHighlightColor(), pad)
            }

            val shipsThatPjMayApplyTo = Global.getSector().playerFleet.fleetData.membersListCopy
                .filter { it.hullSpec.baseHullId in pj.hullIds }
                // If the ship is wearing a permanent paintjob, don't show it in the list.
                .filter { MagicPaintjobManager.getCurrentShipPaintjob(it)?.isPermanent != true }

            if (shipsThatPjMayApplyTo.any()) {
                val count = shipsThatPjMayApplyTo.count()

                val appliedToCount =
                    shipsThatPjMayApplyTo.count { MagicPaintjobManager.getCurrentShipPaintjob(it)?.id == pj.id }

                cellUnderlay.addPara(
                    "$appliedToCount of $count in use",
                    pad,
                    arrayOf(
                        if (appliedToCount > 0) Misc.getHighlightColor() else Misc.getTextColor(),
                        Misc.getHighlightColor()
                    ),
                    appliedToCount.toString(), count.toString()
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

                val shipSelectionCellsPerRow = 2

                // Restore UI state if refreshing
                doAfterRefresh {
                    if (pjBeingViewed != null && pjBeingViewed!!.id == pj.id) {
                        displaySelectShipPopup(
                            placePopupsOnThisPanel,
                            cellWidth,
                            cellHeight,
                            shipsThatPjMayApplyTo,
                            shipSelectionCellsPerRow,
                            opad,
                            pad,
                            pj,
                            padding,
                            imageSize,
                            xPosOfCellOnRow,
                            rowYPos
                        )
                    }
                }

                // When you click on a paintjob, it will show you the ships in your fleet that it may apply to.
                hoverElement.onClick { inputEvent ->
                    if (shipsThatPjMayApplyTo.none()) return@onClick
                    displaySelectShipPopup(
                        panel = placePopupsOnThisPanel,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        shipsThatPjMayApplyTo = shipsThatPjMayApplyTo,
                        shipSelectionCellsPerRow = shipSelectionCellsPerRow,
                        opad = opad,
                        pad = pad,
                        pj = pj,
                        padding = padding,
                        imageSize = imageSize,
                        xPos = xPosOfCellOnRow,
                        yPos = rowYPos
                    )
                }
            }
        }
    }

    private fun createPaintjobName(pj: MagicPaintjobSpec) =
        pj.name + " " + runCatching {
            Global.getSettings().getHullSpec(pj.hullIds.first()).hullName
        }.getOrElse { pj.hullIds.first() }

    private fun displayShipGrid(
        createFromThisPanel: CustomPanelAPI,
        placePopupsOnThisPanel: CustomPanelAPI,
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
            createFromThisPanel,
            width,
            height,
            cellHeight,
            cellWidth,
            padding,
            ships
        ) { cellTooltip, row, ship, index, xPos, yPos, rowYPos ->
            val paintjobsForShip =
                MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId, includeShiny = false)
            val shipPaintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship)

            val spriteName = ship.spriteOverride ?: shipPaintjob?.spriteId ?: ship.hullSpec.spriteName
            Global.getSettings().loadTexture(spriteName)
            val shipThumbnailPanel = row.createCustomPanel(imageSize, imageSize, null)
            val shipThumbnailUnderlay = shipThumbnailPanel.createUIElement(imageSize, imageSize, false)
            val shipThumbnailOverlay = shipThumbnailPanel.createUIElement(cellTooltip.widthSoFar, 22f, false)
            shipThumbnailPanel.addUIElement(shipThumbnailUnderlay).inTL(0f, 0f)
            shipThumbnailPanel.addUIElement(shipThumbnailOverlay).inBL(0f, -padding)
                .setXAlignOffset((-(padding / 2)))
            cellTooltip.addCustom(shipThumbnailPanel, 0f)
            shipThumbnailUnderlay.addImage(spriteName, imageSize, imageSize, opad)

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

            if (shipPaintjob?.isPermanent == true) {
                val cellOverlay = shipThumbnailPanel.createUIElement(80f, 10f, false)
                shipThumbnailPanel.addUIElement(cellOverlay).inTMid(imageSize / 2).setXAlignOffset(padding - 4)
//                cellOverlay.addPara(MagicTxt.getString("ml_mp_permanent"), Misc.getHighlightColor(), pad)
                addDarkenCover(row, cellWidth, cellHeight, xPos, yPos, highlightOnHover = false)
                addHoverTooltip(shipThumbnailPanel, cellTooltip) { tooltip ->
                    tooltip.addPara(
                        MagicTxt.getString("ml_mp_permanentTooltip"),
                        Misc.getHighlightColor(),
                        pad
                    )
                }
            }

            val paintjobsNotAppliedCount = paintjobsForShip.count { it.id != shipPaintjob?.id }

            cellTooltip.addPara(
                MagicTxt.getString(
                    if (paintjobsNotAppliedCount > 0) "ml_mp_appliedPaintjobMore"
                    else "ml_mp_appliedPaintjobNoMore"
                ),
                opad + opad,
                arrayOf(
                    if (shipPaintjob != null) Misc.getPositiveHighlightColor() else Misc.getGrayColor(),
                    Misc.getHighlightColor()
                ),
                shipPaintjob?.name ?: MagicTxt.getString("ml_mp_default"),
                paintjobsNotAppliedCount.toString()
            )

            cellTooltip.addPara(ship.shipName, Misc.getHighlightColor(), pad)
            cellTooltip.addPara(ship.variant.displayName, pad)

            val hoverElement = addHoverHighlight(
                panel = row,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                xPos = xPos - padding,
                yPos = yPos
            )

            fun displayPaintjobsForShip(
            ) = displaySelectPaintjobPopup(
                panel = placePopupsOnThisPanel,
                cellWidth = cellWidth,
                paintjobsForShip = paintjobsForShip,
                opad = opad,
                cellHeight = cellHeight,
                pad = pad,
                ship = ship,
                padding = padding,
                imageSize = imageSize,
                xPos = xPos,
                yPos = rowYPos
            )
            doAfterRefresh {
                // Restore UI state if refreshing
                if (shipBeingViewed != null && shipBeingViewed!!.id == ship.id) {
                    displayPaintjobsForShip()
                }
            }

            hoverElement.onClick { inputEvent ->
                // If there are no paintjobs for this ship, or the paintjob is permanent, don't show the popup.
                if (paintjobsForShip.none() || shipPaintjob?.isPermanent == true)
                    return@onClick

                displayPaintjobsForShip()
            }
        }

        return shipGrid
    }

    private fun <T> createGrid(
        rootPanel: CustomPanelAPI,
        gridWidth: Float,
        gridHeight: Float,
        cellHeight: Float,
        cellWidth: Float,
        padding: Float,
        items: List<T>,
        cellBuilder: (tooltip: TooltipMakerAPI, row: CustomPanelAPI, item: T, index: Int, xPosOfCellOnRow: Float, yPosOfCellOnRow: Float, yPosOfRowOnGrid: Float) -> Unit
    ): TooltipMakerAPI {
        val cellsPerRow = (gridWidth / (cellWidth + padding)).toInt()

        @Suppress("ComplexRedundantLet")
        val numRows = (items.count() / cellsPerRow.toFloat())
            .let { ceil(it) }.toInt()
        val gridTooltip = rootPanel.createUIElement(gridWidth, gridHeight, true)

        for (i in 0 until numRows) {
            val row = rootPanel.createCustomPanel(gridWidth, cellHeight, null)

            for (j in 0 until cellsPerRow) {
                val index = i * cellsPerRow + j
                if (index >= items.count()) break

                val xPosOfCell = j * cellWidth + padding + (padding * j)
                val yPosOfRow = i * cellHeight

                val item = items[index]
                // Build cell tooltip
                val cellTooltip = row.createUIElement(cellWidth, cellHeight, false)
                // Add cell tooltip to row, adjusting it so it doesn't go out of bounds.
                row.addUIElement(cellTooltip).inTL((xPosOfCell), 0f)
                // Populate cell tooltip
                cellBuilder(cellTooltip, row, item, index, xPosOfCell, 0f, yPosOfRow)
            }

            // Add row to tooltip
            gridTooltip.addCustom(row, padding)
        }

        return gridTooltip
    }


    private fun addPaintjobHoverTooltipIfNeeded(
        pj: MagicPaintjobSpec,
        cellPanel: CustomPanelAPI,
        pjCellTooltip: TooltipMakerAPI
    ) {
        val shouldShowUnlockConditions =
            pj.id !in MagicPaintjobManager.unlockedPaintjobIds && !pj.unlockConditions.isNullOrBlank()
        if (!pj.description.isNullOrBlank() || shouldShowUnlockConditions) {
            addHoverTooltip(cellPanel, pjCellTooltip) { tooltip ->
                tooltip.addSectionHeading(createPaintjobName(pj), Alignment.MID, 3f)
                if (!pj.description.isNullOrBlank()) {
                    tooltip.addPara(pj.description!!, 10f)
                    if (shouldShowUnlockConditions)
                        tooltip.addSpacer(10f)
                }
                if (shouldShowUnlockConditions) {
                    tooltip.addPara(pj.unlockConditions!!, Misc.getHighlightColor(), 10f)
                }
            }
        }
    }

    private fun addHoverTooltip(
        cellPanel: CustomPanelAPI,
        pjCellTooltip: TooltipMakerAPI,
        addText: (TooltipMakerAPI) -> Unit
    ) {
        MagicLunaElementInternal().apply {
            addTo(cellPanel, pjCellTooltip.widthSoFar, pjCellTooltip.heightSoFar) { it.inTL(0f, 0f) }
            renderBorder = false
            renderBackground = false
            renderForeground = false
            onHoverEnter {
                pjCellTooltip.addTooltipTo(
                    object : BaseTooltipCreator() {
                        override fun getTooltipWidth(tooltipParam: Any?) = 250f

                        override fun createTooltip(
                            tooltip: TooltipMakerAPI,
                            expanded: Boolean,
                            tooltipParam: Any?
                        ) {
                            addText(tooltip)
                        }
                    },
                    cellPanel, TooltipMakerAPI.TooltipLocation.RIGHT
                )
            }
        }
    }

    private fun displaySelectShipPopup(
        panel: CustomPanelAPI,
        cellWidth: Float,
        cellHeight: Float,
        shipsThatPjMayApplyTo: List<FleetMemberAPI>,
        shipSelectionCellsPerRow: Int,
        opad: Float,
        pad: Float,
        pj: MagicPaintjobSpec,
        padding: Float,
        imageSize: Float,
        xPos: Float,
        yPos: Float
    ) {
        pjBeingViewed = pj
        val actualShipsPerRow = shipsThatPjMayApplyTo.count().coerceAtMost(shipSelectionCellsPerRow)
        val popupWidth = (cellWidth * actualShipsPerRow
                + padding * 3
                + padding * actualShipsPerRow)
        val popupHeight = (cellHeight * (shipsThatPjMayApplyTo.count() / shipSelectionCellsPerRow)
            .coerceAtLeast(1)
                + padding * 2 // title
                + padding * 4)

        val paintjobApplicationDialog = MagicLunaElementInternal()
            .apply {
                addTo(
                    panelAPI = panel,
                    width = popupWidth,
                    height = popupHeight
                )
                {
                    // Constrain to panel bounds
                    it.inTL(
                        xPos.coerceAtMost(panel.position.width - popupWidth - (padding * 2)),
                        yPos.coerceAtMost(panel.position.height - popupHeight - (padding * 2))
                            .coerceAtLeast(0f)
                    )
                }

                renderBackground = true
                renderBorder = true

                // Remove when clicking outside it.
                onClickOutside {
                    removeFromParent()
                    pjBeingViewed = null
                }
            }

        paintjobApplicationDialog.innerElement.addTitle(
            MagicTxt.getString("ml_mp_applyPaintjob"),
            Misc.getBasePlayerColor()
        )
            .position.setYAlignOffset(-pad)

        // Display ships in fleet that this paintjob may apply to (and whether it's applied).
        val grid = createGrid(
            rootPanel = paintjobApplicationDialog.elementPanel, gridWidth = popupWidth, gridHeight = popupHeight,
            cellHeight = cellHeight, cellWidth = cellWidth, padding = padding, items = shipsThatPjMayApplyTo
        ) { cellTooltip, row, fleetShip, _, xPosOnRow, yPosOnRow, yPosOfRowOnGrid ->
            val currentShipPaintjob = MagicPaintjobManager.getCurrentShipPaintjob(fleetShip)
            val isWearingPj = currentShipPaintjob?.id == pj.id
            val spriteName = currentShipPaintjob?.spriteId ?: fleetShip.hullSpec.spriteName

            cellTooltip.addPara(fleetShip.shipName, Misc.getHighlightColor(), opad).setAlignment(Alignment.MID)
            val title = cellTooltip.prev
            cellTooltip.addImage(
                spriteName, imageSize, imageSize, opad
            )
            val image = cellTooltip.prev
            image.position.belowMid(title, opad)
            if (isWearingPj) cellTooltip.addPara(
                MagicTxt.getString("ml_mp_applied"),
                Misc.getPositiveHighlightColor(),
                opad
            ).setAlignment(Alignment.MID)
            else cellTooltip.addPara("", opad)
            cellTooltip.prev.position.belowMid(image, opad)

            addHoverHighlight(
                panel = row,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                xPos = xPosOnRow,
                yPos = yPosOnRow,
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
        }

        paintjobApplicationDialog.elementPanel.addUIElement(grid).inTL(0f, 30f)
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
    ): MagicLunaElementInternal {
        shipBeingViewed = ship
        // Add null as the first item as a default paintjob.
        val items = listOf(null) + paintjobsForShip
        val paintjobSelectionCellsPerRow = 4
        val popupWidth = ((cellWidth + padding) * items.count()
            .coerceAtMost(paintjobSelectionCellsPerRow)
                + opad * 6) // padding
        val popupHeight = (cellHeight * ceil(items.count().toFloat() / paintjobSelectionCellsPerRow)
            .coerceAtLeast(1f)
                + opad * 2 // title
                + opad * 4) // padding

        val paintjobSelectionDialog = MagicLunaElementInternal()
            .apply {
                addTo(
                    panelAPI = panel,
                    width = popupWidth,
                    height = popupHeight
                )
                {
                    // Constrain to panel bounds
                    it.inTL(
                        xPos.coerceAtMost(panel.position.width - popupWidth - (padding * 2)),
                        yPos.coerceAtMost(panel.position.height - popupHeight - (padding * 2))
                            .coerceAtLeast(0f)
                    )
                }

                renderBackground = true
                renderBorder = true

                // Remove when clicking outside it.
                onClickOutside {
                    removeFromParent()
                    shipBeingViewed = null
                }
            }

        paintjobSelectionDialog.innerElement.addTitle(
            MagicTxt.getString("ml_mp_selectPaintjob"),
            Misc.getBasePlayerColor()
        )
            .position.setYAlignOffset(-pad * 1.5f)

        // Display paintjobs that may apply to this ship.
        val grid = createGrid(
            rootPanel = paintjobSelectionDialog.elementPanel,
//            cellsPerRow = paintjobSelectionCellsPerRow,
            gridWidth = popupWidth,
            gridHeight = popupHeight,
            cellHeight = cellHeight,
            cellWidth = cellWidth,
            padding = padding,
            items = items
        ) { pjCellTooltip, row, paintjob, index, xPosOfCellOnRow, yPosOfCellOnRow, rowYPos ->
            val isWearingPj = MagicPaintjobManager.getCurrentShipPaintjob(ship)?.id == paintjob?.id
            val spriteName = paintjob?.spriteId ?: ship.hullSpec.spriteName
            val isUnlocked = paintjob == null || MagicPaintjobManager.unlockedPaintjobIds.contains(paintjob.id)

            val cellPanel = row.createCustomPanel(cellWidth, cellHeight, null)
            val cellUnderlay = cellPanel.createUIElement(cellWidth, cellHeight, false)
            cellPanel.addUIElement(cellUnderlay).inTL(0f, 0f)
            pjCellTooltip.addCustom(cellPanel, 0f)

            cellUnderlay.addPara(
                paintjob?.name ?: MagicTxt.getString("ml_mp_default"),
                if (paintjob == null) Misc.getTextColor() else Misc.getHighlightColor(),
                opad
            )
                .apply {
                    setAlignment(Alignment.MID)
                }
            val title = cellUnderlay.prev
            Global.getSettings().loadTexture(spriteName)

            cellUnderlay.addImage(
                spriteName, imageSize, imageSize, opad
            )
            val image = cellUnderlay.prev
            cellUnderlay.prev.position.belowMid(title, opad)
            if (isWearingPj) {
                cellUnderlay.addPara(MagicTxt.getString("ml_mp_applied"), Misc.getPositiveHighlightColor(), opad)
                    .setAlignment(Alignment.MID)
            } else
                cellUnderlay.addPara("", opad)
            cellUnderlay.prev.position.belowMid(image, opad)

            if (!isUnlocked) {
                val cellOverlay = cellPanel.createUIElement(80f, 10f, false)
                cellPanel.addUIElement(cellOverlay).inTMid(imageSize / 2 + padding)
                cellOverlay.addPara(MagicTxt.getString("ml_mp_locked"), Misc.getNegativeHighlightColor(), pad)
                    .setAlignment(Alignment.MID)
            }

            if (paintjob != null)
                addPaintjobHoverTooltipIfNeeded(
                    pj = paintjob,
                    cellPanel = cellPanel,
                    pjCellTooltip = pjCellTooltip
                )

            if (!isUnlocked)
                addDarkenCover(
                    panel = row,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    xPos = xPosOfCellOnRow,
                    yPos = yPosOfCellOnRow,
                    highlightOnHover = true
                )
            else
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

        return paintjobSelectionDialog
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
        val baselineAlpha = 0.4f
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