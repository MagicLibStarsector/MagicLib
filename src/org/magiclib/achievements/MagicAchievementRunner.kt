package org.magiclib.achievements

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.lazywizard.lazylib.ext.logging.w

internal class MagicAchievementRunner : EveryFrameScript {
    val logger: Logger = Global.getLogger(MagicAchievementRunner::class.java)

    val disabledAchievementScripts = mutableSetOf<String>()

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        MagicAchievementManager.getInstance().achievements.values
            .filter { it.specId !in disabledAchievementScripts }
            .forEach { achievement ->
                try {
                    if (!achievement.isComplete) {
                        achievement.advanceInternal(amount)
                    }
                } catch (e: Exception) {
                    val errorStr =
                        "Error running achievement '${achievement.specId}' from mod '${achievement.modName}'!"
                    logger.w(
                        ex = e,
                        message = { errorStr })
                    disabledAchievementScripts.add(achievement.specId)
                    achievement.errorMessage = errorStr

                    // If dev mode, crash.
                    if (Global.getSettings().isDevMode) {
                        throw e
                    }
                }
            }
    }
}