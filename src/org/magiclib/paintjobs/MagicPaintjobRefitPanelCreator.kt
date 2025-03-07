package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.loading.specs.HullVariantSpec
import org.lwjgl.input.Keyboard
import org.magiclib.kotlin.setAlpha
import java.awt.Color

internal class MagicPaintjobRefitPanelCreator {
    companion object {
        var SHIP_PREVIEW_CLASS: Class<*>? = null
        var SHIPS_FIELD: String? = null
    }

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

        // button should be removed on modules
        if (buttonPanel != null && fleetMember == null) hullmodsPanel.removeComponent(buttonPanel)

        // button already exists
        if (buttonPanel != null && existingElements.contains(buttonPanel as UIComponentAPI)) return

        // addHullmods button should always exist
        val addButton = existingElements.filter { ReflectionUtils.hasMethodOfName("getText", it) }.find {
            (ReflectionUtils.invoke("getText", it) as String).contains("Add")
        } ?: return

        // make a new button
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
            val shipDisplay = ReflectionUtils.invoke("getShipDisplay", refitPanel) as? UIPanelAPI
            //val fleetMember = ReflectionUtils.invoke("getMember", refitPanel) as? FleetMemberAPI
            val variant = shipDisplay?.let { ReflectionUtils.invoke("getCurrentVariant", it) as? HullVariantSpec }

            val paintjobPanel = createMagicPaintjobRefitPanel(variant!!)
            coreUI.addComponent(paintjobPanel)

            paintjobPanel.position.setXAlignOffset(refitPanel.position.x-paintjobPanel.position.x)
            paintjobPanel.position.setYAlignOffset(refitPanel.position.y-paintjobPanel.position.y+40)

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