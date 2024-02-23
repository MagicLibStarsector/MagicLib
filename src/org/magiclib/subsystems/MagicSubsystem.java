package org.magiclib.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTxt;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.Objects;

public abstract class MagicSubsystem {
    public static String BLANK_KEY = "N/A";
    public static String STATUS_OUT_OF_RANGE = MagicTxt.getString("subsystemState_OutOfRange");
    public static String STATUS_NO_TARGET = MagicTxt.getString("subsystemState_NoTarget");
    public static String STATUS_FLUX_TOO_HIGH = MagicTxt.getString("subsystemState_FluxCapped");

    protected static int ORDER_MOD_MODULAR = 4;
    protected static int ORDER_MOD_UNIQUE = 5;
    protected static int ORDER_FACTION_MODULAR = 6;
    protected static int ORDER_FACTION_UNIQUE = 7;
    protected static int ORDER_SHIP_MODULAR = 8;
    protected static int ORDER_SHIP_UNIQUE = 9;

    protected String key = BLANK_KEY;
    protected int keyIndex = -1;
    protected boolean inited = false;
    protected boolean calledOnDeath = false;

    protected final ShipAPI ship;
    protected final MutableShipStatsAPI stats;
    protected final ShipAIData aiData;

    protected State state = State.READY;
    protected IntervalUtil stateInterval;
    protected int charges = -1;
    protected IntervalUtil chargeInterval;
    protected boolean activeElapsed = false;

    private float inDuration = -1f;
    private float activeDuration = -1f;
    private float outDuration = -1f;
    private float cooldownDuration = -1f;
    private float chargeGenerationDuration = -1f;

    public MagicSubsystem(ShipAPI ship) {
        this.ship = ship;
        this.stats = ship.getMutableStats();
        this.aiData = new ShipAIData(ship);
    }

    /**
     * How "important" this system is. Systems with a higher order value get placed first when assigning keys, meaning
     * the highest order subsystem will get assigned the first key index. If two subsystems have the same order, then
     * it will be picked based on alphabetical order of {@link MagicSubsystem#getDisplayText()}.
     * This also affects rendering order.
     * There are some static ints in this class for orders that are common. See {@link MagicSubsystem#ORDER_SHIP_UNIQUE}
     * and others.
     *
     * @return order of the subsystem
     */
    public int getOrder() {
        return 0;
    }

    /**
     * Called after key is set and subsystem is added to ship. Sets up intervals and duration values. Always call super.
     */
    protected void init() {
        if (inited) return;

        inited = true;
        this.inDuration = getBaseInDuration();
        this.activeDuration = getBaseActiveDuration();
        this.outDuration = getBaseOutDuration();
        this.cooldownDuration = getBaseCooldownDuration();
        this.chargeGenerationDuration = getBaseChargeRechargeDuration();
        this.charges = calcMaxCharges();

        this.chargeInterval = new IntervalUtil(chargeGenerationDuration, chargeGenerationDuration);
        this.stateInterval = new IntervalUtil(inDuration, inDuration);
    }

    /**
     * @return Whether to assign a key to this subsystem.
     */
    public boolean canAssignKey() {
        return true;
    }

    /**
     * How long the subsystem is in State.IN for.
     * To modify after adding the subsystem to the ship, use {@link MagicSubsystem#setInDuration(float, boolean)}
     *
     * @return
     */
    public float getBaseInDuration() {
        return 0f;
    }

    /**
     * How long the subsystem is active for.
     * For toggle subsystems, this is the minimum duration that the subsystem must be active for before it can be turned off.
     * To modify after adding the subsystem to the ship, use {@link MagicSubsystem#setActiveDuration(float, boolean)}
     *
     * @return
     */
    public abstract float getBaseActiveDuration();

    /**
     * How long the subsystem is in State.OUT for.
     * To modify after adding the subsystem to the ship, use {@link MagicSubsystem#setOutDuration(float, boolean)}
     *
     * @return
     */
    public float getBaseOutDuration() {
        return 0f;
    }

    /**
     * How long the subsystem is in State.COOLDOWN for.
     * To modify after adding the subsystem to the ship, use {@link MagicSubsystem#setCooldownDuration(float, boolean)}
     *
     * @return
     */
    public abstract float getBaseCooldownDuration();

    /**
     * The subsystem will show "NO TARGET" on the HUD if no target is selected.
     * Will also prevent system activation.
     *
     * @return Whether the subsystem requires a target.
     */
    public boolean requiresTarget() {
        return false;
    }

    /**
     * The subsystem will show "NO TARGET" on the HUD if a friendly target is selected.
     * Will also prevent system activation.
     *
     * @return Whether the subsystem requires a hostile target.
     */
    public boolean targetOnlyEnemies() {
        return true;
    }

    /**
     * The subsystem will show "OUT OF RANGE" on the HUD if the selected target is out of range.
     * Will also prevent system activation.
     * This is only used if {@link MagicSubsystem#requiresTarget} returns true.
     *
     * @return the subsystem's range
     */
    protected float getRange() {
        return -1f;
    }

    /**
     * Uses system stats to calculate the actual base range based on {@link MagicSubsystem#scaleSystemStat(float, float)}
     * @return range
     */
    public int calcRange() {
        float baseValue = getRange();
        if (baseValue == 0) {
            return 0;
        }

        return (int) scaleSystemStat(baseValue, ship.getMutableStats().getSystemRangeBonus().computeEffective(baseValue));
    }

    /**
     * Flux cost on activation.
     *
     * @return flat amount of flux
     */
    public float getFluxCostFlatOnActivation() {
        return 0f;
    }

    /**
     * Flux cost on activation as percent of base flux capacity of the ship.
     *
     * @return percent of base flux
     */
    public float getFluxCostPercentOnActivation() {
        return 0f;
    }

    /**
     * Set flux cost on activation to be hard flux.
     *
     * @return is hard flux?
     */
    public boolean isHardFluxForActivation() {
        return false;
    }

    /**
     * Flux cost while active. This method should return a per-second value, but will be split across all frames.
     *
     * @return flat amount of flux per second
     */
    public float getFluxCostFlatPerSecondWhileActive() {
        return 0f;
    }

    /**
     * Flux cost on activation as percent of base flux capacity of the ship. This method should return a per-second value, but will be split across all frames.
     *
     * @return percent of base flux per second
     */
    public float getFluxCostPercentPerSecondWhileActive() {
        return 0f;
    }

    /**
     * Set flux cost while active to be hard flux.
     *
     * @return is hard flux?
     */
    public boolean isHardFluxPerSecondWhileActive() {
        return false;
    }

    /**
     * Returns ratio of how "complete" the state is.
     * if state is 5 seconds long and 3 seconds have passed,
     * it will return 0.6f.
     * This method uses the internal interval.
     *
     * @return
     */
    public float getStateCompleteRatio() {
        return Math.max(Math.min(stateInterval.getElapsed() / stateInterval.getIntervalDuration(), 1f), 0f);
    }

    /**
     * Calculates how "effective" the system is based on its state and the time remaining in that state.
     * If IN, then calculation is {@link MagicSubsystem#getStateCompleteRatio()}
     * If ACTIVE, then 1f is returned.
     * If OUT, then calculation is 1 - {@link MagicSubsystem#getStateCompleteRatio()}
     * Otherwise, returns 0f.
     *
     * @return effectLevel
     */
    public float getEffectLevel() {
        if (state == State.IN) {
            return getStateCompleteRatio();
        }

        if (state == State.ACTIVE) {
            return 1f;
        }

        if (state == State.OUT) {
            return 1f - getStateCompleteRatio();
        }

        return 0f;
    }

    public boolean isToggle() {
        return false;
    }

    /**
     * Base amount of charges unaffected by any stats.
     * @return
     */
    public boolean hasCharges() {
        return getMaxCharges() > 0;
    }

    protected int getMaxCharges() {
        return 0;
    }

    protected float scaleSystemStat(float base, float effective) {
        return base + (effective - base) * getSystemStatsEffectMult();
    }

    /**
     * Uses system stats to calculate the actual max charges based on {@link MagicSubsystem#scaleSystemStat(float, float)}
     * @return charges
     */
    public int calcMaxCharges() {
        float baseValue = getMaxCharges();
        if (baseValue == 0) {
            return 0;
        }

        return (int) scaleSystemStat(baseValue, ship.getMutableStats().getSystemUsesBonus().computeEffective(baseValue));
    }

    /**
     * How long the subsystem takes to gain a charge.
     * To modify after adding the subsystem to the ship, use {@link MagicSubsystem#setChargeGenerationDuration(float, boolean)}
     *
     * @return
     */
    public float getBaseChargeRechargeDuration() {
        return 0f;
    }

    /**
     * How much the subsystem is affected by ship system stats like Systems Expertise.
     * Set to 0 to disable the mult.
     * @return a multiplier for those stats.
     */
    public float getSystemStatsEffectMult() {
        return 1f;
    }

    public boolean usesChargesOnActivate() {
        return true;
    }

    public boolean canUseWhileOverloaded() {
        return false;
    }

    public boolean canUseWhileVenting() {
        return false;
    }

    /**
     * Returns whether the subsystem can be activated. For toggle subsystems, also returns whether the subsystem can be
     * deactivated.
     * Should check for parameters related to the ship, like if flux is less than a certain amount.
     *
     * @return
     */
    public boolean canActivate() {
        return true;
    }

    /**
     * Returns whether the subsystem can be activated. For toggle subsystems, also returns whether the subsystem can be
     * deactivated.
     * Should check for internal parameters, like if state == READY or if the subsystem has charges.
     * This method also checks for flux cost of activating the subsystem.
     * This method also checks for targets if {@link MagicSubsystem#requiresTarget()} returns true.
     * This method also checks for system range if it requires a target and {@link MagicSubsystem#calcRange()} is equal to or above zero.
     * If overridden, you probably want to call super.
     *
     * @return
     */
    public boolean canActivateInternal() {
        if (!isToggle() || state == State.READY) {
            if (usesChargesOnActivate() && hasCharges() && charges <= 0) {
                return false;
            }

            if (requiresTarget()) {
                if (ship.getShipTarget() == null) {
                    return false;
                } else if (targetOnlyEnemies() && ship.getOwner() == ship.getShipTarget().getOwner()) {
                    return false;
                } else if (calcRange() >= 0 && MathUtils.getDistance(ship, ship.getShipTarget()) > calcRange()) {
                    return false;
                }
            }

            if (getFluxCostFlatOnActivation() > 0f) {
                if (ship.getFluxTracker().getCurrFlux() + getFluxCostFlatOnActivation() >= ship.getFluxTracker().getMaxFlux()) {
                    return false;
                }
            }

            if (getFluxCostPercentOnActivation() > 0f) {
                if (ship.getFluxTracker().getCurrFlux() + getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity() >= ship.getFluxTracker().getMaxFlux()) {
                    return false;
                }
            }
        }

        if (isToggle() && state == State.ACTIVE && stateInterval.intervalElapsed()) {
            return true;
        }

        return state == State.READY;
    }

    public abstract boolean shouldActivateAI(float amount);

    public boolean getAdvancesWhileDead() {
        return false;
    }

    /**
     * Runs when the key is pressed to activate the subsystem, or the AI activates it.
     * For toggle subsystems, also runs when the key is pressed to deactivate the subsystem.
     * If state == State.ACTIVE, then the toggled subsystem was just deactivated.
     */
    public void onActivate() {

    }

    /**
     * When state is set to COOLDOWN.
     */
    public void onFinished() {

    }

    /**
     * Called when the ship dies while system is active (IN, ACTIVE, OUT states)
     */
    public void onShipDeath() {
        onFinished();
    }

    /**
     * Called when the state is switched to something else.
     *
     * @param oldState the old state of the subsystem
     */
    public void onStateSwitched(State oldState) {

    }

    /**
     * Runs every frame while the game is not paused.
     *
     * @param amount time elapsed in last frame
     */
    public void advance(float amount, boolean isPaused) {

    }

    /**
     * Player ship only.
     *
     * @return whether the subsystem's assigned key is pressed
     */
    public boolean isKeyDown() {
        if (getAssignedKey() >= 0) {
            return Keyboard.isKeyDown(getAssignedKey());
        }

        return false;
    }

    /**
     * Handles activation of the subsystem, including setting the state, increasing flux, and taking charges.
     * Override {@link MagicSubsystem#onActivate()} unless you have a good idea what you're doing here.
     */
    public void activate() {
        onActivate();

        if (isToggle() && state == State.ACTIVE && stateInterval.intervalElapsed()) {
            activeElapsed = false;
            setState(State.OUT);
        } else {
            setState(State.IN);
            if (hasCharges() && usesChargesOnActivate()) {
                charges--;
            }

            if (getFluxCostFlatOnActivation() > 0f) {
                ship.getFluxTracker().increaseFlux(getFluxCostFlatOnActivation(), isHardFluxForActivation());
            }

            if (getFluxCostPercentOnActivation() > 0f) {
                ship.getFluxTracker().increaseFlux(getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity(), isHardFluxForActivation());
            }
        }
    }

    /**
     * Called to set the state of the system. This changes the stateInterval and resets it, and also calls
     * {@link MagicSubsystem#onStateSwitched(State)}, which you should override unless you have a good idea what
     * you're doing.
     *
     * @param newState
     */
    public void setState(State newState) {
        if (newState == State.IN) {
            state = State.IN;
            stateInterval.setInterval(getInDuration(), getInDuration());
        } else if (newState == State.ACTIVE) {
            state = State.ACTIVE;
            stateInterval.setInterval(getActiveDuration(), getActiveDuration());
        } else if (newState == State.OUT) {
            state = State.OUT;
            stateInterval.setInterval(getOutDuration(), getOutDuration());
        } else if (newState == State.COOLDOWN) {
            state = State.COOLDOWN;
            stateInterval.setInterval(getCooldownDuration(), getCooldownDuration());
        } else if (newState == State.READY) {
            state = State.READY;
        }
        onStateSwitched(newState);
    }

    /**
     * Called every frame that the combat engine is not paused. Handles all internal functions of the subsystem,
     * like activating it, adding charges, handling the state interval, and all other things the subsystem needs to do
     * frame-by-frame during active combat. Override {@link MagicSubsystem#advance(float, boolean)} unless you call super or
     * know exactly what you're doing.
     *
     * @param amount frame time
     */
    public void advanceInternal(float amount) {
        boolean isPaused = Global.getCombatEngine().isPaused();

        if (!isPaused) {
            boolean alive = ship.isAlive() && !ship.isHulk() && ship.getOwner() != 100;
            if (!alive) {
                if (!calledOnDeath) {
                    if (isOn()) {
                        onShipDeath();
                    }
                    calledOnDeath = true;
                }
            }

            if (!getAdvancesWhileDead() && !alive) return;

            if (state != State.READY && !stateInterval.intervalElapsed()) {
                if (state == State.COOLDOWN) {
                    stateInterval.advance(scaleSystemStat(amount, ship.getMutableStats().getSystemCooldownBonus().computeEffective(amount)));
                } else {
                    stateInterval.advance(amount);
                }
            }

            if (charges < calcMaxCharges()) {
                if (chargeInterval.intervalElapsed()) {
                    charges++;
                    chargeInterval.setInterval(getChargeGenerationDuration(), getChargeGenerationDuration());
                } else {
                    chargeInterval.advance(scaleSystemStat(amount, ship.getMutableStats().getSystemRegenBonus().computeEffective(amount)));
                }
            }

            boolean shouldActivate = false;
            //Global.getCombatEngine().isUIAutopilotOn() is backwards! returns true when player is piloting.
            if (Global.getCombatEngine().getPlayerShip() == ship && Global.getCombatEngine().isUIAutopilotOn()) {
                if (isKeyDown()) {
                    shouldActivate = true;
                }
            } else {
                shouldActivate = shouldActivateAI(amount);
            }

            if (shouldActivate && (canUseWhileOverloaded() || !ship.getFluxTracker().isOverloaded()) && (canUseWhileVenting() || !ship.getFluxTracker().isVenting())) {
                boolean internalActivate = canActivateInternal();
                boolean shipActivate = canActivate();

                if (internalActivate && shipActivate) {
                    activate();
                }
            }

            //Charge flux.
            if (isOn()) {
                if (getFluxCostFlatPerSecondWhileActive() > 0f) {
                    ship.getFluxTracker().increaseFlux(getFluxCostFlatPerSecondWhileActive() * amount, isHardFluxPerSecondWhileActive());
                }

                if (getFluxCostPercentPerSecondWhileActive() > 0f) {
                    ship.getFluxTracker().increaseFlux(getFluxCostPercentPerSecondWhileActive() * ship.getHullSpec().getFluxCapacity() * amount, isHardFluxPerSecondWhileActive());
                }
            }

            if (stateInterval.intervalElapsed() || stateInterval.getIntervalDuration() == 0f) {
                if (state == State.IN) {
                    setState(State.ACTIVE);
                } else if (state == State.OUT) {
                    activeElapsed = false;
                    setState(State.COOLDOWN);
                    onFinished();
                } else if (state == State.COOLDOWN) {
                    setState(State.READY);
                } else if (state == State.ACTIVE) {
                    activeElapsed = true;

                    if (!isToggle()) {
                        setState(State.OUT);
                    }
                }
            }
        }

        advance(amount, isPaused);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * If {@link MagicSubsystem#getKey()} returns a non-null non-empty string, retrieves the index for its value from {@link Keyboard#getKeyIndex(String)}
     * Otherwise uses {@link MagicSubsystem#getKeyIndex()} to retrieve the key from the {@link MagicSubsystemsManager#getKeyForIndex(int)} method
     *
     * @return keycode
     */
    public final int getAssignedKey() {
        if (!Objects.equals(getKey(), BLANK_KEY)) {
            if (getKey().equals("LALT") || getKey().equals("L-ALT") || getKey().equals("L-MENU") || getKey().equals("LMENU")) {
                return Keyboard.KEY_LMENU;
            }
            if (getKey().equals("RALT") || getKey().equals("R-ALT") || getKey().equals("R-MENU") || getKey().equals("RMENU")) {
                return Keyboard.KEY_RMENU;
            }
            return Keyboard.getKeyIndex(getKey());
        }

        return MagicSubsystemsManager.getKeyForIndex(getKeyIndex());
    }

    /**
     * The automatically-assigned key index when a subsystem is added to a ship.
     *
     * @return key index for the {@link MagicSubsystemsManager#getKeyForIndex(int)} method
     */
    public int getKeyIndex() {
        return keyIndex;
    }

    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    public State getState() {
        return state;
    }

    public int getCharges() {
        return charges;
    }

    public void setCharges(int charges) {
        this.charges = charges;
    }

    public boolean isIn() {
        return state == State.IN;
    }

    public float getInDuration() {
        if (inDuration == -1) {
            return getBaseInDuration();
        }
        return inDuration;
    }

    /**
     * Sets the in duration. If the state is currently IN, then the stateInterval will be affected according
     * to the preserve parameter.
     *
     * @param inDuration duration to set
     * @param preserve   If true, time elapsed in current interval will be preserved.
     *                   Otherwise stateInterval will be reset to 0 elapsed time, as if the interval is reset.
     */
    public void setInDuration(float inDuration, boolean preserve) {
        if (state == State.IN) {
            setAndPreserveInterval(stateInterval, inDuration, preserve);
        }
        this.inDuration = inDuration;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public float getActiveDuration() {
        if (activeDuration == -1) {
            return getBaseActiveDuration();
        }
        return activeDuration;
    }

    /**
     * Sets the active duration. If the state is currently ACTIVE, then the stateInterval will be affected according
     * to the preserve parameter.
     *
     * @param activeDuration duration to set
     * @param preserve       If true, time elapsed in current interval will be preserved.
     *                       Otherwise stateInterval will be reset to 0 elapsed time, as if the interval is reset.
     */
    public void setActiveDuration(float activeDuration, boolean preserve) {
        if (state == State.ACTIVE) {
            setAndPreserveInterval(stateInterval, activeDuration, preserve);
        }
        this.activeDuration = activeDuration;
    }

    public boolean isOut() {
        return state == State.OUT;
    }

    public float getOutDuration() {
        if (outDuration == -1) {
            return getBaseOutDuration();
        }
        return outDuration;
    }

    /**
     * Sets the out duration. If the state is currently OUT, then the stateInterval will be affected according
     * to the preserve parameter.
     *
     * @param outDuration duration to set
     * @param preserve    If true, time elapsed in current interval will be preserved.
     *                    Otherwise stateInterval will be reset to 0 elapsed time, as if the interval is reset.
     */
    public void setOutDuration(float outDuration, boolean preserve) {
        if (state == State.OUT) {
            setAndPreserveInterval(stateInterval, outDuration, preserve);
        }
        this.outDuration = outDuration;
    }

    public boolean isCooldown() {
        return state == State.COOLDOWN;
    }

    public float getCooldownDuration() {
        if (cooldownDuration == -1) {
            return getBaseCooldownDuration();
        }
        return cooldownDuration;
    }

    /**
     * Sets the cooldown duration. If the state is currently COOLDOWN, then the stateInterval will be affected according
     * to the preserve parameter.
     *
     * @param cooldownDuration duration to set
     * @param preserve         If true, time elapsed in current interval will be preserved.
     *                         Otherwise stateInterval will be reset to 0 elapsed time, as if the interval is reset.
     */
    public void setCooldownDuration(float cooldownDuration, boolean preserve) {
        if (state == State.COOLDOWN) {
            setAndPreserveInterval(stateInterval, cooldownDuration, preserve);
        }
        this.cooldownDuration = cooldownDuration;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    /**
     * @return If state is IN, ACTIVE, or OUT.
     */
    public boolean isOn() {
        return state == State.IN || state == State.ACTIVE || state == State.OUT;
    }

    /**
     * @return If state is READY or COOLDOWN.
     */
    public boolean isOff() {
        return state == State.READY || state == State.COOLDOWN;
    }

    public float getChargeGenerationDuration() {
        if (chargeGenerationDuration == -1) {
            return getBaseChargeRechargeDuration();
        }
        return chargeGenerationDuration;
    }

    /**
     * Sets the charge interval duration.
     *
     * @param chargeGenerationDuration duration to set
     * @param preserve                 If true, time elapsed in current interval will be preserved.
     *                                 Otherwise chargeInterval will be reset to 0 elapsed time, as if the interval is reset.
     */
    public void setChargeGenerationDuration(float chargeGenerationDuration, boolean preserve) {
        this.chargeGenerationDuration = chargeGenerationDuration;
        setAndPreserveInterval(chargeInterval, chargeGenerationDuration, preserve);
    }

    private void setAndPreserveInterval(IntervalUtil interval, float newTime, boolean preserve) {
        float currTime = interval.getElapsed();
        interval.setInterval(newTime, newTime);
        if (preserve)
            interval.setElapsed(currTime);
    }

    /**
     * This is appended to the key used to activate the subsystem.
     * Also used for assigning key indices if Order of two subsystems matches.
     *
     * @return Your subsystem name.
     */
    public abstract String getDisplayText();

    /**
     * A short description of what your subsystem does. Only shows while INFO is toggled.
     *
     * @return summary
     */
    public String getBriefText() {
        return null;
    }


    /**
     * Some extra info that displays to the right of the status bar. Always visible if not null/empty.
     * By default, displays info related to why the system can't be activated.
     *
     * @return extra info
     */
    public String getExtraInfoText() {
        if (requiresTarget()) {
            if (ship.getShipTarget() == null) {
                return STATUS_NO_TARGET;
            } else if (targetOnlyEnemies() && ship.getOwner() == ship.getShipTarget().getOwner()) {
                return STATUS_NO_TARGET;
            } else if (calcRange() >= 0 && MathUtils.getDistance(ship, ship.getShipTarget()) > calcRange()) {
                return STATUS_OUT_OF_RANGE;
            }
        }

        if (getFluxCostFlatOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostFlatOnActivation() >= ship.getFluxTracker().getMaxFlux()) {
                return STATUS_FLUX_TOO_HIGH;
            }
        }

        if (getFluxCostPercentOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity() >= ship.getFluxTracker().getMaxFlux()) {
                return STATUS_FLUX_TOO_HIGH;
            }
        }
        return null;
    }

    /**
     * Color of the text to the right of the status bar.
     * By default, copies the System Can't Be Used Color for when a shipsystem is out of range or needs a target.
     *
     * @return color of extra info text
     */
    public Color getExtraInfoColor() {
        return getHUDColor().darker().darker();
    }

    /**
     * This prints beneath the subsystem bar.
     *
     * @return Ammo text to display.
     */
    public String getAmmoText() {
        return MagicTxt.getString("subsystemChargesText", String.valueOf(charges));
    }

    /**
     * Prints to the left of the status bar.
     *
     * @return state text to display
     */
    public String getStateText() {
        return this.state.getText();
    }

    /**
     * How full the status bar will appear.
     *
     * @return a float between 0 and 1
     */
    public float getBarFill() {
        float fill = 0f;
        if (state == State.IN) {
            fill = stateInterval.getElapsed() / (this.getInDuration() + this.getActiveDuration());
        } else if (state == State.ACTIVE) {
            if (isToggle() && activeElapsed) {
                fill = 1f;
            } else {
                fill = (this.getInDuration() + stateInterval.getElapsed()) / (this.getInDuration() + this.getActiveDuration());
            }
        } else if (state == State.OUT) {
            fill = 1f - (stateInterval.getElapsed() / (this.getOutDuration() + this.getCooldownDuration()));
        } else if (state == State.COOLDOWN) {
            fill = 1f - ((this.getOutDuration() + stateInterval.getElapsed()) / (this.getOutDuration() + this.getCooldownDuration()));
        } else if (state == State.READY) {
            if (hasCharges() && charges == 0) {
                fill = this.chargeInterval.getElapsed() / this.chargeInterval.getIntervalDuration();
            } else {
                fill = 0f;
            }
        }

        return Math.max(Math.min(fill, 1f), 0f);
    }

    /**
     * The color to display all the info about the system in
     *
     * @return a color
     */
    public Color getHUDColor() {
        return MagicUI.GREENCOLOR;
    }

    /**
     * The displayed key text to activate the system. Only displayed if the system has an actual key to display.
     * Uses {@link MagicSubsystem#getAssignedKey()} to find the key responsible for activating the system.
     *
     * @return displayed key
     */
    public String getKeyText() {
        int keycode = getAssignedKey();

        switch (keycode) {
            case -1:
                return BLANK_KEY;
            case Keyboard.KEY_LMENU:
                return "L-ALT";
            case Keyboard.KEY_RMENU:
                return "R-ALT";
            case Keyboard.KEY_LCONTROL:
                return "L-CTRL";
            case Keyboard.KEY_RCONTROL:
                return "R-CTRL";
            case Keyboard.KEY_LSHIFT:
                return "L-SHIFT";
            case Keyboard.KEY_RSHIFT:
                return "R-SHIFT";
            case Keyboard.KEY_CAPITAL:
                return "CAPS";
            case Keyboard.KEY_RETURN:
                return "ENTER";
            case Keyboard.KEY_BACK:
                return "BKSPC";
            default:
                return Keyboard.getKeyName(keycode).toUpperCase();
        }
    }

    /**
     * Number of "bar heights" that the subsystem needs.
     * By default, 1. For systems with charges, 2.
     *
     * @return bar count
     */
    public int getNumHUDBars() {
        if (hasCharges()) {
            return 2;
        }
        return 1;
    }

    public Vector2f getBarLocationForBarNum(Vector2f baseBarLoc, int barNum) {
        return Vector2f.add(baseBarLoc, new Vector2f(0f, -CombatUI.BAR_HEIGHT * barNum), null);
    }

    /**
     * Draws the subsystem info on the HUD.
     *
     * @param viewport              viewport to draw to
     * @param rootLoc               root location of subsystem info
     * @param barLoc                location to draw (top left)
     * @param displayAdditionalInfo display additional subsystem info (hotkey, brief)
     * @param longestNameWidth      longest name width of all subsystems on the ship, for making the UI look uniform
     */
    public void drawHUDBar(ViewportAPI viewport, Vector2f rootLoc, Vector2f barLoc, boolean displayAdditionalInfo, float longestNameWidth) {
        String nameText = getDisplayText();
        String keyText = getKeyText();

        if (!displayAdditionalInfo && !keyText.equals(BLANK_KEY)) {
            nameText = MagicTxt.getString("subsystemNameWithKeyText", nameText, keyText);
        }

        boolean displayStateText = true;
        if (requiresTarget()) {
            if (ship.getShipTarget() == null) {
                displayStateText = false;
            } else if (targetOnlyEnemies() && ship.getOwner() == ship.getShipTarget().getOwner()) {
                displayStateText = false;
            } else if (calcRange() >= 0 && MathUtils.getDistance(ship, ship.getShipTarget()) > calcRange()) {
                displayStateText = false;
            }
        }

        if (getFluxCostFlatOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostFlatOnActivation() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        if (getFluxCostPercentOnActivation() > 0f) {
            if (ship.getFluxTracker().getCurrFlux() + getFluxCostPercentOnActivation() * ship.getHullSpec().getFluxCapacity() >= ship.getFluxTracker().getMaxFlux()) {
                displayStateText = false;
            }
        }

        String stateText = getStateText();
        if (!displayStateText) {
            stateText = null;
        }

        float additionalBarPadding = Math.max(0f, longestNameWidth - CombatUI.STATUS_BAR_PADDING);
        CombatUI.drawSubsystemStatus(
                ship,
                getBarFill(),
                nameText,
                getExtraInfoText(),
                getExtraInfoColor(),
                stateText,
                keyText,
                getBriefText(),
                displayAdditionalInfo,
                getNumHUDBars(),
                barLoc,
                additionalBarPadding,
                rootLoc
        );

        if (hasCharges()) {
            CombatUI.renderAuxiliaryStatusBar(
                    ship,
                    CombatUI.INFO_TEXT_PADDING,
                    false,
                    CombatUI.STATUS_BAR_PADDING - CombatUI.INFO_TEXT_PADDING + additionalBarPadding,
                    CombatUI.STATUS_BAR_WIDTH,
                    chargeInterval.getElapsed() / chargeInterval.getIntervalDuration(),
                    getAmmoText(),
                    null,
                    false, getBarLocationForBarNum(barLoc, 1)
            );
        }
    }

    /**
     * Renders onto the world viewport.
     *
     * @param viewport the world viewport
     */
    public void renderWorld(ViewportAPI viewport) {

    }

    public enum State {
        READY,
        IN,
        ACTIVE,
        OUT,
        COOLDOWN;

        private final String text;

        State() {
            text = MagicTxt.getString("subsystemState_" + Misc.ucFirst(this.name().toLowerCase()));
        }

        /**
         * Display text for the subsystem state.
         *
         * @return display text
         */
        public String getText() {
            return text;
        }
    }
}
