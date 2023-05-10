// By Tartiflette and DeathFly

package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

/**
 * Fake beam generator. Create a visually convincing beam from arbitrary coordinates.
 * It however has several limitation:
 * - It deal damage instantly and is therefore only meant to be used for burst beams.
 * - It cannot be "cut" by another object passing between the two ends, thus a very short duration is preferable.
 * - Unlike vanilla, it deals full damage to armor, be careful when using HIGH_EXPLOSIVE damage type.
 * It's usage is recommended for short snappy beams or for FX.
 * <p>
 *
 * <img src="https://static.wikia.nocookie.net/starfarergame/images/4/4d/MagicFakeBeam_spawnFakeBeam.gif/revision/latest?cb=20181024094938" />
 */
public class MagicFakeBeam {

    /////////////////////////////////////////
    //                                     //
    //             FAKE BEAM               //
    //                                     //
    /////////////////////////////////////////    

    /**
     * Fake beam generator. Create a visually convincing beam from arbitrary coordinates.
     * It however has several limitation:
     * - It deal damage instantly and is therefore only meant to be used for burst beams.
     * - It cannot be "cut" by another object passing between the two ends, thus a very short duration is preferable.
     * - Unlike vanilla, it deals full damage to armor, be careful when using HIGH_EXPLOSIVE damage type.
     * It's usage is recommended for short snappy beams or for FXs
     *
     * @param engine       Combat engine
     * @param from         Point of origin of the beam
     * @param range        Maximum range of the beam
     * @param angle        Angle of the beam
     * @param width        Width of the beam
     * @param full         Duration of the beam at full opacity
     * @param fading       Duration of the beam fading
     * @param impactSize   Size of the impact glow
     * @param core         Core color of the beam
     * @param fringe       Fringe color of the beam
     * @param normalDamage Base damage of the beam
     * @param type         Damage type
     * @param emp          Emp damage
     * @param source       Damage source to calculate skill and ship damage bonuses
     */

    public static void spawnFakeBeam(CombatEngineAPI engine, Vector2f from, float range, float angle, float width, float full, float fading, float impactSize, Color core, Color fringe, float normalDamage, DamageType type, float emp, ShipAPI source) {

        CombatEntityAPI theTarget = null;
        float damage = normalDamage;

        //default end point
        Vector2f end = MathUtils.getPoint(from, range, angle);

        //list all nearby entities that could be hit
        List<CombatEntityAPI> entity = CombatUtils.getEntitiesWithinRange(from, range + 500);
        if (!entity.isEmpty()) {
            for (CombatEntityAPI e : entity) {

                //ignore un-hittable stuff like phased ships
                if (e.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }

                //damage can be reduced against some modded ships
                float newDamage = normalDamage;

                Vector2f col = new Vector2f(1000000, 1000000);
                //ignore everything but ships...
                if (e instanceof ShipAPI) {
                    if (
                            e != source
                                    &&
                                    ((ShipAPI) e).getParentStation() != e
                                    &&
                                    e.getCollisionClass() != CollisionClass.NONE
                                    &&
                                    !(e.getCollisionClass() == CollisionClass.FIGHTER && e.getOwner() == source.getOwner() && !((ShipAPI) e).getEngineController().isFlamedOut())
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {

                        //check for a shield impact, then hull and take the closest one                  
                        ShipAPI s = (ShipAPI) e;

                        //find the collision point with shields/hull
                        Vector2f hitPoint = getShipCollisionPoint(from, end, s, angle);
                        if (hitPoint != null) {
                            col = hitPoint;
                        }

                        //check for modded ships with damage reduction
                        if (s.getHullSpec().getBaseHullId().startsWith("exigency_")) {
                            newDamage = normalDamage / 2;
                        }
                    }
                } else
                    //...and asteroids!
                    if (
                            (e instanceof CombatAsteroidAPI
                                    ||
                                    (e instanceof MissileAPI)
                                            &&
                                            e.getOwner() != source.getOwner()
                            )
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {
                        Vector2f cAst = getCollisionPointOnCircumference(from, end, e.getLocation(), e.getCollisionRadius());
                        if (cAst != null) {
                            col = cAst;
                        }
                    }

                //if there was an impact and it is closer than the curent beam end point, set it as the new end point and store the target to apply damage later damage
                if (
                        col.x != 1000000 &&
                                MathUtils.getDistanceSquared(from, col) < MathUtils.getDistanceSquared(from, end)) {
                    end = col;
                    theTarget = e;
                    damage = newDamage;
                }
            }

            //if the beam impacted something, apply the damage
            if (theTarget != null) {

                //damage
                engine.applyDamage(
                        theTarget,
                        end,
                        damage,
                        type,
                        emp,
                        false,
                        true,
                        source
                );
                //impact flash
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 2 + impactSize,
                        1,
                        full + fading,
                        fringe
                );
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 4 + impactSize / 2,
                        1,
                        full,
                        core
                );
            }

            //Add the beam to the plugin            
            //public static void addBeam(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe)      
            MagicFakeBeamPlugin.addBeam(full, fading, width, from, angle, MathUtils.getDistance(from, end) + 10, core, fringe);
        }
    }

    /**
     * Fake beam generator. Create a visually convincing beam from arbitrary coordinates.
     * It however has several limitation:
     * - It deal damage instantly and is therefore only meant to be used for burst beams.
     * - It cannot be "cut" by another object passing between the two ends, thus a very short duration is preferable.
     * - Unlike vanilla, it deals full damage to armor, be careful when using HIGH_EXPLOSIVE damage type.
     * It's usage is recommended for short snappy beams or for FXs
     *
     * @param engine        Combat engine
     * @param from          Point of origin of the beam
     * @param range         Maximum range of the beam
     * @param angle         Angle of the beam
     * @param widthIn       Width at the source
     * @param widthOut      Width at the tip
     * @param growth        Width change over time, can be negative
     * @param textureCore   Texture for the core, has to be vertical and loaded in the "fx" category of the settings. Same thing as Magic Trails textures.
     * @param textureFringe Texture of the fringe of the beam
     * @param textureLength Visual length on the texture loop
     * @param textureScroll Scrolling speed of the texture en pixels/s
     * @param smoothIn      fade length at the base of the beam
     * @param smoothOut     fade length at the tip
     * @param full          Duration of the beam at full opacity
     * @param fading        Duration of the beam fading
     * @param impactSize    Size of the impact glow
     * @param core          Core color of the beam
     * @param fringe        Fringe color of the beam
     * @param normalDamage  Base damage of the beam
     * @param type          Damage type
     * @param emp           Emp damage
     * @param source        Damage source to calculate skill and ship damage bonuses
     */

    public static void spawnAdvancedFakeBeam(CombatEngineAPI engine, Vector2f from, float range, float angle, float widthIn, float widthOut, float growth, String textureCore, String textureFringe, float textureLength, float textureScroll, float smoothIn, float smoothOut, float full, float fading, float impactSize, Color core, Color fringe, float normalDamage, DamageType type, float emp, ShipAPI source) {

        CombatEntityAPI theTarget = null;
        float damage = normalDamage;

        //default end point
        Vector2f end = MathUtils.getPoint(from, range, angle);

        //list all nearby entities that could be hit
        List<CombatEntityAPI> entity = CombatUtils.getEntitiesWithinRange(from, range + 500);
        if (!entity.isEmpty()) {
            for (CombatEntityAPI e : entity) {

                //ignore un-hittable stuff like phased ships
                if (e.getCollisionClass() == CollisionClass.NONE) {
                    continue;
                }

                //damage can be reduced against some modded ships
                float newDamage = normalDamage;

                Vector2f col = new Vector2f(1000000, 1000000);
                //ignore everything but ships...
                if (e instanceof ShipAPI) {
                    if (
                            e != source
                                    &&
                                    ((ShipAPI) e).getParentStation() != e
                                    &&
                                    e.getCollisionClass() != CollisionClass.NONE
                                    &&
                                    !(e.getCollisionClass() == CollisionClass.FIGHTER && e.getOwner() == source.getOwner() && !((ShipAPI) e).getEngineController().isFlamedOut())
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {

                        //check for a shield impact, then hull and take the closest one                  
                        ShipAPI s = (ShipAPI) e;

                        //find the collision point with shields/hull
                        Vector2f hitPoint = getShipCollisionPoint(from, end, s, angle);
                        if (hitPoint != null) {
                            col = hitPoint;
                        }

                        //check for modded ships with damage reduction
                        if (s.getHullSpec().getBaseHullId().startsWith("exigency_")) {
                            newDamage = normalDamage / 2;
                        }
                    }
                } else
                    //...and asteroids!
                    if (
                            (e instanceof CombatAsteroidAPI
                                    ||
                                    (e instanceof MissileAPI)
                                            &&
                                            e.getOwner() != source.getOwner()
                            )
                                    &&
                                    CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                    ) {
                        Vector2f cAst = getCollisionPointOnCircumference(from, end, e.getLocation(), e.getCollisionRadius());
                        if (cAst != null) {
                            col = cAst;
                        }
                    }

                //if there was an impact and it is closer than the curent beam end point, set it as the new end point and store the target to apply damage later damage
                if (
                        col.x != 1000000 &&
                                MathUtils.getDistanceSquared(from, col) < MathUtils.getDistanceSquared(from, end)) {
                    end = col;
                    theTarget = e;
                    damage = newDamage;
                }
            }

            //if the beam impacted something, apply the damage
            if (theTarget != null) {

                //damage
                engine.applyDamage(
                        theTarget,
                        end,
                        damage,
                        type,
                        emp,
                        false,
                        true,
                        source
                );
                //impact flash
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 2 + impactSize,
                        1,
                        full + fading,
                        fringe
                );
                engine.addHitParticle(
                        end,
                        new Vector2f(),
                        (float) Math.random() * impactSize / 4 + impactSize / 2,
                        1,
                        full,
                        core
                );
            }

            //Add the beam to the plugin            

            //min length
            if (MathUtils.isWithinRange(from, end, smoothIn + smoothOut)) {
                end = MathUtils.getPoint(from, smoothIn + smoothOut + 2, angle);
            }

            float ID = MagicTrailPlugin.getUniqueID();
            SpriteAPI texture = Global.getSettings().getSprite("fx", textureCore);

            MagicTrailPlugin.addTrailMemberAdvanced(
                    null,
                    ID,
                    texture,
                    from,
                    0,
                    0,
                    angle,
                    0,
                    0,
                    widthIn / 3,
                    widthIn / 3 + growth,
                    core,
                    fringe,
                    1,
                    0,
                    full,
                    fading,
                    GL_SRC_ALPHA,
                    GL_ONE,
                    textureLength,
                    textureScroll,
                    new Vector2f(),
                    null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER,
                    1
            );

            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, MathUtils.getPoint(from, smoothIn, angle), 0, 0, angle, 0, 0, widthIn / 2, widthIn * 0.75f + growth, core, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, MathUtils.getPoint(end, smoothOut, angle + 180), 0, 0, angle, 0, 0, widthOut / 2, widthOut * 0.75f + growth, core, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, end, 0, 0, angle, 0, 0, widthOut / 3, widthOut / 3 + growth, core, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);

            ID = MagicTrailPlugin.getUniqueID();
            texture = Global.getSettings().getSprite("fx", textureFringe);

            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, from, 0, 0, angle, 0, 0, widthIn / 2, widthIn / 2 + growth, fringe, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, MathUtils.getPoint(from, smoothIn, angle), 0, 0, angle, 0, 0, widthIn, widthIn + growth, fringe, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, MathUtils.getPoint(end, smoothOut, angle + 180), 0, 0, angle, 0, 0, widthOut, widthOut + growth, fringe, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
            MagicTrailPlugin.addTrailMemberAdvanced(null, ID, texture, end, 0, 0, angle, 0, 0, widthOut / 2, widthOut / 2 + growth, fringe, fringe, 1, 0, full, fading, GL_SRC_ALPHA, GL_ONE, textureLength, textureScroll, new Vector2f(), null,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER, 1);
        }
    }

    /////////////////////////////////////////
    //                                     //
    //             SHIP HIT                //
    //                                     //
    /////////////////////////////////////////

    // return the collision point of segment segStart to segEnd and a ship (will consider shield).
    // if segment can not hit the ship, will return null.
    // if segStart hit the ship, will return segStart.
    // if segStart hit the shield, will return segStart.
    public static Vector2f getShipCollisionPoint(Vector2f segStart, Vector2f segEnd, ShipAPI ship, float aim) {

        // if target can not be hit, return null
        if (ship.getCollisionClass() == CollisionClass.NONE) {
            return null;
        }
        ShieldAPI shield = ship.getShield();

        // Check hit point when shield is off.
        if (shield == null || shield.isOff()) {
            return CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
        } // If ship's shield is on, thing goes complicated...
        else {

            Vector2f circleCenter = shield.getLocation();
            float circleRadius = shield.getRadius();
            //the beam already start within the shield radius:
            if (MathUtils.isPointWithinCircle(segStart, circleCenter, circleRadius)) {
                if (shield.isWithinArc(segStart)) {
                    return MathUtils.getPoint(segStart, 15, aim);
                } else {
                    return CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
                }
            } else //the beam start from outside:
            {
                Vector2f tmp1 = getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
                if (tmp1 != null && shield.isWithinArc(tmp1)) {
                    return MathUtils.getPoint(tmp1, 1, aim);
                } else {
                    return MathUtils.getPoint(
                            CollisionUtils.getCollisionPoint(segStart, segEnd, ship),
                            1,
                            aim
                    );
                }
            }
        }
    }

    /////////////////////////////////////////
    //                                     //
    //       CIRCLE COLLISION POINT        //
    //                                     //
    /////////////////////////////////////////

    // return the first intersection point of segment segStart to segEnd and circumference.
    // if segStart is outside the circle and segment can not intersection with the circumference, will return null.
    // if segStart is inside the circle, will return segStart.
    public static Vector2f getCollisionPointOnCircumference(Vector2f segStart, Vector2f segEnd, Vector2f circleCenter, float circleRadius) {

        Vector2f startToEnd = Vector2f.sub(segEnd, segStart, null);
        Vector2f startToCenter = Vector2f.sub(circleCenter, segStart, null);
        double ptLineDistSq = (float) Line2D.ptLineDistSq(segStart.x, segStart.y, segEnd.x, segEnd.y, circleCenter.x, circleCenter.y);
        float circleRadiusSq = circleRadius * circleRadius;

        // if lineStart is within the circle, return it directly
        if (startToCenter.lengthSquared() < circleRadiusSq) {
            return segStart;
        }

        // if lineStart is outside the circle and segment can not reach the circumference, return null
        if (ptLineDistSq > circleRadiusSq || startToCenter.length() - circleRadius > startToEnd.length()) {
            return null;
        }

        // calculate the intersection point.
        startToEnd.normalise(startToEnd);
        double dist = Vector2f.dot(startToCenter, startToEnd) - Math.sqrt(circleRadiusSq - ptLineDistSq);
        startToEnd.scale((float) dist);
        return Vector2f.add(segStart, startToEnd, null);
    }
}
