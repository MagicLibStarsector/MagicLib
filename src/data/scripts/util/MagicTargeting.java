/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagicTargeting {    
    
    public static enum targetSeeking{
        NO_RANDOM,
        LOCAL_RANDOM,
        FULL_RANDOM,
        IGNORE_SOURCE,
    } 
    
    public static enum missilePriority{
        RANDOM,
        DAMAGE_PRIORITY,
        HIGHTEST_DAMAGE,
    } 
    
    private static Map<ShipAPI.HullSize, Integer> WEIGHT = new HashMap<>();
    
    /**
     * Select a proper target from a missile
     * 
     * @param missile
     * The missile concerned.
     * 
     * @param seeks
     * Does the missile find a random target or tries to hit the ship's one?
     * 
     *  NO_RANDOM, 
     * If the launching ship has a valid target, the missile will pursue it.
     * If there is no target, it will check for an unselected cursor target.
     * If there is none, it will pursue its closest valid threat.    
     *
     *  LOCAL_RANDOM, 
     * If the ship has a target, the missile will pick a random valid threat 
     * around that one. 
     * If the ship has none, the missile will pursue a random valid threat 
     * around the cursor, or itself.
     * Can produce strange behavior if used with a limited search cone.
     * 
     *  FULL_RANDOM, 
     * The missile will always seek a random valid threat around itself.
     * 
     *  IGNORE_SOURCE,
     * The missile will pick the closest target of interest. Useful for MIRVs.
     * 
     * @param maxRange
     * Range in which the missile seek a target in game units.
     * 
     * @param searchCone
     * Angle in which the missile will seek the target. 
     * Set to 360 or more to ignore.
     * 
     * @param fighterWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param frigateWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param destroyerWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param cruiserWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param capitalWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @return 
     * ShipAPI target
     */
    
    public static CombatEntityAPI pickMissileTarget(MissileAPI missile, targetSeeking seeks, Integer maxRange, Integer searchCone, Integer fighterWeight, Integer frigateWeight, Integer destroyerWeight, Integer cruiserWeight, Integer capitalWeight){
        
        CombatEngineAPI engine = Global.getCombatEngine();
        
        ShipAPI theTarget=null;        
        ShipAPI source = missile.getSource();
        
        if(seeks==targetSeeking.LOCAL_RANDOM || seeks==targetSeeking.NO_RANDOM){ 

            //CHECK FOR A SHIP TARGET
            //FULL_RANDOM AND IGNORE_SOURCE ONLY NEED TO LOOK AROUND THE MISSILE ITSELF AND CAN IGNORE THAT PART
        
            if(source!=null && source.isAlive()){ //SOURCE IS ALIVE
                
                if(source.getWeaponGroupFor(missile.getWeapon())!=null ){
                    boolean auto = (
                            source.getWeaponGroupFor(missile.getWeapon()).isAutofiring()                             
                            && (
                                engine.getPlayerShip()!=source 
                                || 
                                source.getSelectedGroupAPI()!=source.getWeaponGroupFor(missile.getWeapon()
                                )
                            ));
                    
                    if(auto){
                        //WEAPON IN AUTOFIRE
                        theTarget=source.getWeaponGroupFor(missile.getWeapon()).getAutofirePlugin(missile.getWeapon()).getTargetShip();
                    } else { //WEAPON IS PURPOSELY FIRED
                        theTarget=source.getShipTarget();
                    }
                }

                //pointer target if needed
                if(theTarget==null){
                    for(ShipAPI s : engine.getShips()){
                        if(s.isAlive() && s.getOwner()!= missile.getOwner() && MathUtils.isWithinRange(s,source.getMouseTarget(),100)){
                            theTarget=s;
                        }                    
                    }
                }

                //targeting failsafes:
                if(theTarget!=null && (!theTarget.isAlive() || !Global.getCombatEngine().isEntityInPlay(theTarget) || theTarget.getOwner()==source.getOwner() || !CombatUtils.isVisibleToSide(theTarget, missile.getOwner()))){
                    theTarget=null;
                }

                //if there is a current target and the missile doesn't target randomly, look no further.
                //Manual targeting ignore the size priorities.            
                //////////
                if(theTarget!=null && seeks==targetSeeking.NO_RANDOM){
                    return theTarget;
                }
                //////////
            }
        }
        
        //PRIORITY WEIGHTS:   
        WEIGHT.put(ShipAPI.HullSize.FIGHTER, fighterWeight);            
        WEIGHT.put(ShipAPI.HullSize.FRIGATE, frigateWeight);
        WEIGHT.put(ShipAPI.HullSize.DESTROYER, destroyerWeight);
        WEIGHT.put(ShipAPI.HullSize.CRUISER, cruiserWeight);
        WEIGHT.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalWeight);            
        
        
        if(seeks==targetSeeking.IGNORE_SOURCE || seeks==targetSeeking.NO_RANDOM){
            
            //NO RANDOM / IGNORE_SOURCE:
            //GET CLOSEST VALID TARGET
            //ONLY TRIGGERS FOR NO_RANDOM IF THERE WASN'T ALREADY A SHIP TARGET
            theTarget = getClosestTargetInCone(engine, missile, maxRange, Math.max(25, Math.min(360, searchCone)));
            
        } else {      
            //FULL/LOCAL RANDOM:
            //WHERE TO LOOK        
            Vector2f lookAround;
            if(seeks == targetSeeking.LOCAL_RANDOM && theTarget!=null){
                lookAround=new Vector2f (theTarget.getLocation());
            } else {
                lookAround = new Vector2f (missile.getLocation());
            }
            
            //GET A RANDOM TARGET AROUND THAT POINT
            theTarget = getRandomTargetInCone(engine, missile, lookAround, maxRange, Math.max(25, Math.min(360, searchCone)));
        }
        
        
        //////////
        if(theTarget!=null){
            return theTarget;
        }        
        //////////
        
        
        //NORMAL PICK FAILED, GET THE CLOSEST TARGET (ignore the search cone and the range)
        theTarget=AIUtils.getNearestEnemy(missile);
        
        //////////
        if (theTarget!=null && WEIGHT.get(theTarget.getHullSize())>0 && CombatUtils.isVisibleToSide(theTarget, missile.getOwner())){
            return theTarget;
        }
        //////////
        
        //STILL NO TARGET, RETURN NULL
        
        //////////
        return null;
        //////////
        
    }
    
    /**
     * Select a proper target from a ship
     * 
     * @param source
     * The ship concerned.
     * 
     * @param seeks
     * Does the ship find a random target or tries to hit its selected one?
     * 
     *  NO_RANDOM, 
     * If the ship has a valid target, the script will return it.
     * If there is no target, it will check for an unselected cursor target.
     * If there is none, it will return its closest valid threat.    
     *
     *  LOCAL_RANDOM, 
     * If the ship has a target, the script will pick a random valid threat around that one. 
     * If the ship has none, the script will pick a random valid threat around the cursor, 
     * If none are found, the script will pick a random valid threat around itself, 
     * Can produce strange behavior if used with a limited search cone.
     * 
     *  FULL_RANDOM, 
     * The script will always return a random valid threat around the ship itself.
     * 
     *  IGNORE_SOURCE,
     * The script will pick the closest target of interest.
     * 
     * @param maxRange
     * Range within which the script seeks a target in game units.
     * 
     * @param searchCone
     * Angle within which the script will seek the target. 
     * Set to 360 or more to ignore.
     * 
     * @param fighterWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param frigateWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param destroyerWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param cruiserWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @param capitalWeight
     * Target priority, set to 0 to ignore that class. 
     * Other values only used for random targeting.
     * 
     * @return 
     * ShipAPI target
     */
    public static CombatEntityAPI pickShipTarget(ShipAPI source, targetSeeking seeks, Integer maxRange, Integer searchCone, Integer fighterWeight, Integer frigateWeight, Integer destroyerWeight, Integer cruiserWeight, Integer capitalWeight){
        
        CombatEngineAPI engine = Global.getCombatEngine();
        
        ShipAPI theTarget=null;
        
        if(seeks==targetSeeking.LOCAL_RANDOM || seeks==targetSeeking.NO_RANDOM){ 

            //CHECK FOR A SHIP TARGET
            //FULL_RANDOM AND IGNORE_SOURCE ONLY NEED TO LOOK AROUND THE MISSILE ITSELF AND CAN IGNORE THAT PART
                
            if(source.getShipTarget()!=null ){
                    theTarget=source.getShipTarget();
            } else {
            //pointer target if needed
                for(ShipAPI s : engine.getShips()){
                    if(s.isAlive() && s.getOwner()!= source.getOwner() && MathUtils.isWithinRange(s,source.getMouseTarget(),100)){
                        theTarget=s;
                        break;
                    } 
                }
            }

            //targeting failsafes:
            if(theTarget!=null && (!theTarget.isAlive() || !Global.getCombatEngine().isEntityInPlay(theTarget) || theTarget.getOwner()==source.getOwner() || !CombatUtils.isVisibleToSide(theTarget, source.getOwner()))){
                theTarget=null;
            }

            //if there is a current target and the missile doesn't target randomly, look no further.
            //Manual targeting ignore the size priorities.            
            //////////
            if(theTarget!=null && seeks==targetSeeking.NO_RANDOM){
                return theTarget;
            }
            //////////
            
        }
        
        //PRIORITY WEIGHTS:   
        WEIGHT.put(ShipAPI.HullSize.FIGHTER, fighterWeight);            
        WEIGHT.put(ShipAPI.HullSize.FRIGATE, frigateWeight);
        WEIGHT.put(ShipAPI.HullSize.DESTROYER, destroyerWeight);
        WEIGHT.put(ShipAPI.HullSize.CRUISER, cruiserWeight);
        WEIGHT.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalWeight);            
        
        
        if(seeks==targetSeeking.IGNORE_SOURCE || seeks==targetSeeking.NO_RANDOM){
            
            //NO RANDOM / IGNORE_SOURCE:
            //GET CLOSEST VALID TARGET
            //ONLY TRIGGERS FOR NO_RANDOM IF THERE WASN'T ALREADY A SHIP TARGET
            theTarget = getClosestTargetInCone(engine, source, maxRange, Math.max(25, Math.min(360, searchCone)));
            
        } else {      
            //FULL/LOCAL RANDOM:
            //WHERE TO LOOK        
            Vector2f lookAround;
            if(seeks == targetSeeking.LOCAL_RANDOM && theTarget!=null){
                lookAround=new Vector2f (theTarget.getLocation());
            } else {
                lookAround = new Vector2f (source.getLocation());
            }
            
            //GET A RANDOM TARGET AROUND THAT POINT
            theTarget = getRandomTargetInCone(engine, source, lookAround, maxRange, Math.max(25, Math.min(360, searchCone)));
        }
        
        
        //////////
        if(theTarget!=null){
            return theTarget;
        }        
        //////////
        
        
        //NORMAL PICK FAILED, GET THE CLOSEST TARGET (ignore the search cone and the range)
        theTarget=AIUtils.getNearestEnemy(source);
        
        //////////
        if (theTarget!=null && WEIGHT.get(theTarget.getHullSize())>0 && CombatUtils.isVisibleToSide(theTarget, source.getOwner())){
            return theTarget;
        }
        //////////
        
        //STILL NO TARGET, RETURN NULL
        
        //////////
        return null;
        //////////
        
    }
    
    
    /**
     * Picks a random enemy missile within parameters:
     * 
     * @param source
     * CombatEntity looking for a missile
     * 
     * @param priority
     * Does the source look for a complete random or has priorities
     * 
     *  RANDOM, 
     * Pure random pick within search zone.
     *  
     *  DAMAGE_PRIORITY
     * Picks high damage missiles within the search zone first but still has some randomness.
     * 
     *  HIGHEST_DAMAGE
     * Picks the highest damage missile within the search zone.
     *
     * @param lookAround
     * Point around which to look for missiles;
     * 
     * @param direction
     * Direction to look at (only used with a limited search cone)
     * 
     * @param searchCone
     * Angle within which the script will seek the target. 
     * Set to 360 or more to ignore.
     * 
     * @param maxRange
     * Range within which the script seeks a target in game units.
     * 
     * @return 
     */
    public static MissileAPI randomMissile(CombatEntityAPI source, missilePriority priority, Vector2f lookAround, float direction, Integer searchCone, Integer maxRange){
        
        CombatEngineAPI engine = Global.getCombatEngine();
        MissileAPI candidate = null;
        boolean allAspect=(searchCone>=360);
        
        WeightedRandomPicker<MissileAPI> missilePicker = new WeightedRandomPicker<>();
        
        List<MissileAPI> missiles = engine.getMissiles();
        if(missiles.isEmpty()){
            return null;
        }
        
        for (MissileAPI m : missiles){
            
            if(!m.isFading() && m.getOwner()!=source.getOwner()){ //is the missile alive and hostile
                
                if(CombatUtils.isVisibleToSide(m, source.getOwner()) && MathUtils.isPointWithinCircle(lookAround, m.getLocation(), maxRange)){ //is it around
                    
                    if(allAspect || Math.abs(MathUtils.getShortestRotation(source.getFacing(), VectorUtils.getAngle(source.getLocation(), m.getLocation())))<searchCone/2){ //is it within cone of attack
                        
                        switch(priority){
                            
                            case RANDOM: //random target, all missiles have the same weight
                                missilePicker.add(m);
                                break;
                            
                            case DAMAGE_PRIORITY: //damage priority, all missiles have their damage as weight
                                missilePicker.add(m, m.getDamageAmount());
                                break;
                                
                            case HIGHTEST_DAMAGE: //highest damage selected outright
                                if(candidate==null){
                                    candidate=m;
                                } else 
                                    if(m.getDamageAmount()>candidate.getDamageAmount()){
                                        candidate=m;
                                    }
                                break;
                                
                            default:
                           
                        }
                        
                    }
                    
                }
                
            }
            
        }
        //there is a candidate,
        if(candidate!=null){
            return candidate;
        }
        //no candidate, try to pick random
        if(!missilePicker.isEmpty()){
            return missilePicker.pick();
        }
        //nothing? return null
        return null;
    }
    
    private static ShipAPI getClosestTargetInCone(CombatEngineAPI engine, CombatEntityAPI source, Integer maxRange, Integer searchCone){
        ShipAPI candidate = null;
        boolean allAspect=(searchCone>=360);
        Integer range= maxRange*maxRange;
        
        for (ShipAPI s : engine.getShips()){
            
            if(s.isAlive() && s.getOwner()!=source.getOwner() && WEIGHT.get(s.getHullSize())>0){ //is the ship targetable
                
                if(CombatUtils.isVisibleToSide(s, source.getOwner()) && MathUtils.getDistanceSquared(source, s)<range){ //is it closer
                    
                    if(allAspect || Math.abs(MathUtils.getShortestRotation(source.getFacing(), VectorUtils.getAngle(source.getLocation(), s.getLocation())))<searchCone/2){ //is it in cone
                        
                        candidate=s;
                        range=(int)MathUtils.getDistanceSquared(source, s);
                        
                    }
                }
            }            
        }
        return candidate;
    }
    
    
    private static ShipAPI getRandomTargetInCone(CombatEngineAPI engine, CombatEntityAPI source, Vector2f lookAround, Integer maxRange, Integer searchCone){
        ShipAPI candidate = null;
        boolean allAspect=(searchCone>=360); 
        
        WeightedRandomPicker<ShipAPI> targetPicker = new WeightedRandomPicker<>();
        
        for (ShipAPI s : engine.getShips()){
            
            if(s.isAlive() && s.getOwner()!=source.getOwner() && WEIGHT.get(s.getHullSize())>0){ //is the ship targetable
                
                if(CombatUtils.isVisibleToSide(s, source.getOwner()) && MathUtils.isWithinRange(lookAround, s.getLocation(),maxRange)){ //is it close
                    
                    if(allAspect || Math.abs(MathUtils.getShortestRotation(source.getFacing(), VectorUtils.getAngle(source.getLocation(), s.getLocation())))<searchCone/2){ //is it in cone
                        
                        targetPicker.add(s, WEIGHT.get(s.getHullSize()));
                        
                    }
                }
            }
        }
        
        if(!targetPicker.isEmpty()){
            candidate = targetPicker.pick();
        }
        
        return candidate;
    }            
}