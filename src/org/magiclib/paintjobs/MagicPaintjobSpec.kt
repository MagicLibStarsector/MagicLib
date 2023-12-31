package org.magiclib.paintjobs

data class MagicPaintjobSpec @JvmOverloads constructor(
    val modId: String,
    val modName: String,
    val id: String,
    @Deprecated("Use hullIds instead")
    val hullId: String,
    val hullIds: List<String> = listOf(hullId),
    var name: String,
    var unlockConditions: String? = null,
    var description: String? = null,
    var unlockedAutomatically: Boolean = true,
    var spriteId: String,
    var tags: List<String>?,
) {
    val isShiny: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_SHINY) == true

    val isPermanent: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_PERMA_PJ) == true || isShiny

    val isUnlockable = !isShiny
}