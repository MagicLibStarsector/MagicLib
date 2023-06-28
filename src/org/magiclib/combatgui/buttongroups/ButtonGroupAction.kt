package org.magiclib.combatgui.buttongroups

/**
 * implement this interface to tell a button group what action to perform
 *
 * Example implementation:
 *
 * <pre>
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
 * </pre>
 *
 * @author Jannes
 * @since 1.2.0
 */
interface ButtonGroupAction {
    /**
     * this method will get called when a button in this button group gets clicked by the user
     *
     * @param data list of data of all currently active buttons (maybe empty)
     * @param selectedButtonData data of the button that was clicked if it was selected (null if button was deselected)
     * @param deselectedButtonData data of the button that was clicked if it was deselected (null if button was selected)
     *
     * Example implementation:
     * <pre>
     * @Override
     * public void execute(@NotNull List<?> data, @Nullable Object selectedButtonData, @Nullable Object unselectedButtonData) {
     *     Global.getLogger(this.getClass()).info("A button in the button group was clicked. Button group data:");
     *     Global.getLogger(this.getClass()).info(data);
     * }
     * </pre>
     */
    fun execute(data: List<Any>, selectedButtonData: Any?, deselectedButtonData: Any? = null)

    /**
     * Override this function to perform some kind of action when a button of the group is hovered over
     *
     * Example implementation:
     * <pre>
     * @Override
     * public void onHover() {
     * }
     * </pre>
     */
    fun onHover() {}
}