package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver


class MagicPaintjobCampaignRefitAdder : EveryFrameScript {
    private val panelCreator = MagicPaintjobRefitPanelCreator()
    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        if (!Global.getSector().isPaused) return //Return if not paused
        if (Global.getSector().campaignUI.currentCoreTab != CoreUITabId.REFIT) return //Return if not Refit

        val state = AppDriver.getInstance().currentState
        if (state !is CampaignState) return

        val newCoreUI = (ReflectionUtils.invoke("getEncounterDialog", state)?.let { dialog ->
            ReflectionUtils.invoke("getCoreUI", dialog) as? UIPanelAPI
        } ?: ReflectionUtils.invoke("getCore", state) as? UIPanelAPI) ?: return

        val borderContainer = newCoreUI.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("setBorderInsetLeft", it) } as? UIPanelAPI ?: return
        val refitTab = borderContainer.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("goBackToParentIfNeeded", it) } as? UIPanelAPI ?: return

        panelCreator.addPaintjobButton(refitTab)
    }
}