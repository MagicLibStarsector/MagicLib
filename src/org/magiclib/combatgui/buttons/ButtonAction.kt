package org.magiclib.combatgui.buttons

/**
 * Implement this and override execute to instruct buttons what they should do when clicked
 *
 * @author Jannes
 * @since 1.2.0
 */
interface ButtonAction {
    /**
     * Will get executed when button is clicked
     */
    fun execute()
}