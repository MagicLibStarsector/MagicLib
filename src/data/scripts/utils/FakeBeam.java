// By Tartiflette and DeathFly

package data.scripts.util;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class FakeBeam {

    //
    //Fake beam generator. 
    //
    //Create a visually convincing beam from arbitrary coordinates.
    //It however has several limitation:
    // - It deal damage instantly and is therefore only meant to be used for burst beams.
    // - It cannot be "cut" by another object passing between the two ends, thus a very short duration is preferable.
    // - Unlike vanilla, it deals full damage to armor, be carefull when using HIGH_EXPLOSIVE damage type.
    //
    // Most of the parameters are self explanatory but just in case:
    //
    //engine : combat Engine
    //start : source point of the beam
    //range : maximum effective range of the beam (it will visually fade a few pixels farther)
    //aim : direction of the beam
    //width : width of the beam
    //fading : duration of the beam
    //normalDamage : nominal burst damage of the beam (don't forget to calculate the skill modifiers before that)
    //               may be modified when fighting some modded factions like Exigency.
    //type : damage type of the beam
    //emp : nominal emp damage if any
    //source : ship dealing the damage
    //size : glow size on the impact point
    //duration : duration of the impact glow (should be at least as long as the beam fading)
    //color : color of the impact glow
    //
    //Note that there is no control over the beam's color, you'll have to directly modify the fakeBeamFX.png for that
    //        
    
    /////////////////////////////////////////
    //                                     //
    //             FAKE BEAM               //
    //                                     //
    /////////////////////////////////////////    
        
    public static void applyFakeBeamEffect (CombatEngineAPI engine, Vector2f start, float range, float aim, float width, float fading, float normalDamage, DamageType type, float emp, ShipAPI source, float size, float duration, Color color)
    {            
        CombatEntityAPI theTarget= null;
        float damage = normalDamage;

        //default end point
        Vector2f end = MathUtils.getPointOnCircumference(start,range,aim);

        //list all nearby entities that could be hit
        List <CombatEntityAPI> entity = CombatUtils.getEntitiesWithinRange(start, range+500);
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
                            CollisionUtils.getCollides(start, end, e.getLocation(), e.getCollisionRadius())
                            ){

                        //check for a shield impact, then hull and take the closest one                  
                        ShipAPI s = (ShipAPI) e;

                        //find the collision point with shields/hull
                        Vector2f hitPoint = getShipCollisionPoint(start, end, s, aim);
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
                               CollisionUtils.getCollides(start, end, e.getLocation(), e.getCollisionRadius())
                               ){                               
                    Vector2f cAst = getCollisionPointOnCircumference(start,end,e.getLocation(),e.getCollisionRadius());
                    if ( cAst != null){
                        col = cAst;
                    }
                }

                //if there was an impact and it is closer than the curent beam end point, set it as the new end point and store the target to apply damage later damage
                if (
                        col.x != 1000000 &&
                        MathUtils.getDistanceSquared(start, col) < MathUtils.getDistanceSquared(start, end)) {
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
                        (float)Math.random()*size/2+size,
                        1,
                        (float)Math.random()*duration/2+duration,
                        color
                );
                engine.addHitParticle(
                        end,
                        theTarget.getVelocity(),
                        (float)Math.random()*size/4+size/2,
                        1,
                        0.1f,
                        Color.WHITE
                );
            }           
                
            //create the visual effect
            Map <String,Float> VALUES = new HashMap<>();
            VALUES.put("t", fading); //duration
            VALUES.put("w", width); //width
            VALUES.put("h", MathUtils.getDistance(start, end)+10); //length
            VALUES.put("x", start.x); //origin X
            VALUES.put("y", start.y); //origin Y
            VALUES.put("a", aim); //angle
            
            //Add the beam to the plugin
            FakeBeamPlugin.addMember(VALUES);
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
//            Vector2f circleCenter = shield.getLocation();
//            float circleRadius = shield.getRadius();
//            // calculate the shield collision point
//            Vector2f tmp1 = getCollisionPointOnCircumference(segStart, segEnd, circleCenter, circleRadius);
//            if (tmp1 != null) {
//                // OK! hit the shield in face
//                if (shield.isWithinArc(tmp1)) {
//                    return tmp1;
//                } else {
//                    // if the hit come outside the shield's arc but it hit the shield's "edge", find that point.
//                    boolean hit = false;
//                    Vector2f tmp = new Vector2f(segEnd);
//
//                    //the beam cannot go farther than it's max range or the hull
//                    Vector2f hullHit = CollisionUtils.getCollisionPoint(segStart, segEnd, ship);
//                    if (hullHit != null) {
//                        tmp = hullHit;
//                        hit = true;
//                    }
//                    Vector2f shieldEdge1 = MathUtils.getPointOnCircumference(circleCenter, circleRadius, MathUtils.clampAngle(shield.getFacing() + shield.getActiveArc() / 2));
//                    Vector2f tmp2 = CollisionUtils.getCollisionPoint(segStart, tmp, circleCenter, shieldEdge1);
//                    if (tmp2 != null) {
//                        tmp = tmp2;
//                        hit = true;
//                    }
//                    Vector2f shieldEdge2 = MathUtils.getPointOnCircumference(circleCenter, circleRadius, MathUtils.clampAngle(shield.getFacing() - shield.getActiveArc() / 2));
//                    Vector2f tmp3 = CollisionUtils.getCollisionPoint(segStart, tmp, circleCenter, shieldEdge2);
//                    if (tmp3 != null) {
//                        tmp = tmp3;
//                        hit = true;
//                    }
//                    tmp = MathUtils.getPointOnCircumference(tmp, 1, aim);
//                    // return null if do not hit anything.
//                    return hit ? tmp : null;
//                }
//            }
            
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
