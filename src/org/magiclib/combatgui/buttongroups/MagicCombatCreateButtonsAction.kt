package org.magiclib.combatgui.buttongroups

/**
 * Implement this interface to create buttons for a button group
 * Consider using [MagicCombatCreateSimpleButtons] rather than creating your own action
 *
 * Example implementation:
 *
 * ```java
 *     public class CreateMyButtons implements MagicCombatCreateButtonsAction{
 *         @Override
 *         public void createButtons(MagicCombatDataButtonGroup group){
 *             group.addButton("MyButton", "button data, e.g. a String", "My tooltip", false);
 *             // repeat for additional buttons or maybe use a loop
 *         }
 *     }
 * ```
 *
 * @author Jannes
 * @since 1.3.0
 */
interface MagicCombatCreateButtonsAction {
    /**
     * Call group.addButton in this method to add a button to the group
     *
     * Example implementation:
     * ```java
     * @Override
     * public void createButtons(MagicCombatDataButtonGroup group){
     *     group.addButton("MyButton", "button data, e.g. a String", "My tooltip", false);
     *     // repeat for additional buttons or maybe use a loop
     * }
     * ```
     */
    fun createButtons(group: MagicCombatDataButtonGroup)
}