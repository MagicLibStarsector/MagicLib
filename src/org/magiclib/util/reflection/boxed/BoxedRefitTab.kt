package org.magiclib.util.reflection.boxed

import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtilsSafe.safeInvoke
import org.magiclib.util.reflection.UIFinder.getRefitTab

/**
 * A boxed version of the refit tab, providing access to the refit tab and its children's methods via reflection.
 */
class BoxedRefitTab private constructor(override val target: UIPanelAPI) : BoxedUIElement(target),
    UIPanelAPI by target {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun get(refitTab: UIPanelAPI? = getRefitTab()): BoxedRefitTab? {
            return refitTab?.let { BoxedRefitTab(it) }
        }

        private const val METHOD_GET_REFIT_PANEL = "getRefitPanel"
        private const val METHOD_GET_SHIP_DISPLAY = "getShipDisplay"
        private const val METHOD_GET_CURRENT_VARIANT = "getCurrentVariant"
        private const val METHOD_GET_MEMBER = "getMember"
        private const val METHOD_SYNC_WITH_CURRENT_VARIANT = "syncWithCurrentVariant"
        private const val METHOD_UPDATE_MODULES = "updateModules"
        private const val METHOD_UPDATE_FROM_CURRENT_VARIANT = "updateFromCurrentVariant"
        private const val METHOD_UPDATE_BUTTON_POSITIONS_TO_ZOOM_LEVEL = "updateButtonPositionsToZoomLevel"
        private const val METHOD_SET_SUPPRESS_MESSAGES = "setSuppressMessages"
        private const val METHOD_SET_EDITED_SINCE_SAVE = "setEditedSinceSave"
        private const val METHOD_SAVE_CURRENT_VARIANT = "saveCurrentVariant"
    }

    val refitPanel: UIPanelAPI = run {
        check(target.getMethodsMatching(METHOD_GET_REFIT_PANEL).isNotEmpty()) {
            "${target::class.java.name} has no $METHOD_GET_REFIT_PANEL method, Is this not a refit tab?"
        }
        target.safeInvoke(METHOD_GET_REFIT_PANEL) as? UIPanelAPI
            ?: error("$METHOD_GET_REFIT_PANEL did not return a UIPanelAPI")
    }

    val shipDisplay: UIPanelAPI by lazy {
        check(refitPanel.getMethodsMatching(METHOD_GET_SHIP_DISPLAY).isNotEmpty()) {
            "${refitPanel::class.java.name} has no $METHOD_GET_SHIP_DISPLAY method. Is this not a refit panel?"
        }
        refitPanel.safeInvoke(METHOD_GET_SHIP_DISPLAY) as? UIPanelAPI
            ?: error("$METHOD_GET_SHIP_DISPLAY did not return a UIPanelAPI")
    }

    /**
     * Return the current variant being edited in the refit tab (not yet saved)
     */
    fun getCurrentVariant(): ShipVariantAPI? =
        shipDisplay.safeInvoke(METHOD_GET_CURRENT_VARIANT) as? ShipVariantAPI

    /**
     * Rebuilds the module slot buttons on the ship. (for ships with modules, such as stations.)
     */
    fun updateModules() =
        shipDisplay.safeInvoke(METHOD_UPDATE_MODULES)

    /**
     * Repositions all slot/module buttons to match the display's current zoom scale
     */
    fun updateButtonPositionsToZoomLevel() =
        shipDisplay.safeInvoke(METHOD_UPDATE_BUTTON_POSITIONS_TO_ZOOM_LEVEL)

    /**
     * Refreshes all the refit sub-panels (weapons, fighters, hullmods, etc.) so they
     * match the variant currently being edited.
     *
     * This effectively applies the variant visibly to the one seen in the refit tab.
     */
    fun syncWithCurrentVariant() =
        refitPanel.safeInvoke(METHOD_SYNC_WITH_CURRENT_VARIANT)

    /*
    /**
     * Rebuilds the live preview Ship instance (sprite, stats, CR effects) from the current variant. Does not update anything on the refit tab other than the ship.
     */
    fun updateFromCurrentVariant() =
        shipDisplay.safeInvoke(METHOD_UPDATE_FROM_CURRENT_VARIANT)
    */ // This method gets called by syncWithCurrentVariant; thus to avoid confusion it was commented out.

    /**
     * Return the fleet member currently loaded into the refit panel
     */
    fun getCurrentMember(): FleetMemberAPI? =
        refitPanel.safeInvoke(METHOD_GET_MEMBER) as? FleetMemberAPI

    /**
     * Toggles whether the ship display shows messages (e.g. 1000 credits spent on ...)
     */
    fun setSuppressMessages(value: Boolean) =
        shipDisplay.safeInvoke(METHOD_SET_SUPPRESS_MESSAGES, value)


    /**
     * Marks the refit as having unsaved changes; toggles the Save/Undo buttons' enabled state
     */
    fun setEditedSinceSave(value: Boolean) =
        refitPanel.safeInvoke(METHOD_SET_EDITED_SINCE_SAVE, value)

    /**
     * Commits the in-progress variant to the fleet member (clones it, sets source, notifies listeners)
     *
     * This is typically called on leaving the refit tab or switching to edit a different ship.
     */
    @JvmOverloads
    fun saveCurrentVariant(forceMessage: Boolean = false) =
        refitPanel.safeInvoke(METHOD_SAVE_CURRENT_VARIANT, forceMessage)

    //fun recreateUI() =
    //    refitPanel.safeInvoke("recreateUI")
}