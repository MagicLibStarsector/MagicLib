package org.magiclib.subsystems.drones

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils

class HoveringFormation : DroneFormation() {
    override fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float) {
        if (drones.isEmpty()) return
        val angleIncrease = 360f / drones.size

        drones.onEachIndexed { index, (drone, controller) ->
            val shipLoc = ship.location
            val angle = angleIncrease * (index - 1)
            val point = MathUtils.getPointOnCircumference(shipLoc, ship.collisionRadius * 1.5f, angle)
            controller.move(point, drone)

            val iter = Global.getCombatEngine().shipGrid.getCheckIterator(drone.location, 1000f, 1000f)

            var target: ShipAPI? = null
            var distance = 100000f
            for (it in iter) {
                if (it is ShipAPI) {
                    if (it.isFighter) continue
                    if (Global.getCombatEngine().getFleetManager(it.owner).owner == Global.getCombatEngine()
                            .getFleetManager(drone.owner).owner
                    ) continue
                    if (it.isHulk) continue
                    val distanceBetween = MathUtils.getDistance(it, ship)
                    if (distance > distanceBetween) {
                        distance = distanceBetween
                        target = it
                    }
                }
            }

            if (target != null) {
                controller.rotate(Misc.getAngleInDegrees(drone.location, target.location), drone)
            } else {
                controller.rotate(ship.facing + MathUtils.getRandomNumberInRange(-10f, 10f), drone)
            }
        }
    }
}