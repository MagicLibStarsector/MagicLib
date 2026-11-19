package org.magiclib.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript
import com.fs.starfarer.api.ui.UIPanelAPI
import org.magiclib.ReflectionUtilsSafe.safeInvoke
import org.magiclib.kotlin.internal.findChildWithMethod
import org.magiclib.util.api.getActualCurrentTab
import org.magiclib.util.reflection.boxed.BoxedRefitTab
import org.magiclib.util.taskscheduler.SystemTaskScheduler

/**
 * Makes d-mod repair on `restoreToBaseHull` hulls properly restore the hull to the base hull.
 *
 * In the base-game, built-in d-mods can't be removed while `isRestoreToBase` is true as of 0.98.5a-RC8.
 *
 * This clears the `restoreToBaseHull` flag while the repair dialog is open and restores it when it closes.
 * If no d-mods remain on closing the dialog, this converts the variant to its base hull and refreshes the refit screen.
 *
 * Only affects hulls from mods registered with [enableForMod].
 */
class MagicDeferredDHullRestore: EveryFrameScript {
    companion object {
        private val modIdsWithDeferredRestore: MutableSet<String> = mutableSetOf()

        /** Opts in for hulls from the mod with this id. */
        @JvmStatic
        fun enableForMod(modID: String) {
            modIdsWithDeferredRestore.add(modID)
        }

        /** Whether [modID] was registered via [enableForMod]. */
        @JvmStatic
        fun isEnabledFor(modID: String?): Boolean =
            modID != null && modID in modIdsWithDeferredRestore
    }

    private var hitError = false
    override fun isDone(): Boolean = hitError
    override fun runWhilePaused(): Boolean = true
    override fun advance(amount: Float) {
        try {
            val sector = Global.getSector()
            if (!sector.isPaused) return
            if (sector.campaignUI.getActualCurrentTab() != CoreUITabId.REFIT) return
            val refitTab = BoxedRefitTab.get() ?: return
            val coreUI = refitTab.getCoreUI() as? UIPanelAPI ?: return
            val variant = refitTab.getCurrentVariant() ?: return
            if (variant.hullSpec.isRestoreToBase && isEnabledFor(variant.hullSpec.sourceMod?.id)) {
                val modPicker = coreUI.findChildWithMethod("isRepairDmodsMode") as? UIPanelAPI ?: return
                if (modPicker.safeInvoke("isRepairDmodsMode") as? Boolean != true) return
                variant.hullSpec.isRestoreToBase = false
                SystemTaskScheduler.performEvery { handle ->
                    if (coreUI.findChildWithMethod("isRepairDmodsMode") as? UIPanelAPI == null) {
                        variant.hullSpec.isRestoreToBase = true
                        handle.cancel()
                        val currentVariant = refitTab.getCurrentVariant() ?: return@performEvery
                        if (!currentVariant.hasDMods()) {
                            FieldRepairsScript.restoreToNonDHull(currentVariant)
                            refitTab.syncWithCurrentVariant()
                            refitTab.updateModules()
                            refitTab.updateButtonPositionsToZoomLevel()
                            refitTab.saveCurrentVariant()
                            val playerFleet = Global.getSector().playerFleet
                            playerFleet.fleetData.setSyncNeeded(); playerFleet.fleetData.syncIfNeeded()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Global.getLogger(this.javaClass).error(e)
            hitError = true
        }
    }
}