package org.magiclib.paintjobs

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver

class MagicPaintjobCombatRefitAdder : BaseEveryFrameCombatPlugin() {
    private val panelCreator = MagicPaintjobRefitPanelCreator()
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val newCoreUI = (AppDriver.getInstance().currentState as? TitleScreenState)?.let {
            ReflectionUtils.invoke("getScreenPanel", it) as? UIPanelAPI
        } ?: return
        cacheShipPreviewClass(newCoreUI)

        val delegateChild = newCoreUI.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("dismiss", it) } as? UIPanelAPI ?: return
        val oldCoreUI = delegateChild.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("getMissionInstance", it) } as? UIPanelAPI ?: return
        val holographicBG = oldCoreUI.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("forceFoldIn", it) } ?: return
        val refitTab = holographicBG.let { ReflectionUtils.invoke("getCurr", it) } as? UIPanelAPI ?: return

        panelCreator.addPaintjobButton(refitTab)
    }

    private fun cacheShipPreviewClass(newCoreUI : UIPanelAPI){
        if (MagicPaintjobRefitPanelCreator.SHIP_PREVIEW_CLASS != null) return
        val missionWidget = newCoreUI.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("getMissionList", it) } as? UIPanelAPI ?: return
        val holographicBG = missionWidget.getChildrenCopy()[1] // 2 of the same class in the tree here
        val missionDetail = holographicBG.let { ReflectionUtils.invoke("getCurr", it) } as? UIPanelAPI ?: return
        val missionShipPreview = missionDetail.getChildrenCopy().find { ReflectionUtils.hasConstructorOfParameters(it, missionDetail.javaClass) } as? UIPanelAPI ?: return
        val shipPreview = missionShipPreview.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("isSchematicMode", it) } ?: return
        MagicPaintjobRefitPanelCreator.SHIP_PREVIEW_CLASS = shipPreview.javaClass
        MagicPaintjobRefitPanelCreator.SHIPS_FIELD = ReflectionUtils.getFieldsOfType(shipPreview, Array<Ship>::class.java)[0]
    }
}