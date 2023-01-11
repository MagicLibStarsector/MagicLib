//By Tartiflette with DeathFly's help
//draw arbitrary beam sprites wherever you need them and fade them out
package org.magiclib.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MagicFakeBeamPlugin extends BaseEveryFrameCombatPlugin {

    private SpriteAPI core = Global.getSettings().getSprite("beams", "fakeBeamCore");
    private SpriteAPI fringe = Global.getSettings().getSprite("beams", "fakeBeamFringe");

    public static List<fakeBeamData> BEAMS = new ArrayList();

    /**
     * Fake beam renderer
     * Draw the actual fake beam, can be directly called when it is only for visual effects
     *
     * @param duration Duration of the beam at full opacity
     * @param fading   Duration of the beam fading
     * @param width    Width of the beam
     * @param from     Point of origin of the beam
     * @param angle    Angle of the beam
     * @param length   Length of the beam (remember that some tip fading will occur)
     * @param core     Core color of the beam
     * @param fringe   Fringe color of the beam
     */
    public static void addBeam(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe) {
        fakeBeamData DATA = new fakeBeamData(duration, fading, width, from, angle, length, core, fringe);
        BEAMS.add(DATA);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        //reinitialize the map 
        BEAMS.clear();
    }

    @Override
    public void renderInWorldCoords(ViewportAPI view) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }

        if (!BEAMS.isEmpty()) {
            //get elapsed time out of pause
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());

            //go through all the fake beams
            for (Iterator<fakeBeamData> iter = BEAMS.iterator(); iter.hasNext(); ) {
                fakeBeamData entry = iter.next();

                if (entry.FULL < -entry.FADING) {
                    //beam expended and faded, remove
                    iter.remove();
                } else {
                    //opacity is 1 while the full beam is not expended, then fades as the full time gets negative until fading time - full time = 0
                    float opacity = 1;
                    if (entry.FULL < 0) {
                        opacity = (entry.FADING + entry.FULL) / entry.FADING;
                    }
                    render(
                            core, //Sprite to draw
                            entry.WIDTH * opacity, //Width entry srinking with the opacity
                            entry.LENGTH * 2, //Height entry, multiplied by two because centered
                            entry.ANGLE, //Angle entry
                            entry.CORE, //color...
                            Math.min(1, Math.max(0, (opacity * 2) - 1)), //opacity duh!
                            entry.FROM.x, //X position entry
                            entry.FROM.y //Y position entry
                    );

                    render(
                            fringe, //Sprite to draw
                            entry.WIDTH * opacity, //Width entry srinking with the opacity
                            entry.LENGTH * 2, //Height entry, multiplied by two because centered
                            entry.ANGLE, //Angle entry
                            entry.FRINGE, //color...
                            opacity, //opacity duh!
                            entry.FROM.x, //X position entry
                            entry.FROM.y //Y position entry
                    );

                    entry.FULL = entry.FULL - amount;
                }
            }
        }
    }

    private void render(SpriteAPI sprite, float width, float height, float angle, Color color, float opacity, float posX, float posY) {
        //where the magic happen
        sprite.setColor(color);
        sprite.setAlphaMult(opacity);
        sprite.setSize(width, height);
        sprite.setAdditiveBlend();
        sprite.setAngle(angle - 90);
        sprite.renderAtCenter(posX, posY);
    }

    public static class fakeBeamData {
        private float FULL;
        private float FADING;
        private final float WIDTH;
        private final Vector2f FROM;
        private final float ANGLE;
        private final float LENGTH;
        private final Color CORE;
        private final Color FRINGE;

        public fakeBeamData(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe) {
            this.FULL = duration;
            this.FADING = fading;
            this.WIDTH = width;
            this.FROM = from;
            this.ANGLE = angle;
            this.LENGTH = length;
            this.CORE = core;
            this.FRINGE = fringe;
        }
    }
}
