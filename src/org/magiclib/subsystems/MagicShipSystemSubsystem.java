package org.magiclib.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

/**
 * Calls relevant hooks in a ship system.
 */
public abstract class MagicShipSystemSubsystem extends MagicSubsystem {
    private ShipSystemSpecAPI systemSpec = Global.getSettings().getShipSystemSpec(getShipSystemId());
    private ShipSystemStatsScript system = systemSpec.getStatsScript();

    public MagicShipSystemSubsystem(ShipAPI ship) {
        super(ship);
    }

    public abstract String getShipSystemId();

    @Override
    public boolean hasCharges() {
        return getMaxCharges() > 0;
    }

    @Override
    public int getMaxCharges() {
        int uses = system.getUsesOverride(ship);
        if (uses < 0) {
            uses = systemSpec.getMaxUses(ship.getMutableStats());
        }
        return uses;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        float regen = system.getRegenOverride(ship);
        if (regen < 0) {
            regen = systemSpec.getRegen(ship.getMutableStats());
        }
        return regen;
    }

    @Override
    public float getBaseInDuration() {
        float in = system.getInOverride(ship);
        if (in < 0) {
            in = systemSpec.getIn();
        }
        return in;
    }

    @Override
    public float getBaseActiveDuration() {
        float active = system.getActiveOverride(ship);
        if (active < 0) {
            active = systemSpec.getActive();
        }
        return active;
    }

    @Override
    public float getBaseOutDuration() {
        float active = system.getOutOverride(ship);
        if (active < 0) {
            active = systemSpec.getOut();
        }
        return active;
    }

    @Override
    public float getBaseCooldownDuration() {
        return systemSpec.getCooldown(stats);
    }

    @Override
    public String getDisplayText() {
        String name = system.getDisplayNameOverride(translateState(state), this.getEffectLevel());
        if (name == null) {
            name = systemSpec.getName();
        }
        return name;
    }

    private static ShipSystemStatsScript.State translateState(State CAstate) {
        if (CAstate == State.ACTIVE) return ShipSystemStatsScript.State.ACTIVE;
        if (CAstate == State.IN) return ShipSystemStatsScript.State.IN;
        if (CAstate == State.OUT) return ShipSystemStatsScript.State.OUT;
        if (CAstate == State.COOLDOWN) return ShipSystemStatsScript.State.COOLDOWN;
        if (CAstate == State.READY) return ShipSystemStatsScript.State.IDLE;
        return null;
    }

    public void advance(float amount, boolean isPaused) {
        if (!isPaused && state != State.READY && state != State.COOLDOWN) {
            system.apply(ship.getMutableStats(), systemSpec.getId(), translateState(getState()), getEffectLevel());
        }

        if (ship == Global.getCombatEngine().getPlayerShip()) {
            int statusIndex = 0;
            ShipSystemStatsScript.StatusData statusData;
            do {
                statusData = system.getStatusData(statusIndex, translateState(getState()), getEffectLevel());
                if (statusData != null) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(getShipSystemId() + statusIndex, systemSpec.getIconSpriteName(), systemSpec.getName(), statusData.text, statusData.isDebuff);
                }
            } while (statusData != null);
        }
    }

    @Override
    public void onStateSwitched(State oldState) {
        if (state == State.COOLDOWN) {
            system.unapply(ship.getMutableStats(), systemSpec.getId());
        }
    }
}
