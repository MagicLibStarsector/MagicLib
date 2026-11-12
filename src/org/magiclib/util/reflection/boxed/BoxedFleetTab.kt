package org.magiclib.util.reflection.boxed

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtilsSafe.safeInvoke
import org.magiclib.util.reflection.UIFinder.getFleetTab

/**
 * A boxed version of the fleet tab, providing access to the fleet tab and its children's methods via reflection.
 */
class BoxedFleetTab private constructor(override val target: UIPanelAPI) : BoxedUIElement(target),
    UIPanelAPI by target {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun get(fleetTab: UIPanelAPI? = getFleetTab()): BoxedFleetTab? {
            return fleetTab?.let { BoxedFleetTab(it) }
        }

        private var postUpdateFleetPanelCallbacks = linkedSetOf<() -> Unit>()

        /**
         * Adds a callback to be called after the fleet panel contents are updated via [updateFleetPanelContents].
         *
         * This is useful when you need to read or alter the fleet panel contents after it has been updated.
         *
         * The added callbacks are static, not per-instance. They persist until game restart.
         */
        @JvmStatic
        fun addPostUpdateFleetPanelCallback(callback: () -> Unit) {
            if (postUpdateFleetPanelCallbacks.contains(callback)) return

            postUpdateFleetPanelCallbacks.add(callback)
        }

        @JvmStatic
        fun removePostUpdateFleetPanelCallback(callback: () -> Unit) {
            postUpdateFleetPanelCallbacks.remove(callback)
        }

        private const val METHOD_GET_FLEET_PANEL = "getFleetPanel"
        private const val METHOD_GET_FLEET_DATA = "getFleetData"
        private const val METHOD_UPDATE_LIST_CONTENTS = "updateListContents"
        private const val METHOD_GET_CLICK_AND_DROP_HANDLER = "getClickAndDropHandler"
        private const val METHOD_GET_PICKED_UP_MEMBER = "getPickedUpMember"
        private const val METHOD_GET_SUBMARKET = "getSubmarket"
        private const val METHOD_GET_MODE = "getMode"
    }

    val fleetPanel: UIPanelAPI = run {
        check(target.getMethodsMatching(METHOD_GET_FLEET_PANEL).isNotEmpty()) {
            "${target::class.java.name} has no $METHOD_GET_FLEET_PANEL method, Is this not a fleet tab?"
        }
        target.safeInvoke(METHOD_GET_FLEET_PANEL) as? UIPanelAPI
            ?: error("$METHOD_GET_FLEET_PANEL did not return a UIPanelAPI")
    }

    /**
     * Returns the fleet data displayed in the fleet tab.
     */
    fun getFleetData(): FleetDataAPI? {
        return fleetPanel.safeInvoke(METHOD_GET_FLEET_DATA) as? FleetDataAPI
    }

    /**
     * Updates the fleet panel contents. This effectively refreshes the fleet panel, updating all displayed ships.
     *
     * This will also call all registered post update callbacks after updating the contents. See [addPostUpdateFleetPanelCallback]
     */
    fun updateFleetPanelContents() {
        fleetPanel.safeInvoke(METHOD_UPDATE_LIST_CONTENTS)

        postUpdateFleetPanelCallbacks.toList().forEach {
            runCatching { it.invoke() }.onFailure { e -> Global.getLogger(this::class.java).error("Fleet panel callback failed", e) }
        }
    }

    /**
     * Gets the fleet member currently picked up by the player, if any.
     *
     * This is the fleet member that the player has clicked on and is currently moving with their mouse.
     */
    fun getPickedUpMember(): FleetMemberAPI? {
        val clickAndDropHandler = fleetPanel.safeInvoke(METHOD_GET_CLICK_AND_DROP_HANDLER) ?: return null
        return clickAndDropHandler.safeInvoke(METHOD_GET_PICKED_UP_MEMBER) as? FleetMemberAPI
    }

    /**
     * Gets the submarket currently open in the fleet tab, if any.
     */
    fun getSubmarket(): SubmarketAPI? {
        return fleetPanel.safeInvoke(METHOD_GET_SUBMARKET) as? SubmarketAPI
    }

    /**
     * Represents the current UI mode of the fleet panel.
     */
    enum class Mode {
        /** Fleet tab opened in sector */
        FLEET,

        /** Fleet tab in market buy mode */
        MARKET_BUY,

        /** Fleet tab in market sell mode */
        MARKET_SELL,

        /** Fleet tab in storage give mode */
        STORAGE_STORE,

        /** Fleet tab in storage take mode */
        STORAGE_TAKE,

        /** Unknown mode */
        UNKNOWN,

        /** Failed to get mode */
        ERROR,
    }

    /**
     * Gets the current [Mode] of the fleet panel.
     *
     * i.e. what the fleet tab is currently being used for:
     * plain fleet management, buying/selling at a market, storing/taking cargo from storage.
     */
    fun getMode(): Mode {
        val mode = fleetPanel.safeInvoke(METHOD_GET_MODE) as? Enum<*> ?: return Mode.ERROR
        return when (mode.ordinal) {
            0 -> { // NORMAL
                Mode.FLEET
            }
            1 -> { // BUY
                Mode.MARKET_BUY
            }
            2 -> { // SELL
                Mode.MARKET_SELL
            }
            3 -> { // GIVE
                Mode.STORAGE_STORE
            }
            4 -> { // TAKE
                Mode.STORAGE_TAKE
            }
            else -> Mode.UNKNOWN
        }
    }
}