package org.magiclib.combatgui.buttongroups

/**
 * Simple implementation of [MagicCombatCreateButtonsAction] interface that creates
 * a button for each entry in names.
 * @param names list of display names of buttons. Must not be null and defines the number of buttons created
 * @param data list of data that buttons shall contain. If null or too short, button names will be used as data.
 * @param tooltips list of tooltips to use for buttons. If null or too short, no tooltip will be used.
 *
 * @author Jannes
 * @since 1.3.0
 */
class MagicCombatCreateSimpleButtons(
    private val names: List<String>,
    private val data: List<Any>?,
    private val tooltips: List<String>?
) : MagicCombatCreateButtonsAction {
    override fun createButtons(group: MagicCombatDataButtonGroup) {
        names.forEachIndexed { index, s ->
            val d = data?.getOrNull(index) ?: s
            val tt = tooltips?.getOrNull(index) ?: ""
            group.addButton(s, d, tt, false)
        }
    }
}