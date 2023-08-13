package org.magiclib.achievements

data class MagicAchievement(
    val modId: String,
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val `class`: String,
    val image: String?,
    val hasProgressBar: Boolean,
    val spoilerLevel: SpoilerLevel,
)
enum class SpoilerLevel {
    Visible,
    Spoilered,
    Hidden
}