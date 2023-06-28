package org.magiclib.combatgui.buttongroups

/**
 * Implement this interface to create buttons for a button group
 * Consider using CreateSimpleButtons rather than creating your own action
 *
 * Example implementation:
 *
 * <pre>
 *     public class CreateMyButtons implements CreateButtonsAction{
 *         @Override
 *         public void createButtons(DataButtonGroup group){
 *             group.addButton("MyButton", "button data, e.g. a String", "My tooltip", false);
 *             // repeat for additional buttons or maybe use a loop
 *         }
 *     }
 * </pre>
 *
 * @author Jannes
 * @since 1.2.0
 */
interface CreateButtonsAction {
    /**
     * call group.addButton in this method to add a button to the group
     *
     * Example implementation:
     * <pre>
     * @Override
     * public void createButtons(DataButtonGroup group){
     *     group.addButton("MyButton", "button data, e.g. a String", "My tooltip", false);
     *     // repeat for additional buttons or maybe use a loop
     * }
     * </pre>
     */
    fun createButtons(group: DataButtonGroup)
}