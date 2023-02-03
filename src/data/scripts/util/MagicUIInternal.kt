package data.scripts.util

import data.scripts.util.MagicUI

class MagicUIInternal {
    companion object {
        @JvmStatic
        internal fun callRenderMethods() {
            MagicUI.drawStatusBarMap()
        }
    }
}