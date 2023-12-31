package org.magiclib.combatgui.buttongroups

/**
 * defines an action to be performed whenever anything happens
 * Use this to e.g. disable buttons that are incompatible with other active buttons
 *
 * @author Jannes
 * @since 1.3.0
 */
interface MagicCombatRefreshButtonsAction {
    /**
     * will get called every frame
     * @param group to perform actions on. Use group.getActiveButtonData() and group.buttons to interact with buttons
     */
    fun refreshButtons(group: MagicCombatDataButtonGroup)
}