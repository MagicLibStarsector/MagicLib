package data.scripts.util

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
            MagicUI.drawStatusBarMap()
        }
    }
}