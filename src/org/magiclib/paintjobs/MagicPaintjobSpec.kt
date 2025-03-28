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
    val isShiny: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_SHINY) == true

    val isPermanent: Boolean
        get() = tags?.contains(MagicPaintjobManager.PJTAG_PERMA_PJ) == true || isShiny

    val isUnlockable = !isShiny

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