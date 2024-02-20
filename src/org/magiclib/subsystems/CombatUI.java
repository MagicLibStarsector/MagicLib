package org.magiclib.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author tomatopaste
 * A few methods in the UTILS section were copied from MagicLib when they were private and inaccessible, in addition
 * to several static fields at the beginning of the class. References to these methods can probably be replaced with
 * references to the methods in the original MagicLib UI file.
 *
 * All other code where applicable:
 *
 * MIT License
 *
 * Copyright (c) 2024 tomatopaste
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class CombatUI {
    //Color of the HUD when the ship is alive or the hud
    public static final Color GREENCOLOR;
    //Color of the HUD when the ship is not alive.
    public static final Color BLUCOLOR;
    //Color of the HUD for the red colour.
    public static final Color REDCOLOR;
    private static DrawableString TODRAW14;

    private static final Vector2f PERCENTBARVEC1 = new Vector2f(21f, 0f); // Just 21 pixel of width of difference.
    private static final Vector2f PERCENTBARVEC2 = new Vector2f(50f, 58f);

    private static final float UIscaling = Global.getSettings().getScreenScaleMult();
    public static final float STATUS_BAR_WIDTH = 45f * UIscaling;
    public static final float STATUS_BAR_HEIGHT = 9f * UIscaling;
    public static final float STATUS_BAR_PADDING = 200f * UIscaling;
    public static final float BAR_HEIGHT = 13f * UIscaling;
    public static final float INFO_TEXT_PADDING = 20f * UIscaling;

    private static final String[] alphabet = new String[] {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
    };


    // Used to determine if the background sprite has been rendered for the spatial ship/drones graph
    public static boolean hasRendered = false;

    public static boolean getHasRendered() {
        return hasRendered;
    }

    static {
        GREENCOLOR = Global.getSettings().getColor("textFriendColor");
        BLUCOLOR = Global.getSettings().getColor("textNeutralColor");
        REDCOLOR = Global.getSettings().getColor("textEnemyColor");

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();

            if (UIscaling > 1f) { //mf
                TODRAW14.setFontSize(14f * UIscaling);
            }

        } catch (FontException ignored) {
        }
    }

    ///////////////////////////////////
    //                               //
    //         SUBSYSTEM GUI         //
    //                               //
    ///////////////////////////////////

    /**
     * Draws the status bar for a basic subsystem, imitating the shipsystem UI. If show info mode is enabled, it will
     * take up an additional UI slot.
     *
     * If the subsystem script attempts to render additional HUD elements (e.g. using the renderAuxiliaryStatusBar
     * method) then they will not have an effect on the output location. This must be predetermined by the input
     * guiBarCount parameter to make room for them.
     *
     * @author tomatopaste
     * @param ship Player ship
     * @param fill Value 0 to 1, how full the bar is from left to right
     * @param name Name of subsystem
     * @param infoText Info string opportunity
     * @param stateText Subsystem activity status
     * @param hotkey Hotkey string of key used to activate subsystem
     * @param flavourText A brief description of what the subsystem does
     * @param showInfoText If the subsystem is in show info mode
     * @param guiBarCount The number of gui bars this subsystem will use
     * @param inputLoc the Input location (top left) of the subsystem GUI element
     * @param rootLoc the Root location of subsystem GUI elements
     * @return The output location (bottom left) of GUI element
     */
    public static Vector2f drawSubsystemStatus(
            ShipAPI ship,
            float fill,
            String name,
            String infoText,
            String stateText,
            String hotkey,
            String flavourText,
            boolean showInfoText,
            int guiBarCount,
            Vector2f inputLoc,
            Vector2f rootLoc
    ) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.equals(engine.getPlayerShip()) || engine.isUIShowingDialog()) return null;
        if (engine.getCombatUI() == null || engine.getCombatUI().isShowingCommandUI() || !engine.isUIShowingHUD()) return null;

        Color colour = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        final int bars = (showInfoText) ? guiBarCount + 1 : guiBarCount;

        Vector2f loc = new Vector2f(inputLoc);

//        openGL11ForText();

        TODRAW14.setMaxWidth(6969);

        TODRAW14.setText(name);
        TODRAW14.setBaseColor(Color.BLACK);
        TODRAW14.draw(loc.x + 1, loc.y - 1);
        TODRAW14.setBaseColor(colour);
        TODRAW14.draw(loc);

        Vector2f hotkeyTextLoc = new Vector2f(loc);
        hotkeyTextLoc.y -= BAR_HEIGHT * UIscaling * (guiBarCount);
        hotkeyTextLoc.x += INFO_TEXT_PADDING;

        if (hotkey == null || MagicSubsystem.BLANK_KEY.equals(hotkey)) {
            TODRAW14.setText(MagicTxt.getString("subsystemHotkeyAutomaticText"));
        } else {
            TODRAW14.setText(MagicTxt.getString("subsystemHotkeyText", hotkey));
        }

        float hotkeyTextWidth = TODRAW14.getWidth();
        if (showInfoText) {
            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(hotkeyTextLoc.x + 1, hotkeyTextLoc.y - 1);
            TODRAW14.setBaseColor(colour);
            TODRAW14.draw(hotkeyTextLoc);
        }

        if (showInfoText && flavourText != null && !flavourText.isEmpty()) {
            Vector2f flavourTextLoc = new Vector2f(hotkeyTextLoc);
            flavourTextLoc.x += hotkeyTextWidth + 20f * UIscaling;
            TODRAW14.setText(MagicTxt.getString("subsystemBriefText",  flavourText));
            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(flavourTextLoc.x + 1, flavourTextLoc.y - 1);
            TODRAW14.setBaseColor(colour);
            TODRAW14.draw(flavourTextLoc);
        }

        Vector2f boxLoc = new Vector2f(loc);
        boxLoc.x += STATUS_BAR_PADDING;

        final float boxHeight = STATUS_BAR_HEIGHT;
        final float boxEndWidth = STATUS_BAR_WIDTH;

        float boxWidth = boxEndWidth * fill;

        if (stateText != null && !stateText.isEmpty()) {
            Vector2f stateLoc = new Vector2f(boxLoc);
            TODRAW14.setText(stateText);
            stateLoc.x -= TODRAW14.getWidth() + (4f * UIscaling);
            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(stateLoc.x + 1, stateLoc.y - 1);
            TODRAW14.setBaseColor(colour);
            TODRAW14.draw(stateLoc);
        }

        if (infoText != null && !infoText.isEmpty()) {
            Vector2f infoLoc = new Vector2f(boxLoc);
            infoLoc.x += boxEndWidth + (5f * UIscaling);
            TODRAW14.setText(infoText);
            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(infoLoc.x + 1, infoLoc.y - 1);
            TODRAW14.setBaseColor(colour);
            TODRAW14.draw(infoLoc);
        }

//        closeGL11ForText();

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glViewport(0, 0, width, height);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, 0, height, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTranslatef(0.01f, 0.01f, 0);

        Vector2f nodeLoc = new Vector2f(loc);
        nodeLoc.y -= 4f * UIscaling;
        nodeLoc.x -= 2f * UIscaling;

        Vector2f titleLoc = getSubsystemTitleLoc(ship);
        boolean isHigh = loc.y > titleLoc.y;

        glLineWidth(UIscaling);
        glBegin(GL_LINE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(nodeLoc.x, nodeLoc.y);

        nodeLoc.y += (isHigh) ? -6f * UIscaling : 6f * UIscaling;
        nodeLoc.x -= 6f * UIscaling;
        glVertex2f(nodeLoc.x, nodeLoc.y);

        boolean isTitleHigh = rootLoc.y > titleLoc.y - 16f;
        nodeLoc.y = getSubsystemTitleLoc(ship).y;
        nodeLoc.y -= 16f;
        nodeLoc.y -= (isTitleHigh) ? -6f * UIscaling : 6f * UIscaling;
        glVertex2f(nodeLoc.x, nodeLoc.y);

        glEnd();

        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= 3f * UIscaling;

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += UIscaling;
        shadowLoc.y -= UIscaling;
        glBegin(GL_TRIANGLE_STRIP);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(shadowLoc.x, shadowLoc.y);
        glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
        glVertex2f(shadowLoc.x, shadowLoc.y - boxHeight);
        glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y - boxHeight);
        glEnd();

        glBegin(GL_TRIANGLE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxRenderLoc.x, boxRenderLoc.y);
        glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y);
        glVertex2f(boxRenderLoc.x, boxRenderLoc.y - boxHeight);
        glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y - boxHeight);
        glEnd();

        Vector2f boxEndBarLoc = new Vector2f(boxRenderLoc);
        boxEndBarLoc.x += boxEndWidth;

        glLineWidth(UIscaling);
        glBegin(GL_LINES);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        glEnd();

        glLineWidth(UIscaling);
        glBegin(GL_LINES);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y);
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y - boxHeight);
        glEnd();

        glDisable(GL_BLEND);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();

        loc.y -= bars * BAR_HEIGHT;
        return loc;
    }

    /**
     * Renders another subsystem HUD element intended for drone subsystems, but can be used for any purpose.
     * This HUD element has a decorative line and indentation indicating an auxiliary status to a primary subsystem HUD
     * element.
     *
     * @param ship       Player ship
     * @param indent     Indentation px from input position
     * @param indentLine Draw a line in the indentation.
     * @param fillStartX Start position of progress bar
     * @param fillLength Length of progress bar
     * @param fillLevel  Fill level of progress bar
     * @param text1      Text 1
     * @param text2      Text 2
     * @param inputLoc   Input location. This will be modified according to the X indent specified
     */
    public static void renderAuxiliaryStatusBar(ShipAPI ship, float indent, boolean indentLine, float fillStartX, float fillLength, float fillLevel, String text1, String text2, Vector2f inputLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        Color colour = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        inputLoc.x += indent * UIscaling;

        Vector2f boxLoc = new Vector2f(inputLoc);
        boxLoc.x += fillStartX * UIscaling;

        final float boxHeight = 9f * UIscaling;
        final float boxEndWidth = fillLength * UIscaling;

        float boxWidth = boxEndWidth * fillLevel;

        //openGL11ForText();

        TODRAW14.setMaxWidth(6969);

        TODRAW14.setText(text1);
        TODRAW14.setBaseColor(Color.BLACK);
        TODRAW14.draw(inputLoc.x + 1, inputLoc.y - 1);
        TODRAW14.setBaseColor(colour);
        TODRAW14.draw(inputLoc);

        if (text2 != null && !text2.isEmpty()) {
            Vector2f text2Pos = new Vector2f(boxLoc);
            text2Pos.x += boxEndWidth + (4f * UIscaling);
            TODRAW14.setText(text2);
            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(text2Pos.x + 1, text2Pos.y - 1);
            TODRAW14.setBaseColor(colour);
            TODRAW14.draw(text2Pos);
        }

        //closeGL11ForText();

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glViewport(0, 0, width, height);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, 0, height, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTranslatef(0.01f, 0.01f, 0);

        if (indentLine) {
            Vector2f nodeLoc = new Vector2f(inputLoc);
            nodeLoc.x -= 2f * UIscaling;
            nodeLoc.y -= 11f * UIscaling;

            glLineWidth(UIscaling);
            glBegin(GL_LINE_STRIP);
            glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
            glVertex2f(nodeLoc.x, nodeLoc.y);

            nodeLoc.x -= 6f * UIscaling;
            nodeLoc.y += 6f * UIscaling;
            glVertex2f(nodeLoc.x, nodeLoc.y);

            nodeLoc.y += 4f * UIscaling; //7
            glVertex2f(nodeLoc.x, nodeLoc.y);

            glEnd();
        }

        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= 3f * UIscaling;

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += UIscaling;
        shadowLoc.y -= UIscaling;
        glBegin(GL_TRIANGLE_STRIP);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(shadowLoc.x, shadowLoc.y);
        glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
        glVertex2f(shadowLoc.x, shadowLoc.y - boxHeight);
        glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y - boxHeight);
        glEnd();

        glBegin(GL_TRIANGLE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxRenderLoc.x, boxRenderLoc.y);
        glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y);
        glVertex2f(boxRenderLoc.x, boxRenderLoc.y - boxHeight);
        glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y - boxHeight);
        glEnd();

        Vector2f boxEndBarLoc = new Vector2f(boxRenderLoc);
        boxEndBarLoc.x += boxEndWidth;

        glLineWidth(UIscaling);
        glBegin(GL_LINES);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        glEnd();

        glLineWidth(UIscaling);
        glBegin(GL_LINES);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y);
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y - boxHeight);
        glEnd();

        glDisable(GL_BLEND);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }

    /**
     * Determines the root location (top left corner) of Subsystem UI elements by calculating total height of all
     * subsystems on player ship
     * @param ship Player ship
     * @param numBars Sum of number of gui slots from all subsystems
     * @param barHeight Height of an individual bar (DO NOT SCALE WITH UI)
     * @return Root location
     */
    public static Vector2f getSubsystemsRootLocation(ShipAPI ship, int numBars, float barHeight) {
        Vector2f loc = new Vector2f(529f, 74f);
        Vector2f.add(loc, getUIElementOffset(ship, ship.getVariant()), loc);

        float height = numBars * barHeight * UIscaling;

        int numWeapons = getNumWeapons(ship);
        float maxWeaponHeight = numWeapons * (13f * UIscaling) + 30f;
        if (numWeapons == 0) maxWeaponHeight -= 5f * UIscaling;

        final float minOffset = 10f * UIscaling;
        float weaponOffset = maxWeaponHeight + minOffset;

        loc.y = weaponOffset + height;

        loc.x *= UIscaling;

        return loc;
    }

    /**
     * Determines the number of weapons appearing the weapon list HUD element of the vanilla UI
     * @param ship Player ship
     * @return Number of weapons in active group
     */
    public static int getNumWeapons(ShipAPI ship) {
        WeaponGroupAPI groups = ship.getSelectedGroupAPI();
        List<WeaponAPI> weapons = (groups == null) ? null : groups.getWeaponsCopy();
        return (weapons == null) ? 0 : weapons.size();
    }

    /**
     * Finds the SUBSYSTEM deco element root location
     * @param ship Player ship
     * @return Location
     */
    public static Vector2f getSubsystemTitleLoc(ShipAPI ship) {
        Vector2f loc = new Vector2f(529f, 72f);
        Vector2f.add(loc, getUIElementOffset(ship, ship.getVariant()), loc);
        loc.scale(UIscaling);

        return loc;
    }

    /**
     * Draws the SUBSYSTEM deco element
     * @param ship Player ship
     * @param showInfo If the "more info" mode is enabled
     * @param rootLoc Root location of the subsystems UI elements {@link CombatUI#getSubsystemsRootLocation(ShipAPI, int, float)}
     */
    public static void drawSubsystemsTitle(ShipAPI ship, boolean showInfo, Vector2f rootLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.equals(engine.getPlayerShip()) || engine.isUIShowingDialog()) return;
        if (engine.getCombatUI() == null || engine.getCombatUI().isShowingCommandUI() || !engine.isUIShowingHUD()) return;

        Color colour = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        float barHeight = 13f * UIscaling;
        Vector2f loc = getSubsystemTitleLoc(ship);
        String info = MagicTxt.getString("subsystemInfoText", Keyboard.getKeyName(MagicSubsystemsManager.INSTANCE.getInfoHotkey()));

//        openGL11ForText();


        Vector2f titleTextLoc = new Vector2f(loc);
        Vector2f infoTextLoc = new Vector2f(rootLoc);
        infoTextLoc.y += (barHeight + 8f) * UIscaling;
        //infoTextLoc.x -= 4f * UIscaling;

        TODRAW14.setText(MagicTxt.getString("subsystemTitleText"));
        titleTextLoc.x -= TODRAW14.getWidth() + (14f * UIscaling);

        TODRAW14.setBaseColor(Color.BLACK);
        TODRAW14.draw(titleTextLoc.x + 1f, titleTextLoc.y - 1f);
        TODRAW14.setBaseColor(colour);
        TODRAW14.draw(titleTextLoc);

        if (showInfo) {
            int alpha = (int) MathUtils.clamp(
                    (255f * 1.5f) - (Global.getCombatEngine().getTotalElapsedTime(false) - MagicSubsystemsCombatPlugin.Companion.getInfoHotkeyLastPressed()) * 255f,
                    65f,
                    255f);
            Color backgroundColor = new Color(0, 0, 0, alpha);
            Color foregroundColor = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);

            TODRAW14.setText(info);
            TODRAW14.setBaseColor(backgroundColor);
            TODRAW14.draw(infoTextLoc.x + 1f, infoTextLoc.y - 1f);
            TODRAW14.setBaseColor(foregroundColor);
            TODRAW14.draw(infoTextLoc);
        }

//        closeGL11ForText();
//
//        openGLForMisc();

        glLineWidth(UIscaling);
        glBegin(GL_LINE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());

        Vector2f sysBarNode = new Vector2f(loc);

        final float length = 354f * UIscaling; //354
        sysBarNode.x -= length;
        sysBarNode.y += 4f * UIscaling;
        glVertex2f(sysBarNode.x, sysBarNode.y);

        //sysBarNode.x += length - (16f * UIscaling);
        sysBarNode.x += length - (113f * UIscaling);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        sysBarNode.x += 20f * UIscaling;
        sysBarNode.y -= 20f * UIscaling;
        glVertex2f(sysBarNode.x, sysBarNode.y);

        sysBarNode.x += (85f - 6f * UIscaling);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        boolean isTitleHigh = rootLoc.y > loc.y - 16f;
        sysBarNode.y += (isTitleHigh) ? 6f * UIscaling : -6f * UIscaling;
        sysBarNode.x += 6f * UIscaling;
        glVertex2f(sysBarNode.x, sysBarNode.y);

        glEnd();

//        closeGLForMisc();
    }

    /**
     * Used to draw the Pearson Exotronics drone system HUD elements. The same Root location is provided to each
     * method used to render each individual element, only the Offset is different for each one.
     * The following methods can be arbitrarily rendered for useful UI indicators of various things.
     * @param mothership mothership
     * @param tiles array of booleans indicating which drones are alive
     * @param extra Number of extra drones available for deployment (may be different to reserve)
     * @param text1 Text 1
     * @param text2 Text 2
     * @param cooldown Forge cooldown
     * @param reserve Number of drones in reserve
     * @param reserveMax Maximum number of drones in reserve (max number forge can produce)
     * @param activeState Drone orders state
     * @param numStates Number of drones orders states
     * @param state State name
     * @param icon State sprite icon
     * @param drones Map of drone sprites (supports multiple drone sprites)
     * @param background Background image (default was a Pearson logo)
     * @param shipSprite Mothership sprite
     */
    public static void drawDroneSystemStateWidget(
            ShipAPI mothership,
            boolean[] tiles,
            int extra,
            String text1,
            String text2,
            float cooldown,
            int reserve,
            int reserveMax,
            int activeState,
            int numStates,
            String state,
            SpriteAPI icon,
            Map<ShipAPI, SpriteDimWrapper> drones,
            SpriteAPI background,
            SpriteDimWrapper shipSprite
    ) {
        if (mothership != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI() == null || Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        final Vector2f tileDim = new Vector2f(100f, 20f);
        final Vector2f statusDim = new Vector2f(100f, 12f);
        final Vector2f reserveDim = new Vector2f(12f, 12f);
        final Vector2f stateRender = new Vector2f(22f, 22f);
        final Vector2f iconDim = new Vector2f(32f, 32f);
        final Vector2f spatialDim = new Vector2f(128f, 128f);

        final Vector2f edgePad = new Vector2f(-16f - reserveDim.x - (2f * UIscaling), 16f);
        edgePad.scale(UIscaling);
        Vector2f start = Vector2f.add(new Vector2f(Global.getSettings().getScreenWidth() * UIscaling, 0f), edgePad, new Vector2f());
        Vector2f.add(start, MagicSubsystemsManager.getWidgetOffsetVector(), start);

        Color colour = (mothership.isAlive()) ? GREENCOLOR : BLUCOLOR;

        Vector2f decoSize = new Vector2f(tileDim.x, tileDim.y + statusDim.y);

        // decorator L bar thing
        decoRender(Color.BLACK, start, new Vector2f(UIscaling, -UIscaling), decoSize);
        decoRender(colour, start, new Vector2f(0f, 0f), decoSize);

        Vector2f reserveStart = new Vector2f(start);

        final Vector2f decoPad = new Vector2f(-4f, 4f);
        decoPad.scale(UIscaling);
        Vector2f.add(start, decoPad, start);

        // chevrons
        tileRender(Color.BLACK, start, new Vector2f(UIscaling, -UIscaling), tileDim, tiles, extra, text1);
        float hPad = tileRender(colour, start, new Vector2f(0f, 0f), tileDim, tiles, extra, text1);

        final float pad = UIscaling * 4f;
        start.y += tileDim.y + pad;

        // cooldown bar
        boolean full = reserve >= reserveMax && cooldown > 0.95f;
        statusRender(Color.BLACK, start, new Vector2f(UIscaling, -UIscaling), statusDim, text2, cooldown, hPad, full);
        statusRender(colour, start, new Vector2f(0f, 0f), statusDim, text2, cooldown, hPad, full);

        // reserve squares
        reserveRender(Color.BLACK, reserveStart, new Vector2f(UIscaling, -UIscaling), reserveDim, reserve, reserveMax);
        reserveRender(colour, reserveStart, new Vector2f(0f, 0f), reserveDim, reserve, reserveMax);

        // hexagons, arrow, title text
        start.y += statusDim.y + pad;
        stateRender(Color.BLACK, start, new Vector2f(UIscaling, -UIscaling), stateRender, state, numStates, activeState);
        stateRender(colour, start, new Vector2f(0f, 0f), stateRender, state, numStates, activeState);

        // icon
        start.x += UIscaling;
        start.y += iconDim.y + pad;
        iconRender(Color.BLACK, icon, start, new Vector2f(UIscaling, -UIscaling), iconDim);
        iconRender(colour, icon, start, new Vector2f(0f, 0f), iconDim);
        start.x -= UIscaling;

        // spatial widget
        start.y += pad;
        spatialRender(colour, start, new Vector2f(0f, 0f), spatialDim, background, drones, mothership, shipSprite);
    }

    /**
     * Renders HUD element of mothership and drones with shields
     * @param colour HUD Color
     * @param start Root location
     * @param offset Offset of HUD element from root location
     * @param size Size of element
     * @param background Background sprite
     * @param drones Drones mapped to their sprite
     * @param mothership Mothership
     * @param shipSprite Mothership sprite
     */
    public static void spatialRender(
            Color colour,
            Vector2f start,
            Vector2f offset,
            Vector2f size,
            SpriteAPI background,
            Map<ShipAPI, SpriteDimWrapper> drones,
            ShipAPI mothership,
            SpriteDimWrapper shipSprite
    ) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        boolean renderBG = !hasRendered;
        hasRendered = true;

        if (renderBG) {
            openGLForMisc();

            background.setColor(colour);
            background.setAlphaMult(0.08f);
            background.setSize(dim.x, dim.y);

            background.render(node.x - dim.x, node.y);
        }

        final float d1 = mothership.getShieldRadiusEvenIfNoShield();
        float d2 = d1 * d1;
        for (ShipAPI drone : drones.keySet()) {
            float d2s = Vector2f.sub(drone.getLocation(), mothership.getLocation(), new Vector2f()).lengthSquared();
            d2 = Math.max(d2, d2s);
        }

        float zoom = 0.4f * dim.x / (float) Math.sqrt(d2);

        Vector2f center = new Vector2f(node.x - (dim.x * 0.5f), node.y + (dim.y * 0.5f));

        if (renderBG) {
            shipSprite.sprite.setColor(Color.BLACK);
            shipSprite.sprite.setSize(dim.x * zoom * 1.1f, dim.y * zoom * shipSprite.ratio * 1.1f);
            shipSprite.sprite.renderAtCenter(center.x, center.y);

            shipSprite.sprite.setColor(colour);
            shipSprite.sprite.setSize(dim.x * zoom, dim.y * zoom * shipSprite.ratio);
            shipSprite.sprite.renderAtCenter(center.x, center.y);
        }

        closeGLForMisc();

        openGLForMisc();
        if (mothership.getShield() != null && mothership.getShield().isOn()) {
            drawShieldArc(mothership, colour, zoom, center, 0.2f, 90f - mothership.getFacing());
        }
        closeGLForMisc();

        openGLForMisc();
        for (ShipAPI drone : drones.keySet()) {
            SpriteDimWrapper sprite = drones.get(drone);

            Vector2f d = Vector2f.sub(drone.getLocation(), mothership.getLocation(), new Vector2f());
            d.scale(zoom);
            VectorUtils.rotate(d, 90f - mothership.getFacing());

            sprite.sprite.setAngle(drone.getFacing() - mothership.getFacing());
            float x = d.x + center.x + (sprite.sprite.getCenterX() * 0.5f);
            float y = d.y + center.y + (sprite.sprite.getCenterY() * 0.5f);

            sprite.sprite.setColor(Color.BLACK);
            sprite.sprite.setSize(zoom * sprite.width * 1.05f, zoom * sprite.height * 1.05f);
            sprite.sprite.renderAtCenter(x, y);

            sprite.sprite.setColor(colour);
            sprite.sprite.setSize(zoom * sprite.width, zoom * sprite.height);
            sprite.sprite.renderAtCenter(x, y);
        }
        closeGLForMisc();

        openGLForMisc();
        for (ShipAPI drone : drones.keySet()) {
            if (drone.getShield() != null && drone.getShield().isOn()) {
                SpriteDimWrapper sprite = drones.get(drone);

                Vector2f d = Vector2f.sub(drone.getShieldCenterEvenIfNoShield(), mothership.getLocation(), new Vector2f());
                d.scale(zoom);
                VectorUtils.rotate(d, -mothership.getFacing() + 90f);

                float x = d.x + center.x;
                float y = d.y + center.y;

                drawShieldArc(drone, colour, zoom, new Vector2f(x, y), 0.5f, 90f - mothership.getFacing());
            }
        }
        closeGLForMisc();
    }

    /**
     * Draws a curved line imitating the shield arc of a ship
     * @param ship Ship
     * @param colour Colour
     * @param zoom Zoom mult
     * @param center Center loc of ship on hud
     * @param alpha Alpha mult
     * @param angleOffset Angle of shield
     */
    public static void drawShieldArc(ShipAPI ship, Color colour, float zoom, Vector2f center, float alpha, float angleOffset) {
        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                alpha
        );

        List<Vector2f> points = new ArrayList<>();
        float angle = ship.getShield().getActiveArc();

        Vector2f i1 = new Vector2f(ship.getShieldRadiusEvenIfNoShield() * zoom, 0f);
        VectorUtils.rotate(i1, ship.getShield().getFacing() + (angle * 0.5f) + angleOffset);

        int intervals = Math.max(5, (int) (angle / 20f));
        points.add(new Vector2f(i1));
        float interval = ship.getShield().getActiveArc() / intervals;
        for (int i = 0; i < intervals; i++) {
            VectorUtils.rotate(i1, -interval);
            points.add(new Vector2f(i1));
        }

        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
        glBegin(GL_LINE_STRIP);
        for (Vector2f point : points) {
            glVertex2f(point.x + center.x, point.y + center.y);
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);
    }

    /**
     * Unused but useful for UI debugging
     * @param x X pos
     * @param y Y pos
     */
    public static void drawDot(float x, float y) {
        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x, y + 4f);
        glVertex2f(x - 4f, y);
        glVertex2f(x + 4f, y);
        glVertex2f(x, y - 4f);
        glEnd();
    }

    /**
     * Simple wrapper to make drawing sprites easier (not sure why this was necessary, but it is what it is)
     * Only requires an input SpriteAPI
     */
    public static class SpriteDimWrapper {
        public final SpriteAPI sprite;
        public final float width;
        public final float height;
        public final float ratio;

        public SpriteDimWrapper(SpriteAPI sprite) {
            this.sprite = sprite;
            width = sprite.getWidth();
            height = sprite.getHeight();
            ratio = height / width;
        }
    }

    /**
     * Renders the drone mode icon (but can render any sprite arbitrarily)
     * @param colour Colour
     * @param sprite Sprite
     * @param start Root location
     * @param offset Offset of HUD element from root location
     * @param size Dimensions of icon
     */
    public static void iconRender(Color colour, SpriteAPI sprite, Vector2f start, Vector2f offset, Vector2f size) {
        if (sprite == null) return;

        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        sprite.setSize(size.x, size.y);
        sprite.setColor(colour);

        openGLForMisc();
        sprite.render(node.x, node.y - (size.y * 0.25f));
        closeGLForMisc();
    }

    /**
     * Renders a series of hexagons used to indicate an active state out of a series of possible states.
     * @param colour Colour
     * @param start Root position
     * @param offset Offset of HUD element from root location
     * @param size Dimensions of element (hexagons will be scaled to fit)
     * @param text Text to render
     * @param num Number of states (hence number of hexagons)
     * @param active Index of active state (other hexagons will appear darker)
     */
    public static void stateRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, String text, int num, int active) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        float w = num * size.x;

        openGLForMisc();

        float x1 = 0.05f * size.x;
        float x2 = 0.3f * size.x;
        float x3 = 0.7f * size.x;
        float x4 = 0.95f * size.x;

        float y1 = 0.1f * size.x + node.y;
        float y2 = 0.5f * size.y + node.y;
        float y3 = 0.9f * size.y + node.y;

        float x = node.x - w;
        for (int i = 0; i < num; i++) {
            Color c = i == active ? colour : colour.darker().darker();

            glColor4f(
                    c.getRed() / 255f,
                    c.getGreen() / 255f,
                    c.getBlue() / 255f,
                    1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
            );

            glBegin(GL_TRIANGLE_FAN);

            glVertex2f(x + x1, y2);
            glVertex2f(x + x2, y1);
            glVertex2f(x + x3, y1);
            glVertex2f(x + x4, y2);
            glVertex2f(x + x3, y3);
            glVertex2f(x + x2, y3);

            glEnd();

            x += size.x;
        }

        float x5 = node.x - w + (0.25f * dim.x);
        float x6 = node.x;
        float y5 = node.y + dim.y + (2f * UIscaling);
        float y6 = y5 + UIscaling;

        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
        );

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x5, y5);
        glVertex2f(x5, y6);
        glVertex2f(x6, y5);
        glVertex2f(x6, y6);
        glEnd();

        float x7 = (active * size.x) + (0.5f * size.x) + node.x - w;
        float x8 = x7 + (dim.x * 0.25f);
        float x9 = x7 - (dim.x * 0.25f);
        float y7 = y5 + (6f * UIscaling);

        glBegin(GL_TRIANGLES);
        glVertex2f(x8, y6);
        glVertex2f(x7, y7);
        glVertex2f(x9, y6);
        glEnd();

        closeGLForMisc();

        openGL11ForText();

        x = node.x - w;
        for (int i = 0; i < num; i++) {
            TODRAW14.setText(i < 11 ? alphabet[i] : "NIL");

            float tx = Math.round((0.5f * dim.x) - (0.5f * TODRAW14.getWidth()));
            float ty = Math.round((0.5f * dim.y) + (0.5f * TODRAW14.getHeight()));

            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(tx + x, ty + node.y);
            x += size.x;
        }

        TODRAW14.setText(text);
        TODRAW14.setBaseColor(colour);
        TODRAW14.draw(node.x - TODRAW14.getWidth(), node.y + dim.y + (10f * UIscaling) + TODRAW14.getHeight());

        closeGL11ForText();
    }

    /**
     * Renders indicator squares as progress up a vertical stack. Used to indicate reserve drone capacity.
     * @param colour Color
     * @param start Root location
     * @param offset Offset of HUD element from root location
     * @param size Dimensions of HUD element
     * @param num Number of reserve drones
     * @param max Maximum number of reserve drones
     */
    public static void reserveRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, int num, int max) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        float x1 = node.x + (dim.x * 0.2f);
        float x2 = node.x + (dim.x * 0.8f);
        float y1 = dim.y * 0.2f;
        float y2 = dim.y * 0.8f;

        float y = node.y;

        openGLForMisc();

        for (int i = 0; i < max; i++) {
            glBegin(GL_TRIANGLE_STRIP);

            Color c = i >= num ? colour.darker().darker() : colour;

            glColor4f(
                    c.getRed() / 255f,
                    c.getGreen() / 255f,
                    c.getBlue() / 255f,
                    1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
            );

            glVertex2f(x1, y + y1);
            glVertex2f(x1, y + y2);
            glVertex2f(x2, y + y1);
            glVertex2f(x2, y + y2);

            y += dim.x;
            glEnd();
        }

        float x3 = node.x - UIscaling;
        float x4 = node.x + dim.x + UIscaling;
        float y3 = dim.x * max + node.y;
        float y4 = y3 + UIscaling;

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x3, y3);
        glVertex2f(x3, y4);
        glVertex2f(x4, y3);
        glVertex2f(x4, y4);
        glEnd();

        closeGLForMisc();

        int overflow = Math.max(num - max, 0);

        TODRAW14.setText("+" + overflow);
        TODRAW14.setBaseColor(colour);
        float x5 = node.x + (2f * UIscaling);
        float y5 = y + TODRAW14.getHeight() + UIscaling;

        openGL11ForText();
        TODRAW14.draw(x5, y5);
        closeGL11ForText();
    }

    /**
     * Renders edge deco
     * @param colour Colour
     * @param start Root location
     * @param offset Offset of HUD element from root location
     * @param size Dimensions of HUD element
     */
    private static void decoRender(Color colour, Vector2f start, Vector2f offset, Vector2f size) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        final Vector2f decoPad = new Vector2f(-4f, 4f);
        decoPad.scale(UIscaling);

        openGLForMisc();

        // edge deco

        glBegin(GL_TRIANGLE_STRIP);

        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
        );

        float px = 0.5f * decoPad.x;
        float py = 0.5f * decoPad.y;

        glVertex2f(node.x + decoPad.x - (dim.x * 1.2f), node.y);
        glVertex2f(node.x + decoPad.x - (dim.x * 1.2f), node.y + py);
        glVertex2f(node.x + px, node.y);
        glVertex2f(node.x + px, node.y + py);
        glVertex2f(node.x, node.y + py);
        glVertex2f(node.x + px, node.y + (decoPad.y + (1.2f * dim.y)));
        glVertex2f(node.x, node.y + (decoPad.y + (1.2f * dim.y)));

        glEnd();

        closeGLForMisc();
    }

    /**
     * Draws a cooldown bar with text
     * @param colour Color
     * @param start Root location
     * @param offset Offset from root location
     * @param size Dimensions of element
     * @param text Text
     * @param fill Fill level of progress bar
     * @param hPad Horizontal padding for element
     * @param full If the progress bar is full
     */
    public static void statusRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, String text, float fill, float hPad, boolean full) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        dim.x += hPad;

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        openGL11ForText();

        final float textPad = 10f * UIscaling;

        TODRAW14.setText(text);
        TODRAW14.setBaseColor(colour);

        float ty = node.y + (dim.y * 0.5f) + (int) (TODRAW14.getHeight() * 0.5f) + 1f;

        TODRAW14.draw(node.x - TODRAW14.getWidth() - dim.x - textPad, ty);

        closeGL11ForText();

        openGLForMisc();

        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
        );

        float y1 = node.y + (dim.y * 0.1f);
        float y2 = node.y + (dim.y * 0.9f);
        float x1 = node.x - (dim.x * fill);
        float x2 = node.x;

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x2, y1);
        glVertex2f(x2, y2);
        glVertex2f(x1, y1);
        glVertex2f(x1, y2);
        glEnd();

        float y5 = node.y + (dim.y * 0.1f);
        float y6 = y5 + UIscaling;
        float y7 = node.y + (dim.y * 0.9f);
        float y8 = y7 - UIscaling;
        float x5 = node.x - dim.x;
        float x6 = x5 + (4f * UIscaling);

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x5, y5);
        glVertex2f(x5, y6);
        glVertex2f(x6, y5);
        glVertex2f(x6, y6);
        glEnd();
        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x5, y7);
        glVertex2f(x5, y8);
        glVertex2f(x6, y7);
        glVertex2f(x6, y8);
        glEnd();

        closeGLForMisc();

        if (full) {
            openGL11ForText();

            TODRAW14.setText("reserve full");

            TODRAW14.setBaseColor(Color.BLACK);
            TODRAW14.draw(node.x - (int) (0.5f * dim.x) - (int) (0.5f * TODRAW14.getWidth()), ty);

            closeGL11ForText();
        }
    }

    /**
     * @return left text width
     */
    private static float tileRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, boolean[] tiles, int extra, String text) {
        Vector2f dim = new Vector2f(size);
        dim.scale(UIscaling);
        offset.scale(UIscaling);

        Vector2f node = Vector2f.add(start, offset, new Vector2f());

        TODRAW14.setText("+" + extra);
        float pad = TODRAW14.getWidth();

        openGLForMisc();

        // chevron tiles

        float y1 = node.y + (0.9f * dim.y);
        float y2 = node.y + (0.4f * dim.y);
        float y3 = node.y + (0.1f * dim.y);

        float interval = dim.x / tiles.length;
        float x1 = interval * 0.1f;
        float x2 = interval * 0.5f;
        float x3 = interval * 0.9f;

        float xp = node.x - pad - dim.x;
        for (boolean tile : tiles) {
            glBegin(GL_TRIANGLE_STRIP);

            Color c = tile ? colour : colour.darker().darker();

            glColor4f(
                    c.getRed() / 255f,
                    c.getGreen() / 255f,
                    c.getBlue() / 255f,
                    1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
            );

            glVertex2f(xp + x1, y3);
            glVertex2f(xp + x2, y2);
            glVertex2f(xp + x2, y1);
            glVertex2f(xp + x3, y3);

            glEnd();

            xp += interval;
        }

        closeGLForMisc();

        // Text rendering

        openGL11ForText();

        TODRAW14.setBaseColor(colour);

        // extra text
        // TODRAW14 already set
        float x4 = node.x - TODRAW14.getWidth();
        float y4 = TODRAW14.getHeight() + node.y;

        TODRAW14.draw(x4, y4);

        final float textPad = 10f * UIscaling;

        TODRAW14.setText(text);
        float x5 = node.x - dim.x - pad - textPad - TODRAW14.getWidth();
        float y5 = node.y + (dim.y * 0.5f) + (int) (TODRAW14.getHeight() * 0.5f) + 1f;

        TODRAW14.draw(x5, y5);

        closeGL11ForText();

        return pad;
    }

    ///// UTILS /////

    /**
     * Taken from MagicLib when it was a private method
     * Get the UI Element Offset.
     * (Depends on the weapon groups and wings present)
     *
     * @param ship The player ship.
     * @param variant The variant of the ship.
     * @return the offset.
     */
    public static Vector2f getUIElementOffset(ShipAPI ship, ShipVariantAPI variant) {
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
                return CombatUI.PERCENTBARVEC1;
            }
            return new Vector2f(30f + ((numEntries - 2) * 13f), 18f + ((numEntries - 2) * 26f));
        } else {
            if (numEntries < 2) {
                return CombatUI.PERCENTBARVEC2;
            }
            return new Vector2f(59f + ((numEntries - 2) * 13f), 76f + ((numEntries - 2) * 26f));
        }
    }

    /**
     * Taken from MagicLib when it was a private method
     * GL11 to start, when you want render text of Lazyfont.
     */
    private static void openGL11ForText() {
        glPushAttrib(GL_ENABLE_BIT);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
        glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Taken from MagicLib when it was a private method
     * GL11 to close, when you want render text of Lazyfont.
     */
    private static void closeGL11ForText() {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glPopMatrix();
        glPopAttrib();
    }

    /**
     * @author tomatopaste
     * Sets OpenGL state for rendering in HUD coordinates
     */
    public static void openGLForMisc() {
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
     * @author tomatopaste
     */
    public static void closeGLForMisc() {
        glDisable(GL_BLEND);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }
}