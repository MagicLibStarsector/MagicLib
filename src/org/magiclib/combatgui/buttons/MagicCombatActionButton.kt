package org.magiclib.combatgui.buttons

/**
 * Simple button that does something (defined by button action) when clicked
 * If possible, use [org.magiclib.combatgui.MagicCombatGuiBase.addButton] rather than using this directly
 *
 * @author Jannes
 * @since 1.3.0
 */
class MagicCombatActionButton(
    private val action: MagicCombatButtonAction? = null, info: MagicCombatButtonInfo
) : MagicCombatButtonBase(info) {
    override fun advance(): Boolean {
        if (isClicked()) {
            action?.run { execute() }
            isActive = true
            return true
        }
        isActive = false
        return false
    }
}