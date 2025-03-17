package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import org.lwjgl.input.Keyboard
import org.magiclib.kotlin.setAlpha
import java.awt.Color

internal class MagicPaintjobRefitPanelCreator(var inCampaign: Boolean) {
    private var buttonPanel: CustomPanelAPI? = null
    fun addPaintjobButton(refitTab: UIPanelAPI) {
        val refitPanel = refitTab.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("syncWithCurrentVariant", it)
        } as? UIPanelAPI ?: return

        val statsAndHullmodsPanel = refitPanel.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("getColorFor", it)
        } as? UIPanelAPI ?: return

        val hullmodsPanel = statsAndHullmodsPanel.getChildrenCopy().find {
            ReflectionUtils.hasMethodOfName("removeNotApplicableMods", it)
        } as? UIPanelAPI ?: return

        val fleetMember = ReflectionUtils.invoke("getMember", refitPanel) as? FleetMemberAPI
        val existingElements = hullmodsPanel.getChildrenCopy()
        val lastElement = existingElements.last()

        // button should be removed on modules, ships with a perma paintjob, or ships without any possible paintjobs
        if(!MagicPaintjobManager.isEnabled || fleetMember == null ||
            MagicPaintjobManager.getCurrentShipPaintjob(fleetMember)?.isPermanent == true ||
            MagicPaintjobManager.getPaintjobsForHull(fleetMember.hullId).isEmpty() ){
            buttonPanel?.let{ hullmodsPanel.removeComponent(it) }
            return
        }

        // button already exists
        if (buttonPanel != null && existingElements.contains(buttonPanel!!)) return

        // addHullmods button should always exist
        val addButton = existingElements.filter { ReflectionUtils.hasMethodOfName("getText", it) }.find {
            (ReflectionUtils.invoke("getText", it) as String).contains("Add")
        } ?: return

        // make a new button
        MagicPaintjobCombatRefitAdder.combatEngineHash = Global.getCombatEngine()?.hashCode()

        buttonPanel = Global.getSettings().createCustom(addButton.position.width, addButton.position.height, null)
        hullmodsPanel.addComponent(buttonPanel).belowLeft(lastElement, 3f)
        val mainElement = buttonPanel!!.createUIElement(addButton.position.width, addButton.position.height, false)
        buttonPanel!!.addUIElement(mainElement).inTL(0f, 0f)

        mainElement.setButtonFontOrbitron20()
        val paintjobButtonColor = Color(240, 160, 0, 130)
        val paintjobButtonTextColor = paintjobButtonColor.brighter().setAlpha(255)
        val paintjobButton = mainElement.addButton(
            "Paintjob",
            null,
            paintjobButtonTextColor,
            paintjobButtonColor,
            Alignment.MID,
            CutStyle.ALL,
            addButton.position.width,
            addButton.position.height,
            0f
        )
        paintjobButton.position.inTL(0f, 0f)
        paintjobButton.setShortcut(Keyboard.KEY_S, true)
        paintjobButton.onClick {
            val coreUI = ReflectionUtils.invoke("getCoreUI", refitPanel) as UIPanelAPI
            val width = if(inCampaign) (refitTab.position.width - 343).coerceIn(667f, 700f) else 667f
            val height = if(inCampaign) (refitTab.position.height - 12).coerceIn(722f, 800f) else 722f
            val paintjobPanel = createMagicPaintjobRefitPanel(refitTab, refitPanel, width, height, inCampaign)
            coreUI.addComponent(paintjobPanel)

            // the numbers might look like magic, but they are actually offsets from where the vanilla refit panel ends up.
            // the other calcs here do ensure correct relative placement
            val xOffset = refitTab.position.x - paintjobPanel.position.x +
                    if(inCampaign) (refitTab.position.width - 1037).coerceIn(6f, 213f) else 6f
            val yOffset = refitTab.position.y + refitTab.position.height - paintjobPanel.position.y - height - 6
            paintjobPanel.position.setXAlignOffset(xOffset)
            paintjobPanel.position.setYAlignOffset(yOffset)

            // add back button here to make sure its lined up with existing button
            val goBackButton = paintjobPanel.addButton(
                "Go Back",
                paintjobButtonTextColor,
                paintjobButtonColor,
                Alignment.MID,
                CutStyle.ALL,
                addButton.position.width,
                addButton.position.height
            )

            goBackButton.position.setXAlignOffset(paintjobButton.position.x-goBackButton.position.x)
            goBackButton.position.setYAlignOffset(paintjobButton.position.y-goBackButton.position.y)
            goBackButton.setShortcut(Keyboard.KEY_S, true)
            goBackButton.onClick {
                paintjobPanel.getParent()!!.removeComponent(paintjobPanel)
            }
        }
    }
}