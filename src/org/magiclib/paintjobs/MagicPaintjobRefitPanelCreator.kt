package org.magiclib.paintjobs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.loading.specs.HullVariantSpec
import org.magiclib.kotlin.setAlpha
import java.awt.Color

internal class MagicPaintjobRefitPanelCreator {
    companion object {
        var SHIP_PREVIEW_CLASS: Class<*>? = null
        var SHIPS_FIELD: String? = null
    }

    private var buttonPanel: CustomPanelAPI? = null
    private var paintjobPanel: CustomPanelAPI? = null
    fun addPaintjobButton(refitTab: UIPanelAPI) {
        val refitPanel = refitTab.getChildrenCopy()
            .find { ReflectionUtils.hasMethodOfName("syncWithCurrentVariant", it) } as? UIPanelAPI ?: return
        val statsAndHullmodsPanel =
            refitPanel.getChildrenCopy().find { ReflectionUtils.hasMethodOfName("getColorFor", it) } as? UIPanelAPI
                ?: return
        val hullmodsPanel = statsAndHullmodsPanel.getChildrenCopy()
            .find { ReflectionUtils.hasMethodOfName("removeNotApplicableMods", it) } as? UIPanelAPI ?: return
        val fleetMember = ReflectionUtils.invoke("getMember", refitPanel) as? FleetMemberAPI

        val existingElements = hullmodsPanel.getChildrenCopy()
        val lastElement = existingElements.last()

        // button should be removed on modules
        if (buttonPanel != null && fleetMember == null) hullmodsPanel.removeComponent(buttonPanel)

        // button already exists
        if (buttonPanel != null && existingElements.contains(buttonPanel as UIComponentAPI)) {
            return
        }
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

        paintjobButton.onClick {
            if (paintjobButton.text == "Paintjob") {

                val shipDisplay = ReflectionUtils.invoke("getShipDisplay", refitPanel) as? UIPanelAPI
                //val fleetMember = ReflectionUtils.invoke("getMember", refitPanel) as? FleetMemberAPI
                val variant = shipDisplay?.let { ReflectionUtils.invoke("getCurrentVariant", it) as? HullVariantSpec }
                val shipPreviews = makeShipPreviews(variant!!)
                val paintjobPanel = createMagicPaintjobRefitPanel()
                val coreUI = ReflectionUtils.invoke("getCoreUI", refitPanel) as UIPanelAPI
                coreUI.addComponent(paintjobPanel)

                val inCampaign = ReflectionUtils.invoke("isCampaignUI", coreUI) as Boolean
                if (inCampaign) paintjobPanel.position.inTL(refitPanel.position.x, refitPanel.position.y)

                var prev: UIPanelAPI? = null
                shipPreviews.forEach { preview ->
                    paintjobPanel.addComponent(preview).let { pos ->
                        if (prev == null) pos.inTL(0f, 10f) else pos.rightOfTop(prev, 3f)
                        prev = preview
                    }
                }
            }
        }
    }

    private fun makeShipPreviews(hullVariantSpec: HullVariantSpec): List<UIPanelAPI> {
        val baseHullPaintjobs = MagicPaintjobManager.getPaintjobsForHull(
            (hullVariantSpec as ShipVariantAPI).hullSpec.baseHullId,
            includeShiny = false
        )
        val shipPreviews = mutableListOf<UIPanelAPI>()
        (listOf(null) + baseHullPaintjobs).forEach { baseHullPaintjob ->
            val shipPreview = ReflectionUtils.instantiate(SHIP_PREVIEW_CLASS!!)!!

            ReflectionUtils.invoke("setVariant", shipPreview, hullVariantSpec)
            ReflectionUtils.invoke("overrideVariant", shipPreview, hullVariantSpec)
            ReflectionUtils.invoke("setShowBorder", shipPreview, false)
            ReflectionUtils.invoke("setScaleDownSmallerShipsMagnitude", shipPreview, 1f)
            ReflectionUtils.invoke("adjustOverlay", shipPreview, 0f, 0f)
            (shipPreview as UIPanelAPI).position.setSize(200f, 200f)

            // make the ship list so the ships exist when we try and get them
            ReflectionUtils.invoke("prepareShip", shipPreview)
            val ships = ReflectionUtils.get(SHIPS_FIELD!!, shipPreview) as Array<ShipAPI>

            // if the paintjob exists, replace the sprites
            if (baseHullPaintjob != null) {
                ships.forEach { ship ->
                    val paintjobs =
                        MagicPaintjobManager.getPaintjobsForHull(ship.hullSpec.baseHullId, includeShiny = false)
                    paintjobs.forEach { paintjob ->
                        if (paintjob.id.contains(baseHullPaintjob.id, true)) {
                            MagicPaintjobManager.applyPaintjob(null, ship, paintjob)
                        }
                    }

                }
            }
            shipPreviews.add(shipPreview)

        }
        return shipPreviews
    }
}