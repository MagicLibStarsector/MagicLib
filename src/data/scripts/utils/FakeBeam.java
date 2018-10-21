// By Tartiflette and DeathFly

package data.scripts.utils;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatAsteroidAPI;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.plugins.FakeBeamPlugin;
import java.awt.geom.Line2D;
import java.util.List;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class FakeBeam {       

    /**
     * Fake beam generator. Create a visually convincing beam from arbitrary coordinates.
     * It however has several limitation:
     * - It deal damage instantly and is therefore only meant to be used for burst beams.
     * - It cannot be "cut" by another object passing between the two ends, thus a very short duration is preferable.
     * - Unlike vanilla, it deals full damage to armor, be careful when using HIGH_EXPLOSIVE damage type.
     * It's usage is recommended for short snappy beams or for FXs
     * 
     * @param engine
     * Combat engine
     * 
     * @param from
     * Point of origin of the beam
     * 
     * @param range
     * Maximum range of the beam
     * 
     * @param angle
     * Angle of the beam
     * 
     * @param width
     * Width of the beam
     * 
     * @param full
     * Duration of the beam at full opacity
     * 
     * @param fading
     * Duration of the beam fading
     * 
     * @param impactSize
     * Size of the impact glow
     * 
     * @param core
     * Core color of the beam
     * 
     * @param fringe
     * Fringe color of the beam
     * 
     * @param normalDamage
     * Base damage of the beam
     * 
     * @param type
     * Damage type
     * 
     * @param emp
     * Emp damage
     * 
     * @param source
     * Damage source to calculate skill and ship damage bonuses
     */
        
    /////////////////////////////////////////
    //                                     //
    //             FAKE BEAM               //
    //                                     //
    /////////////////////////////////////////    
    
    public static void applyFakeBeamEffect (CombatEngineAPI engine, Vector2f from, float range, float angle, float width, float full, float fading, float impactSize, Color core, Color fringe, float normalDamage, DamageType type, float emp, ShipAPI source) {            
        
        CombatEntityAPI theTarget= null;
        float damage = normalDamage;

        //default end point
        Vector2f end = MathUtils.getPointOnCircumference(from,range,angle);

        //list all nearby entities that could be hit
        List <CombatEntityAPI> entity = CombatUtils.getEntitiesWithinRange(from, range+500);
        if (!entity.isEmpty()){
            for (CombatEntityAPI e : entity){

                //ignore un-hittable stuff like phased ships
                if (e.getCollisionClass() == CollisionClass.NONE){continue;}

                //damage can be reduced against some modded ships
                float newDamage = normalDamage;

                Vector2f col = new Vector2f(1000000,1000000);                  
                //ignore everything but ships...
                if (e instanceof ShipAPI ){                    
                    if(                        
                            e!=source
                            &&
                            ((ShipAPI)e).getParentStation()!=e
                            &&
                            e.getCollisionClass()!=CollisionClass.NONE
                            &&
                            !(e.getCollisionClass()==CollisionClass.FIGHTER && e.getOwner()==source.getOwner() && !((ShipAPI)e).getEngineController().isFlamedOut())
                            &&
                            CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                            ){

                        //check for a shield impact, then hull and take the closest one                  
                        ShipAPI s = (ShipAPI) e;

                        //find the collision point with shields/hull
                        Vector2f hitPoint = getShipCollisionPoint(from, end, s, angle);
                        if ( hitPoint != null ){
                            col = hitPoint;
                        }

                        //check for modded ships with damage reduction
                        if (s.getHullSpec().getBaseHullId().startsWith("exigency_")){
                            newDamage = normalDamage/2;
                        }
                    }
                } else 
                    //...and asteroids!
                       if ( 
                               (e instanceof CombatAsteroidAPI 
                               ||
                               (e instanceof MissileAPI)
                               &&
                               e.getOwner()!=source.getOwner()
                               )
                               && 
                               CollisionUtils.getCollides(from, end, e.getLocation(), e.getCollisionRadius())
                               ){                               
                    Vector2f cAst = getCollisionPointOnCircumference(from,end,e.getLocation(),e.getCollisionRadius());
                    if ( cAst != null){
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
            if (theTarget!=null){
                
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
                        theTarget.getVelocity(),
                        (float)Math.random()*impactSize/2+impactSize,
                        1,
                        (float)Math.random()*0.1f+0.15f,
                        fringe
                );
                engine.addHitParticle(
                        end,
                        theTarget.getVelocity(),
                        (float)Math.random()*impactSize/4+impactSize/2,
                        1,
                        0.1f,
                        core
                );
            }           
            
            //Add the beam to the plugin            
            //public static void addBeam(float duration, float fading, float width, Vector2f from, float angle, float length, Color core, Color fringe){            
            FakeBeamPlugin.addBeam(full, fading, width, from, angle, MathUtils.getDistance(from, end)+10, core, fringe);
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
            if (MathUtils.isPointWithinCircle(segStart, circleCenter, circleRadius)){
                if (shield.isWithinArc(segStart)){
                    return MathUtils.getPointOnCircumference(segStart, 15, aim);
                } else {
                    return CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
                }
            } else //the beam start from outside:
            {
                Vector2f tmp1 = getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
                if (tmp1!=null && shield.isWithinArc(tmp1)){
                    return MathUtils.getPointOnCircumference(tmp1, 1, aim);
                } else {
                    return MathUtils.getPointOnCircumference(
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
