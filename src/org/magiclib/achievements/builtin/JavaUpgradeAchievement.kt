package org.magiclib.achievements.builtin

import org.magiclib.achievements.MagicAchievement

class JavaUpgradeAchievement : MagicAchievement() {
    override fun onSaveGameLoaded() {
        if (!System.getProperty("java.runtime.version").contains("1.7.0", ignoreCase = true)) {
            completeAchievement()
            saveChanges()
        }
    }
}