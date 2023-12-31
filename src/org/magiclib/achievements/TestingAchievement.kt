package org.magiclib.achievements

import com.fs.starfarer.api.Global
import org.magiclib.util.MagicMisc
import org.magiclib.util.MagicVariables


/**
 * The part that would normally be in the csv, but in code to hide it a bit.
 */
internal class TestingAchievementSpec : MagicAchievementSpec(
    modId = MagicVariables.MAGICLIB_ID,
    modName = Global.getSettings().modManager.getModSpec(MagicVariables.MAGICLIB_ID).name,
    id = "testing",
    name = "Testing",
    description = "MagicLib test that also has a very long description to check text wrapping and make sure it doesn't overlap or cause any other problems. It certainly shouldn't go to three lines! Definitely not, the full thing should only appear in the tooltip.",
    tooltip = null,
    script = TestingAchievement::class.java.name,
    image = null,
    spoilerLevel = MagicAchievementSpoilerLevel.Visible,
    rarity = MagicAchievementRarity.Common
)

/**
 * The logic for the achievement, which is always in code.
 */
internal class TestingAchievement : MagicTargetListAchievement() {
    override fun onSaveGameLoaded(isComplete: Boolean) {
        super.onSaveGameLoaded(isComplete)

        if (isComplete) return

        setTargets(
            mapOf(
                "1" to "One day passed",
                "2" to "Two days passed",
                "3" to "Three days passed",
            )
        )
        saveChanges()
    }

    override fun advanceAfterInterval(amount: Float) {
        super.advanceAfterInterval(amount)

        if (MagicMisc.getElapsedDaysSinceGameStart() >= 1f) {
            setTargetComplete("1")
            saveChanges()
        }

        if (MagicMisc.getElapsedDaysSinceGameStart() >= 2f) {
            setTargetComplete("2")
            saveChanges()
        }

        if (MagicMisc.getElapsedDaysSinceGameStart() >= 3f) {
            setTargetComplete("3")
            saveChanges()
        }
    }
}