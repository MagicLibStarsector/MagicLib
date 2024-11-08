package org.magiclib.achievements

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import org.magiclib.paintjobs.MagicPaintjobManager

/**
 * Abstract class for achievements that require the player to kill a certain number of ships.
 *
 * Usage:
 *
 * ```java
 * public class MyAchievement extends ShipKillsAchievement {
 *    public MyAchievement() {
 *     super(Collections.singletonList("onslaught"), 25, Collections.singletonList("paintjobId"));
 *    }
 * }
 *
 * @property playerShipHullIds Hull IDs of ships that count towards this achievement.
 * @property killCount Number of ships the player must kill to complete this achievement.
 * @property rewardedPaintjobIds Paintjob IDs to unlock when this achievement is completed.
 */
abstract class ShipKillsAchievement(
    val playerShipHullIds: List<String>,
    val killCount: Float,
    val rewardedPaintjobIds: List<String>
) : MagicAchievement() {
    companion object {
        val shipIdToListener: MutableMap<String, SharedCombatDamageListener> =
            mutableMapOf()

        /**
         * Percent of damage to hull the player must deal to a ship to count as a kill.
         */
        var damageRatio = 0.7f
    }

    protected val killedShipIds = mutableListOf<String>()

    override fun onSaveGameLoaded(isComplete: Boolean) {
        super.onSaveGameLoaded(isComplete)
        if (isComplete) {
            // If achievement is marked as complete, call `onCompleted`.
            // It _should_ already have been called, but maybe someone edited the text file to unlock it,
            // or maybe a new paintjob was added to this achievement after the player completed it, so
            // if we don't re-unlock the paintjobs here, the new ones won't be unlocked.
            onCompleted(null)
            return
        }
    }

    override fun onCompleted(completedByPlayer: PersonAPI?) {
        rewardedPaintjobIds.forEach { MagicPaintjobManager.unlockPaintjob(it) }
    }

    override fun advanceInCombat(amount: Float, events: MutableList<InputEventAPI>?, isSimulation: Boolean) {
        super.advanceInCombat(amount, events, isSimulation)

        if (isComplete || isSimulation) return

        val ships = Global.getCombatEngine().ships

        for (ship in ships.filter { !it.isAlive && it.id !in killedShipIds }) {
            processKill(shipIdToListener[ship.id]!!.damageInfoPerShipId[ship.id])
            killedShipIds.add(ship.id)
        }

        applyShipListeners(ships)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        shipIdToListener.clear()
        Global.getCombatEngine()?.ships?.forEach { it.removeListenerOfClass(SharedCombatDamageListener::class.java) }
    }

    open fun applyShipListeners(shipsInCombat: List<ShipAPI>) {
        // ensure all alive ships have a listener to watch for damage
        for (ship in shipsInCombat) {
            if (!shipIdToListener.containsKey(ship.id)) {
                // Add listener if ship is alive and doesn't have one.
                ship.removeListenerOfClass(SharedCombatDamageListener::class.java)
                val listener = SharedCombatDamageListener()
                ship.addListener(listener)
                shipIdToListener[ship.id] = listener
            }
        }

        // Remove listeners from ships that aren't in combat anymore (e.g. from a previous battle)
        shipIdToListener.keys.filter { storedShipId -> storedShipId !in shipsInCombat.map { it.id } }
            .forEach { shipIdToListener.remove(it) }
    }

    open fun processKill(damageInfoPerShipId: SharedCombatDamageListener.DamageInfo?) {
        if (damageInfoPerShipId == null) return
        val combatEngine = Global.getCombatEngine() ?: return

        // Make sure the ship that was killed was an enemy.
        if (damageInfoPerShipId.damagedShipId !in combatEngine.getFleetManager(FleetSide.ENEMY)
                .allEverDeployedCopy.map { it.ship.id }
        )
            return

        val ship = combatEngine.ships.firstOrNull { it.id == damageInfoPerShipId.damagedShipId } ?: return

        if (ship.isFighter) return // get real

        // Check that the player was using a ship that counts towards this achievement.
        if (combatEngine.playerShip?.hullSpec?.hullId !in playerShipHullIds) return

        val totalDamage = damageInfoPerShipId.amountFromOthers + damageInfoPerShipId.amountFromPlayer
        val playerDamageRatio = damageInfoPerShipId.amountFromPlayer / totalDamage

        if (playerDamageRatio >= damageRatio) {
            val maxProgress = maxProgress ?: return
            val prev =
                (memory["killsByHull${combatEngine.playerShip?.hullSpec?.hullId}"] as? String?)?.toIntOrNull() ?: 0
            val newValue = (prev + 1).coerceAtMost(maxProgress.toInt())

            memory["killsByHull${combatEngine.playerShip?.hullSpec?.hullId}"] = (newValue).toString()
            progress = newValue.toFloat()

            if (newValue >= maxProgress) {
                completeAchievement()
            }
        }
    }

    override fun getHasProgressBar() = true
    override fun getMaxProgress() = killCount
}

open class SharedCombatDamageListener : DamageListener {
    data class DamageInfo(val damagedShipId: String, var amountFromPlayer: Float, var amountFromOthers: Float)

    val damageInfoPerShipId: MutableMap<String, DamageInfo> = mutableMapOf()

    override fun reportDamageApplied(source: Any?, target: CombatEntityAPI?, result: ApplyDamageResultAPI?) {
        val combatEngine = Global.getCombatEngine()

        // Don't count devmode, simulation, or null ships.
        if (Global.getSettings().isDevMode || Global.getCombatEngine().isSimulation || source == null || result == null) {
            return
        }

        // Check ship hitting another ship.
        if (target !is ShipAPI || source !is ShipAPI) {
            return
        }

        // Only count hull damage.
        if (result.damageToHull <= 0f) {
            return
        }

        val isPlayerShip = source.id == combatEngine.playerShip.id

        damageInfoPerShipId[target.id] = damageInfoPerShipId.getOrElse(target.id) {
            DamageInfo(
                damagedShipId = target.id,
                amountFromPlayer = 0f,
                amountFromOthers = 0f
            )
        }
            .also { info ->
                if (isPlayerShip) {
                    info.amountFromPlayer += result.damageToHull
                } else {
                    info.amountFromOthers += result.damageToHull
                }
            }
    }
}
