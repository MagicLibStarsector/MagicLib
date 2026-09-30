package org.magiclib.subsystems.drones

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags

/**
 * Gives drones the game's regular fighter AI back (MagicDroneSubsystem strips it on spawn) and lets you switch
 * between regrouping on the mothership and going out to fight.
 *
 * @param mode Current mode. Can be changed at any time.
 * @param modeSelector Optional. If set, it is asked for the mode every frame and [mode] is ignored.
 */
class FighterAIFormation @JvmOverloads constructor(
    var mode: Mode = Mode.ENGAGE,
    var modeSelector: ((ShipAPI) -> Mode)? = null
) : DroneFormation() {

    enum class Mode {
        /** Regroup. Drones escort the mothership and only defend it. This sets the ESCORT_OTHER_SHIP AI flag. */
        REGROUP,

        /** Drones attack the nearest enemy within the wing's range of the mothership, and escort it otherwise. */
        ENGAGE,

        /** Drones have no attachment to the mothership and attack anything they can reach */
        ROAM
    }

    private val warnedNoWing: MutableSet<ShipAPI> = mutableSetOf()

    override fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float) {
        val current =
            if (ship.isAlive && !ship.isHulk)
                modeSelector?.invoke(ship) ?: mode
            else
                Mode.ROAM

        for (drone in drones.keys) {
            val wing = drone.wing
            if (wing == null) {
                // FighterAI does nothing without a wing.
                if (warnedNoWing.add(drone)) {
                    Global.getLogger(FighterAIFormation::class.java)
                        .warn("Drone ${drone.hullSpec.hullId} has no wing, so FighterAIFormation can't drive it.")
                }
                continue
            }

            val sourceShip = if (current == Mode.ROAM) null else ship
            if (wing.sourceShip !== sourceShip) wing.sourceShip = sourceShip

            if (drone.shipAI == null) drone.resetDefaultAI()

            if (current == Mode.REGROUP) {
                drone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.ESCORT_OTHER_SHIP, 1f, ship)
            } else {
                drone.aiFlags.unsetFlag(ShipwideAIFlags.AIFlags.ESCORT_OTHER_SHIP)
            }
        }
    }
}