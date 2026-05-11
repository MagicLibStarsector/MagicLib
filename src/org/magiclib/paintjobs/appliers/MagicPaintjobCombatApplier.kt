package org.magiclib.paintjobs.appliers

import com.fs.graphics.Sprite
import com.fs.graphics.util.Fader
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.combat.CombatEngine
import com.fs.starfarer.combat.CombatFleetManager
import com.fs.starfarer.combat.CombatState
import com.fs.starfarer.combat.entities.Ship
import com.fs.state.AppDriver
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtils.invoke
import org.magiclib.internalextensions.findChildWithMethod
import org.magiclib.internalextensions.getChildrenCopy
import org.magiclib.paintjobs.MagicPaintjobManager
import org.magiclib.paintjobs.appliers.MagicPaintjobApplierUtils.getScreenPanel
import java.awt.Color

internal class MagicPaintjobCombatApplier : BaseEveryFrameCombatPlugin() {
    val checkTimeInterval = 0.5f // check roughly twice per second
    var lastCheckTime = checkTimeInterval
    var hasRelevantShips = false

    var errorOccured = false
    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        if (!MagicPaintjobManager.isEnabled) return // return if not enabled
        if (errorOccured) return

        if (Global.getCurrentState() == GameState.TITLE)
            return

        val engine = CombatEngine.getInstance()

        // If no paintjobs in player or enemy fleet, do not continue. cache result and update less often for performance reasons
        lastCheckTime += amount
        if (lastCheckTime >= checkTimeInterval) {
            lastCheckTime = 0f

            val enemyFleet: CampaignFleetAPI? = engine.enemyFleetManager.campaignFleet
            val playerFleet: CampaignFleetAPI? = engine.playerFleetManager.campaignFleet

            hasRelevantShips = when {
                playerFleet == null || enemyFleet == null -> true // If either are null for some reason, check for paintjobs to avoid missing any.
                playerFleet.fleetData.membersListCopy.any { MagicPaintjobManager.hasPaintjob(it) } -> true
                enemyFleet.fleetData.membersListCopy.any { MagicPaintjobManager.hasPaintjob(it) } -> true
                else -> false
            }
        }

        if (!hasRelevantShips) return

        val state = AppDriver.getInstance().currentState as? CombatState ?: return

        try {
            applyPaintJobsOnMap(state)

            applyPaintJobsOnTooltip(state, engine)
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error("Error when trying to apply paintjobs in combat", e)
            errorOccured = true
        }
    }

    private var prevForceUpdateShinyIcon = false
    //private var centerTooltipHash: Int = 0
    private fun applyPaintJobsOnTooltip(state: CombatState, engine: CombatEngine) {
        if (!state.isShowingDeploymentDialog // No need to run this code if the tooltip is not open
            && engine.isPaused // Just to be sure
        ) return
        // When a dialog is open in combat, combat is probably paused so performance isn't as needed. Members in dialog pickers can move around, especially in the fancy simulator dialog which may reset sprites. So run this function every frame if the dialog is open.

        val centerTooltip = getScreenPanel()?.findChildWithMethod("fleetMemberClicked") as? UIPanelAPI ?: return

        /*
        val newHash = centerTooltip.hashCode()
        // Only try to replace sprites once every time the shown tooltip changes
        if (centerTooltipHash == newHash
            && !engine.isSimulation // The simulator has a special tooltip which allows the user to switch tabs. To avoid having to check for if the currently open tab has changed, just skip this if statement. The Simulator doesn't need the performance anyway.
        )
            return

        centerTooltipHash = newHash
        */

        val innerPanel = centerTooltip.invoke("getInnerPanel") as? UIPanelAPI ?: return
        val fleetList1 = innerPanel.getChildrenCopy().find { it.getMethodsMatching("turnOffCRandHullBars").isNotEmpty() }
        val fleetList2 = innerPanel.getChildrenCopy().findLast { it.getMethodsMatching("turnOffCRandHullBars").isNotEmpty() }

        //val fleetList3 = centerTooltip.getFieldsMatching(type = fleetList1.javaClass).getOrNull(0)?.get(centerTooltip) as? UIPanelAPI
        //val fleetList4 = centerTooltip.getFieldsMatching(type = fleetList1.javaClass).getOrNull(1)?.get(centerTooltip) as? UIPanelAPI

        val forceUpdateShinyIcon = Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Mouse.isButtonDown(0)

        if(fleetList1 != null)
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList1,
                showShinyIcon = true,
                forceUpdateShinyIcon = forceUpdateShinyIcon || prevForceUpdateShinyIcon,
                diffPositioning = true,
            )
        if(fleetList2 != null && fleetList2 !== fleetList1)
            MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList2,
                showShinyIcon = true,
                forceUpdateShinyIcon = forceUpdateShinyIcon || prevForceUpdateShinyIcon,
                diffPositioning = true
            )
        //if(fleetList3 != null && fleetList3 !== fleetList1 && fleetList3 !== fleetList2)
        //    MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList3, true, true)
        //if(fleetList4 != null && fleetList4 !== fleetList1 && fleetList4 !== fleetList2 && fleetList4 !== fleetList3)
        //    MagicPaintjobApplierUtils.applyPaintjobsToShipList(fleetList4, true, true)

        prevForceUpdateShinyIcon = forceUpdateShinyIcon
    }

    private val appliedPaintjobsOnMap = mutableSetOf<String>()
    private fun applyPaintJobsOnMap(state: CombatState) {
        if (!state.isShowingCommandUI) return // No need to run this code if the command UI is not open

        val screenPanel = getScreenPanel() ?: return
        val mapDisplay = screenPanel.findChildWithMethod("getMapDisplay") ?: return
        val icons = mapDisplay.invoke("getIcons") as? Map<*, *> ?: return
        val shipIcons = icons.filterKeys { it is Ship }
        shipIcons.forEach { (key, value) ->
            val icon = value ?: return@forEach
            val ship = key as? ShipAPI ?: return@forEach

            val member = ship.fleetMember ?: return@forEach
            if (ship.variant?.hasHullMod("ML_skinSwap") != true) return@forEach
            if (member.id in appliedPaintjobsOnMap) return@forEach

            val memberIcon = icon.getFieldsMatching()
                .filter { it.type != Color::class.java && it.type != Float::class.java && it.type != Ship::class.java && it.type != Sprite::class.java && it.type != Fader::class.java && it.type != CombatFleetManager::class.java }
                .map { it.get(icon) }
                .find { it?.getFieldsMatching(type = FleetMember::class.java)?.isNotEmpty() == true }
                ?: return@forEach

            MagicPaintjobApplierUtils.changeIconSprite(key.fleetMember, memberIcon)
            appliedPaintjobsOnMap.add(key.fleetMember.id)
        }
    }
}