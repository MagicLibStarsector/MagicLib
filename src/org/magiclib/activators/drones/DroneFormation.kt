package org.magiclib.activators.drones

import com.fs.starfarer.api.combat.ShipAPI

abstract class DroneFormation {
    abstract fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float)
}