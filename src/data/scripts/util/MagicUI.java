package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Dark.Revenant, Tartiflette
 */

public class MagicUI {
    
    ///////////////////////////////////
    //                               //
    //          UI WIDGET            //
    //                               //
    ///////////////////////////////////    
        
    //UI WIDGET
    private static final Color BORDER;
    // Used to smoothly interpolate between status colors
    private static Color lastColor;
    private static float lastUpdate;
    
    /**
     * Draws a small UI bar next to the ship's system
     * 
     * @param ship
     * Ship concerned (the element will only be drawn if that ship is the player ship)
     * 
     * @param intendedColor
     * Color of the filling. If null, the filling will be UI-green
     * 
     * @param fill
     * Filling level
     * 
     * @param blendTime 
     * Time to smoothly switch between colors. Can be set to 0 for instant changes
     */
    public static void drawSystemBar(ShipAPI ship, Color intendedColor, float fill, float blendTime) {
        
        if (!ship.isAlive() || ship!=Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        
        if (Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }
        
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());
        final float boxWidth = 27f;
        final float boxHeight = 7f;

        // Used to properly interpolate between colors
        final CombatEngineAPI engine = Global.getCombatEngine();
        final float elapsed = engine.getTotalElapsedTime(true);
        float alpha = 1;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            alpha = 0.28f;
        }

        // Calculate what color to use        
        Color actualColor;
        if(blendTime>0){
            
            Color targetColor;
            if(intendedColor==null){
                targetColor=BORDER;
            } else {
                targetColor=intendedColor;
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
                actualColor = interpolateColor(lastColor, targetColor, progress);
            }
        } else {
            if(intendedColor==null){
                actualColor=BORDER;
            } else {
                actualColor=intendedColor;
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

        final Vector2f boxLoc = Vector2f.add(new Vector2f(497f, 80f),
                                             getUIElementOffset(ship, ship.getVariant()), null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(498f, 79f),
                                                getUIElementOffset(ship, ship.getVariant()), null);

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
        GL11.glColor4f(BORDER.getRed() / 255f, BORDER.getGreen() / 255f, BORDER.getBlue() / 255f,
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
                       alpha * (actualColor.getAlpha() / 255f) *
                       (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
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
     * @param ship
     * Ship concerned (the element will only be drawn if that ship is the player ship)
     * 
     * @param intendedColor
     * Color of the filling. If null, the filling will be UI-green
     * 
     * @param blendTime 
     * Time to smoothly switch between colors. Can be set to 0 for instant changes
     */
    public static void drawSystemBox(ShipAPI ship, Color intendedColor, float blendTime) {
        
        if (!ship.isAlive() || ship!=Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        
        if (Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }
        
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());
        final float boxSide = 7f;

        // Used to properly interpolate between colors
        final CombatEngineAPI engine = Global.getCombatEngine();
        final float elapsed = engine.getTotalElapsedTime(true);
        float alpha = 1;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            alpha = 0.28f;
        }

        
        // Calculate what color to use        
        Color actualColor;
        if(blendTime>0){
            
            Color targetColor;
            if(intendedColor==null){
                targetColor=BORDER;
            } else {
                targetColor=intendedColor;
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
                actualColor = interpolateColor(lastColor, targetColor, progress);
            }
        } else {
            if(intendedColor==null){
                actualColor=BORDER;
            } else {
                actualColor=intendedColor;
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

        final Vector2f boxLoc = Vector2f.add(new Vector2f(497f, 80f),
                                             getUIElementOffset(ship, ship.getVariant()), null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(498f, 79f),
                                                getUIElementOffset(ship, ship.getVariant()), null);

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
        GL11.glColor4f(BORDER.getRed() / 255f, BORDER.getGreen() / 255f, BORDER.getBlue() / 255f,
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
                       alpha * (actualColor.getAlpha() / 255f) *
                       (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxSide , boxLoc.y);
        GL11.glVertex2f(boxLoc.x, boxLoc.y + boxSide);
        GL11.glVertex2f(boxLoc.x + boxSide , boxLoc.y + boxSide);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
    
    static {
        BORDER = Global.getSettings().getColor("textFriendColor");
    }
    
    private static Color interpolateColor(Color old, Color dest, float progress) {
        final float clampedProgress = Math.max(0f, Math.min(1f, progress));
        final float antiProgress = 1f - clampedProgress;
        final float[] ccOld = old.getComponents(null), ccNew = dest.getComponents(null);
        return new Color((ccOld[0] * antiProgress) + (ccNew[0] * clampedProgress),
                         (ccOld[1] * antiProgress) + (ccNew[1] * clampedProgress),
                         (ccOld[2] * antiProgress) + (ccNew[2] * clampedProgress),
                         (ccOld[3] * antiProgress) + (ccNew[3] * clampedProgress));
    }
    
    private static Vector2f getUIElementOffset(ShipAPI ship, ShipVariantAPI variant) {
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
                String id = variant.getWeaponId(slot);
                if (id != null) {
                    uniqueWeapons.add(id);
                }
            }
            numEntries += uniqueWeapons.size();
        }
        if (variant.getFittedWings().isEmpty()) {
            if (numEntries < 2) {
                return new Vector2f(0f, 0f);
            }
            return new Vector2f(30f + ((numEntries - 2) * 13f), 18f + ((numEntries - 2) * 26f));
        } else {
            if (numEntries < 2) {
                return new Vector2f(29f, 58f);
            }
            return new Vector2f(59f + ((numEntries - 2) * 13f), 76f + ((numEntries - 2) * 26f));
        }
    }
}