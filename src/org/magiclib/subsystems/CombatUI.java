package org.magiclib.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicTxt;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author tomatopaste
 * A few methods in the UTILS section were copied from MagicLib when they were private and inaccessible, in addition
 * to several static fields at the beginning of the class. References to these methods can probably be replaced with
 * references to the methods in the original MagicLib UI file.
 * <p>
 * All other code where applicable:
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2024 tomatopaste
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class CombatUI {
    public static final float STATUS_BAR_WIDTH = 45f;
    public static final float STATUS_BAR_HEIGHT = 9f;
    public static final float STATUS_BAR_PADDING = 200f;
    public static final float BAR_HEIGHT = 13f;
    public static final float INFO_TEXT_PADDING = 20f;

    private static final String[] alphabet = new String[]{
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
    };


    // Used to determine if the background sprite has been rendered for the spatial ship/drones graph
    public static boolean hasRenderedSpatial = false;

    public static boolean getHasRenderedSpatial() {
        return hasRenderedSpatial;
    }

    ///////////////////////////////////
    //                               //
    //         SUBSYSTEM GUI         //
    //                               //
    ///////////////////////////////////

    /**
     * Draws the status bar for a basic subsystem, imitating the shipsystem UI. If show info mode is enabled, it will
     * take up an additional UI slot.
     * <p>
     * If the subsystem script attempts to render additional HUD elements (e.g. using the renderAuxiliaryStatusBar
     * method) then they will not have an effect on the output location. This must be predetermined by the input
     * guiBarCount parameter to make room for them.
     *
     * @param ship            Player ship
     * @param fill            Value 0 to 1, how full the bar is from left to right
     * @param name            Name of subsystem
     * @param extraText       Info string opportunity. Appears to the right of the status bar.
     * @param extraTextColor  color of extraText. if null, uses standard text color
     * @param stateText       Subsystem activity status. Appears to left of status bar.
     * @param hotkey          Hotkey string of key used to activate subsystem
     * @param briefText       A brief description of what the subsystem does
     * @param showInfoText    If the subsystem is in show info mode
     * @param guiBarCount     The number of gui bars this subsystem will use
     * @param inputLoc        the Input location (top left) of the subsystem GUI element
     * @param extraBarPadding
     * @param rootLoc         the Root location of subsystem GUI elements
     * @return The output location (bottom left) of GUI element
     * @author tomatopaste
     */
    public static Vector2f drawSubsystemStatus(
            ShipAPI ship,
            float fill,
            String name,
            String extraText,
            Color extraTextColor,
            String stateText,
            String hotkey,
            String briefText,
            boolean showInfoText,
            int guiBarCount,
            Vector2f inputLoc,
            float extraBarPadding,
            Vector2f rootLoc) {
        return drawSubsystemStatus(ship, fill, name, MagicUI.GREENCOLOR, extraText, extraTextColor, stateText, hotkey, briefText, showInfoText, guiBarCount, inputLoc, extraBarPadding, rootLoc);
    }
    /**
     * Draws the status bar for a basic subsystem, imitating the shipsystem UI. If show info mode is enabled, it will
     * take up an additional UI slot.
     * <p>
     * If the subsystem script attempts to render additional HUD elements (e.g. using the renderAuxiliaryStatusBar
     * method) then they will not have an effect on the output location. This must be predetermined by the input
     * guiBarCount parameter to make room for them.
     *
     * @param ship            Player ship
     * @param fill            Value 0 to 1, how full the bar is from left to right
     * @param name            Name of subsystem
     * @param hudColor        Color of the subsystem bar and text.
     * @param extraText       Info string opportunity. Appears to the right of the status bar.
     * @param extraTextColor  color of extraText. if null, uses standard text color
     * @param stateText       Subsystem activity status. Appears to left of status bar.
     * @param hotkey          Hotkey string of key used to activate subsystem
     * @param briefText       A brief description of what the subsystem does
     * @param showInfoText    If the subsystem is in show info mode
     * @param guiBarCount     The number of gui bars this subsystem will use
     * @param inputLoc        the Input location (top left) of the subsystem GUI element
     * @param extraBarPadding
     * @param rootLoc         the Root location of subsystem GUI elements
     * @return The output location (bottom left) of GUI element
     * @author tomatopaste
     */
    public static Vector2f drawSubsystemStatus(
            ShipAPI ship,
            float fill,
            String name,
            Color hudColor,
            String extraText,
            Color extraTextColor,
            String stateText,
            String hotkey,
            String briefText,
            boolean showInfoText,
            int guiBarCount,
            Vector2f inputLoc,
            float extraBarPadding,
            Vector2f rootLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Color colour = (ship.isAlive()) ? hudColor : MagicUI.BLUCOLOR;

        final int bars = (showInfoText) ? guiBarCount + 1 : guiBarCount;

        Vector2f loc = MagicUI.scaledCopy(inputLoc);

        MagicUI.openGL11ForTextWithinViewport();

        MagicUI.addTextNonScaling(ship, name, colour, loc, false);

        Vector2f hotkeyTextLoc = new Vector2f(loc);
        hotkeyTextLoc.y -= MagicUI.scale(BAR_HEIGHT * guiBarCount);
        hotkeyTextLoc.x += MagicUI.scale(INFO_TEXT_PADDING);

        String hotkeyText = MagicTxt.getString("subsystemHotkeyAutomaticText");
        if (hotkey != null && !MagicSubsystem.BLANK_KEY.equals(hotkey)) {
            hotkeyText = MagicTxt.getString("subsystemHotkeyText", hotkey);
        }

        float hotkeyTextWidth = MagicUI.TODRAW14.getWidth();
        if (showInfoText) {
            MagicUI.addTextNonScaling(ship, hotkeyText, colour, hotkeyTextLoc, false);
        }

        if (showInfoText && briefText != null && !briefText.isEmpty()) {
            Vector2f flavourTextLoc = new Vector2f(hotkeyTextLoc);
            flavourTextLoc.x += hotkeyTextWidth + MagicUI.scale(20f);

            String briefTextFormat = MagicTxt.getString("subsystemBriefText", briefText);
            MagicUI.addTextNonScaling(ship, briefTextFormat, colour, flavourTextLoc, false);
        }

        Vector2f boxLoc = new Vector2f(loc);
        boxLoc.x += MagicUI.scale(STATUS_BAR_PADDING) + MagicUI.scale(extraBarPadding);

        final float boxHeight = MagicUI.scale(STATUS_BAR_HEIGHT);
        final float boxEndWidth = MagicUI.scale(STATUS_BAR_WIDTH);

        float boxWidth = boxEndWidth * fill;

        if (stateText != null && !stateText.isEmpty()) {
            Vector2f stateLoc = new Vector2f(boxLoc);
            MagicUI.TODRAW14.setText(stateText);
            stateLoc.x -= MagicUI.TODRAW14.getWidth() + MagicUI.scale(4f);

            MagicUI.addTextNonScaling(ship, stateText, colour, stateLoc, false);
        }

        if (extraText != null && !extraText.isEmpty()) {
            if (extraTextColor == null) {
                extraTextColor = colour;
            }

            Vector2f infoLoc = new Vector2f(boxLoc);
            infoLoc.x += boxEndWidth + MagicUI.scale(5f);

            MagicUI.addTextNonScaling(ship, extraText, extraTextColor, infoLoc, false);
        }

        MagicUI.closeGL11ForTextWithinViewport();

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

        //render connecting line for system
        Vector2f nodeLoc = new Vector2f(loc);
        nodeLoc.y -= MagicUI.scale(4f);
        nodeLoc.x -= MagicUI.scale(2f);

        Vector2f titleLoc = MagicUI.scaledCopy(getSubsystemTitleLoc(ship));
        boolean isHigh = loc.y > titleLoc.y;

        //starts at subsystem name
        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(nodeLoc.x, nodeLoc.y);

        //connects with vertical line created by selected weapon group widget
        nodeLoc.y += MagicUI.scale((isHigh) ? -6f : 6f);
        nodeLoc.x -= MagicUI.scale(6f);
        glVertex2f(nodeLoc.x, nodeLoc.y);

        //goes up to subsystem title
        boolean isTitleHigh = rootLoc.y > titleLoc.y - MagicUI.scale(16f);
        nodeLoc.y = titleLoc.y;
        nodeLoc.y -= MagicUI.scale(16f);
        nodeLoc.y -= MagicUI.scale((isTitleHigh) ? -6f : 6f);
        glVertex2f(nodeLoc.x, nodeLoc.y);

        glEnd();

        //render subsystem status bar
        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= MagicUI.scale(3f);

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += MagicUI.UI_SCALING;
        shadowLoc.y -= MagicUI.UI_SCALING;
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

        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINES);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        glEnd();

        glLineWidth(MagicUI.UI_SCALING);
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

        return Vector2f.add(inputLoc, new Vector2f(0f, bars * BAR_HEIGHT), null);
    }

    /**
     * Renders another subsystem HUD element intended for drone subsystems, but can be used for any purpose.
     * This HUD element has a decorative line and indentation indicating an auxiliary status to a primary subsystem HUD
     * element.
     *
     * @param ship        Player ship
     * @param indent      Indentation px from input position
     * @param indentLine  Draw a line in the indentation.
     * @param fillStartX  Start position of progress bar
     * @param fillLength  Length of progress bar
     * @param fillLevel   Fill level of progress bar
     * @param text1       Text 1
     * @param text2       Text 2. Can be null or empty.
     * @param text2OnLeft Whether to draw text2 on the left or right, if it is provided.
     * @param inputLoc    Input location
     */
    public static void renderAuxiliaryStatusBar(ShipAPI ship, float indent, boolean indentLine, float fillStartX, float fillLength, float fillLevel, String text1, String text2, boolean text2OnLeft, Vector2f inputLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Color colour = (ship.isAlive()) ? MagicUI.GREENCOLOR : MagicUI.BLUCOLOR;

        Vector2f textLoc = new Vector2f(inputLoc);
        textLoc.x += indent;
        MagicUI.scale(textLoc);

        Vector2f boxLoc = new Vector2f(inputLoc);
        boxLoc.x += indent + fillStartX;
        MagicUI.scale(boxLoc);

        final float boxEndWidth = MagicUI.scale(fillLength);
        float boxWidth = boxEndWidth * fillLevel;

        MagicUI.openGL11ForTextWithinViewport();
        MagicUI.addTextNonScaling(ship, text1, colour, textLoc, false);

        if (text2 != null && !text2.isEmpty()) {
            Vector2f text2Pos = new Vector2f(boxLoc);
            MagicUI.TODRAW14.setText(text2);

            if (text2OnLeft) {
                text2Pos.x -= MagicUI.TODRAW14.getWidth() + MagicUI.scale(4f);
            } else {
                text2Pos.x += boxEndWidth + MagicUI.scale(4f);
            }

            MagicUI.addTextNonScaling(ship, text2, colour, text2Pos, false);
        }

        MagicUI.closeGL11ForTextWithinViewport();

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
            Vector2f nodeLoc = new Vector2f(textLoc);
            nodeLoc.x -= MagicUI.scale(2f);
            nodeLoc.y -= MagicUI.scale(11f);

            glLineWidth(MagicUI.UI_SCALING);
            glBegin(GL_LINE_STRIP);
            glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
            glVertex2f(nodeLoc.x, nodeLoc.y);

            nodeLoc.x -= MagicUI.scale(6f);
            nodeLoc.y += MagicUI.scale(6f);
            glVertex2f(nodeLoc.x, nodeLoc.y);

            nodeLoc.y += MagicUI.scale(4f); //7
            glVertex2f(nodeLoc.x, nodeLoc.y);

            glEnd();
        }

        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= MagicUI.scale(3f);
        final float boxHeight = MagicUI.scale(STATUS_BAR_HEIGHT);

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += MagicUI.UI_SCALING;
        shadowLoc.y -= MagicUI.UI_SCALING;
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

        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINES);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        glEnd();

        glLineWidth(MagicUI.UI_SCALING);
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
     * subsystems on player ship.
     *
     * @param ship      Player ship
     * @param numBars   Sum of number of gui slots from all subsystems
     * @param barHeight Height of an individual bar (DO NOT SCALE WITH UI)
     * @return Root location. Not scaled by UI scaling.
     */
    public static Vector2f getSubsystemsRootLocation(ShipAPI ship, int numBars, float barHeight) {
        Vector2f loc = new Vector2f(529f, 74f);
        Vector2f.add(loc, MagicUI.getUIElementOffset(ship, ship.getVariant(), MagicUI.PERCENTBARVEC1, MagicUI.PERCENTBARVEC2), loc);

        float height = numBars * barHeight;

        int numWeapons = getNumWeapons(ship);
        float maxWeaponHeight = numWeapons * 13f + 30f;
        if (numWeapons == 0) maxWeaponHeight -= 5f;

        final float minOffset = 10f;
        float weaponOffset = maxWeaponHeight + minOffset;

        loc.y = weaponOffset + height;

        return loc;
    }

    /**
     * Determines the number of weapons appearing the weapon list HUD element of the vanilla UI
     *
     * @param ship Player ship
     * @return Number of weapons in active group
     */
    public static int getNumWeapons(ShipAPI ship) {
        WeaponGroupAPI groups = ship.getSelectedGroupAPI();
        List<WeaponAPI> weapons = (groups == null) ? null : groups.getWeaponsCopy();
        return (weapons == null) ? 0 : weapons.size();
    }

    /**
     * Finds the SUBSYSTEM deco element root location.
     *
     * @param ship Player ship
     * @return Location. Not scaled to UI scaling.
     */
    public static Vector2f getSubsystemTitleLoc(ShipAPI ship) {
        Vector2f loc = new Vector2f(529f, 72f);
        Vector2f.add(loc, MagicUI.getUIElementOffset(ship, ship.getVariant(), MagicUI.PERCENTBARVEC1, MagicUI.PERCENTBARVEC2), loc);

        return loc;
    }

    /**
     * Draws the SUBSYSTEM deco element.
     *
     * @param ship     Player ship
     * @param showInfo If the "more info" mode is enabled
     * @param rootLoc  Root location of the subsystems UI elements {@link CombatUI#getSubsystemsRootLocation(ShipAPI, int, float)}
     */
    public static void drawSubsystemsTitle(ShipAPI ship, boolean showInfo, Vector2f rootLoc, boolean displayAdditionalInfo) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Color colour = (ship.isAlive()) ? MagicUI.GREENCOLOR : MagicUI.BLUCOLOR;

        float barHeight = MagicUI.scale(13f);
        Vector2f loc = MagicUI.scale(getSubsystemTitleLoc(ship));

        String infoText = MagicTxt.getString(displayAdditionalInfo ? "subsystemLessInfoText" : "subsystemMoreInfoText", Keyboard.getKeyName(MagicSubsystemsManager.INSTANCE.getInfoHotkey()));

        MagicUI.openGL11ForTextWithinViewport();

        Vector2f titleTextLoc = new Vector2f(loc);
        Vector2f infoTextLoc = new Vector2f(rootLoc);
        MagicUI.scale(infoTextLoc);
        infoTextLoc.y += barHeight + MagicUI.scale(8f);
        //infoTextLoc.x -= 4f * MagicUI.UI_SCALING;

        String titleText = MagicTxt.getString("subsystemTitleText");
        MagicUI.TODRAW14.setText(titleText);
        titleTextLoc.x -= MagicUI.TODRAW14.getWidth() + MagicUI.scale(14f);
        MagicUI.addTextNonScaling(ship, titleText, colour, titleTextLoc, false);

        if (showInfo) {
            int alpha = (int) MathUtils.clamp(
                    (255f * 1.5f) - (Global.getCombatEngine().getTotalElapsedTime(true) - MagicSubsystemsCombatPlugin.Companion.getInfoHotkeyLastPressed()) * 255f,
                    MagicSubsystemsManager.getInfoTextMaxFadeout(),
                    255f);
            Color foregroundColor = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);

            MagicUI.addTextNonScaling(ship, infoText, foregroundColor, infoTextLoc, false);
        }

        MagicUI.closeGL11ForTextWithinViewport();

        MagicUI.openGLForMiscWithinViewport();

        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINE_STRIP);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());

        Vector2f sysBarNode = new Vector2f(loc);

        final float length = MagicUI.scale(354f);

        //initial point beneath leftmost point of ship system name, above weapon groups
        sysBarNode.x -= length;
        sysBarNode.y += MagicUI.scale(4f);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        //above weapons groups, below ship system
        sysBarNode.x += length - MagicUI.scale(113f);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        //above first weapon group, ends beneath ship system status test and above LINKED/ALTERNATING text
        sysBarNode.x += MagicUI.scale(20f);
        sysBarNode.y -= MagicUI.scale(20f);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        //ends shortly after SUBSYSTEMS text.
        sysBarNode.x += MagicUI.scale(85f - 6f);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        //tiny diagonal line, intersects with vertical line created by selected weapon group widget
        boolean isTitleHigh = rootLoc.y > loc.y - MagicUI.scale(16f);
        sysBarNode.y += MagicUI.scale((isTitleHigh) ? 6f : -6f);
        sysBarNode.x += MagicUI.scale(6f);
        glVertex2f(sysBarNode.x, sysBarNode.y);

        glEnd();

        MagicUI.closeGLForMiscWithinViewport();
    }

    /**
     * Used to draw the Pearson Exotronics drone system HUD elements. The same Root location is provided to each
     * method used to render each individual element, only the Offset is different for each one.
     * The following methods can be arbitrarily rendered for useful UI indicators of various things.
     *
     * @param mothership  mothership
     * @param tiles       array of booleans indicating which drones are alive
     * @param extra       Number of extra drones available for deployment (may be different to reserve)
     * @param text1       Text 1
     * @param text2       Text 2
     * @param cooldown    Forge cooldown
     * @param reserve     Number of drones in reserve
     * @param reserveMax  Maximum number of drones in reserve (max number forge can produce)
     * @param activeState Drone orders state
     * @param numStates   Number of drones orders states
     * @param state       State name
     * @param icon        State sprite icon
     * @param drones      Map of drone sprites (supports multiple drone sprites)
     * @param background  Background image (default was a Pearson logo)
     * @param shipSprite  Mothership sprite
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
        final Vector2f tileDim = new Vector2f(100f, 20f);
        final Vector2f statusDim = new Vector2f(100f, 12f);
        final Vector2f reserveDim = new Vector2f(12f, 12f);
        final Vector2f stateRender = new Vector2f(22f, 22f);
        final Vector2f iconDim = new Vector2f(32f, 32f);
        final Vector2f spatialDim = new Vector2f(128f, 128f);

        final Vector2f edgePad = new Vector2f(-16f - 2f - reserveDim.x, 16f);

        Vector2f start = Vector2f.add(new Vector2f(Global.getSettings().getScreenWidthPixels(), 0f), edgePad, new Vector2f());
        Vector2f.add(start, MagicSubsystemsManager.getWidgetOffsetVector(), start);

        Color colour = (mothership.isAlive()) ? MagicUI.GREENCOLOR : MagicUI.BLUCOLOR;

        Vector2f decoSize = new Vector2f(tileDim.x, tileDim.y + statusDim.y);

        // decorator L bar thing
        decoRender(Color.BLACK, start, new Vector2f(1, -1), decoSize);
        decoRender(colour, start, new Vector2f(0f, 0f), decoSize);

        final Vector2f decoPad = new Vector2f(-4f, 4f);
        Vector2f.add(start, decoPad, start);

        // chevrons
        tileRender(Color.BLACK, start, new Vector2f(1, -1), tileDim, tiles, extra, text1, true);
        float hPad = tileRender(colour, start, new Vector2f(0f, 0f), tileDim, tiles, extra, text1, true);

        final float pad = 4f;
        start.y += tileDim.y + pad;

        // cooldown bar
        boolean full = reserve >= reserveMax && cooldown > 0.95f;
        statusRender(Color.BLACK, start, new Vector2f(1, -1), statusDim, text2, cooldown, hPad, full);
        statusRender(colour, start, new Vector2f(0f, 0f), statusDim, text2, cooldown, hPad, full);

        // reserve squares
        Vector2f reserveStart = new Vector2f(start);
        reserveRender(Color.BLACK, reserveStart, new Vector2f(1, -1), reserveDim, reserve, reserveMax);
        reserveRender(colour, reserveStart, new Vector2f(0f, 0f), reserveDim, reserve, reserveMax);

        if (numStates > 0) {
            // hexagons, arrow, title text
            start.y += statusDim.y + pad;
            stateRender(Color.BLACK, start, new Vector2f(1, -1), stateRender, state, numStates, activeState);
            stateRender(colour, start, new Vector2f(0f, 0f), stateRender, state, numStates, activeState);

            // icon
            start.x += 1;
            start.y += iconDim.y + pad;
            iconRender(Color.BLACK, icon, start, new Vector2f(1, -1), iconDim);
            iconRender(colour, icon, start, new Vector2f(0f, 0f), iconDim);
            start.x -= 1;
        }

        // spatial widget
        start.y += pad;
        spatialRender(colour, start, new Vector2f(0f, 0f), spatialDim, background, drones, mothership, shipSprite);
    }

    /**
     * Renders HUD element of mothership and drones with shields
     *
     * @param colour     HUD Color
     * @param start      Root location
     * @param offset     Offset of HUD element from root location
     * @param size       Size of element
     * @param background Background sprite
     * @param drones     Drones mapped to their sprite
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
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        boolean renderBG = !hasRenderedSpatial;
        hasRenderedSpatial = true;

        if (renderBG) {
            MagicUI.openGLForMiscWithinViewport();

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
        float d2Sqrt = (float) Math.sqrt(d2);

        float zoom = 0.4f * dim.x / d2Sqrt;

        Vector2f center = new Vector2f(node.x - (dim.x * 0.5f), node.y + (dim.y * 0.5f));

        if (renderBG) {
            Color baseShipColor = shipSprite.sprite.getColor();

            shipSprite.sprite.setAngle(mothership.getFacing() - 90f);
            shipSprite.sprite.setColor(Color.BLACK);
            shipSprite.sprite.setSize(dim.x * zoom * 1.1f, dim.y * zoom * shipSprite.ratio * 1.1f);
            shipSprite.sprite.renderAtCenter(center.x, center.y);

            shipSprite.sprite.setColor(colour);
            shipSprite.sprite.setSize(dim.x * zoom, dim.y * zoom * shipSprite.ratio);
            shipSprite.sprite.renderAtCenter(center.x, center.y);

            shipSprite.sprite.setColor(baseShipColor);

            if (mothership.getShield() != null && mothership.getShield().isOn()) {
                drawShieldArc(mothership, colour, zoom, center, 0.2f, 90f - mothership.getFacing());
            }

            MagicUI.closeGLForMiscWithinViewport();
        }

        MagicUI.openGLForMiscWithinViewport();
        for (ShipAPI drone : drones.keySet()) {
            SpriteDimWrapper sprite = drones.get(drone);

            Vector2f droneDiff = Vector2f.sub(drone.getLocation(), mothership.getLocation(), new Vector2f());
            droneDiff.scale(zoom);
            VectorUtils.rotate(droneDiff, 90f - mothership.getFacing());

            sprite.sprite.setAngle(drone.getFacing() - mothership.getFacing());
            float x = droneDiff.x + center.x + (sprite.sprite.getCenterX() * 0.5f);
            float y = droneDiff.y + center.y + (sprite.sprite.getCenterY() * 0.5f);

            sprite.sprite.setColor(Color.BLACK);
            sprite.sprite.setSize(zoom * sprite.width * 1.05f, zoom * sprite.height * 1.05f);
            sprite.sprite.renderAtCenter(x, y);

            sprite.sprite.setColor(colour);
            sprite.sprite.setSize(zoom * sprite.width, zoom * sprite.height);
            sprite.sprite.renderAtCenter(x, y);

            if (drone.getShield() != null && drone.getShield().isOn()) {
                Vector2f unscaledCenter = Vector2f.add(start, offset, null);
                Vector2f.add(unscaledCenter, new Vector2f(-size.x * 0.5f, size.y * 0.5f), unscaledCenter);
                float unscaledZoom = 0.4f * size.x / d2Sqrt;

                Vector2f droneShieldDiff = Vector2f.sub(drone.getShieldCenterEvenIfNoShield(), mothership.getLocation(), new Vector2f());
                droneShieldDiff.scale(unscaledZoom);
                VectorUtils.rotate(droneShieldDiff, -mothership.getFacing() + 90f);

                float shieldX = droneShieldDiff.x + unscaledCenter.x;
                float shieldY = droneShieldDiff.y + unscaledCenter.y;

                drawShieldArc(drone, colour, unscaledZoom, new Vector2f(shieldX, shieldY), 0.5f, 90f - mothership.getFacing());
            }
        }
        MagicUI.closeGLForMiscWithinViewport();
    }

    /**
     * Draws a curved line imitating the shield arc of a ship.
     *
     * @param ship        Ship
     * @param colour      Colour
     * @param zoom        Zoom mult
     * @param center      Center loc of ship on HUD.
     * @param alpha       Alpha mult
     * @param angleOffset Angle of shield
     */
    public static void drawShieldArc(ShipAPI ship, Color colour, float zoom, Vector2f center, float alpha, float angleOffset) {
        Vector2f node = MagicUI.scaledCopy(center);
        float scaledZoom = MagicUI.scale(zoom);

        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                alpha
        );

        List<Vector2f> points = new ArrayList<>();
        float angle = ship.getShield().getActiveArc();

        Vector2f i1 = new Vector2f(ship.getShieldRadiusEvenIfNoShield() * scaledZoom, 0f);
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
            glVertex2f(point.x + node.x, point.y + node.y);
        }
        glEnd();
        glDisable(GL_LINE_SMOOTH);
    }

    /**
     * Unused but useful for UI debugging.
     *
     * @param x X pos
     * @param y Y pos
     */
    public static void drawDot(float x, float y) {
        x = MagicUI.scale(x);
        y = MagicUI.scale(y);

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
     *
     * @author tomatopaste
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
     *
     * @param colour Colour
     * @param sprite Sprite
     * @param start  Root location
     * @param offset Offset of HUD element from root location
     * @param size   Dimensions of icon
     */
    public static void iconRender(Color colour, SpriteAPI sprite, Vector2f start, Vector2f offset, Vector2f size) {
        if (sprite == null) return;

        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        sprite.setSize(dim.x, dim.y);
        sprite.setColor(colour);

        MagicUI.openGLForMiscWithinViewport();
        sprite.render(node.x, node.y - (dim.y * 0.25f));
        MagicUI.closeGLForMiscWithinViewport();
    }

    /**
     * Renders a series of hexagons used to indicate an active state out of a series of possible states.
     *
     * @param colour Colour
     * @param start  Root position
     * @param offset Offset of HUD element from root location
     * @param size   Dimensions of element (hexagons will be scaled to fit)
     * @param text   Text to render
     * @param num    Number of states (hence number of hexagons)
     * @param active Index of active state (other hexagons will appear darker)
     */
    public static void stateRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, String text, int num, int active) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        float w = num * dim.x;

        MagicUI.openGLForMiscWithinViewport();

        float x1 = 0.05f * dim.x;
        float x2 = 0.3f * dim.x;
        float x3 = 0.7f * dim.x;
        float x4 = 0.95f * dim.x;

        float y1 = 0.1f * dim.x + node.y;
        float y2 = 0.5f * dim.y + node.y;
        float y3 = 0.9f * dim.y + node.y;

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

            x += dim.x;
        }

        float x5 = node.x - w + (0.25f * dim.x);
        float x6 = node.x;
        float y5 = node.y + dim.y + MagicUI.scale(2f);
        float y6 = y5 + MagicUI.UI_SCALING;

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

        float x7 = (active * dim.x) + (0.5f * dim.x) + node.x - w;
        float x8 = x7 + (dim.x * 0.25f);
        float x9 = x7 - (dim.x * 0.25f);
        float y7 = y5 + MagicUI.scale(6f);

        glBegin(GL_TRIANGLES);
        glVertex2f(x8, y6);
        glVertex2f(x7, y7);
        glVertex2f(x9, y6);
        glEnd();

        MagicUI.closeGLForMiscWithinViewport();

        MagicUI.openGL11ForTextWithinViewport();

        x = node.x - w;
        for (int i = 0; i < num; i++) {
            MagicUI.TODRAW14.setText(i < 11 ? alphabet[i] : "NIL");

            float tx = Math.round((0.5f * dim.x) - (0.5f * MagicUI.TODRAW14.getWidth()));
            float ty = Math.round((0.5f * dim.y) + (0.5f * MagicUI.TODRAW14.getHeight()));

            MagicUI.TODRAW14.setBaseColor(Color.BLACK);
            MagicUI.TODRAW14.draw(tx + x, ty + node.y);
            x += dim.x;
        }

        MagicUI.TODRAW14.setText(text);
        MagicUI.TODRAW14.setBaseColor(colour);
        MagicUI.TODRAW14.draw(node.x - MagicUI.TODRAW14.getWidth(), node.y + dim.y + MagicUI.scale(10f) + MagicUI.TODRAW14.getHeight());

        MagicUI.closeGL11ForTextWithinViewport();
    }

    /**
     * Renders indicator squares as progress up a vertical stack. Used to indicate reserve drone capacity.
     *
     * @param colour Color
     * @param start  Root location
     * @param offset Offset of HUD element from root location
     * @param size   Dimensions of HUD element
     * @param num    Number of reserve drones
     * @param max    Maximum number of reserve drones
     */
    public static void reserveRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, int num, int max) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        float x1 = node.x + (dim.x * 0.2f);
        float x2 = node.x + (dim.x * 0.8f);
        float y1 = dim.y * 0.2f;
        float y2 = dim.y * 0.8f;

        float y = node.y;

        MagicUI.openGLForMiscWithinViewport();

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

            y += dim.y;
            glEnd();
        }

        float x3 = node.x - MagicUI.UI_SCALING;
        float x4 = node.x + dim.x + MagicUI.UI_SCALING;
        float y3 = dim.x * max + node.y;
        float y4 = y3 + MagicUI.UI_SCALING;

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x3, y3);
        glVertex2f(x3, y4);
        glVertex2f(x4, y3);
        glVertex2f(x4, y4);
        glEnd();

        MagicUI.closeGLForMiscWithinViewport();


        MagicUI.openGL11ForTextWithinViewport();

        int overflow = Math.max(num - max, 0);
        String overflowText = "+" + overflow;
        MagicUI.TODRAW14.setText(overflowText);
        float x5 = node.x + MagicUI.scale(2f);
        float y5 = y + MagicUI.TODRAW14.getHeight() + MagicUI.UI_SCALING;

        MagicUI.addTextNonScaling(null, overflowText, colour, new Vector2f(x5, y5), false);

        MagicUI.closeGL11ForTextWithinViewport();
    }

    /**
     * Renders edge deco
     *
     * @param colour Colour
     * @param start  Root location
     * @param offset Offset of HUD element from root location
     * @param size   Dimensions of HUD element
     */
    private static void decoRender(Color colour, Vector2f start, Vector2f offset, Vector2f size) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        final Vector2f decoPad = MagicUI.scaledVector(-4f, 4f);

        MagicUI.openGLForMiscWithinViewport();

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

        MagicUI.closeGLForMiscWithinViewport();
    }

    /**
     * Draws a cooldown bar with text. Draws "subsystemDroneReservesFullText" in center of bar if "full" is true.
     * The root location is on the left side of the bar.
     *
     * @param colour Color
     * @param start  Root location.
     * @param offset Offset from root location.
     * @param size   Dimensions of element.
     * @param text   Text
     * @param fill   Fill level of progress bar
     * @param hPad   Additional padding between text and bar.
     * @param full   If the progress bar is full
     */
    public static void statusRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, String text, float fill, float hPad, boolean full) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        MagicUI.openGL11ForTextWithinViewport();

        final float textPad = MagicUI.scale(10f);
        float tx = node.x - MagicUI.TODRAW14.getWidth() - dim.x - textPad;
        float ty = node.y + (int) (dim.y * 0.5f) + (int) (MagicUI.TODRAW14.getHeight() * 0.5f) + MagicUI.scale(2f);
        Vector2f textPos = new Vector2f(tx, ty);
        MagicUI.addTextNonScaling(null, text, colour, textPos, false);

        MagicUI.closeGL11ForTextWithinViewport();

        Vector2f.add(node, MagicUI.scaledVector(hPad, 0f), null);

        MagicUI.openGLForMiscWithinViewport();

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
        float y6 = y5 + MagicUI.UI_SCALING;
        float y7 = node.y + (dim.y * 0.9f);
        float y8 = y7 - MagicUI.UI_SCALING;
        float x5 = node.x - dim.x;
        float x6 = x5 + MagicUI.scale(4f);

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

        MagicUI.closeGLForMiscWithinViewport();

        if (full) {
            MagicUI.openGL11ForTextWithinViewport();

            MagicUI.TODRAW14.setText(MagicTxt.getString("subsystemDroneReservesFullText"));

            MagicUI.TODRAW14.setBaseColor(Color.BLACK);
            MagicUI.TODRAW14.draw(node.x - (int) (0.5f * dim.x) - (int) (0.5f * MagicUI.TODRAW14.getWidth()), ty);

            MagicUI.closeGL11ForTextWithinViewport();
        }
    }

    /**
     * Draws a cooldown bar with text. This one fills left-to-right and has the little end bar.
     * Also positioned at the leftmost side of text.
     *
     * @param colour Color
     * @param start  Root location.
     * @param offset Offset from root location.
     * @param size   Dimensions of element.
     * @param text   Text
     * @param fill   Fill level of progress bar
     * @param hPad   Additional padding between text and bar.
     * @param full   If the progress bar is full
     * @return Top right end pos of bar
     */
    public static Vector2f systemlikeStatusRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, String text, float fill, float hPad, boolean full) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f loc = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(loc);

        MagicUI.openGL11ForTextWithinViewport();
        MagicUI.addTextNonScaling(null, text, colour, loc, false);
        MagicUI.closeGL11ForTextWithinViewport();

        Vector2f.add(loc, MagicUI.scaledVector(hPad + 80f, -3f), loc);

        MagicUI.openGLForMiscWithinViewport();

        glColor4f(
                colour.getRed() / 255f,
                colour.getGreen() / 255f,
                colour.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()
        );

        float x1 = loc.x;
        float y1 = loc.y;
        float x2 = loc.x + dim.x * fill;
        float y2 = loc.y - dim.y;

        glBegin(GL_TRIANGLE_STRIP);
        glVertex2f(x1, y1);
        glVertex2f(x2, y1);
        glVertex2f(x1, y2);
        glVertex2f(x2, y2);
        glEnd();

        Vector2f boxEndBarLoc = new Vector2f(loc.x + dim.x, y1);

        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINES);
        glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - dim.y - 1);
        glEnd();

        glLineWidth(MagicUI.UI_SCALING);
        glBegin(GL_LINES);
        glColor4f(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y);
        glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y - dim.y);
        glEnd();

        MagicUI.closeGLForMiscWithinViewport();

        if (full) {
            MagicUI.openGL11ForTextWithinViewport();

            MagicUI.TODRAW14.setText(MagicTxt.getString("subsystemDroneReservesFullText"));
            MagicUI.TODRAW14.setBaseColor(Color.BLACK);
            MagicUI.TODRAW14.draw((int) (loc.x + dim.x / 2f - MagicUI.TODRAW14.getWidth() / 2f), (int) (loc.y + MagicUI.TODRAW14.getHeight() / 4f));

            MagicUI.closeGL11ForTextWithinViewport();
        }

        return boxEndBarLoc;
    }

    /**
     * Renders a series of chevron tiles
     *
     * @param colour     color for rendering
     * @param start      where to start rendering
     * @param offset     offset from start
     * @param size       size of chevrons
     * @param tiles      tile data. false is darker
     * @param extra      number of "extra" drones deployed. if -1, does not render
     * @param text       optional displayed text
     * @param textOnLeft if true, text is rendered on left
     * @return width of +"extra" text if rendered. otherwise 0
     */
    public static float tileRender(Color colour, Vector2f start, Vector2f offset, Vector2f size, boolean[] tiles, int extra, String text, boolean textOnLeft) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        String extraText = "+" + extra;
        float pad = 0f;
        if (extra >= 0) {
            MagicUI.TODRAW14.setText(extraText);
            pad = MagicUI.TODRAW14.getWidth();
        }

        MagicUI.openGLForMiscWithinViewport();

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

        MagicUI.closeGLForMiscWithinViewport();

        // Text rendering

        MagicUI.openGL11ForTextWithinViewport();

        if (extra >= 0) {
            // extra text
            // TODRAW14 already set
            float x4 = node.x - MagicUI.TODRAW14.getWidth();
            float y4 = MagicUI.TODRAW14.getHeight() + node.y;
            MagicUI.addTextNonScaling(null, extraText, colour, new Vector2f(x4, y4), false);
        }

        if (text != null && !text.isEmpty()) {
            MagicUI.TODRAW14.setText(text);

            final float textPad = MagicUI.scale(10f);
            float x5 = node.x + dim.x + pad + textPad + MagicUI.TODRAW14.getWidth();
            if (textOnLeft) {
                x5 = node.x - dim.x - pad - textPad - MagicUI.TODRAW14.getWidth();
            }
            float y5 = node.y + (dim.y * 0.5f) + (int) (MagicUI.TODRAW14.getHeight() * 0.5f) + MagicUI.UI_SCALING;

            MagicUI.addTextNonScaling(null, text, colour, new Vector2f(x5, y5), false);
        }

        MagicUI.closeGL11ForTextWithinViewport();

        return pad;
    }


    /**
     * Renders a series of dims.
     *
     * @param colour     color for rendering
     * @param start      where to start rendering
     * @param offset     offset from start
     * @param dimWrapper sprite to render
     * @param size       size of chevrons
     * @param tiles      tile data. false is darker
     * @param extra      number of "extra" drones deployed. if -1, does not render
     * @param text       optional displayed text
     * @param textOnLeft if true, text is rendered on left
     * @return width of +"extra" text if rendered. otherwise 0
     */
    public static float dimRender(Color colour, Vector2f start, Vector2f offset, SpriteDimWrapper dimWrapper, Vector2f size, boolean[] tiles, int extra, String text, boolean textOnLeft) {
        Vector2f dim = MagicUI.scaledCopy(size);
        Vector2f node = Vector2f.add(start, offset, new Vector2f());
        MagicUI.scale(node);

        String extraText = "+" + extra;
        float pad = 0f;
        if (extra >= 0) {
            MagicUI.TODRAW14.setText(extraText);
            pad = MagicUI.TODRAW14.getWidth();
        }

        MagicUI.openGLForMiscWithinViewport();

        // chevron tiles
        dimWrapper.sprite.setSize(dim.x, dim.y);

        float interval = dim.x + pad;
        float xp = node.x;
        for (boolean tile : tiles) {
            float x = xp + interval / 2f;
            float y = node.y;
            Color c = tile ? colour : colour.darker().darker();
            dimWrapper.sprite.setColor(c);
            dimWrapper.sprite.renderAtCenter(x, y);

            xp += interval;
        }

        MagicUI.closeGLForMiscWithinViewport();

        // Text rendering

        MagicUI.openGL11ForTextWithinViewport();

        if (extra >= 0) {
            // extra text
            // TODRAW14 already set
            float x4 = node.x - MagicUI.TODRAW14.getWidth();
            float y4 = MagicUI.TODRAW14.getHeight() + node.y;
            MagicUI.addTextNonScaling(null, extraText, colour, new Vector2f(x4, y4), false);
        }

        if (text != null && !text.isEmpty()) {
            MagicUI.TODRAW14.setText(text);

            final float textPad = MagicUI.scale(10f);
            float x5 = node.x + dim.x + pad + textPad + MagicUI.TODRAW14.getWidth();
            if (textOnLeft) {
                x5 = node.x - dim.x - pad - textPad - MagicUI.TODRAW14.getWidth();
            }
            float y5 = node.y + (dim.y * 0.5f) + (int) (MagicUI.TODRAW14.getHeight() * 0.5f) + MagicUI.UI_SCALING;

            MagicUI.addTextNonScaling(null, text, colour, new Vector2f(x5, y5), false);
        }

        MagicUI.closeGL11ForTextWithinViewport();

        return pad;
    }
}