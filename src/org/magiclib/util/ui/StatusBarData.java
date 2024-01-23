package org.magiclib.util.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;

import java.awt.*;

/**
 * @author President Matt Damon
 */
public class StatusBarData {

    private ShipAPI ship;
    private float fill;
    private Color innerColor;
    private Color borderColor;
    private float secondFill;
    private String text;
    private int number;
    private float lastRefreshed;
    private Vector2f overridePos = null;

    public StatusBarData(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        this.ship = ship;
        this.fill = fill;
        this.innerColor = innerColor;
        this.borderColor = borderColor;
        this.secondFill = secondFill;
        this.text = text;
        this.number = number;
        refresh();
    }

    public void setData(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        this.ship = ship;
        this.fill = fill;
        this.innerColor = innerColor;
        this.borderColor = borderColor;
        this.secondFill = secondFill;
        this.text = text;
        this.number = number;
        refresh();
    }

    public Vector2f getOverridePos() {
        return overridePos;
    }

    public void setOverridePos(Vector2f overridePos) {
        this.overridePos = overridePos;
    }

    public float getLastRefreshed() {
        return lastRefreshed;
    }

    public void refresh() {
        this.lastRefreshed = Global.getCombatEngine().getTotalElapsedTime(true);
    }

    /**
     * Draws the status bar to the screen.
     *
     * @param offset offset for position, only used if overridePos class variable is null
     */
    public void drawToScreen(Vector2f offset) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f statusBarLoc;
        if (overridePos != null) {
            statusBarLoc = overridePos;
        } else {
            statusBarLoc = Vector2f.add(offset, MagicUI.getInterfaceOffsetFromStatusBars(ship, ship.getVariant()), null);
        }
        final Vector2f boxLoc = Vector2f.add(new Vector2f(224f, 120f), statusBarLoc, null);

        MagicUI.addInterfaceStatusBar(ship, boxLoc, fill, innerColor, borderColor, secondFill);
        if (MagicUI.TODRAW14 != null) {
            if (text != null && !text.isEmpty()) {
                final Vector2f textLoc = Vector2f.add(new Vector2f(176f, 131f), statusBarLoc, null);
                MagicUI.addInterfaceStatusText(ship, text, textLoc);
            }
            if (number >= 0) {
                final Vector2f numberLoc = Vector2f.add(new Vector2f(355f, 131f), statusBarLoc, null);
                MagicUI.addInterfaceStatusNumber(ship, number, numberLoc);
            }
        }
    }

    public ShipAPI getShip() {
        return ship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatusBarData that = (StatusBarData) o;

        if (Float.compare(that.fill, fill) != 0) return false;
        if (Float.compare(that.secondFill, secondFill) != 0) return false;
        if (number != that.number) return false;
        if (!ship.equals(that.ship)) return false;
        if (!innerColor.equals(that.innerColor)) return false;
        if (!borderColor.equals(that.borderColor)) return false;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        int result = ship.hashCode();
        result = 31 * result + (fill != +0.0f ? Float.floatToIntBits(fill) : 0);
        result = 31 * result + innerColor.hashCode();
        result = 31 * result + borderColor.hashCode();
        result = 31 * result + (secondFill != +0.0f ? Float.floatToIntBits(secondFill) : 0);
        result = 31 * result + text.hashCode();
        result = 31 * result + number;
        return result;
    }
}
