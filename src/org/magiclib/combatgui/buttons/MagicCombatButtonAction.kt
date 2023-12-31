package org.magiclib.combatgui.buttons

/**
 * Implement this and override execute to instruct buttons what they should do when clicked.
 *
 * Example implementation:
 * ```java
 * public class ExampleButtonAction implements MagicCombatButtonAction {
 *     @Override
 *     public void execute() {
 *         Global.getLogger(this.getClass()).info("Button was clicked. This message should show up in starsector.log");
 *     }
 * }
 * ```
 *
 * @author Jannes
 * @since 1.3.0
 */
interface MagicCombatButtonAction {
    /**
     * Will get executed when button is clicked.
     *
     * Example implementation:
     * ```java
     * @Override
     * public void execute() {
     *     Global.getLogger(this.getClass()).info("Button was clicked. This message should show up in starsector.log");
     * }
     * ```
     */
    fun execute()
}