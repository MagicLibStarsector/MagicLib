package org.magiclib.subsystems.drones

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils

class SpinningCircleFormation : DroneFormation() {
    var currentRotation = MathUtils.getRandomNumberInRange(30f, 90f)
    var rotationSpeed = 0.2f

    override fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float) {
        val angleIncrease = 360 / drones.size
        var angle = 0f

        currentRotation += rotationSpeed
        angle += currentRotation

        for ((drone, controller) in drones) {
            val shipLoc = ship.location
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

            angle += angleIncrease
        }
    }
}