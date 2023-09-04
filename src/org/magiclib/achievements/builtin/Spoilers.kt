package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.listeners.SurveyPlanetListener
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

/**
 * The part that would normally be in the csv, but in code to hide it better from prying eyes.
 */
internal class OldEarthAchievementSpec : MagicAchievementSpec(
    MagicVariables.MAGICLIB_ID,
    Global.getSettings().modManager.getModSpec(MagicVariables.MAGICLIB_ID).name,
    "oldearth",
    "Old Earth",
    "Surveyed a planet with 50% hazard or better.",
    null,
    "org.magiclib.achievements.builtin.OldEarthAchievement",
    null,
    false,
    MagicAchievementSpoilerLevel.Hidden,
    MagicAchievementRarity.Legendary
)

internal class OldEarthAchievement : MagicAchievement(), SurveyPlanetListener {
    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()
        Global.getSector().listenerManager.addListener(this, true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun reportPlayerSurveyedPlanet(planet: PlanetAPI?) {
        if ((planet?.market?.hazardValue ?: 0f) <= 0.50f) {
            completeAchievement()
            saveChanges()
        }
    }
}