package org.magiclib.achievements

import com.fs.starfarer.api.EveryFrameScript

internal class MagicAchievementRunner : EveryFrameScript {

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        MagicAchievementManager.getInstance().achievements.values.forEach { achievement ->
            if (!achievement.isComplete) {
                achievement.advanceInternal(amount)
            }
        }
    }
}