package org.magiclib.util.internal

/**
 * The game's OP cost code (`HullVariantSpec.updateStatsForOpCosts`) loops over a ship's set of hullmods and calls
 * `applyEffectsBeforeShipCreation` on each one that affects OP costs. Adding or removing a hullmod during that
 * loop crashes the game. Code that changes hullmods *from inside a hullmod effect* should check [isRunning] first.
 *
 * Fixes https://github.com/MagicLibStarsector/MagicLib/issues/42.
 */
object OpCostLoop {
    private const val HULL_VARIANT_SPEC = "com.fs.starfarer.loading.specs.HullVariantSpec"
    private const val OP_COST_METHOD = "updateStatsForOpCosts"

    /**
     * True when called from inside the game's OP cost loop.
     */
    @JvmStatic
    fun isRunning(): Boolean = StackWalker.getInstance().walk { frames ->
        frames.anyMatch { it.className == HULL_VARIANT_SPEC && it.methodName == OP_COST_METHOD }
    }
}
