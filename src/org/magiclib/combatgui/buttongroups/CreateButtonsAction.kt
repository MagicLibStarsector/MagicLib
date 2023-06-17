package org.magiclib.combatgui.buttongroups

/**
 * Implement this interface to create buttons for a button group
 * Consider using CreateSimpleButtons rather than creating your own action
 *
 * @author Jannes
 * @since 1.2.0
 */
interface CreateButtonsAction {
    /**
     * call group.addButton in this method to add a button to the group
     */
    fun createButtons(group: DataButtonGroup)
}