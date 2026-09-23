package org.magiclib.util.reflection

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUIAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.impl.codex.CodexDialogAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.state.AppDriver
import org.magiclib.ReflectionUtils.getMethodsMatching
import org.magiclib.ReflectionUtilsSafe.safeInvoke
import org.magiclib.kotlin.internal.findChildWithMethod
import org.magiclib.kotlin.internal.getChildrenCopy
import org.magiclib.util.api.getActualCurrentTab

object UIFinder {

    @JvmStatic
    fun getScreenPanel(): UIPanelAPI? {
        val state = AppDriver.getInstance().currentState
        return state.safeInvoke("getScreenPanel") as? UIPanelAPI
    }

    // Tip: Core UI is a child of the screen panel
    @JvmStatic
    fun getCoreUI(): CoreUIAPI? {
        val state = AppDriver.getInstance().currentState
        if (state is CampaignUIAPI) {
            return (state.currentInteractionDialog?.let { dialog ->
                dialog.safeInvoke("getCoreUI") as? CoreUIAPI
            } ?: state.safeInvoke("getCore") as? CoreUIAPI)
        }// else if (state is TitleScreenState || state is CombatState) {
        //  return null
        //  }
        return null
    }

    @JvmOverloads
    @JvmStatic
    fun getCaptainPickerDialog(coreUI: CoreUIAPI? = getCoreUI()): CaptainPickerDialog? {
        val children = coreUI?.safeInvoke("getChildrenNonCopy") as? MutableList<*> ?: return null
        return children.firstOrNull { it is CaptainPickerDialog } as? CaptainPickerDialog
    }

    @JvmOverloads
    @JvmStatic
    fun getBorderContainer(coreUI: CoreUIAPI? = getCoreUI()): UIPanelAPI? {
        return (coreUI as? UIPanelAPI)?.findChildWithMethod("setBorderInsetLeft") as? UIPanelAPI
    }

    @JvmOverloads
    @JvmStatic
    fun getCurrentTab(coreUI: CoreUIAPI? = getCoreUI()): UIPanelAPI? {
        return coreUI?.safeInvoke("getCurrentTab") as? UIPanelAPI
    }

    @JvmStatic
    fun getRefitTab(): UIPanelAPI? {
        if (Global.getCurrentState() == GameState.CAMPAIGN) {
            return if (Global.getSector()?.campaignUI?.getActualCurrentTab() == CoreUITabId.REFIT)
                getCurrentTab()
            else
                null
            //return getBorderContainer()?.findChildWithMethod("goBackToParentIfNeeded") as? UIPanelAPI
        } else { // Get title-screen mission refit tab
            val delegateChild = getScreenPanel()?.findChildWithMethod("dismiss") as? UIPanelAPI ?: return null
            val oldCoreUI = delegateChild.findChildWithMethod("getMissionInstance") as? UIPanelAPI ?: return null
            val holographicBG = oldCoreUI.findChildWithMethod("forceFoldIn") ?: return null

            return holographicBG.safeInvoke("getCurr") as? UIPanelAPI
        }
    }

    @JvmStatic
    fun getIntelTab(): UIPanelAPI? {
        return if (Global.getSector()?.campaignUI?.getActualCurrentTab() != CoreUITabId.INTEL)
            null
        else
            getCurrentTab()
    }

    @JvmStatic
    fun getFleetTab(): UIPanelAPI? {
        return if (Global.getSector()?.campaignUI?.getActualCurrentTab() != CoreUITabId.FLEET)
            null
        else
            getCurrentTab()
    }

    @JvmStatic
    fun getCargoTab(): UIPanelAPI? {
        return if (Global.getSector()?.campaignUI?.getActualCurrentTab() != CoreUITabId.CARGO)
            null
        else
            getCurrentTab()
    }

    @JvmStatic
    fun getCodexDialog(): CodexDialogAPI? {
        val gameState = Global.getCurrentState()

        if (Global.getSettings().isShowingCodex) { //isShowingCodex does not work in all cases as of 0.98
            val appState = AppDriver.getInstance().currentState

            if (gameState == GameState.COMBAT) {
                //Combat F2 with ship selected, simulator ship F2.
                if (appState.getMethodsMatching("getRibbon").isNotEmpty()) {
                    val ribbon = appState.safeInvoke("getRibbon") as? UIPanelAPI?
                    val temp = ribbon?.safeInvoke("getParent") as? UIPanelAPI?
                    val codex = temp?.getChildrenCopy()?.find { it is CodexDialogAPI } as? CodexDialogAPI
                    if (codex != null) return codex
                }
                //Note that the codex that opens from clicking the combat "More Info" question mark button appears in the below and not the above
            }

            //F2 press, and in some other places
            val codexOverlayPanel = appState.safeInvoke("getOverlayPanelForCodex") as? UIPanelAPI?
            val codex = codexOverlayPanel?.getChildrenCopy()?.find { it is CodexDialogAPI } as? CodexDialogAPI
            if (codex != null)
                return codex

            //Codex button in main menu and ESC menu
            return getScreenPanel()?.getChildrenCopy()?.find { it is CodexDialogAPI } as? CodexDialogAPI
        }

        if (gameState == GameState.CAMPAIGN && Global.getSector()?.campaignUI?.getActualCurrentTab() == CoreUITabId.FLEET) {
            //F2 while hovering over ship in the fleet screen. Clicking the question mark in the fleet screen. Does not include hovering over the question mark and pressing F2, that is handled differently for some reason.
            val coreUI = getCoreUI() as? UIPanelAPI

            val codex = coreUI?.getChildrenCopy()?.find { it is CodexDialogAPI } as? CodexDialogAPI
            if (codex != null) return codex
        }

        //Check for the codex that opens when clicking a ship in the title-screen missions.
        if (gameState == GameState.TITLE) {
            return getScreenPanel()?.getChildrenCopy()?.find { it is CodexDialogAPI } as? CodexDialogAPI
        }

        return null
    }
}