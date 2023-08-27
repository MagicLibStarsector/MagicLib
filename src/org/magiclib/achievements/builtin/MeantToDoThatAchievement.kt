package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.achievements.MagicAchievement

class MeantToDoThatAchievement : MagicAchievement() {
    private val _shipIdToListener: MutableMap<String, DamageDetectorListener> = mutableMapOf()

    override fun advanceInCombat(
        amount: Float,
        events: MutableList<InputEventAPI>?,
        isSimulation: Boolean
    ) {
        if (isComplete || isSimulation) return

        val ships = Global.getCombatEngine().ships
        applyShipListeners(ships)
    }

    internal inner class DamageDetectorListener : DamageListener {
        override fun reportDamageApplied(source: Any?, target: CombatEntityAPI?, result: ApplyDamageResultAPI?) {
            val combatEngine = Global.getCombatEngine()

            if (Global.getSettings().isDevMode || source == null || result == null) {
                return
            }

            if (target !is ShipAPI || source !is ShipAPI || source.id != combatEngine.playerShip.id) {
                return
            }

            if (result.damageToHull <= 0f || target.isAlive || source.isAlive) {
                return
            }

            if (result.isDps) {
                return
            }

            // Always seems to be high explosive type, but just in case, allow any damage type.

            Global.getLogger(MeantToDoThatAchievement::class.java)
                .info("Damage (${result.damageToHull} ${result.type.name} to hull) applied to ${target.id} by ${source.id}, with both dead.")
            completeAchievement()
            saveChanges()

            for (ship in Global.getCombatEngine().ships) {
                ship.removeListenerOfClass(DamageDetectorListener::class.java)
            }
        }
    }

    private fun applyShipListeners(ships: List<ShipAPI>) {
        // ensure all alive ships have a listener to watch for damage
        for (ship in ships) {
            if (!ship.isAlive) {
                // Remove listener if ship is dead.
                if (_shipIdToListener.containsKey(ship.id)) {
                    ship.removeListenerOfClass(DamageDetectorListener::class.java)
                    _shipIdToListener.remove(ship.id)
                }
            } else if (!_shipIdToListener.containsKey(ship.id)) {
                // Add listener if ship is alive and doesn't have one.
                ship.removeListenerOfClass(DamageDetectorListener::class.java)
                val listener = DamageDetectorListener()
                ship.addListener(listener)
                _shipIdToListener[ship.id] = listener
            }
        }
    }
}
