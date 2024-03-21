package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

//thanks to ruddygreat for providing this
//code vaguely copied from the wikipedia article for a PID controller, using tomatopaste's CMUtilWs as reference for when things weren't lining up
//basic PID controller for smooth moves

public class PIDController {

    //error for x, y & rotation
    public float lastErrorX = 0;
    public float lastErrorY = 0;
    public float lastErrorR = 0;

    //strafe accel is this % of forward accel, used as a mult for the y factors
    public float strafeRatio = 0.5f;

    //proporional values
    public final float KpX;
    public final float KpY;
    public final float KpR;

    //derivative values
    public final float KdX;
    public final float KdY;
    public final float KdR;

    //if error for either value is *BELOW* this, then movement/rotation will not be performed.
    public float allowedLocationalErrorSquared = 1f;
    public float allowedRotationalError = 1f;

    //if locational error is below the defined allowed value, then deceleration will be performed.
    public boolean decelerateIfBelowError = true;

    /**
     * @param Kp movement proportional. higher value increases overshoot.
     * @param Kd movement derivative. higher value dampens oscillation.
     * @param Rp rotational proportional. higher value increases overshoot.
     * @param Rd rotational derivative. higher value dampens oscillation.
     */
    public PIDController(float Kp, float Kd, float Rp, float Rd) {

        this.KpX = Kp;
        this.KpY = Kp * strafeRatio;
        this.KpR = Rp;

        this.KdX = Kd;
        this.KdY = Kd * strafeRatio;
        this.KdR = Rd;
    }

    public PIDController copy() {
        return new PIDController(KpX, KdX, KpR, KdR);
    }

    public void move(Vector2f dest, ShipAPI drone) {
        Vector2f diff = Vector2f.sub(dest, drone.getLocation(), new Vector2f());
        if (diff.lengthSquared() < allowedLocationalErrorSquared) {
            if (decelerateIfBelowError) {
                drone.giveCommand(ShipCommand.DECELERATE, null, 0);
            }
            return;
        }

        //this one line is from tomato
        //rotate the vector for ??? reasons
        VectorUtils.rotate(diff, 90f - drone.getFacing());

        float errorX = diff.x;
        float derivativeX = (errorX - lastErrorX) / Global.getCombatEngine().getElapsedInLastFrame();
        float outputX = KpX * errorX + KdX * derivativeX;
        ShipCommand commandX = outputX > 0f ? ShipCommand.STRAFE_RIGHT : ShipCommand.STRAFE_LEFT;
        drone.giveCommand(commandX, null, 0);
        lastErrorX = errorX;

        float errorY = diff.y;
        float derivativeY = (errorY - lastErrorY) / Global.getCombatEngine().getElapsedInLastFrame();
        float outputY = KpY * errorY + KdY * derivativeY;
        ShipCommand commandY = outputY > 0f ? ShipCommand.ACCELERATE : ShipCommand.ACCELERATE_BACKWARDS;
        drone.giveCommand(commandY, null, 0);
        lastErrorY = errorY;

    }

    public void rotate(float destFacing, ShipAPI drone) {

        float rotationError = MathUtils.getShortestRotation(drone.getFacing(), destFacing);
        if (Math.abs(rotationError) < allowedRotationalError) {
            return;
        }

        float derivativeR = (rotationError - lastErrorR) / Global.getCombatEngine().getElapsedInLastFrame();
        float outputR = KpR * rotationError + KdR * derivativeR;
        ShipCommand commandR = outputR > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
        drone.giveCommand(commandR, null, 0);
        lastErrorR = rotationError;
    }
}

