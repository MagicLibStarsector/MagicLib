package org.magiclib.paintjobs.appliers

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignEntity
import com.fs.starfarer.ui.impl.StandardTooltipV2
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtils.invoke
import org.magiclib.internalextensions.findChildWithMethod
import org.magiclib.internalextensions.getChildrenCopy
import org.magiclib.paintjobs.MagicPaintjobManager
import org.magiclib.paintjobs.appliers.MagicPaintjobApplierUtils.isIdle

internal class MagicPaintjobCampaignApplier : EveryFrameScript {
    var errorOccured = false

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = true
    override fun advance(amount: Float) {
        if (!MagicPaintjobManager.isEnabled) return // return if not enabled
        if (errorOccured) return

        try {
            applyToCommandTooltip()
            applyToInteractionDialog()
            applyToCampaignCircleFleets()
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error("Error when trying to apply paintjobs in the campaign", e)
            errorOccured = true
        }
    }

    private var prevLocation: LocationAPI? = null
    private var fleetsApplied = mutableSetOf<String>()
    private fun applyToCampaignCircleFleets() {
        val curLocation = Global.getSector().currentLocation
        if (curLocation !== prevLocation) { // If location change
            fleetsApplied.clear()

            prevLocation = curLocation
        }

        curLocation.fleets.forEach { fleet ->
            if (!fleet.isVisibleToPlayerFleet)
                return@forEach
            if (fleet.id in fleetsApplied)
                return@forEach

            val views = fleet.views ?: return@forEach
            if (views.isEmpty()) return@forEach

            views.filterNotNull().forEach { view ->
                val member = view.member ?: return@forEach

                MagicPaintjobApplierUtils.changeIconSprite(member, view)
            }
            fleetsApplied.add(fleet.id)
        }
    }

    private var centerTooltipHash: Int = 0
    private fun applyToInteractionDialogSelectCraft() {
        val sector = Global.getSector()
        val ui = sector.campaignUI
        val visual = ui.currentInteractionDialog.visualPanel as? UIPanelAPI ?: return

        val centerTooltip = visual.findChildWithMethod("fleetMemberClicked") as? UIPanelAPI

        val centerTooltipHash = centerTooltip.hashCode()
        // Only try to replace sprites once every time the shown tooltip changes
        if (this.centerTooltipHash == centerTooltipHash)
            return

        this.centerTooltipHash = centerTooltipHash

        val innerPanel = centerTooltip?.invoke("getInnerPanel") as? UIPanelAPI ?: return

        val fleetList1 = innerPanel.getChildrenCopy().find { it.getMethodsMatching("turnOffCRandHullBars").isNotEmpty() }
        val fleetList2 = innerPanel.getChildrenCopy().findLast { it.getMethodsMatching("turnOffCRandHullBars").isNotEmpty() }

        //, if (centerTooltip is FleetMemberRecoveryDialog) 1 else 0)
        if(fleetList1 != null)
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList1,true)
        if(fleetList2 != null && fleetList2 !== fleetList1)
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList2,true)
    }

    private var lastOptionsHash: Int? = null
    private fun applyToInteractionDialog() {
        val sector = Global.getSector()
        val ui = sector.campaignUI
        if (ui.currentInteractionDialog == null) return

        val battle = (ui.currentInteractionDialog?.plugin?.context as? FleetEncounterContext)?.battle
        /*val interactionFleet = if (battle != null) {
            battle.nonPlayerCombined
        } else {
            ui.currentInteractionDialog.interactionTarget as? CampaignFleetAPI
        }*/

        // If no paintjobs in either fleet, do not continue
        if (battle != null) {
            if(battle.snapshotBothSides.all { fleet -> fleet.fleetData.snapshot.none { member -> MagicPaintjobManager.hasPaintjob(member) } })
                return
        } else {
            val interactionFleet = ui.currentInteractionDialog.interactionTarget as? CampaignFleetAPI
            val playerFleet = Global.getSector().playerFleet

            if (playerFleet?.fleetData?.membersListCopy?.any { MagicPaintjobManager.hasPaintjob(it) } != true &&
                interactionFleet?.fleetData?.membersListCopy?.any { MagicPaintjobManager.hasPaintjob(it) } != true
            ) return
        }

        applyToInteractionDialogSelectCraft()

        val optionList = ui.currentInteractionDialog?.optionPanel?.savedOptionList ?: return
        val optionsHash = optionList
            .map { it.hashCode() }
            .hashCode()

        // Only try to replace sprites once every time the options list changes.
        if (lastOptionsHash == optionsHash)
            return

        lastOptionsHash = optionsHash


        val visual = ui.currentInteractionDialog.visualPanel as? UIPanelAPI ?: return

        val topRightPanel = visual.getChildrenCopy().findLast { it.getFieldsMatching(type = FleetEncounterContextPlugin::class.java).isNotEmpty() } as? UIPanelAPI ?: return
        val topRightFleetPanel = (topRightPanel.getChildrenCopy().getOrNull(0) as? UIPanelAPI)
            ?.getChildrenCopy()?.getOrNull(0) as? UIPanelAPI ?: return // Scroller
        val topRightPlayerFleet = (topRightFleetPanel.getChildrenCopy().getOrNull(0) as? UIPanelAPI)?.invoke("getAllLists") as? List<*> ?: return
        val topRightEnemyFleet = (topRightFleetPanel.getChildrenCopy().getOrNull(1) as? UIPanelAPI)?.invoke("getAllLists") as? List<*> ?: return

        topRightPlayerFleet.forEach {
            if (it !is UIComponentAPI) return@forEach
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(it, true)
        }
        topRightEnemyFleet.forEach {
            if (it !is UIComponentAPI) return@forEach
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(it, true)
        }
    }

    private var updateTicksRepeating: Int = 2
    private var visibilityLevelToPlayerFleet: SectorEntityToken.VisibilityLevel = SectorEntityToken.VisibilityLevel.NONE
    private var currentHoveredFleetID: String? = null
    private fun applyToCommandTooltip() {
        val sector = Global.getSector()
        val ui = sector.campaignUI
        if (!ui.isIdle())
            return
        val engine = CampaignEngine.getInstance() ?: return
        val tooltip = engine.invoke("getTooltipManager")
        val hoveredFleet = tooltip?.getFieldsMatching(type = CampaignEntity::class.java)?.getOrNull(0)?.get(tooltip) as? CampaignFleetAPI
        if (hoveredFleet == null) {
            currentHoveredFleetID = null
            visibilityLevelToPlayerFleet = SectorEntityToken.VisibilityLevel.NONE
            updateTicksRepeating = 0
            return
        }

        // Only replace the sprites in the tooltip once
        if (updateTicksRepeating == 0 && hoveredFleet.id == currentHoveredFleetID) {

            // If game is unpaused, check for if state of visibility has changed, which would update and clear the paintjob.
            if(!sector.isPaused && hoveredFleet.visibilityLevelToPlayerFleet != visibilityLevelToPlayerFleet) {
                if(visibilityLevelToPlayerFleet != SectorEntityToken.VisibilityLevel.NONE)
                    updateTicksRepeating = 2
                visibilityLevelToPlayerFleet = hoveredFleet.visibilityLevelToPlayerFleet
            }

            return
        }

        if(updateTicksRepeating != 0)
            updateTicksRepeating--

        currentHoveredFleetID = hoveredFleet.id

        val paintJobMembers = hoveredFleet.fleetData.membersListCopy.filter { member -> MagicPaintjobManager.hasPaintjob(member) }
        if (paintJobMembers.isEmpty())
            return

        val tooltipPanel = tooltip.getFieldsMatching(type = StandardTooltipV2::class.java).getOrNull(0)?.get(tooltip) as? UIPanelAPI
            ?: return
        val shipList = (tooltipPanel.getChildrenCopy().getOrNull(0) as? UIPanelAPI)?.findChildWithMethod("turnOffCRandHullBars")
            ?: return

        MagicPaintjobApplierUtils.applyPaintjobsToShipList(shipList, true)
    }
}