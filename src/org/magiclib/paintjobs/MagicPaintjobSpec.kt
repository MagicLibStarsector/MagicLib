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
    var ventsSpec: PaintjobVentsSpec?,
    var paintjobFamily: String?
) {
    private var shiny = tags?.contains(MagicPaintjobManager.PJTAG_SHINY) == true
    /**
     * Override if this paintjob is shiny or not with the input value.
     *
     * Loses effect on game restart
     */
    fun setShiny(value: Boolean) {
        shiny = value
    }
    val isShiny: Boolean
        get() = shiny

    private var permanent = tags?.contains(MagicPaintjobManager.PJTAG_PERMA_PJ) == true || isShiny
    /**
     * Override if this paintjob is permanent or not with the input value.
     *
     * Loses effect on game restart
     */
    fun setPermanent(value: Boolean) {
        permanent = value
    }

    val isPermanent: Boolean
        get() = permanent

    private val hidden = tags?.contains(MagicPaintjobManager.PJTAG_HIDDEN) == true
    val isHidden: Boolean
        get() = hidden

    var _isUnlockable = false
    /**
     * Make this paintjob unlockable regardless of if this is not normally unlockable.
     *
     * Loses effect on game restart
     */
    fun setIsUnlockable(value: Boolean) {
        _isUnlockable = value
    }
    val isUnlockable = _isUnlockable || (!isShiny && !isHidden)

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

    data class PaintjobVentsSpec(
        var ventCoreColor: Color?,
        var ventFringeColor: Color?,
    )
}

data class MagicWeaponPaintjobSpec(
    val modId: String,
    val id: String,
    val paintjobFamilies: Set<String> = setOf(),
    val weaponIds: Set<String> = setOf(),
    var spriteMap: Map<String, String>?,
)