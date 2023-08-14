package org.magiclib.achievements

data class MagicAchievementSpec(
    val modId: String,
    val id: String,
    var name: String,
    var description: String,
    var script: String,
    var image: String?,
    var hasProgressBar: Boolean,
    var spoilerLevel: SpoilerLevel,
) {
    fun clone() = this.copy()
}