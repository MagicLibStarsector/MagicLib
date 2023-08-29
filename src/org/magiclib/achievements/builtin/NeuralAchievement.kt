package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.achievements.MagicAchievement
import org.magiclib.kotlin.isAutomated

/**
 * Used Neural Link to control an automated ship.
 */
class NeuralAchievement : MagicAchievement() {
    override fun advanceInCombat(amount: Float, events: MutableList<InputEventAPI>?, isSimulation: Boolean) {
        super.advanceInCombat(amount, events, isSimulation)
        val engine = Global.getCombatEngine() ?: return
        if (isSimulation || engine.isPaused) return

        val shipsPilotedByPlayer = engine.ships
            .filter { it.captain?.isPlayer == true && it.isAlive && !it.isFighter }

        if (shipsPilotedByPlayer.count() > 1 && shipsPilotedByPlayer.any { it.isAutomated() }) {
            completeAchievement()
            Global.getLogger(this::class.java)
                .info("NeuralAchievement completed when piloting ${shipsPilotedByPlayer.joinToString { it.hullSpec.hullName }}.")
            saveChanges()
        }
    }
}
