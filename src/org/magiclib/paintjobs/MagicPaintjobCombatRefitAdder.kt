package org.magiclib.paintjobs

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver
import org.magiclib.ReflectionUtils
import org.magiclib.ReflectionUtils.getConstructorsMatching
import org.magiclib.internalextensions.*

class MagicPaintjobCombatRefitAdder : BaseEveryFrameCombatPlugin() {
    companion object {
        var SHIP_PREVIEW_CLASS: Class<*>? = null
        var SHIPS_FIELD: String? = null
    }
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val newCoreUI = (AppDriver.getInstance().currentState as? TitleScreenState)?.let {
            ReflectionUtils.invoke(it, "getScreenPanel") as? UIPanelAPI
        } ?: return
        cacheShipPreviewClass(newCoreUI)
        if (!MagicPaintjobManager.isEnabled) return // return if not enabled

        val delegateChild = newCoreUI.findChildWithMethod("dismiss") as? UIPanelAPI ?: return
        val oldCoreUI = delegateChild.findChildWithMethod("getMissionInstance") as? UIPanelAPI ?: return
        val holographicBG = oldCoreUI.findChildWithMethod("forceFoldIn") ?: return

        val refitTab = holographicBG.let {
            ReflectionUtils.invoke(it, "getCurr")
        } as? UIPanelAPI ?: return

        MagicPaintjobRefitPanelCreator.addPaintjobButton(refitTab, false)
    }

    private fun cacheShipPreviewClass(newCoreUI: UIPanelAPI) {
        if (SHIP_PREVIEW_CLASS != null) return

        val missionWidget = newCoreUI.findChildWithMethod("getMissionList") as? UIPanelAPI ?: return
        val holographicBG = missionWidget.getChildrenCopy()[1] // 2 of the same class in the tree here

        val missionDetail = holographicBG.let {
            ReflectionUtils.invoke(it, "getCurr")
        } as? UIPanelAPI ?: return

        val missionShipPreview = missionDetail.getChildrenCopy().find {
            it.javaClass.getConstructorsMatching(parameterTypes = arrayOf(missionDetail.javaClass)).isNotEmpty()
        } as? UIPanelAPI ?: return

        val shipPreview = missionShipPreview.findChildWithMethod("isSchematicMode") ?: return

        SHIP_PREVIEW_CLASS = shipPreview.javaClass
        val shipFields = ReflectionUtils.getFieldsMatching(shipPreview, type = Array<Ship>::class.java)
        SHIPS_FIELD = shipFields[0].name // only one field should be Array<Ship>
    }
}