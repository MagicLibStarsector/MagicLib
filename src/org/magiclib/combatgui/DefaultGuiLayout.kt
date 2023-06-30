package org.magiclib.combatgui

import java.awt.Color

/**
 * Best guess GUI layout, feel free to pass this to [GuiBase] to get started quickly.
 * In the long term, you probably want to create your own [GuiLayout].
 *
 * @author Jannes
 * @since 1.2.0
 */
val defaultGuiLayout = GuiLayout(
    0.05f, 0.8f, 100f, 20f, 0.5f, Color.WHITE,
    5f, 0.4f, 0.2f, 25f, "graphics/fonts/insignia15LTaa.fnt", 0.4f, 0.4f
)