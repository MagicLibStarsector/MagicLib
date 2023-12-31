package org.magiclib.combatgui.buttons

/**
 * Text to display when hovering over a button and position of that text in pixels.
 *
 * @param x position to display tooltip at in screen coordinates
 * @param y position to display tooltip at in screen coordinates
 * @param txt text to display
 *
 * @author Jannes
 * @since 1.3.0
 */
data class MagicCombatHoverTooltip(val x: Float, val y: Float, var txt: String)