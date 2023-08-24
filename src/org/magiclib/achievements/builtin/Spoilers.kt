package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import org.magiclib.achievements.MagicAchievement
import org.magiclib.achievements.MagicAchievementRarity
import org.magiclib.achievements.MagicAchievementSpec
import org.magiclib.achievements.MagicAchievementSpoilerLevel
import org.magiclib.util.MagicMisc
import org.magiclib.util.MagicVariables

internal object Spoilers {
    @JvmStatic
    fun getSpoilerAchievementSpecs(): List<MagicAchievementSpec> {
        return listOf(
            OverInvestedAchievementSpec()
        )
    }
}

/**
 * The part that would normally be in the csv, but in code to hide it better from prying eyes.
 */
internal class OverInvestedAchievementSpec : MagicAchievementSpec(
    MagicVariables.MAGICLIB_ID,
    Global.getSettings().modManager.getModSpec(MagicVariables.MAGICLIB_ID).name,
    "overinvested",
    "Overinvested",
    "Played the same save for 30 cycles.",
    null,
    "org.magiclib.achievements.builtin.OverInvestedAchievement",
    null,
    false,
    MagicAchievementSpoilerLevel.Hidden,
    MagicAchievementRarity.Epic
)

/**
 * The logic for the achievement, which is always in code.
 */
internal class OverInvestedAchievement : MagicAchievement() {
    override fun advanceAfterInterval(amount: Float) {
        if (MagicMisc.getElapsedDaysSinceGameStart() > (365 * 30)) {
            completeAchievement()
            saveChanges()
        }
    }
}