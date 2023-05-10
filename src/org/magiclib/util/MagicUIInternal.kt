package org.magiclib.util

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
            MagicUI.drawStatusBarMap()
        }
    }
}