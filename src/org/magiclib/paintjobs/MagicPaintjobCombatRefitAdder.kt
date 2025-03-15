package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver

class MagicPaintjobCombatRefitAdder : BaseEveryFrameCombatPlugin() {
    companion object {
        var SHIP_PREVIEW_CLASS: Class<*>? = null
        var SHIPS_FIELD: String? = null
        var combatEngineHash: Int? = null // this only semi works, but whatever
    }

    private val panelCreator = MagicPaintjobRefitPanelCreator(false)
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        if(combatEngineHash != null && combatEngineHash != Global.getCombatEngine().hashCode()) return

        val newCoreUI = (AppDriver.getInstance().currentState as? TitleScreenState)?.let {
            ReflectionUtils.invoke("getScreenPanel", it) as? UIPanelAPI
        } ?: return
        cacheShipPreviewClass(newCoreUI)
        if (!MagicPaintjobManager.isEnabled) return // return if not enabled

        val delegateChild = newCoreUI.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("dismiss", it)
        } as? UIPanelAPI ?: return

        val oldCoreUI = delegateChild.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("getMissionInstance", it)
        } as? UIPanelAPI ?: return

        val holographicBG = oldCoreUI.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("forceFoldIn", it)
        } ?: return

        val refitTab = holographicBG.let {
            ReflectionUtils.invoke("getCurr", it)
        } as? UIPanelAPI ?: return

        panelCreator.addPaintjobButton(refitTab)
    }

    private fun cacheShipPreviewClass(newCoreUI: UIPanelAPI) {
        if (SHIP_PREVIEW_CLASS != null) return

        val missionWidget = newCoreUI.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("getMissionList", it)
        } as? UIPanelAPI ?: return

        val holographicBG = missionWidget.getChildrenCopy()[1] // 2 of the same class in the tree here

        val missionDetail = holographicBG.let {
            ReflectionUtils.invoke("getCurr", it)
        } as? UIPanelAPI ?: return

        val missionShipPreview = missionDetail.getChildrenCopy().find {
            ReflectionUtils.hasConstructorOfParameters(it, missionDetail.javaClass)
        } as? UIPanelAPI ?: return

        val shipPreview = missionShipPreview.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("isSchematicMode", it)
        } ?: return

        SHIP_PREVIEW_CLASS = shipPreview.javaClass
        val shipFields = ReflectionUtils.getFieldsOfType(shipPreview, Array<Ship>::class.java)
        SHIPS_FIELD = shipFields[0] // only one field should be Array<Ship>
    }
}