package org.magiclib.subsystems.drones;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

// TODO: Remove this, use the one in org.magiclib.util instead.
// It's still needed for backwards compat because DroneFormation uses it.
public class PIDController {
    // Delegate to the one in org.magiclib.util so logic isn't duplicated.
    public final org.magiclib.util.PIDController controller;

    /**
     * @param Kp movement proportional. higher value increases overshoot.
     * @param Kd movement derivative. higher value dampens oscillation.
     * @param Rp rotational proportional. higher value increases overshoot.
     * @param Rd rotational derivative. higher value dampens oscillation.
     */
    public PIDController(float Kp, float Kd, float Rp, float Rd) {
        controller = new org.magiclib.util.PIDController(Kp, Kd, Rp, Rd);
    }

    public PIDController copy() {
        return new PIDController(controller.KpX, controller.KdX, controller.KpR, controller.KdR);
    }

    public void move(Vector2f dest, ShipAPI drone) {
        controller.move(dest, drone);
    }

    public void rotate(float destFacing, ShipAPI drone) {
        controller.rotate(destFacing, drone);
    }
}

