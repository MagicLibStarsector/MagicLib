package data.scripts.util

import org.lwjgl.opengl.GLContext

/**
 * @since 0.46.0
 */
@Suppress("DEPRECATION")
@Deprecated("Switch to org.magiclib")
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