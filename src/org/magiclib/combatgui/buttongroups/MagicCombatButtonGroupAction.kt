package org.magiclib.combatgui.buttongroups

/**
 * implement this interface to tell a button group what action to perform
 *
 * Example implementation:
 *
 * ```java
 * public class ExampleButtonGroupAction implements ButtonGroupAction {
 *     @Override
 *     public void execute(@NotNull List<?> data, @Nullable Object selectedButtonData, @Nullable Object unselectedButtonData) {
 *         Global.getLogger(this.getClass()).info("A button in the button group was clicked. Button group data:");
 *         Global.getLogger(this.getClass()).info(data);
 *     }
 *
 *     @Override
 *     public void onHover() {
 *     }
 * }
 * ```
 *
 * @author Jannes
 * @since 1.3.0
 */
interface MagicCombatButtonGroupAction {
    /**
     * This method will get called when a button in this button group gets clicked by the user
     *
     * @param data list of data of all currently active buttons (maybe empty)
     * @param selectedButtonData data of the button that was clicked if it was selected (null if button was deselected)
     * @param deselectedButtonData data of the button that was clicked if it was deselected (null if button was selected)
     *
     * Example implementation:
     * ```java
     * @Override
     * public void execute(@NotNull List<?> data, @Nullable Object selectedButtonData, @Nullable Object unselectedButtonData) {
     *     Global.getLogger(this.getClass()).info("A button in the button group was clicked. Button group data:");
     *     Global.getLogger(this.getClass()).info(data);
     * }
     * ```
     */
    fun execute(data: List<Any>, selectedButtonData: Any?, deselectedButtonData: Any? = null)

    /**
     * Override this function to perform some kind of action when a button of the group is hovered over
     *
     * Example implementation:
     * ```java
     * @Override
     * public void onHover() {
     *   Global.getLogger(this.getClass()).info("A button in the button group was hovered over");
     * }
     * ```
     */
    fun onHover() {}
}