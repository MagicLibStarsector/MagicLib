package org.magiclib.activators

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicUI

class ActivatorCombatPlugin : BaseEveryFrameCombatPlugin() {
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        advanceActivators(amount)
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {
        viewport?.let {
            drawActivatorsUI(it)
        }
    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {
        viewport?.let {
            drawActivatorsWorld(it)
        }
    }


    /**
     * Called automatically during combat by [ActivatorCombatPlugin].
     */
    fun advanceActivators(amount: Float) {
        val combatEngine = Global.getCombatEngine() ?: return
        for (ship in combatEngine.ships) {
            ActivatorManager.getActivatorsForShipCopy(ship)?.forEach {
                if (!combatEngine.isPaused) {
                    it.advanceInternal(amount * ship.mutableStats.timeMult.modifiedValue)
                }

                it.advanceEveryFrame()
            }
        }
    }

    /**
     * Called automatically during combat by [ActivatorCombatPlugin].
     */
    fun drawActivatorsUI(viewport: ViewportAPI) {
        val combatEngine = Global.getCombatEngine() ?: return

        if (combatEngine.combatUI == null || combatEngine.combatUI.isShowingCommandUI || combatEngine.combatUI.isShowingDeploymentDialog || !combatEngine.isUIShowingHUD) {
            return
        }

        combatEngine.playerShip?.let { ship ->
            ActivatorManager.getActivatorsForShipCopy(ship)?.let {
                var lastVec = MagicUI.getHUDRightOffset(ship)
                for (activator in it) {
                    activator.drawHUDBar(viewport, lastVec)
                    lastVec = Vector2f.add(lastVec, Vector2f(0f, 22f), null)
                }
            }
        }
    }

    /**
     * Called automatically during combat by [ActivatorCombatPlugin].
     */
    fun drawActivatorsWorld(viewport: ViewportAPI) {
        for (ship in Global.getCombatEngine()?.ships.orEmpty()) {
            ActivatorManager.getActivatorsForShipCopy(ship)?.forEach {
                it.renderWorld(viewport)
            }
        }
    }
}