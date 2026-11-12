package org.magiclib.util.reflection.boxed

import com.fs.starfarer.api.impl.codex.CodexDialogAPI
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.codex2.CodexDetailPanel
import org.magiclib.ReflectionUtilsSafe.safeGet
import org.magiclib.util.reflection.UIFinder.getCodexDialog

/**
 * A boxed version of the codex dialog, providing access to the codex dialog and its children's methods via reflection.
 */
class BoxedCodexDialog private constructor(override val target: UIPanelAPI) : BoxedUIElement(target),
    UIPanelAPI by target,
    CodexDialogAPI by target as CodexDialogAPI {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun get(codexDialog: CodexDialogAPI? = getCodexDialog()): BoxedCodexDialog? {
            return codexDialog?.let { BoxedCodexDialog(it as UIPanelAPI) }
        }

        private const val FIELD_PLUGIN = "plugin"
        private const val METHOD_GET_PARAM = "getParam"
    }

    /**
     * Gets the detail panel currently attached to this dialog if any.
     *
     * i.e. the right-hand panel that renders the details of whichever codex entry is selected.
     */
    fun getDetailPanel(): UIPanelAPI? {
        return target.safeGet(type = CodexDetailPanel::class.java) as? UIPanelAPI
    }

    /**
     * Gets the [CodexEntryPlugin] currently shown in the detail panel if any.
     */
    fun getEntryPlugin(): CodexEntryPlugin? {
        val codexDetailPanel = getDetailPanel() ?: return null
        val codexEntry = codexDetailPanel.safeGet(name = FIELD_PLUGIN) as? CodexEntryPlugin ?: return null

        return codexEntry
    }

    /**
     * Gets the `param` object of the codex entry currently shown in the detail panel if any,
     *
     * e.g. a `FleetMemberAPI`, `ShipHullSpecAPI`, `HullModSpecAPI`, `WeaponSpecAPI`, `SpecialItemSpecAPI`, etc.
     */
    fun getEntryParam(): Any? {
        return getEntryPlugin()?.param
        // Note: as of 0.98a, .param2 is always a FleetMemberAPI used for rendering purposes.
        // .param2 is only used when viewing ShipHullSpecAPI's of moduled ships in the codex, a FleetMemberAPI will be generated and put in .param2 to render.
    }
}