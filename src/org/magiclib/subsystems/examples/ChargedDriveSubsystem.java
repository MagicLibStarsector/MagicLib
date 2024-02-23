package org.magiclib.subsystems.examples;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;

public class ChargedDriveSubsystem extends MagicSubsystem {
    private static final float ESTIMATED_DRIVE_RANGE = 400f;
    private static final Color engineColor = new Color(0x00FF80);

    private float speedBoost = 0f;

    public ChargedDriveSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseInDuration() {
        return 0.15f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 1f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 3f;
    }

    @Override
    public int getMaxCharges() {
        return 3;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        return 12f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        ShipAPI target = ship.getShipTarget();
        if (target != null) {
            float score = 0f;

            if (target.getFluxTracker().isOverloadedOrVenting()) {
                score += 6f;
            } else {
                score += target.getFluxLevel() * 6f;
            }

            score -= ship.getFluxLevel() * 12f;

            float engagementRange = aiData.getEngagementRange() + ESTIMATED_DRIVE_RANGE;
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            if (dist > engagementRange) {
                score -= 3f;
            } else {
                score += 3f;
            }

            float avgRange = aiData.getAverageWeaponRange(false);
            score += Math.min(avgRange / dist, 8f);

            return score > 10f;
        }

        return false;
    }

    @Override
    public void onActivate() {
        speedBoost = 150f;
    }

    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        if (state == State.IN || state == State.ACTIVE || state == State.OUT) {
            ship.getEngineController().fadeToOtherColor(this, engineColor, new Color(0, 0, 0, 0), getEffectLevel(), 0.67f);
            ship.getEngineController().extendFlame(this, 2f * getEffectLevel(), 0f * getEffectLevel(), 0f * getEffectLevel());

            stats.getAcceleration().modifyFlat(getDisplayText(), 5000f);
            stats.getDeceleration().modifyFlat(getDisplayText(), 5000f);
            stats.getTurnAcceleration().modifyFlat(getDisplayText(), 5000f);
            stats.getMaxTurnRate().modifyFlat(getDisplayText(), 15f);
            stats.getMaxTurnRate().modifyPercent(getDisplayText(), 100f);

            ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            ship.giveCommand(ShipCommand.ACCELERATE, new Vector2f(0f, 1f), -1);
        }

        if (state == State.ACTIVE) {
            stats.getMaxSpeed().modifyPercent(getDisplayText(), speedBoost);

            stats.getTurnAcceleration().modifyFlat(getDisplayText(), 200f);
            stats.getMaxTurnRate().modifyFlat(getDisplayText(), 15f);
            stats.getMaxTurnRate().modifyPercent(getDisplayText(), 100f);
        }

        if (state == State.OUT) {
            speedBoost = Math.max(0f, speedBoost - amount * 60f);
            stats.getMaxSpeed().modifyPercent(getDisplayText(), speedBoost);
        }
    }

    @Override
    public void onFinished() {
        stats.getMaxSpeed().unmodify(getDisplayText());
        stats.getAcceleration().unmodify(getDisplayText());
        stats.getDeceleration().unmodify(getDisplayText());
        stats.getTurnAcceleration().unmodify(getDisplayText());
        stats.getMaxTurnRate().unmodify(getDisplayText());
    }

    @Override
    public String getDisplayText() {
        return "Charged Drive";
    }
}
