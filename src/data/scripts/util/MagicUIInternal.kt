package data.scripts.util

import org.lwjgl.opengl.GLContext

/**
 * @since 0.46.0
 */
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