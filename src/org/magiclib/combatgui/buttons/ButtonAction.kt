package org.magiclib.combatgui.buttons

/**
 * Implement this and override execute to instruct buttons what they should do when clicked
 *
 * Example implementation:
 * <pre>
 * public class ExampleButtonAction implements ButtonAction {
 *     @Override
 *     public void execute() {
 *         Global.getLogger(this.getClass()).info("Button was clicked. This message should show up in starsector.log");
 *     }
 * }
 * </pre>
 *
 * @author Jannes
 * @since 1.2.0
 */
interface ButtonAction {
    /**
     * Will get executed when button is clicked
     *
     * Example implementation:
     * <pre>
     * @Override
     * public void execute() {
     *     Global.getLogger(this.getClass()).info("Button was clicked. This message should show up in starsector.log");
     * }
     * </pre>
     */
    fun execute()
}