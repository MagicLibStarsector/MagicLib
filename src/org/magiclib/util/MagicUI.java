package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.ui.StatusBarData;

import java.awt.*;
import java.util.List;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * Draws a small UI charge-bar/tick box next to the normal ship-system for special systems.
 *
 * @author Dark.Revenant, Tartiflette, LazyWizard, Snrasha
 */
public class MagicUI {
    //Color of the HUD when the ship is alive or the hud
    public static final Color GREENCOLOR;
    //Color of the HUD when the ship is not alive.
    public static final Color BLUCOLOR;
    //Color of the HUD for the red color.
    public static final Color REDCOLOR;
    public static DrawableString TODRAW14;
    public static DrawableString TODRAW10;

    public static final Vector2f SYSTEMBARVEC1 = new Vector2f(0, 0);
    public static final Vector2f SYSTEMBARVEC2 = new Vector2f(29f, 58f);
    public static final Vector2f PERCENTBARVEC1 = new Vector2f(21f, 0f); // Just 21 pixel of width of difference.
    public static final Vector2f PERCENTBARVEC2 = new Vector2f(50f, 58f);
    public static final Vector2f PERCENTBARPAD = new Vector2f(0, 14f);

    public static final float UI_SCALING = Global.getSettings().getScreenScaleMult();

    private static final Map<String, StatusBarData> statusBars = new LinkedHashMap<>();
    private static Color lastColor;
    private static float lastUpdate;
    private static long dialogTime = 0;
    private static long commandTime = 0;
    private static long hudTime = 0;


    static {
        GREENCOLOR = Global.getSettings().getColor("textFriendColor");
        BLUCOLOR = Global.getSettings().getColor("textNeutralColor");
        REDCOLOR = Global.getSettings().getColor("textEnemyColor");

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();

            fontdraw = LazyFont.loadFont("graphics/fonts/victor10.fnt");
            TODRAW10 = fontdraw.createText();

            if (UI_SCALING != 1) {
                TODRAW14.setFontSize(TODRAW14.getFontSize() * UI_SCALING);
                TODRAW10.setFontSize(TODRAW10.getFontSize() * UI_SCALING);
            }
        } catch (FontException ex) {
        }
    }

    public static float getTextWidth(String text) {
        String oldText = TODRAW14.getText();
        TODRAW14.setText(text);
        float width = TODRAW14.getWidth();
        TODRAW14.setText(oldText);
        return width;
    }

    /**
     * Gets unscaled text width by unscaling the Victor14 font and then rescaling it.
     * @param text text to measure
     * @return unscaled width
     */
    public static float getTextWidthUnscaled(String text) {
        float fontSize = TODRAW14.getFontSize();
        String oldText = TODRAW14.getText();
        TODRAW14.setFontSize(14);
        TODRAW14.setText(text);
        float width = TODRAW14.getWidth();
        TODRAW14.setFontSize(fontSize);
        TODRAW14.setText(oldText);
        return width;
    }

    public static void setTextAligned(LazyFont.TextAlignment alignment) {
        TODRAW14.setAlignment(alignment);
    }

    /**
     * Scales the vector by the UI_SCALING value. This modifies and returns the destination vector.
     * If the destination vector is null, a new Vector is returned with the scaled source.
     * The source vector is not modified unless it is also provided as the dest argument.
     * @param source provided vector
     * @param dest destination vector
     * @return dest vector as a scaled copy of source
     */
    public static Vector2f scale(Vector2f source, Vector2f dest) {
        if (dest == null) {
            dest = new Vector2f();
        }

        dest.set(source.x * UI_SCALING, source.y * UI_SCALING);

        return dest;
    }

    /**
     * Scales the vector by the UI_SCALING value. This modifies and returns the provided vector.
     * If UI_SCALING is not modified (1), then no change is made to the source vector.
     * @param source provided vector
     * @return provided vector scaled
     */
    public static Vector2f scale(Vector2f source) {
        return scale(source, source);
    }

    /**
     * Multiplies the float by the UI_SCALING value.
     * @param source value
     * @return scaled float
     */
    public static float scale(float source) {
        return source * UI_SCALING;
    }

    /**
     * Scales two parameters by UI_SCALING into a Vector2f.
     * @param x x
     * @param y y
     * @return scaled vector
     */
    public static Vector2f scaledVector(float x, float y) {
        return new Vector2f(x * UI_SCALING, y * UI_SCALING);
    }

    /**
     * Copies a vector and scales it by UI_SCALING.
     * @param vec vector
     * @return scaled vector
     */
    public static Vector2f scaledCopy(Vector2f vec) {
        return MagicUI.scale(new Vector2f(vec));
    }

    public static boolean shouldDrawHUD(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine.getCombatUI() != null && !engine.getCombatUI().isShowingCommandUI() && engine.isUIShowingHUD() && !engine.isUIShowingDialog() && ship.equals(engine.getPlayerShip());
    }

    ///////////////////////////////////
    //                               //
    //          UI WIDGET            //
    //                               //
    ///////////////////////////////////

    /**
     * Draws a small UI bar next to the ship's system
     * <pre>
     *     MagicUI.drawSystemBar(
     *         ship,
     *         new Color(255,0,0),
     *         shieldTime/MAX_SHIELD_TIME,
     *         0
     * );
     * </pre>
     * <img src="https://static.wikia.nocookie.net/starfarergame/images/2/28/MagicLib_UI.gif/revision/latest?cb=20181114073410"  />
     *
     * @param ship          Ship concerned (the element will only be drawn if that ship
     *                      is the player ship)
     * @param intendedColor Color of the filling. If null, the filling will be
     *                      UI-green
     * @param fill          Filling level
     * @param blendTime     Time to smoothly switch between colors. Can be set to 0
     *                      for instant changes
     */
    public static void drawSystemBar(ShipAPI ship, Color intendedColor, float fill, float blendTime) {
        if (!ship.isAlive() || ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f boxLoc = Vector2f.add(
                new Vector2f(497f, 80f),
                getInterfaceOffsetFromSystemBar(ship, ship.getVariant()),
                null
        );

        drawSystemBar(ship, boxLoc, intendedColor, fill, blendTime);
    }

    /**
     * Draws a small UI bar at a given location.
     *
     * <img src="https://static.wikia.nocookie.net/starfarergame/images/2/28/MagicLib_UI.gif/revision/latest?cb=20181114073410"  />
     *
     * @param ship          Ship concerned (the element will only be drawn if that ship
     *                      is the player ship)
     * @param boxLoc        Where to draw the bar.
     * @param intendedColor Color of the filling. If null, the filling will be
     *                      UI-green
     * @param fill          Filling level
     * @param blendTime     Time to smoothly switch between colors. Can be set to 0
     *                      for instant changes
     */
    public static void drawSystemBar(ShipAPI ship, Vector2f boxLoc, Color intendedColor, float fill, float blendTime) {

        if (!ship.isAlive() || ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f shadowLoc = Vector2f.add(new Vector2f(1, -1), boxLoc, null);

        if (UI_SCALING != 1) {
            boxLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
        }

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        final float boxWidth = 27f * UI_SCALING;
        final float boxHeight = 7f * UI_SCALING;

        // Used to properly interpolate between colors
        final CombatEngineAPI engine = Global.getCombatEngine();
        final float elapsed = engine.getTotalElapsedTime(true);

        float alpha = getUIAlpha();

        // Calculate what color to use
        Color actualColor;
        if (blendTime > 0) {

            Color targetColor;
            if (intendedColor == null) {
                targetColor = GREENCOLOR;
            } else {
                targetColor = intendedColor;
            }

            if (lastUpdate > elapsed) {
                lastUpdate = elapsed;
            }
            float progress = (elapsed - lastUpdate) / blendTime;
            if (lastColor == null || lastColor == targetColor || progress > 1f) {
                lastColor = targetColor;
                actualColor = lastColor;
                lastUpdate = elapsed;
            } else {
                actualColor = Misc.interpolateColor(lastColor, targetColor, progress);
            }
        } else {
            if (intendedColor == null) {
                actualColor = GREENCOLOR;
            } else {
                actualColor = intendedColor;
            }
        }

        // Set OpenGL flags
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        // Render the drop shadow
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(shadowLoc.x - 1f, shadowLoc.y - 1f);
        GL11.glVertex2f(shadowLoc.x + boxWidth + 1f, shadowLoc.y - 1f);
        GL11.glVertex2f(shadowLoc.x + boxWidth + 1f, shadowLoc.y + boxHeight + 1f);
        GL11.glVertex2f(shadowLoc.x - 1f, shadowLoc.y + boxHeight + 1f);

        // Render the border transparency fix
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxWidth + 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxWidth + 1f, boxLoc.y + boxHeight + 1f);
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y + boxHeight + 1f);

        // Render the border
        GL11.glColor4f(GREENCOLOR.getRed() / 255f, GREENCOLOR.getGreen() / 255f, GREENCOLOR.getBlue() / 255f,
                alpha * (1 - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxWidth + 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxWidth + 1f, boxLoc.y + boxHeight + 1f);
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y + boxHeight + 1f);

        // Render the background
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxLoc.x, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x, boxLoc.y + boxHeight);
        GL11.glEnd();

        // Render the fill element
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(actualColor.getRed() / 255f, actualColor.getGreen() / 255f, actualColor.getBlue() / 255f,
                alpha * (actualColor.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxWidth * fill, boxLoc.y);
        GL11.glVertex2f(boxLoc.x, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x + boxWidth * fill, boxLoc.y + boxHeight);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    /**
     * Draws a small UI box next to the ship's system
     *
     * @param ship          Ship concerned (the element will only be drawn if that ship
     *                      is the player ship)
     * @param intendedColor Color of the filling. If null, the filling will be
     *                      UI-green
     * @param blendTime     Time to smoothly switch between colors. Can be set to 0
     *                      for instant changes
     */
    public static void drawSystemBox(ShipAPI ship, Color intendedColor, float blendTime) {
        if (!ship.isAlive() || ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f boxLoc = Vector2f.add(new Vector2f(497f, 80f),
                getInterfaceOffsetFromSystemBar(ship, ship.getVariant()), null);
        drawSystemBox(ship, boxLoc, intendedColor, blendTime);
    }

    /**
     * Draws a small UI box at a given location.
     *
     * @param ship          Ship concerned (the element will only be drawn if that ship
     *                      is the player ship)
     * @param boxLoc        Where to draw the box.
     * @param intendedColor Color of the filling. If null, the filling will be
     *                      UI-green
     * @param blendTime     Time to smoothly switch between colors. Can be set to 0
     *                      for instant changes
     */
    public static void drawSystemBox(ShipAPI ship, Vector2f boxLoc, Color intendedColor, float blendTime) {
        if (!ship.isAlive() || ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f shadowLoc = Vector2f.add(new Vector2f(1, -1), boxLoc, null);

        if (UI_SCALING != 1) {
            boxLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
        }

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());
        final float boxSide = 7f * UI_SCALING;

        // Used to properly interpolate between colors
        final CombatEngineAPI engine = Global.getCombatEngine();
        final float elapsed = engine.getTotalElapsedTime(true);

        float alpha = getUIAlpha();

        // Calculate what color to use
        Color actualColor;
        if (blendTime > 0) {

            Color targetColor;
            if (intendedColor == null) {
                targetColor = GREENCOLOR;
            } else {
                targetColor = intendedColor;
            }

            if (lastUpdate > elapsed) {
                lastUpdate = elapsed;
            }
            float progress = (elapsed - lastUpdate) / blendTime;
            if (lastColor == null || lastColor == targetColor || progress > 1f) {
                lastColor = targetColor;
                actualColor = lastColor;
                lastUpdate = elapsed;
            } else {
                actualColor = Misc.interpolateColor(lastColor, targetColor, progress);
            }
        } else {
            if (intendedColor == null) {
                actualColor = GREENCOLOR;
            } else {
                actualColor = intendedColor;
            }
        }

        // Set OpenGL flags
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        // Render the drop shadow
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(shadowLoc.x - 1f, shadowLoc.y - 1f);
        GL11.glVertex2f(shadowLoc.x + boxSide + 1f, shadowLoc.y - 1f);
        GL11.glVertex2f(shadowLoc.x + boxSide + 1f, shadowLoc.y + boxSide + 1f);
        GL11.glVertex2f(shadowLoc.x - 1f, shadowLoc.y + boxSide + 1f);

        // Render the border transparency fix
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxSide + 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxSide + 1f, boxLoc.y + boxSide + 1f);
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y + boxSide + 1f);

        // Render the border
        GL11.glColor4f(GREENCOLOR.getRed() / 255f, GREENCOLOR.getGreen() / 255f, GREENCOLOR.getBlue() / 255f,
                alpha * (1 - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxSide + 1f, boxLoc.y - 1f);
        GL11.glVertex2f(boxLoc.x + boxSide + 1f, boxLoc.y + boxSide + 1f);
        GL11.glVertex2f(boxLoc.x - 1f, boxLoc.y + boxSide + 1f);

        // Render the background
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxLoc.x, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxSide, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxSide, boxLoc.y + boxSide);
        GL11.glVertex2f(boxLoc.x, boxLoc.y + boxSide);
        GL11.glEnd();

        // Render the fill element
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(actualColor.getRed() / 255f, actualColor.getGreen() / 255f, actualColor.getBlue() / 255f,
                alpha * (actualColor.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxSide, boxLoc.y);
        GL11.glVertex2f(boxLoc.x, boxLoc.y + boxSide);
        GL11.glVertex2f(boxLoc.x + boxSide, boxLoc.y + boxSide);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    ///////////////////////////////////
    //                               //
    //          STATUS BAR           //
    //                               //
    ///////////////////////////////////

    /**
     * Draw a third status bar above the Flux and Hull ones on the User Interface.
     * With a text of the left and the number on the right.
     * This method automatically places the status bar above other ones if they are already being drawn.
     * This must be called with the same text argument every couple of seconds to remain active.
     * It's not recommended to change text when calling this method, as doing so will create another status bar.
     *
     * @param ship        Player ship.
     * @param fill        Filling level of the bar. 0 to 1
     * @param innerColor  Color of the bar. If null, the vanilla green UI color will be used.
     * @param borderColor Color of the border. If null, the vanilla green UI color will be used.
     * @param secondFill  Wider filling like the soft/hard-flux. 0 to 1.
     * @param text        The text written to the left, automatically cut if too large. Set to null to ignore
     * @param number      The number displayed on the right. Can go from 0 to 999 999. Set to <0 value to ignore
     */
    public static void drawInterfaceStatusBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        drawInterfaceStatusBar(ship, text, fill, innerColor, borderColor, secondFill, text, number);
    }

    /**
     * Draw a status bar at the desired position.
     * With a text of the left and the number on the right.
     * This method automatically places the status bar above other ones if they are already being drawn.
     * This must be called with the same text argument every couple of seconds to remain active.
     * It's not recommended to change text when calling this method, as doing so will create another status bar.
     *
     * @param ship         Player ship.
     * @param statusBarLoc Where to draw the status bar.
     * @param fill         Filling level of the bar. 0 to 1
     * @param innerColor   Color of the bar. If null, the vanilla green UI color will be used.
     * @param borderColor  Color of the border. If null, the vanilla green UI color will be used.
     * @param secondFill   Wider filling like the soft/hard-flux. 0 to 1.
     * @param text         The text written to the left, automatically cut if too large. Set to null to ignore
     * @param number       The number displayed on the right. Can go from 0 to 999 999. Set to <0 value to ignore
     */
    public static void drawInterfaceStatusBar(ShipAPI ship, Vector2f statusBarLoc, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        drawInterfaceStatusBar(ship, text, statusBarLoc, fill, innerColor, borderColor, secondFill, text, number);
    }

    /**
     * Draw a status bar above the Flux and Hull ones on the User Interface.
     * With a text of the left and the number on the right.
     * This method automatically places the status bar above other ones if they are already being drawn.
     * This must be called every couple of seconds to remain active.
     * This method's arguments explicitly define key, allowing for the text to change without generating another
     * status bar.
     *
     * @param ship        Player ship.
     * @param key         Key for the status. Should be unique to your mod and whatever this status bar is displaying.
     * @param fill        Filling level of the bar. 0 to 1
     * @param innerColor  Color of the bar. If null, the vanilla green UI color will be used.
     * @param borderColor Color of the border. If null, the vanilla green UI color will be used.
     * @param secondFill  Wider filling like the soft/hard-flux. 0 to 1.
     * @param text        The text written to the left, automatically cut if too large. Set to null to ignore
     * @param number      The number displayed on the right. Can go from 0 to 999 999. Set to <0 value to ignore
     */
    public static void drawInterfaceStatusBar(ShipAPI ship, String key, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        if (statusBars.containsKey(key)) {
            StatusBarData data = statusBars.get(key);
            data.setData(ship, fill, innerColor, borderColor, secondFill, text, number);
            //refreshes the cleaning interval for this status bar as well.
        } else {
            StatusBarData data = new StatusBarData(ship, fill, innerColor, borderColor, secondFill, text, number);
            statusBars.put(key, data);
        }
    }

    /**
     * Draw a status bar at the desired position.
     * With a text of the left and the number on the right.
     * This method automatically places the status bar above other ones if they are already being drawn.
     * This must be called every couple of seconds to remain active.
     * This method's arguments explicitly define key, allowing for the text to change without generating another
     * status bar.
     *
     * @param ship         Player ship.
     * @param key          Key for the status. Should be unique to your mod and whatever this status bar is displaying.
     * @param statusBarLoc Where to draw the status bar.
     * @param fill         Filling level of the bar. 0 to 1
     * @param innerColor   Color of the bar. If null, the vanilla green UI color will be used.
     * @param borderColor  Color of the border. If null, the vanilla green UI color will be used.
     * @param secondFill   Wider filling like the soft/hard-flux. 0 to 1.
     * @param text         The text written to the left, automatically cut if too large. Set to null to ignore
     * @param number       The number displayed on the right. Can go from 0 to 999 999. Set to <0 value to ignore
     */
    public static void drawInterfaceStatusBar(ShipAPI ship, String key, Vector2f statusBarLoc, float fill, Color innerColor, Color borderColor, float secondFill, String text, int number) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        if (statusBars.containsKey(key)) {
            StatusBarData data = statusBars.get(key);
            data.setOverridePos(statusBarLoc);
            data.setData(ship, fill, innerColor, borderColor, secondFill, text, number);
            //refreshes the cleaning interval for this status bar as well.
        } else {
            StatusBarData data = new StatusBarData(ship, fill, innerColor, borderColor, secondFill, text, number);
            data.setOverridePos(statusBarLoc);
            statusBars.put(key, data);
        }
    }

    /**
     * Draws the status bar map.
     */
    static void drawStatusBarMap() {
        ShipAPI playerShip = Global.getCombatEngine().getPlayerShip();
        if (playerShip != null) {
            Vector2f defaultStatusLocOffset = new Vector2f(0f, 0f);
            Iterator<StatusBarData> dataIterator = statusBars.values().iterator();
            while (dataIterator.hasNext()) {
                StatusBarData data = dataIterator.next();

                //clear status bars that have not been updated or are not for the current ship
                if (data.getShip() != playerShip
                        || Global.getCombatEngine().getTotalElapsedTime(true) - data.getLastRefreshed() >= 5f) {
                    dataIterator.remove();
                    continue;
                }

                data.drawToScreen(defaultStatusLocOffset);

                //do not apply offset if data has its own render position set.
                if (data.getOverridePos() == null) {
                    defaultStatusLocOffset = Vector2f.add(PERCENTBARPAD, defaultStatusLocOffset, null);
                }
            }
        }
    }

    /**
     * Draw a status bar next to the player ship on the top left corner of the hud
     * Can write two bits of text on its left side
     *
     * @param ship        Player ship.
     * @param fill        Filling level of the bar. 0 to 1
     * @param innerColor  Color of the bar. If null, the vanilla green UI color will be used.
     * @param borderColor Color of the border. If null, the vanilla green UI color will be used.
     * @param secondfill  Wider filling like the soft/hard-flux. 0 to 1.
     * @param bottext     Write a text just on the left of the bar. Example: 'flux'. Set to null to ignore
     * @param toptext     Write a text just above the bar. Example: 'player' or 'target'. Set to null to ignore
     * @param offset      Offsets the bar a few pixels upward.
     *                    Can be used for example to display the targeted enemy special status bar.
     */
    public static void drawHUDStatusBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondfill, String bottext, String toptext, boolean offset) {
        Vector2f pos = ship.getLocation();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.getViewport() == null) {
            return;
        }
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        Vector2f pos2 = new Vector2f((int) Global.getCombatEngine().getViewport().convertWorldXtoScreenX(pos.getX()), (int) Global.getCombatEngine().getViewport().convertWorldYtoScreenY(pos.getY()));
        if (offset) {
            pos2.translate(0f, 16f);
        }
        addHUDStatusBar(ship, fill, innerColor, borderColor, secondfill, pos2);
        if (TODRAW10 != null) {
            if (bottext != null && !bottext.isEmpty()) {
                int pixelleft = (int) TODRAW10.getWidth();
                pos2.translate(-pixelleft - 5f, 8f);
                addStatusText(ship, bottext, innerColor, pos2);
            }
            if (toptext != null && !toptext.isEmpty()) {
                pos2.translate(0, 8f);
                addStatusText(ship, toptext, innerColor, pos2);
            }
        }
    }

    ///// UTILS /////

//    private static Color interpolateColor(Color old, Color dest, float progress) {
//        final float clampedProgress = Math.max(0f, Math.min(1f, progress));
//        final float antiProgress = 1f - clampedProgress;
//        final float[] ccOld = old.getComponents(null), ccNew = dest.getComponents(null);
//        return new Color((ccOld[0] * antiProgress) + (ccNew[0] * clampedProgress),
//                (ccOld[1] * antiProgress) + (ccNew[1] * clampedProgress),
//                (ccOld[2] * antiProgress) + (ccNew[2] * clampedProgress),
//                (ccOld[3] * antiProgress) + (ccNew[3] * clampedProgress));
//    }

    /**
     * Get the UI Element Offset for the Shipsystem bar. (Depends if the group
     * layout, or if the player has some wing)
     *
     * @param ship    The player ship.
     * @param variant The variant of the ship.
     * @return The offset who depends of weapon and wing.
     */
    public static Vector2f getInterfaceOffsetFromSystemBar(ShipAPI ship, ShipVariantAPI variant) {
        return getUIElementOffset(ship, variant, SYSTEMBARVEC1, SYSTEMBARVEC2);
    }

    /**
     * Get the UI Element Offset for the Third bar. (Depends of the group
     * layout, or if the player has some wing)
     *
     * @param ship    The player ship.
     * @param variant The variant of the ship.
     * @return The offset who depends of weapon and wing.
     */
    public static Vector2f getInterfaceOffsetFromStatusBars(ShipAPI ship, ShipVariantAPI variant) {
        Vector2f offset = getUIElementOffset(ship, variant, PERCENTBARVEC1, PERCENTBARVEC2);

        if (ship.getPhaseCloak() != null && !ship.getHullSpec().isPhase()) {
            offset = Vector2f.add(new Vector2f(0f, 14f), offset, null);
        }

        if (Global.getCombatEngine() != null && Global.getCombatEngine().getCombatUI().isStrafeToggledOn()) {
            offset = Vector2f.add(new Vector2f(0f, 14f), offset, null);
        }

        return offset;
    }

    /**
     * Get the UI Element Offset.
     * (Depends on the weapon groups and wings present)
     *
     * @param ship    The player ship.
     * @param variant The variant of the ship.
     * @param vec1    Vector2f used if there are no wing and less than 2 weapon groups.
     * @param vec2    Vector2f used if there are wings but less than 2 weapon groups.
     * @return the offset.
     */
    public static Vector2f getUIElementOffset(ShipAPI ship, ShipVariantAPI variant, Vector2f vec1, Vector2f vec2) {
        int numEntries = 0;
        final List<WeaponGroupSpec> weaponGroups = variant.getWeaponGroups();
        final List<WeaponAPI> usableWeapons = ship.getUsableWeapons();
        for (WeaponGroupSpec group : weaponGroups) {
            final Set<String> uniqueWeapons = new HashSet<>(group.getSlots().size());
            for (String slot : group.getSlots()) {
                boolean isUsable = false;
                for (WeaponAPI weapon : usableWeapons) {
                    if (weapon.getSlot().getId().contentEquals(slot)) {
                        isUsable = true;
                        break;
                    }
                }
                if (!isUsable) {
                    continue;
                }
                String id = Global.getSettings().getWeaponSpec(variant.getWeaponId(slot)).getWeaponName();
                if (id != null) {
                    uniqueWeapons.add(id);
                }
            }
            numEntries += uniqueWeapons.size();
        }
        if (variant.getFittedWings().isEmpty()) {
            if (numEntries < 2) {
                return vec1;
            }
            return new Vector2f(30f + ((numEntries - 2) * 13f), 18f + ((numEntries - 2) * 26f));
        } else {
            if (numEntries < 2) {
                return vec2;
            }
            return new Vector2f(59f + ((numEntries - 2) * 13f), 76f + ((numEntries - 2) * 26f));
        }
    }

    /**
     * Draws a small UI bar above the flux bar. The HUD color change to blue
     * when the ship is not alive. Bug: When you left the battle, the hud
     * keep for qew second, no solution found. Bug: Also for other
     * normal drawBox, when paused, they switch brutally of "color".
     *
     * @param ship        Ship concerned (the element will only be drawn if that ship
     *                    is the player ship)
     * @param boxLoc      Where to draw the status bar.
     * @param fill        Filling level
     * @param innerColor  Color of the bar. If null, use the vanilla HUD color.
     * @param borderColor Color of the border. If null, use the vanilla HUD
     *                    color.
     * @param secondfill  Like the hardflux of the fluxbar. 0 per default.
     */
    public static void addInterfaceStatusBar(ShipAPI ship, Vector2f boxLoc, float fill, Color innerColor, Color borderColor, float secondfill) {

        final float boxWidth = 79 * UI_SCALING;
        final float boxHeight = 7 * UI_SCALING;
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(1, -1), boxLoc, null);
        if (UI_SCALING != 1) {
            boxLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
        }

        float alpha = getUIAlpha();

        Color innerCol = innerColor == null ? GREENCOLOR : innerColor;
        Color borderCol = borderColor == null ? GREENCOLOR : borderColor;
        if (!ship.isAlive()) {
            innerCol = BLUCOLOR;
            borderCol = BLUCOLOR;
        }
        float hardfill = secondfill < 0 ? 0 : secondfill;
        hardfill = hardfill > 1 ? 1 : hardfill;
        if (hardfill >= fill) {
            hardfill = fill;
        }
        int pixelHardfill = (int) (boxWidth * hardfill);
        pixelHardfill = pixelHardfill <= 3 ? -pixelHardfill : -3;

        int hfboxWidth = (int) (boxWidth * hardfill);
        int fboxWidth = (int) (boxWidth * fill);

        OpenGLBar(ship, alpha, borderCol, innerCol, fboxWidth, hfboxWidth, boxHeight, boxWidth, pixelHardfill, shadowLoc, boxLoc, true);
    }

    /**
     * Draw the text with the font victor14.
     *
     * @param ship    The player ship
     * @param text    The text to write.
     * @param textLoc Where to draw the text.
     */
    public static void addInterfaceStatusText(ShipAPI ship, String text, Vector2f textLoc) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }
        Color borderCol = GREENCOLOR;
        if (!ship.isAlive()) {
            borderCol = BLUCOLOR;
        }
        float alpha = getUIAlpha();

        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));


        final Vector2f shadowLoc = Vector2f.add(new Vector2f(1, -1),
                textLoc, null);

        if (UI_SCALING != 1) {
            textLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
            TODRAW14.setFontSize(14 * UI_SCALING);
        }

        openGL11ForText();
        TODRAW14.setText(text);
        TODRAW14.setMaxWidth(46 * UI_SCALING);
        TODRAW14.setMaxHeight(14 * UI_SCALING);
        TODRAW14.setBaseColor(shadowcolor);
        TODRAW14.draw(shadowLoc);
        TODRAW14.setBaseColor(color);
        TODRAW14.draw(textLoc);
        closeGL11ForText();

    }

    /**
     * Draw number at the right of the percent bar.
     *
     * @param ship   The player ship died or alive.
     * @param number The number displayed, bounded per the method to 0 at 999
     *               999.
     * @param numberPos where to draw the number
     */
    public static void addInterfaceStatusNumber(ShipAPI ship, int number, Vector2f numberPos) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        if (!Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }
        int numb = number;
        if (numb > 999999) {
            numb = 999999;
        }
        if (numb < 0) {
            numb = 0;
        }

        Color borderCol = GREENCOLOR;
        if (!ship.isAlive()) {
            borderCol = BLUCOLOR;
        }
        float alpha = getUIAlpha();

        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));


        final Vector2f shadowLoc = Vector2f.add(new Vector2f(1, -1), numberPos, null);
        if (UI_SCALING != 1) {
            numberPos.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
            TODRAW14.setFontSize(14 * UI_SCALING);
        }

        openGL11ForText();
        TODRAW14.setText(numb + "");
        float width = TODRAW14.getWidth() - 1;
        TODRAW14.setBaseColor(shadowcolor);
        TODRAW14.draw(shadowLoc.x - width, shadowLoc.y);
        TODRAW14.setBaseColor(color);
        TODRAW14.draw(numberPos.x - width, numberPos.y);
        closeGL11ForText();
    }

    /**
     * Draws a small UI bar above the flux bar. The HUD color change to blu when
     * the ship is not alive. //TODO: Bug: When you left the battle, the hud
     * keep for qew second, no solution found. //TODO: Bug: Also for other
     * normal drawBox, when paused, they switch brutally of "color".
     *
     * @param ship        Ship concerned (the element will only be drawn if that ship
     *                    is the player ship)
     * @param fill        Filling level
     * @param innerColor  Color of the bar. If null, use the vanilla HUD color.
     * @param borderColor Color of the border. If null, use the vanilla HUD
     *                    color.
     * @param secondfill  Like the hardflux of the fluxbar. 0 per default.
     * @param screenPos   The position on the Screen.
     */
    private static void addHUDStatusBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondfill, Vector2f screenPos) {

        final float boxWidth = 59 * UI_SCALING;
        final float boxHeight = 5 * UI_SCALING;

        final Vector2f element = getHUDOffset(ship);
        final Vector2f boxLoc = Vector2f.add(new Vector2f(screenPos.getX(), screenPos.getY()), element, null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(screenPos.getX() + 1f, screenPos.getY() - 1f), element, null);
        boxLoc.scale(UI_SCALING);
        shadowLoc.scale(UI_SCALING);

        float alpha = getUIAlpha();

        Color innerCol = innerColor == null ? GREENCOLOR : innerColor;
        Color borderCol = borderColor == null ? GREENCOLOR : borderColor;
        if (!ship.isAlive()) {
            innerCol = BLUCOLOR;
            borderCol = BLUCOLOR;
        }
        int pixelHardfill = 0;
        float hardfill = secondfill < 0 ? 0 : secondfill;
        hardfill = hardfill > 1 ? 1 : hardfill;
        if (hardfill >= fill) {
            hardfill = fill;
        }
        pixelHardfill = (int) (boxWidth * hardfill);
        pixelHardfill = pixelHardfill <= 3 ? -pixelHardfill : -3;

        int hfboxWidth = (int) (boxWidth * hardfill);
        int fboxWidth = (int) (boxWidth * fill);

        OpenGLBar(ship, alpha, borderCol, innerCol, fboxWidth, hfboxWidth, boxHeight, boxWidth, pixelHardfill, shadowLoc, boxLoc, true);

    }

    /**
     * Calculates a position above the selected weapon group in the HUD.
     * @param ship ship
     * @return position above the selected weapons in the HUD
     */
    public static Vector2f getHUDRightOffset(ShipAPI ship) {
        WeaponGroupAPI selectedGroup = ship.getSelectedGroupAPI();

        float weaponBarHeight = 0f;
        if (selectedGroup != null && selectedGroup.getWeaponsCopy() != null)
            weaponBarHeight = selectedGroup.getWeaponsCopy().size() * 12f;

        Vector2f baseBarLoc = new Vector2f(529f, 40f + weaponBarHeight);
        Vector2f shipOffset = getUIElementOffset(ship, ship.getVariant(), PERCENTBARVEC1, PERCENTBARVEC2);
        Vector2f adjustedLoc = Vector2f.add(baseBarLoc, shipOffset, null);
        return adjustedLoc;
    }

    /**
     * Draws a UI bar. The HUD color changes to blue when the ship is dead.
     *
     * @param ship        Ship concerned (the element will only be drawn if that ship
     *                    is the player ship)
     * @param fill        Filling level
     * @param innerColor  Color of the bar. If null, use the vanilla HUD color.
     * @param borderColor Color of the border. If null, use the vanilla HUD
     *                    color.
     * @param secondFill  Like the hard flux of the flux bar. 0 per default.
     * @param screenPos   The position on the Screen.
     */
    public static void addBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondFill, Vector2f screenPos) {
        addBar(ship, fill, innerColor, borderColor, secondFill, screenPos, 6 * UI_SCALING, 59 * UI_SCALING, true);
    }

    /**
     * Draws a UI bar. The HUD color changes to blue when the ship is dead.
     *
     * @param ship        Ship concerned (the element will only be drawn if that ship
     *                    is the player ship)
     * @param fill        Filling level
     * @param innerColor  Color of the bar. If null, use the vanilla HUD color.
     * @param borderColor Color of the border. If null, use the vanilla HUD
     *                    color.
     * @param secondFill  Like the hardflux of the fluxbar. 0 per default.
     * @param screenPos   The position on the Screen.
     * @param boxHeight   Height of the drawn box.
     * @param boxWidth    Width of the drawn box.
     * @param drawBorders whether to draw the borders of the bar. default is false.
     */
    public static void addBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondFill, Vector2f screenPos, float boxHeight, float boxWidth, boolean drawBorders) {
        final Vector2f boxLoc = new Vector2f(screenPos);
        final Vector2f shadowLoc = new Vector2f(boxLoc.getX() + 1f, boxLoc.getY() - 1f);
        boxLoc.scale(UI_SCALING);
        shadowLoc.scale(UI_SCALING);

        // Used to properly interpolate between colors
        float alpha = 1f;

        Color innerCol = innerColor == null ? GREENCOLOR : innerColor;
        Color borderCol = borderColor == null ? GREENCOLOR : borderColor;
        if (!ship.isAlive()) {
            innerCol = BLUCOLOR;
            borderCol = BLUCOLOR;
        }
        int pixelHardfill = 0;
        float hardfill = secondFill < 0 ? 0 : secondFill;
        hardfill = hardfill > 1 ? 1 : hardfill;
        if (hardfill >= fill) {
            hardfill = fill;
        }
        pixelHardfill = (int) (boxWidth * hardfill);
        pixelHardfill = pixelHardfill <= 3 ? -pixelHardfill : -3;

        int hfboxWidth = (int) (boxWidth * hardfill);
        int fboxWidth = (int) (boxWidth * fill);

        OpenGLBar(ship, alpha, borderCol, innerCol, fboxWidth, hfboxWidth, boxHeight, boxWidth, pixelHardfill, shadowLoc, boxLoc, drawBorders);
    }

    /**
     * Draw text with the font Victor14 where you want on the screen.
     *
     * @param ship      The player ship. Only used to change UI text color if it is dead. Can be null.
     * @param text      Text to display.
     * @param textColor The color of the text. Alpha will be used for both the black background and foreground color.
     * @param screenPos The position on the Screen. Should not be scaled to UI Scaling.
     * @param openGLStack whether to open an OpenGL11 stack for the text
     */
    public static void addText(ShipAPI ship, String text, Color textColor, Vector2f screenPos, boolean openGLStack) {
        Color borderCol = textColor == null ? GREENCOLOR : textColor;
        if (ship != null && !ship.isAlive()) {
            borderCol = BLUCOLOR;
        }

        float alpha = getUIAlpha() * (borderCol.getAlpha() / 255f) * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());

        Color shadowColor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f, alpha);
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f, alpha);

        final Vector2f boxLoc = new Vector2f(screenPos);
        final Vector2f shadowLoc = new Vector2f(screenPos.getX() + 1f, screenPos.getY() - 1f);

        if (UI_SCALING != 1) {
            boxLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
        }

        if (openGLStack) {
            openGL11ForText();
        }

        TODRAW14.setMaxWidth(4600 * UI_SCALING);
        TODRAW14.setMaxHeight(14 * UI_SCALING);
        TODRAW14.setText(text);
        TODRAW14.setBaseColor(shadowColor);
        TODRAW14.draw(shadowLoc);
        TODRAW14.setBaseColor(color);
        TODRAW14.draw(boxLoc);

        if (openGLStack) {
            closeGL11ForText();
        }
    }


    /**
     * Draw text with the font Victor14 where you want on the screen. This method will not apply UI_SCALING and so
     * assumes that the screenPos is already scaled.
     *
     * @param ship      The player ship. Only used to change UI text color if it is dead. Can be null.
     * @param text      Text to display.
     * @param textColor The color of the text. Alpha will be used for both the black background and foreground color.
     * @param screenPos The position on the Screen. Should not be scaled to UI Scaling.
     * @param openGLStack whether to open an OpenGL11 stack for the text
     */
    public static void addTextNonScaling(ShipAPI ship, String text, Color textColor, Vector2f screenPos, boolean openGLStack) {
        Color borderCol = textColor == null ? GREENCOLOR : textColor;
        if (ship != null && !ship.isAlive()) {
            borderCol = BLUCOLOR;
        }

        float alpha = getUIAlpha() * (borderCol.getAlpha() / 255f) * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());

        Color shadowColor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f, alpha);
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f, alpha);

        final Vector2f boxLoc = new Vector2f(screenPos);
        final Vector2f shadowLoc = new Vector2f(screenPos.getX() + MagicUI.UI_SCALING, screenPos.getY() - MagicUI.UI_SCALING);

        if (openGLStack) {
            openGL11ForText();
        }

        TODRAW14.setMaxWidth(4600 * UI_SCALING);
        TODRAW14.setMaxHeight(14 * UI_SCALING);
        TODRAW14.setText(text);
        TODRAW14.setBaseColor(shadowColor);
        TODRAW14.draw(shadowLoc);
        TODRAW14.setBaseColor(color);
        TODRAW14.draw(boxLoc);

        if (openGLStack) {
            closeGL11ForText();
        }
    }

    /**
     * Draw text with the font victor10 where you want on the screen.
     *
     * @param ship      The player ship.
     * @param text      Text to display.
     * @param textColor The color of the text
     * @param screenPos The position on the Screen.
     */
    public static void addStatusText(ShipAPI ship, String text, Color textColor, Vector2f screenPos) {
        Color borderCol = textColor == null ? GREENCOLOR : textColor;
        if (!ship.isAlive()) {
            borderCol = BLUCOLOR;
        }

        float alpha = getUIAlpha();

        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));

        final Vector2f boxLoc = Vector2f.add(new Vector2f(screenPos.getX(), screenPos.getY()),
                getHUDOffset(ship), null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(screenPos.getX() + 1f, screenPos.getY() - 1f),
                getHUDOffset(ship), null);
        if (UI_SCALING != 1) {
            boxLoc.scale(UI_SCALING);
            shadowLoc.scale(UI_SCALING);
            TODRAW10.setFontSize(10 * UI_SCALING);
        }

        openGL11ForText();
        TODRAW10.setText(text);
        TODRAW10.setBaseColor(shadowcolor);
        TODRAW10.draw(shadowLoc);
        TODRAW10.setBaseColor(color);
        TODRAW10.draw(boxLoc);
        closeGL11ForText();
    }

    private static void OpenGLBar(ShipAPI ship, float alpha, Color borderCol, Color innerCol, int fboxWidth, int hfboxWidth, float boxHeight, float boxWidth, int pixelHardfill, Vector2f shadowLoc, Vector2f boxLoc, boolean drawBorders) {
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        // Set OpenGL flags
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        if (ship.isAlive()) {
            // Render the drop shadow
            if (fboxWidth != 0) {
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                        1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
                GL11.glVertex2f(shadowLoc.x - 1, shadowLoc.y);
                GL11.glVertex2f(shadowLoc.x + fboxWidth, shadowLoc.y);
                GL11.glVertex2f(shadowLoc.x - 1, shadowLoc.y + boxHeight + 1);
                GL11.glVertex2f(shadowLoc.x + fboxWidth, shadowLoc.y + boxHeight + 1);
                GL11.glEnd();
            }
        }


        if (drawBorders) {
            // Render the drop shadow of border.
            GL11.glBegin(GL11.GL_LINES);
            GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                    1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
            GL11.glVertex2f(shadowLoc.x + hfboxWidth + pixelHardfill, shadowLoc.y - 1);
            GL11.glVertex2f(shadowLoc.x + 3 + hfboxWidth + pixelHardfill, shadowLoc.y - 1);
            GL11.glVertex2f(shadowLoc.x + hfboxWidth + pixelHardfill, shadowLoc.y + boxHeight);
            GL11.glVertex2f(shadowLoc.x + 3 + hfboxWidth + pixelHardfill, shadowLoc.y + boxHeight);
            GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
            GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y + boxHeight);

            // Render the border transparency fix
            GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                    1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
            GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y - 1);
            GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y - 1);
            GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y);
            GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y + boxHeight);

            // Render the border
            GL11.glColor4f(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                    alpha * (1 - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
            GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y - 1);
            GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y - 1);
            GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y);
            GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y + boxHeight);
            GL11.glEnd();
        }

        // Render the fill element
        if (ship.isAlive()) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glColor4f(innerCol.getRed() / 255f, innerCol.getGreen() / 255f, innerCol.getBlue() / 255f,
                    alpha * (innerCol.getAlpha() / 255f)
                            * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
            GL11.glVertex2f(boxLoc.x, boxLoc.y);
            GL11.glVertex2f(boxLoc.x + fboxWidth, boxLoc.y);
            GL11.glVertex2f(boxLoc.x, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + fboxWidth, boxLoc.y + boxHeight);
            GL11.glEnd();
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

    }

    /**
     * Get the UI Element Offset for the player on the center. (Depends of the
     * collision radius and the zoom)
     *
     * @param ship The player ship.
     * @return The offset.
     */
    private static Vector2f getHUDOffset(ShipAPI ship) {
        ViewportAPI viewport = Global.getCombatEngine().getViewport();
        float mult = viewport.getViewMult();

        return new Vector2f((int) (-ship.getCollisionRadius() / mult),
                (int) (ship.getCollisionRadius() / mult));
    }

    /**
     * GL11 to start, when you want render text of Lazyfont.
     */
    public static void openGL11ForText() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * GL11 to close, when you want render text of Lazyfont.
     */
    public static void closeGL11ForText() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static float getUIAlpha() {
        final float DIALOG_ALPHA = 0.33f;
        final float DIALOG_FADE_OUT_TIME = 333f;
        final float DIALOG_FADE_IN_TIME = 250f;
        final float COMMAND_FADE_OUT_TIME = 1000f;
        final float COMMAND_FADE_IN_TIME = 111f;

        // Used to properly interpolate between UI fade colors
        float alpha;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            dialogTime = System.currentTimeMillis();
            alpha = Misc.interpolate(1f, DIALOG_ALPHA, Math.min((dialogTime - hudTime) / DIALOG_FADE_OUT_TIME, 1f));
        } else if (Global.getCombatEngine().getCombatUI().isShowingCommandUI()) {
            commandTime = System.currentTimeMillis();
            // does not work, MagicRenderPlugin (a BaseEveryFrameCombatPlugin) advance() does not allow drawing into the Command View.
            // renderInUICoords() is needed to do so, however a bunch of stuff breaks when moved there, so this stays broken for now.
            alpha = Misc.interpolate(1f, 0f, Math.min((commandTime - hudTime) / COMMAND_FADE_OUT_TIME, 1f));
        } else if (dialogTime > commandTime) {
            hudTime = System.currentTimeMillis();
            alpha = Misc.interpolate(DIALOG_ALPHA, 1f, Math.min((hudTime - dialogTime) / DIALOG_FADE_IN_TIME, 1f));
        } else {
            hudTime = System.currentTimeMillis();
            alpha = Misc.interpolate(0f, 1f, Math.min((hudTime - commandTime) / COMMAND_FADE_IN_TIME, 1f));
        }
        return MathUtils.clamp(alpha, 0f, 1f);
    }

    private static boolean VIEWPORT_OPENGL_CALLS = false;

    /**
     * Remember to UNSET THIS after your rendering.
     * Use only if you're rendering from an advance method.
     * @param set set it
     */
    public static void setViewportOpenGLCalls(boolean set) {
        VIEWPORT_OPENGL_CALLS = set;
    }

    /**
     * GL11 to start rendering LazyFont text.
     * Will do nothing if {@link MagicUI#setViewportOpenGLCalls(boolean)} is set to false.
     * Set that to false after your render calls if you set it to true.
     */
    public static void openGL11ForTextWithinViewport() {
        openGLForMiscWithinViewport();
    }

    /**
     * GL11 to end after rendering LazyFont text.
     * Will do nothing if {@link MagicUI#setViewportOpenGLCalls(boolean)} is set to false.
     * Set that to false after your render calls if you set it to true.
     */
    public static void closeGL11ForTextWithinViewport() {
        closeGLForMiscWithinViewport();
    }

    /**
     * Sets OpenGL state for rendering in HUD coordinates
     * Will do nothing if {@link MagicUI#setViewportOpenGLCalls(boolean)} is set to false.
     * Set that to false after your render calls if you set it to true.
     * @author tomatopaste
     */
    public static void openGLForMiscWithinViewport() {
        final int w = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int h = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glViewport(0, 0, w, h);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, w, 0, h, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTranslatef(0.01f, 0.01f, 0);
    }

    /**
     * Ends OpenGL state for rendering in HUD coordinates
     * Will do nothing if {@link MagicUI#setViewportOpenGLCalls(boolean)} is set to false.
     * Set that to false after your render calls if you set it to true.
     * @author tomatopaste
     */
    public static void closeGLForMiscWithinViewport() {
        glDisable(GL_BLEND);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }
}