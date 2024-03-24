package org.magiclib.subsystems.drones

import com.fs.starfarer.api.combat.ShipAPI
import org.magiclib.util.PIDController

abstract class DroneFormation {
    abstract fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float)
}