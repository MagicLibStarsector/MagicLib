package org.magiclib.achievements

/**
 * How much information about an achievement is shown to the player in the Intel screen.
 * See each enum for details.
 */
enum class MagicAchievementSpoilerLevel {
    /**
     * The achievement is visible to the player at all times, and its progress is shown.
     */
    Visible,

    /**
     * Appears as an entry in the achievements list, but the name and description is hidden.
     * This is intended for achievements that the player is likely to unlock naturally, but that you don't want to spoil.
     * Avoid using this for achievements that they player would never complete without knowing what to do, as that would be frustrating.
     */
    Spoiler,

    /**
     * Not shown at all until unlocked. This is intended for achievements that the player is unlikely to unlock easily, but may unlock rarely.
     * Good for things that require luck.
     */
    Hidden
}