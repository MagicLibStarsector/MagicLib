package org.magiclib.achievements;

/**
 * An achievement where the mod spec is missing, but the achievement status is saved.
 * This happens when the player removes a mod that has achievements, or when they update a mod and the mod removes achievements.
 * The achievement's progress and spec are recorded in common, so this is loaded and displayed from there.
 * This way, switching mods doesn't delete a player's achievement progress.
 * <p>
 * Unloaded achievements can't be modified, but their state isn't lost. They're just frozen.
 */
public class MagicUnloadedAchievement extends MagicAchievement {
}
