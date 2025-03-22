package org.magiclib.paintjobs


import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import org.lwjgl.input.Keyboard
import org.magiclib.ReflectionUtils
import org.magiclib.internalextensions.*
import java.awt.Color
import org.magiclib.kotlin.setAlpha
import org.magiclib.paintjobs.MagicPaintjobRefitPanel.createMagicPaintjobRefitPanel

internal object MagicPaintjobRefitPanelCreator {
    private val PAINTJOB_BUTTON_COLOR = Color(240, 160, 0, 130)
    private val PAINTJOB_BUTTON_TEXT_COLOR = PAINTJOB_BUTTON_COLOR.brighter().setAlpha(255)
    fun addPaintjobButton(refitTab: UIPanelAPI, inCampaign: Boolean) {
        val refitPanel = refitTab.findChildWithMethod("syncWithCurrentVariant") as? UIPanelAPI ?: return
        val statsAndHullmodsPanel = refitPanel.findChildWithMethod("getColorFor") as? UIPanelAPI ?: return
        val hullmodsPanel = statsAndHullmodsPanel.findChildWithMethod("removeNotApplicableMods") as? UIPanelAPI ?: return

        val fleetMember = ReflectionUtils.invoke("getMember", refitPanel) as? FleetMemberAPI
        val existingElements = hullmodsPanel.getChildrenCopy()
        val lastElement = existingElements.lastOrNull() ?: return // if children is empty, return

        val paintjobButton = existingElements.filterIsInstance<ButtonAPI>().firstOrNull { button ->
            button.customData is String && button.customData == "PAINTJOB_BUTTON"
        }

        // button should be not exist on modules, ships with a perma paintjob, or ships without any possible paintjobs
        val buttonShouldNotExist = !MagicPaintjobManager.isEnabled || fleetMember == null ||
                MagicPaintjobManager.getCurrentShipPaintjob(fleetMember)?.isPermanent == true ||
                MagicPaintjobManager.getPaintjobsForHull(fleetMember.hullId).isEmpty()

        // return if button already exists, or should not exist
        if (paintjobButton != null && buttonShouldNotExist) hullmodsPanel.removeComponent(paintjobButton)
        if (paintjobButton != null || buttonShouldNotExist) return

        // addHullmods button should always exist in hullmodsPanel
        val addButton = existingElements.filter { ReflectionUtils.hasMethodOfName("getText", it) }.find {
            (ReflectionUtils.invoke("getText", it) as String).contains("Add")
        } ?: return

        // make a new button
        val newPaintjobButton = hullmodsPanel.addButton(
            "Paintjob",
            "PAINTJOB_BUTTON",
            PAINTJOB_BUTTON_TEXT_COLOR,
            PAINTJOB_BUTTON_COLOR,
            Alignment.MID,
            CutStyle.ALL,
            Font.ORBITRON_20,
            addButton.width,
            addButton.height
        )

        newPaintjobButton.position.belowLeft(lastElement, 3f)
        newPaintjobButton.setShortcut(Keyboard.KEY_S, true)
        newPaintjobButton.onClick {
            // width/height calcs here are to match vanilla's hullmod panel sizes when screen size grow/shrink
            val width = if(inCampaign) (refitTab.width - 343).coerceIn(667f, 700f) else 667f
            val height = if(inCampaign) (refitTab.height - 12).coerceIn(722f, 800f) else 722f
            val paintjobPanel = createMagicPaintjobRefitPanel(refitTab, refitPanel, width, height)

            val coreUI = ReflectionUtils.invoke("getCoreUI", refitPanel) as UIPanelAPI
            coreUI.addComponent(paintjobPanel)

            // the numbers might look like magic, but they are actually offsets from where the vanilla refit panel ends up.
            // the other calcs here do ensure correct relative placement
            val xOffset = if(inCampaign) (refitTab.width - 1037).coerceIn(6f, 213f) else 6f
            paintjobPanel.xAlignOffset = refitTab.leftX - paintjobPanel.leftX + xOffset
            paintjobPanel.yAlignOffset = refitTab.topY - paintjobPanel.topY - 6

            // add back button here to make sure its lined up with existing button
            val goBackButton = paintjobPanel.addButton(
                "Go Back",
                null,
                PAINTJOB_BUTTON_TEXT_COLOR,
                PAINTJOB_BUTTON_COLOR,
                Alignment.MID,
                CutStyle.ALL,
                Font.ORBITRON_20,
                addButton.width,
                addButton.height
            )

            goBackButton.xAlignOffset = newPaintjobButton.leftX - goBackButton.leftX
            goBackButton.yAlignOffset = newPaintjobButton.bottomY - goBackButton.bottomY
            goBackButton.setShortcut(Keyboard.KEY_S, true)
            goBackButton.onClick {
                paintjobPanel.parent?.removeComponent(paintjobPanel)
            }
        }
    }
}