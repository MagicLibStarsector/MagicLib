package org.magiclib.paintjobs

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import org.magiclib.ReflectionUtils
import org.magiclib.kotlin.internal.findChildWithMethod
import org.magiclib.util.reflection.boxed.BoxedRefitTab

internal class MagicPaintjobCampaignRefitAdder : EveryFrameScript {
    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        if (!MagicPaintjobManager.isEnabled) return // Return if not enabled
        if (Global.getCurrentState() != GameState.CAMPAIGN) return // Return if not campaign
        if (!Global.getSector().isPaused) return // Return if not paused

        val refitTab = BoxedRefitTab.get() ?: return

        MagicPaintjobRefitPanelCreator.addPaintjobButton(refitTab, true)
    }
}