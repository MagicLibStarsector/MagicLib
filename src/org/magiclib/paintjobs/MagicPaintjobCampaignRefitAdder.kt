package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import org.magiclib.ReflectionUtils
import org.magiclib.internalextensions.*

internal class MagicPaintjobCampaignRefitAdder : EveryFrameScript {
    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        if (!MagicPaintjobManager.isEnabled) return // return if not enabled
        if (!Global.getSector().isPaused) return //Return if not paused
        if (Global.getSector().campaignUI.currentCoreTab != CoreUITabId.REFIT) return //Return if not Refit

        val state = AppDriver.getInstance().currentState
        if (state !is CampaignState) return

        val newCoreUI = (ReflectionUtils.invoke("getEncounterDialog", state)?.let { dialog ->
            ReflectionUtils.invoke("getCoreUI", dialog) as? UIPanelAPI
        } ?: ReflectionUtils.invoke("getCore", state) as? UIPanelAPI) ?: return

        val borderContainer = newCoreUI.findChildWithMethod("setBorderInsetLeft") as? UIPanelAPI ?: return
        val refitTab = borderContainer.findChildWithMethod("goBackToParentIfNeeded") as? UIPanelAPI ?: return

        MagicPaintjobRefitPanelCreator.addPaintjobButton(refitTab, true)
    }
}