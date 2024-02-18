package org.magiclib.activators

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI

class ActivatorCombatPlugin : BaseEveryFrameCombatPlugin() {
    override fun advance(amount: Float, events: List<InputEventAPI>) {
        ActivatorManager.advanceActivators(amount)
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {
        viewport?.let {
            ActivatorManager.drawActivatorsUI(it)
        }
    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {
        viewport?.let {
            ActivatorManager.drawActivatorsWorld(it)
        }
    }
}