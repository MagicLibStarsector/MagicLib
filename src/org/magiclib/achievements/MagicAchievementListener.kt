package org.magiclib.achievements

import com.fs.starfarer.api.characters.PersonAPI

/**
 * Called when any achievement is completed.
 *
 * Add with [MagicAchievementManager.addAchievementListener] from your mod plugin's `onApplicationLoad`.
 *
 * @since 1.6.0
 */
fun interface MagicAchievementListener {
    /**
     * @param achievement The completed achievement.
     * @param completedByPlayer The player that completed it, if there is one.
     */
    fun onAchievementCompleted(achievement: MagicAchievement, completedByPlayer: PersonAPI?)
}
