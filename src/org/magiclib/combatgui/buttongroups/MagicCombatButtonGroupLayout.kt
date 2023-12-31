package org.magiclib.combatgui.buttongroups

import java.awt.Color

/**
 * Set of parameters to define positioning/size etc. of a button group.
 * Only use for custom button groups.
 * Coordinates are screen coordinates.
 * @param x position of first button
 * @param y position of first button
 * @param w width of single button
 * @param h height of single button
 * @param a opacity of button outline
 * @param color of button (when not disabled)
 * @param padding blank space between two buttons
 * @param xTooltip x-position where hover tooltip shall be displayed
 * @param yTooltip y-position where hover tooltip shall be displayed
 * @param horizontal if true, buttons will be displayed from left to right, otherwise from top to bottom
 *
 * @author Jannes
 * @since 1.3.0
 */
data class MagicCombatButtonGroupLayout(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val a: Float, val color: Color,
    val padding: Float,
    val xTooltip: Float, val yTooltip: Float,
    val horizontal: Boolean = true
)
