package org.magiclib.util

import org.lwjgl.opengl.GLContext

/**
 * Do not use.
 * @since 0.46.0
 */
class MagicUIInternal {
    companion object {
        /**
         * @since 0.46.0
         */
        @JvmStatic
        internal fun callRenderMethods() {
            if (GLContext.getCapabilities().OpenGL15) {
                MagicUI.drawStatusBarMap()
            }
        }
    }
}