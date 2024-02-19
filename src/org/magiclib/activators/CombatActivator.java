package org.magiclib.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.Objects;

public abstract class CombatActivator {
    public static String BLANK_KEY = "N/A";
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

    public CombatActivator(ShipAPI ship) {
        this.ship = ship;
        this.stats = ship.getMutableStats();
        this.aiData = new ShipAIData(ship);
    }

    /**
     * Called after key is set and activator is added to ship. Sets up intervals and duration values. Always call super.
     */
    protected void init() {
        if (inited) return;

        inited = true;
        this.inDuration = getBaseInDuration();
        this.activeDuration = getBaseActiveDuration();
        this.outDuration = getBaseOutDuration();
        this.cooldownDuration = getBaseCooldownDuration();
        this.chargeGenerationDuration = getBaseChargeRechargeDuration();
        this.charges = getMaxCharges();

        this.chargeInterval = new IntervalUtil(chargeGenerationDuration, chargeGenerationDuration);
        this.stateInterval = new IntervalUtil(inDuration, inDuration);
    }

    /**
     * Whether to assign a key to this activator.
     *
     * @return
     */
    public boolean canAssignKey() {
        return true;
    }

    /**
     * How long the activator is in State.IN for. To modify after adding the subsystem to the ship, use
     * setInDuration().
     *
     * @return
     */
    public float getBaseInDuration() {
        return 0f;
    }

    /**
     * How long the activator is active for.
     * For toggle activators, this is the minimum duration that the activator must be active for before it can be turned off.
     *
     * @return
     */
    public abstract float getBaseActiveDuration();

    /**
     * How long the activator is in State.OUT for. To modify after adding the subsystem to the ship, use
     * setOutDuration().
     *
     * @return
     */
    public float getBaseOutDuration() {
        return 0f;
    }

    /**
     * How long the activator is in State.COOLDOWN for. To modify after adding the subsystem to the ship, use
     * setCooldownDuration().
     *
     * @return
     */
    public abstract float getBaseCooldownDuration();

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

    public boolean hasCharges() {
        return getMaxCharges() > 0;
    }

    public int getMaxCharges() {
        return 0;
    }

    public float getBaseChargeRechargeDuration() {
        return 0f;
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
     * Returns whether the activator can be activated. For toggle activators, also returns whether the activator can be
     * deactivated.
     * Should check for parameters related to the ship, like if flux is less than a certain amount.
     *
     * @return
     */
    public boolean canActivate() {
        return true;
    }

    /**
     * Returns whether the activator can be activated. For toggle activators, also returns whether the activator can be
     * deactivated.
     * Should check for internal parameters, like if state == READY or if the activator has charges.
     *
     * @return
     */
    public boolean canActivateInternal() {
        if (!isToggle() || state == State.READY) {
            if (usesChargesOnActivate() && hasCharges() && charges <= 0) {
                return false;
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
     * Runs when the key is pressed to activate the activator.
     * For toggle activators, also runs when the key is pressed to deactivate the activator. Check if state == State.ACTIVE to
     * check for this case.
     */
    public void onActivate() {

    }

    /**
     * When state is set to COOLDOWN.
     */
    public void onFinished() {

    }

    /**
     * When ship dies while system is active (IN, ACTIVE, OUT states)
     */
    public void onShipDeath() {
        onFinished();
    }

    public void onStateSwitched(State oldState) {

    }

    /**
     * Runs every frame while the game is not paused.
     *
     * @param amount
     */
    public void advance(float amount) {

    }

    /**
     * Runs every frame, even while paused.
     */
    public void advanceEveryFrame() {

    }

    /**
     * Player ship only.
     *
     * @return
     */
    public boolean isKeyDown() {
        if (!canAssignKey()) {
            return false;
        }

        if (getKeyIndex() >= 0) {
            if (ActivatorManager.INSTANCE.getHotkeyList().size() > getKeyIndex()) {
                return Keyboard.isKeyDown(ActivatorManager.INSTANCE.getHotkeyList().get(getKeyIndex()));
            }
            return false;
        }
        if (getKey().equals("LALT")) {
            return Keyboard.isKeyDown(Keyboard.KEY_LMENU);
        }
        if (getKey().equals("RALT")) {
            return Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        }
        return Keyboard.isKeyDown(Keyboard.getKeyIndex(getKey()));
    }

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
        }
    }

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

    public void advanceInternal(float amount) {
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
            stateInterval.advance(amount);
        }

        if (charges < getMaxCharges()) {
            if (chargeInterval.intervalElapsed()) {
                charges++;
                chargeInterval.setInterval(getChargeGenerationDuration(), getChargeGenerationDuration());
            } else {
                chargeInterval.advance(amount);
            }
        }

        State initialState = state;
        boolean shouldActivate = false;
        if (Global.getCombatEngine().getPlayerShip() == ship && ship.getAI() == null) {
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

        advance(amount);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

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
     * @param inDuration
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
     * @param activeDuration
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
     * @param outDuration
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
     * @param cooldownDuration
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
     * If state is IN, ACTIVE, or OUT.
     *
     * @return
     */
    public boolean isOn() {
        return state == State.IN || state == State.ACTIVE || state == State.OUT;
    }

    /**
     * If state is READY or COOLDOWN.
     *
     * @return
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
     * @param chargeGenerationDuration
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
     * Your activator name. This is appended to the key used to activate the activator.
     *
     * @return
     */
    public abstract String getDisplayText();

    /**
     * This prints to the left of the status bar.
     *
     * @return
     */
    public String getAmmoText() {
        String chargeText = "-";
        if (this.hasCharges()) {
            chargeText = String.valueOf(this.charges);
        }
        return chargeText;
    }

    public float getNameTextPadding() {
        return 6f;
    }

    /**
     * Prints to the right of the status bar.
     *
     * @return
     */
    public String getStateText() {
        return this.state.getText();
    }

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

    public Color getHUDColor() {
        return MagicUI.GREENCOLOR;
    }

    public String getKeyText() {
        if (!Objects.equals(key, BLANK_KEY)) {
            return key;
        }

        int keycode = -1;
        if (canAssignKey() && getKeyIndex() >= 0 && ActivatorManager.INSTANCE.getHotkeyList().size() > getKeyIndex()) {
            keycode = ActivatorManager.INSTANCE.getHotkeyList().get(getKeyIndex());
        }

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

    public void drawHUDBar(ViewportAPI viewport, Vector2f barLoc) {
        MagicUI.setTextAligned(LazyFont.TextAlignment.LEFT);

        String nameText;
        if (canAssignKey()) {
            String keyText = getKeyText();
            nameText = String.format("%s (%s)", getDisplayText(), keyText);
        } else {
            nameText = String.format("%s", getDisplayText());
        }

        float nameWidth = MagicUI.getTextWidth(nameText);
        MagicUI.addText(ship, nameText, getHUDColor(), Vector2f.add(barLoc, new Vector2f(0, 10), null), false);

        barLoc = Vector2f.add(barLoc, new Vector2f(nameWidth + getNameTextPadding() + 2f, 0f), null);


        float ammoWidth = MagicUI.getTextWidth(getAmmoText());
        MagicUI.addText(ship, getAmmoText(), getHUDColor(), Vector2f.add(barLoc, new Vector2f(0, 10), null), false);

        barLoc = Vector2f.add(barLoc, new Vector2f(ammoWidth - 2f, 0f), null);

        String stateText = getStateText();
        if (!stateText.isEmpty()) {
            MagicUI.addText(ship, getStateText(), getHUDColor(), Vector2f.add(barLoc, new Vector2f(12 + 4 + 59, 10), null), false);
        }
        MagicUI.addBar(ship, getBarFill(), getHUDColor(), getHUDColor(), 0f, Vector2f.add(barLoc, new Vector2f(12, 0), null));
    }

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
            text = Global.getSettings().getString("ActivatorStates", this.name());
        }

        public String getText() {
            return text;
        }
    }
}
