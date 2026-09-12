package org.magiclib.util.internal

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ArmorGridAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import java.awt.Point
import kotlin.math.*

internal object MiscellaneousUtil {
    internal fun damageAfterArmor(
        damageType: DamageType,
        damage: Float,
        hitStrength: Float,
        armorValue: Float,
        ship: ShipAPI
    ): Pair<Float, Float> {
        val stats: MutableShipStatsAPI = ship.mutableStats

        // Get relevant stats modifiers
        var armorMultiplier = stats.armorDamageTakenMult.modifiedValue // Use var as it changes
        val effectiveArmorMult = stats.effectiveArmorBonus.bonusMult
        var hullMultiplier = stats.hullDamageTakenMult.modifiedValue // Use var as it changes
        val minArmor = stats.minArmorFraction.modifiedValue
        val maxDR = stats.maxArmorDamageReduction.modifiedValue

        // Adjust multipliers based on damage type
        when (damageType) {
            DamageType.FRAGMENTATION -> {
                armorMultiplier *= (0.25f * stats.fragmentationDamageTakenMult.modifiedValue)
                hullMultiplier *= stats.fragmentationDamageTakenMult.modifiedValue
            }
            DamageType.KINETIC -> {
                armorMultiplier *= (0.5f * stats.kineticDamageTakenMult.modifiedValue)
                hullMultiplier *= stats.kineticDamageTakenMult.modifiedValue
            }
            DamageType.HIGH_EXPLOSIVE -> {
                armorMultiplier *= (2f * stats.highExplosiveDamageTakenMult.modifiedValue)
                hullMultiplier *= stats.highExplosiveDamageTakenMult.modifiedValue
            }
            DamageType.ENERGY -> {
                armorMultiplier *= stats.energyDamageTakenMult.modifiedValue
                hullMultiplier *= stats.energyDamageTakenMult.modifiedValue
            }
            DamageType.OTHER -> {}
        }

        // Calculate damage reduction factor
        val effectiveArmor = max(minArmor * ship.armorGrid.armorRating, armorValue) * effectiveArmorMult
        val damageReductionFactor = (hitStrength * armorMultiplier) / (effectiveArmor + hitStrength * armorMultiplier)
        val armorDR = max(1f - maxDR, damageReductionFactor)

        // Calculate damage actually applied after reduction
        val effectiveDamage = damage * armorDR

        // Calculate damage dealt to the armor layer
        val armorDamage = effectiveDamage * armorMultiplier

        // Calculate damage penetrating to the hull
        var hullDamage = 0f
        if (armorDamage > armorValue) {
            // Hull damage is the portion of effective damage corresponding to the excess armor damage
            hullDamage = ((armorDamage - armorValue) / armorDamage) * effectiveDamage * hullMultiplier
        }

        // Return the pair of damages
        return Pair(armorDamage, hullDamage)
    }


    /**
     * Finds the coordinates of a candidate cell likely to have the lowest effective armor.
     * Uses a fast scan based on the minimum unweighted sum in a 5x5 neighborhood.
     * Only considers center cells where the 5x5 kernel fits entirely within the grid.
     *
     * @receiver armorGrid The ArmorGridAPI instance for the ship.
     * @return A Point(x, y) representing the coordinates of the candidate cell,
     *         or null if the grid is smaller than 5x5 or otherwise invalid.
     */
    internal fun ArmorGridAPI.weakestArmorRegion(): Point? {
        val grid: Array<FloatArray> = this.grid ?: return null
        val width = this.leftOf + this.rightOf
        val height = this.below + this.above

        // Grid must be at least 5x5 to contain the kernel
        if (width < 5 || height < 5) {
            return null
        }

        // Fast Scan using Integral Image
        val integralSum = buildIntegralImage(grid, width, height, 1.0f)

        var minSum5x5 = Float.MAX_VALUE
        var candidateX = -1
        var candidateY = -1

        // Iterate only through centers where the 5x5 kernel fits entirely
        for (cx in 2..<width - 2) {
            for (cy in 2..<height - 2) {
                val x5_min = cx - 2
                val y5_min = cy - 2
                val x5_max = cx + 2
                val y5_max = cy + 2

                // Get the unweighted 5x5 sum efficiently
                val currentSum5x5 = getRegionSum(integralSum, x5_min, y5_min, x5_max, y5_max, width, height)

                if (currentSum5x5 < minSum5x5) {
                    minSum5x5 = currentSum5x5
                    candidateX = cx
                    candidateY = cy
                }
            }
        }
        return if (minSum5x5 == Float.MAX_VALUE) null else Point(candidateX, candidateY)
    }

    /**
     * Calculates the accurate effective armor rating for a specific cell,
     * based on the Starsector weighted sum rule, using the precomputed WEIGHT_MASK.
     * Handles boundary conditions correctly (cells outside the grid contribute 0).
     *
     * @receiver The ArmorGridAPI instance for the ship.
     * @param point A Point(x, y) representing the coordinates of the center cell for the calculation.
     * @return The calculated effective armor rating (Float). Returns null if the grid is invalid.
     */
    internal fun ArmorGridAPI.armorAtCell(point: Point): Float? {
        val grid: Array<FloatArray> = this.grid ?: return 0.0f
        val width = this.leftOf + this.rightOf
        val height = this.below + this.above

        if (width <= 0 || height <= 0) return 0.0f

        var effectiveArmor: Float? = null

        // Perform the accurate weighted sum, checking boundaries for each neighbor
        for (relX in -2..2) {
            for (relY in -2..2) {
                val nx = point.x + relX
                val ny = point.y + relY
                // Check if the neighbor cell (nx, ny) is within the grid bounds
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    // Use the precomputed top-level weight mask
                    val weight = ARMOR_MASK[relX + 2][relY + 2]
                    if (effectiveArmor == null) effectiveArmor = 0f
                    if (weight > 0f) effectiveArmor += grid[nx][ny] * weight
                }
            }
        }

        return effectiveArmor
    }


    internal fun Float.isCloseTo(other: Float, epsilon: Float): Boolean {
        if (this.isNaN() || other.isNaN()) return false
        if (this.isInfinite() || other.isInfinite()) return this == other
        return abs(this - other) <= epsilon
    }

    /** Interpolates colors via the LMS space of OkLab.
     *
     *  Produces consistent perceived lightness and consistent hue path during interpolation compared to normal srbg interpolation.
     **/
    internal fun interpolateColorNicely(from: Color, to: Color, progress: Float): Color {
        val fromLMS = from.toLMS()
        val toLMS = to.toLMS()
        val targetLMS = LMS(
            L = Misc.interpolate(fromLMS.L, toLMS.L, progress),
            M = Misc.interpolate(fromLMS.M, toLMS.M, progress),
            S = Misc.interpolate(fromLMS.S, toLMS.S, progress)
        )
        val targetAlpha = Misc.interpolate(from.alpha.toFloat(), to.alpha.toFloat(), progress).roundToInt()
        return targetLMS.toColor(targetAlpha)
    }

    /**
     * Precomputed 5x5 weight mask based on Starsector armor rules.
     * Indices [0..4][0..4] correspond to relative offsets [-2..2][-2..2].
     * - Inner 3x3 cells (including center): weight 1.0
     * - Outer 12 adjacent cells: weight 0.5
     * - Corner 4 cells: weight 0.0
     */
    private val ARMOR_MASK: Array<FloatArray> = run {
        val mask = Array(5) { FloatArray(5) }
        for (relX in -2..2) {
            for (relY in -2..2) {
                val dx = abs(relX)
                val dy = abs(relY)
                mask[relX + 2][relY + 2] = when {
                    dx <= 1 && dy <= 1 -> 1.0f // Inner 3x3
                    (dx == 2 && dy <= 1) || (dy == 2 && dx <= 1) -> 0.5f // Outer 12
                    else -> 0.0f // Corners
                }
            }
        }
        mask // Return the computed mask
    }

    /**
     * Builds an integral image (summed-area table) for efficient region sum queries.
     */
    private fun buildIntegralImage(
        grid: Array<FloatArray>,
        width: Int,
        height: Int,
        scale: Float = 1.0f
    ): Array<FloatArray> {
        val integral = Array(width + 1) { FloatArray(height + 1) } // Padded
        for (x in 0..<width) {
            var rowSum = 0f
            for (y in 0..<height) {
                rowSum += grid[x][y] * scale
                integral[x + 1][y + 1] = integral[x][y + 1] + rowSum
            }
        }
        return integral
    }

    /**
     * Calculates the sum of values within a rectangular region using a precomputed integral image.
     * Handles boundary clamping automatically.
     */
    private fun getRegionSum(
        integral: Array<FloatArray>,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        width: Int,
        height: Int
    ): Float {
        val minX = max(0, x1)
        val minY = max(0, y1)
        val maxX = min(width - 1, x2)
        val maxY = min(height - 1, y2)
        if (minX > maxX || minY > maxY) return 0f
        val iMaxX = maxX + 1
        val iMaxY = maxY + 1
        return integral[iMaxX][iMaxY] - integral[minX][iMaxY] - integral[iMaxX][minY] + integral[minX][minY]
    }

    private data class LMS(val L: Float, val M: Float, val S: Float)

    // sRGB <-> Linear sRGB Conversion
    private fun srgbToLinearSrgb(x: Float): Float {
        return if (x >= 0.04045f) ((x + 0.055f) / 1.055f).pow(2.4f) else (x / 12.92f)
    }

    private fun linearSrgbToSrgb(x: Float): Float {
        return if (x >= 0.0031308f) (1.055f * x.pow(1.0f / 2.4f) - 0.055f) else (12.92f * x)
    }

    // Linear sRGB Conversion <-> LMS of OkLab
    private fun Color.toLMS(): LMS {
        val linearRed = srgbToLinearSrgb(red / 255f)
        val linearGreen = srgbToLinearSrgb(green / 255f)
        val linearBlue = srgbToLinearSrgb(blue / 255f)

        return LMS(
            L = cbrt(0.4122214708f * linearRed + 0.5363325363f * linearGreen + 0.0514459929f * linearBlue),
            M = cbrt(0.2119034982f * linearRed + 0.6806995451f * linearGreen + 0.1073969566f * linearBlue),
            S = cbrt(0.0883024619f * linearRed + 0.2817188376f * linearGreen + 0.6299787005f * linearBlue)
        )
    }

    private fun LMS.toColor(alpha: Int? = null): Color {
        val cubedL = L.pow(3)
        val cubedM = M.pow(3)
        val cubedS = S.pow(3)
        return Color(
            linearSrgbToSrgb(+4.0767416621f * cubedL - 3.3077115913f * cubedM + 0.2309699292f * cubedS).coerceIn(0f, 1f),
            linearSrgbToSrgb(-1.2684380046f * cubedL + 2.6097574011f * cubedM - 0.3413193965f * cubedS).coerceIn(0f, 1f),
            linearSrgbToSrgb(-0.0041960863f * cubedL - 0.7034186147f * cubedM + 1.7076147010f * cubedS).coerceIn(0f, 1f),
            ((alpha ?: 255) / 255f).coerceIn(0f, 1f)
        )
    }

    /**
     * This class exists purely to encapsulate the deprecation of `Class.newInstance()`,
     * which is used in the game code.
     *
     * While `Class.getDeclaredConstructor().newInstance()` should be used instead, it causes a file access / reflection exception and crashes the game.
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    internal fun newInstance(inputClass: Class<*>): Any {
        return inputClass.newInstance()
    }
}