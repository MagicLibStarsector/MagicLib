package org.magiclib.subsystems;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class ShipAIData {
    private final ShipAPI ship;
    private float engagementRange = -1f;


    public ShipAIData(ShipAPI ship) {
        this.ship = ship;
    }

    /**
     * If the ship is a fighter and is returning
     *
     * @return
     */
    public boolean isFighterReturning() {
        boolean returning = false;
        if ((ship.getWing() != null) && ship.getWing().isReturning(ship)) {
            returning = true;
        }
        return returning;
    }

    /**
     * Returns the maximum range of all non-missile weapons on the ship, or a minimum of 1000.
     * If this is a fighter, returns 500f if the fighter is returning, otherwise returns the attack run range determined by the spec.
     *
     * @return see above
     */
    public float getEngagementRange() {
        float engagementRange = this.engagementRange;
        if (ship.getWing() != null) {
            if (isFighterReturning()) {
                engagementRange = 500f;
            } else {
                engagementRange = ship.getWing().getSpec().getAttackRunRange();
            }
        } else if (engagementRange == -1) {
            engagementRange = 1000f;
            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                    continue;
                }
                if (weapon.getRange() > engagementRange) {
                    engagementRange = weapon.getRange();
                }
            }
        }

        return engagementRange;
    }

    public float getAverageWeaponRange(boolean allowMissiles) {
        float averageRange = 0f;
        int countedWeapons = 0;

        for (WeaponAPI weapon : ship.getUsableWeapons()) {
            if (!allowMissiles && weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
                continue;
            }

            averageRange += weapon.getRange();
            countedWeapons++;
        }

        return averageRange / countedWeapons;
    }

    /**
     * Returns the target of the ship.
     * If fighter, this is the carrier if the fighter is returning to it. Otherwise it is the carrier's target.
     *
     * @return
     */
    public CombatEntityAPI getImmediateTarget() {
        ShipwideAIFlags flags = ship.getAIFlags();
        CombatEntityAPI immediateTarget = null;

        if ((ship.getWing() != null) && ship.getWing().getSourceShip() != null) {
            ShipAPI carrier = ship.getWing().getSourceShip();
            if (isFighterReturning()) {
                immediateTarget = carrier;
            } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI) {
                immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET);
            } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI) {
                immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
            } else {
                immediateTarget = carrier.getShipTarget();
            }
        }
        if ((immediateTarget == null) && ship.isFighter() && (flags.getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI)) {
            immediateTarget = (CombatEntityAPI) flags.getCustom(ShipwideAIFlags.AIFlags.CARRIER_FIGHTER_TARGET);
        }
        if ((immediateTarget == null) && (flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI)) {
            immediateTarget = (CombatEntityAPI) flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
        }
        if (immediateTarget == null) {
            immediateTarget = ship.getShipTarget();
        }
        return immediateTarget;
    }

    /**
     * If ship is accelerating forwards, the vector points more forwards
     * If strafing, the vector points more towards direction of strafe
     * If decelerating or moving backwards, the vector points more backwards
     * <p>
     * These rules combine to make a vector, so you can get a diagonal one by moving forwards while strafing.
     *
     * @return
     */
    public Vector2f getDesiredMoveVector() {
        Vector2f direction = new Vector2f();

        if (ship.getEngineController().isAccelerating()) {
            direction.y += 1f;
        } else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
            direction.y -= 1f;
        }
        if (ship.getEngineController().isStrafingLeft()) {
            direction.x -= 1f;
        } else if (ship.getEngineController().isStrafingRight()) {
            direction.x += 1f;
        }
        if (direction.length() <= 0f) {
            direction.y = 1f;
        }

        Misc.normalise(direction);
        VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
        return direction;
    }
}
