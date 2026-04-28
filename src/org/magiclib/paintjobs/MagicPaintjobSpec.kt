package org.magiclib.paintjobs

import java.awt.Color

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
    var engineSpec: PaintjobEngineSpec?,
    var shieldSpec: PaintjobShieldSpec?,
    var paintjobFamily: String?
) {
    private val shiny = tags?.any { it.startsWith(MagicPaintjobManager.PJTAG_SHINY) || it.startsWith(MagicPaintjobManager.PJTAG_SHINY_MEMBER) } == true
    val isShiny: Boolean
        get() = shiny

    val hasShinyFleetTag =
        tags?.any { it.startsWith(MagicPaintjobManager.PJTAG_SHINY) } == true

    val shinyFleetRarity =
        tags?.find { it.startsWith(MagicPaintjobManager.PJTAG_SHINY) }?.substringAfterLast("_")?.toIntOrNull()
            ?: MagicPaintjobShinyAdder.defaultFleetProbability

    val hasShinyMemberTag =
        tags?.any { it.startsWith(MagicPaintjobManager.PJTAG_SHINY_MEMBER) } == true

    val shinyMemberRarity =
        tags?.find { it.startsWith(MagicPaintjobManager.PJTAG_SHINY_MEMBER) }?.substringAfterLast("_")?.toIntOrNull()
            ?: MagicPaintjobShinyAdder.defaultMemberProbability

    val shinyWeight =
        tags?.find { it.startsWith(MagicPaintjobManager.PJTAG_SHINY_WEIGHT) }?.substringAfterLast("_")?.toIntOrNull()
            ?: MagicPaintjobShinyAdder.defaultPaintjobWeight

    init {
        if(hasShinyFleetTag && hasShinyMemberTag)
            throw IllegalArgumentException("Paintjob $id has both fleet and member shiny tags. Please choose one or the other.")
    }

    private val permanent = tags?.contains(MagicPaintjobManager.PJTAG_PERMA_PJ) == true || isShiny
    val isPermanent: Boolean
        get() = permanent

    private val hidden = tags?.contains(MagicPaintjobManager.PJTAG_HIDDEN) == true
    val isHidden: Boolean
        get() = hidden

    val isUnlockable = !isShiny && !isHidden

    data class PaintjobEngineSpec(
        var color: Color?,
        var contrailColor: Color?,
        var	contrailSpawnDistMult: Float?,
        var	contrailWidthMultiplier: Float?,
        var	glowAlternateColor: Color?,
        var	glowSizeMult: Float?
    )

    data class PaintjobShieldSpec(
        var innerColor: Color?,
        var ringColor: Color?,
        var innerRotationRate: Float?,
        var ringRotationRate: Float?,
    )
}

data class MagicWeaponPaintjobSpec(
    val modId: String,
    val id: String,
    val paintjobFamilies: Set<String> = setOf(),
    val weaponIds: Set<String> = setOf(),
    var spriteMap: Map<String, String>?,
)