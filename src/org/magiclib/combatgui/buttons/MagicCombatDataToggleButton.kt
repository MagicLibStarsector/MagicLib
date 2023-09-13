package org.magiclib.combatgui.buttons

/**
 * A Button that can be activated/deactivated by clicking on it.
 * Used in button groups, don't use directly unless you know what you are doing.
 *
 * @author Jannes
 * @since 1.3.0
 */
class MagicCombatDataToggleButton(
    val data: Any, info: MagicCombatButtonInfo
) : MagicCombatButtonBase(info) {
    override fun advance(): Boolean {
        if (isClicked()) {
            isActive = !isActive
            return true
        }
        return false
    }

    /**
     * Returns data set in constructor if active, null otherwise.
     */
    fun getDataIfActive(): Any? {
        return if (isActive) data else null
    }
}