/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class MagicTargeting {
    public enum targetSeeking {
        NO_RANDOM,
        LOCAL_RANDOM,
        FULL_RANDOM,
        IGNORE_SOURCE,
    }

    public enum missilePriority {
        RANDOM,
        DAMAGE_PRIORITY,
        HIGHTEST_DAMAGE,
    }

    private static final Map<ShipAPI.HullSize, Integer> WEIGHT = new HashMap<>();

    /**
     * Generic target picker
     * Will always fall back on the closest target if none are found within the search parameters
     *
     * @param seeker          The CombatEntity concerned. Can only be a ship or a missile.
     * @param seeks           Does the missile find a random target or tries to hit the ship's one?
     *                        <p>
     *                        NO_RANDOM,
     *                        If the launching ship has a valid target within arc, the missile will pursue it.
     *                        If there is no target, it will check for an unselected cursor target within arc.
     *                        If there is none, it will pursue its closest valid threat within arc.
     *                        <p>
     *                        LOCAL_RANDOM,
     *                        If the ship has a target, the missile will pick a random valid threat around that one.
     *                        If the ship has none, the missile will pursue a random valid threat around the cursor, or itself.
     *                        Can produce strange behavior if used with a limited search cone.
     *                        <p>
     *                        FULL_RANDOM,
     *                        The missile will always seek a random valid threat within arc around itself.
     *                        <p>
     *                        IGNORE_SOURCE,
     *                        The missile will pick the closest target of interest. Useful for custom MIRVs.
     * @param maxRange        Range in which the missile seek a target in game units.
     * @param searchCone      Angle in which the missile will seek the target.
     *                        Set to 360 or more to ignore.
     * @param fighterWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param frigateWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param destroyerWeight Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param cruiserWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param capitalWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param failsafe        fallback option: if no suitable target is found within the search cone and distance the script will pick the closest possible target as long as it is within twice the max range.
     * @return ShipAPI target
     */

    public static ShipAPI pickTarget(CombatEntityAPI seeker, targetSeeking seeks, Integer maxRange, Integer searchCone, Integer fighterWeight, Integer frigateWeight, Integer destroyerWeight, Integer cruiserWeight, Integer capitalWeight, boolean failsafe) {

        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI theTarget;
        WeaponAPI weapon = null;
        ShipAPI source;
        if (seeker instanceof MissileAPI) {
            weapon = ((MissileAPI) seeker).getWeapon();
            source = ((MissileAPI) seeker).getSource();
        } else {
            source = (ShipAPI) seeker;
        }

        //PRIORITY WEIGHTS:   
        WEIGHT.put(ShipAPI.HullSize.FIGHTER, fighterWeight);
        WEIGHT.put(ShipAPI.HullSize.FRIGATE, frigateWeight);
        WEIGHT.put(ShipAPI.HullSize.DESTROYER, destroyerWeight);
        WEIGHT.put(ShipAPI.HullSize.CRUISER, cruiserWeight);
        WEIGHT.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalWeight);

        switch (seeks) {
            case NO_RANDOM:

                theTarget = getDirectTarget(engine, source, weapon, seeker.getLocation(), seeker.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) { //if there are none, get closest valid target
                    theTarget = getClosestTargetInCone(engine, seeker, maxRange, searchCone, failsafe);
                }

                return theTarget;

            case LOCAL_RANDOM:

                theTarget = getDirectTarget(engine, source, weapon, seeker.getLocation(), seeker.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) {
                    theTarget = getRandomTargetInCone(engine, seeker, seeker.getLocation(), maxRange, searchCone, failsafe); //if there are none, pick a random threat around the missile
                } else {
                    theTarget = getRandomTargetInCone(engine, seeker, theTarget.getLocation(), maxRange, searchCone, failsafe); //else pick a random threat around the direct target 
                }
                return theTarget;

            case FULL_RANDOM:

                return getRandomTargetInCone(engine, seeker, seeker.getLocation(), maxRange, searchCone, failsafe); //pick a random threat around the missile

            case IGNORE_SOURCE:

                return getClosestTargetInCone(engine, seeker, maxRange, searchCone, failsafe);

            default:
                return null;
        }
    }

    /**
     * Picks a random enemy missile within parameters:
     *
     * @param source     CombatEntity looking for a missile
     * @param priority   Does the source look for a complete random or has priorities
     *                   <p>
     *                   RANDOM,
     *                   Pure random pick within search zone.
     *                   <p>
     *                   DAMAGE_PRIORITY
     *                   Picks high damage missiles within the search zone first but still has some randomness.
     *                   <p>
     *                   HIGHEST_DAMAGE
     *                   Picks the highest damage missile within the search zone.
     * @param lookAround Point around which to look for missiles;
     * @param direction  Direction to look at (only used with a limited search cone)
     * @param searchCone Angle within which the script will seek the target.
     *                   Set to 360 or more to ignore.
     * @param maxRange   Range within which the script seeks a target in game units.
     * @return
     */
    public static MissileAPI randomMissile(CombatEntityAPI source, missilePriority priority, Vector2f lookAround, float direction, Integer searchCone, Integer maxRange) {
        return randomMissile(source, priority, lookAround, direction, searchCone, maxRange, false);
    }

    /**
     * Picks a random enemy missile within parameters:
     *
     * @param source       CombatEntity looking for a missile
     * @param priority     Does the source look for a complete random or has priorities
     *                     <p>
     *                     RANDOM,
     *                     Pure random pick within search zone.
     *                     <p>
     *                     DAMAGE_PRIORITY
     *                     Picks high damage missiles within the search zone first but still has some randomness.
     *                     <p>
     *                     HIGHEST_DAMAGE
     *                     Picks the highest damage missile within the search zone.
     * @param lookAround   Point around which to look for missiles;
     * @param direction    Direction to look at (only used with a limited search cone)
     * @param searchCone   Angle within which the script will seek the target.
     *                     Set to 360 or more to ignore.
     * @param maxRange     Range within which the script seeks a target in game units.
     * @param ignoreFlares
     * @return
     */
    public static MissileAPI randomMissile(CombatEntityAPI source, missilePriority priority, Vector2f lookAround, float direction, Integer searchCone, Integer maxRange, boolean ignoreFlares) {

        CombatEngineAPI engine = Global.getCombatEngine();
        MissileAPI candidate = null;
        boolean allAspect = (searchCone >= 360);

        WeightedRandomPicker<MissileAPI> missilePicker = new WeightedRandomPicker<>();

        List<MissileAPI> missiles = engine.getMissiles();
        if (missiles.isEmpty()) {
            return null;
        }

        for (MissileAPI m : missiles) {

            if (!ignoreFlares || !m.isFlare()) {

                if (!m.isFading() && m.getOwner() != source.getOwner() && m.getCollisionClass() != CollisionClass.NONE && m.getSpec().isRenderTargetIndicator()) { //is the missile alive, hittable and hostile

                    if (CombatUtils.isVisibleToSide(m, source.getOwner()) && MathUtils.isPointWithinCircle(lookAround, m.getLocation(), maxRange)) { //is it around

                        if (allAspect || Math.abs(MathUtils.getShortestRotation(direction, VectorUtils.getAngle(source.getLocation(), m.getLocation()))) < searchCone / 2) { //is it within cone of attack

                            switch (priority) {

                                case RANDOM: //random target, all missiles have the same weight
                                    missilePicker.add(m, 1);
                                    break;

                                case DAMAGE_PRIORITY: //damage priority, all missiles have their damage as weight
                                    missilePicker.add(m, m.getDamageAmount());
                                    break;

                                case HIGHTEST_DAMAGE: //highest damage selected outright
                                    if (candidate == null) {
                                        candidate = m;
                                    } else if (m.getDamageAmount() > candidate.getDamageAmount()) {
                                        candidate = m;
                                    }
                                    break;

                                default:
                            }
                        }
                    }
                }
            }
        }

        //there is a candidate,
        if (priority == missilePriority.HIGHTEST_DAMAGE) {
            return candidate;
        }

        //no candidate, try to pick random
        if (!missilePicker.isEmpty()) {
            return missilePicker.pick(Misc.random);
        }
        //nothing? return null
        return null;
    }

    /**
     * Select a proper target from a missile
     *
     * @param missile         The missile concerned.
     * @param seeks           Does the missile find a random target or tries to hit the ship's one?
     *                        <p>
     *                        NO_RANDOM,
     *                        If the launching ship has a valid target within arc, the missile will pursue it.
     *                        If there is no target, it will check for an unselected cursor target within arc.
     *                        If there is none, it will pursue its closest valid threat within arc.
     *                        <p>
     *                        LOCAL_RANDOM,
     *                        If the ship has a target, the missile will pick a random valid threat around that one.
     *                        If the ship has none, the missile will pursue a random valid threat around the cursor, or itself.
     *                        Can produce strange behavior if used with a limited search cone.
     *                        <p>
     *                        FULL_RANDOM,
     *                        The missile will always seek a random valid threat within arc around itself.
     *                        <p>
     *                        IGNORE_SOURCE,
     *                        The missile will pick the closest target of interest. Useful for custom MIRVs.
     * @param maxRange        Range in which the missile seek a target in game units.
     * @param searchCone      Angle in which the missile will seek the target.
     *                        Set to 360 or more to ignore.
     * @param fighterWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param frigateWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param destroyerWeight Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param cruiserWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param capitalWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @return ShipAPI target
     */

    public static ShipAPI pickMissileTarget(MissileAPI missile, targetSeeking seeks, Integer maxRange, Integer searchCone, Integer fighterWeight, Integer frigateWeight, Integer destroyerWeight, Integer cruiserWeight, Integer capitalWeight) {

        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI theTarget;
        ShipAPI source = missile.getSource();

        //PRIORITY WEIGHTS:   
        WEIGHT.put(ShipAPI.HullSize.FIGHTER, fighterWeight);
        WEIGHT.put(ShipAPI.HullSize.FRIGATE, frigateWeight);
        WEIGHT.put(ShipAPI.HullSize.DESTROYER, destroyerWeight);
        WEIGHT.put(ShipAPI.HullSize.CRUISER, cruiserWeight);
        WEIGHT.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalWeight);

        switch (seeks) {
            case NO_RANDOM:

                theTarget = getDirectTarget(engine, source, missile.getWeapon(), missile.getLocation(), missile.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) { //if there are none, get closest valid target

                    theTarget = getClosestTargetInCone(engine, missile, maxRange, searchCone, false);
                }

                return theTarget;

            case LOCAL_RANDOM:

                theTarget = getDirectTarget(engine, source, missile.getWeapon(), missile.getLocation(), missile.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) {
                    theTarget = getRandomTargetInCone(engine, missile, missile.getLocation(), maxRange, searchCone, false); //if there are none, pick a random threat around the missile
                } else {
                    theTarget = getRandomTargetInCone(engine, missile, theTarget.getLocation(), maxRange, searchCone, false); //else pick a random threat around the direct target 
                }
                return theTarget;

            case FULL_RANDOM:

                return getRandomTargetInCone(engine, missile, missile.getLocation(), maxRange, searchCone, false); //pick a random threat around the missile

            case IGNORE_SOURCE:

                return getClosestTargetInCone(engine, missile, maxRange, searchCone, false);

            default:
                return null;
        }
    }

    /**
     * Select a proper target from a ship
     *
     * @param source          The ship concerned.
     * @param seeks           Does the ship find a random target or tries to hit its selected one?
     *                        <p>
     *                        NO_RANDOM,
     *                        If the ship has a valid target, the script will return it.
     *                        If there is no target, it will check for an unselected cursor target.
     *                        If there is none, it will return its closest valid threat.
     *                        <p>
     *                        LOCAL_RANDOM,
     *                        If the ship has a target, the script will pick a random valid threat around that one.
     *                        If the ship has none, the script will pick a random valid threat around the cursor,
     *                        If none are found, the script will pick a random valid threat around itself,
     *                        Can produce strange behavior if used with a limited search cone.
     *                        <p>
     *                        FULL_RANDOM,
     *                        The script will always return a random valid threat around the ship itself.
     *                        <p>
     *                        IGNORE_SOURCE,
     *                        The script will pick the closest target of interest.
     * @param maxRange        Range within which the script seeks a target in game units.
     * @param searchCone      Angle within which the script will seek the target.
     *                        Set to 360 or more to ignore.
     * @param fighterWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param frigateWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param destroyerWeight Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param cruiserWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @param capitalWeight   Target priority, set to 0 to ignore that class.
     *                        Other values only used for random targeting.
     * @return ShipAPI target
     */
    public static ShipAPI pickShipTarget(ShipAPI source, targetSeeking seeks, Integer maxRange, Integer searchCone, Integer fighterWeight, Integer frigateWeight, Integer destroyerWeight, Integer cruiserWeight, Integer capitalWeight) {

        CombatEngineAPI engine = Global.getCombatEngine();

        ShipAPI theTarget;

        //PRIORITY WEIGHTS:   
        WEIGHT.put(ShipAPI.HullSize.FIGHTER, fighterWeight);
        WEIGHT.put(ShipAPI.HullSize.FRIGATE, frigateWeight);
        WEIGHT.put(ShipAPI.HullSize.DESTROYER, destroyerWeight);
        WEIGHT.put(ShipAPI.HullSize.CRUISER, cruiserWeight);
        WEIGHT.put(ShipAPI.HullSize.CAPITAL_SHIP, capitalWeight);

        switch (seeks) {
            case NO_RANDOM:

                theTarget = getDirectTarget(engine, source, null, source.getLocation(), source.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) { //if there are none, get closest valid target

                    theTarget = getClosestTargetInCone(engine, source, maxRange, searchCone, false);
                }

                return theTarget;

            case LOCAL_RANDOM:

                theTarget = getDirectTarget(engine, source, null, source.getLocation(), source.getFacing(), searchCone); //get deliberate target

                if (theTarget == null) {
                    theTarget = getRandomTargetInCone(engine, source, source.getLocation(), maxRange, searchCone, false); //if there are none, pick a random threat around the missile
                } else {
                    theTarget = getRandomTargetInCone(engine, source, theTarget.getLocation(), maxRange, searchCone, false); //else pick a random threat around the direct target 
                }
                return theTarget;

            case FULL_RANDOM:

                return getRandomTargetInCone(engine, source, source.getLocation(), maxRange, searchCone, false); //pick a random threat around the missile

            case IGNORE_SOURCE:

                return getClosestTargetInCone(engine, source, maxRange, searchCone, false);

            default:
                return null;
        }
    }

    private static ShipAPI getDirectTarget(CombatEngineAPI engine, ShipAPI source, WeaponAPI checkWeaponGroup, Vector2f loc, float aim, Integer searchCone) {
        if (source != null && source.isAlive()) { //SOURCE IS ALIVE
            boolean allAspect = (searchCone >= 360);
            //AUTO FIRE TARGET
            if (checkWeaponGroup != null) {
                if (source.getWeaponGroupFor(checkWeaponGroup) != null) {
                    //WEAPON IN AUTOFIRE
                    if (source.getWeaponGroupFor(checkWeaponGroup).isAutofiring()  //weapon group is autofiring
                            && source.getSelectedGroupAPI() != source.getWeaponGroupFor(checkWeaponGroup)) { //weapon group is not the selected group
                        ShipAPI weaponTarget = source.getWeaponGroupFor(checkWeaponGroup).getAutofirePlugin(checkWeaponGroup).getTargetShip();
                        if (weaponTarget != null //weapon target exist
                                && (
                                allAspect // either missile has full arc
                                        || Math.abs(MathUtils.getShortestRotation(aim, VectorUtils.getAngle(loc, weaponTarget.getLocation()))) < searchCone / 2 //or missile has limited arc and the target is within
                        )
                        ) {
                            //then return the auto-fire target
                            return source.getWeaponGroupFor(checkWeaponGroup).getAutofirePlugin(checkWeaponGroup).getTargetShip();
                        }
                    }
                }
            }

            //SHIP TARGET
            ShipAPI shipTarget = source.getShipTarget();
            if (
                    shipTarget != null
                            && shipTarget.isAlive()
                            && shipTarget.getOwner() != source.getOwner()
                            && CombatUtils.isVisibleToSide(shipTarget, source.getOwner())
                            && (
                            allAspect
                                    || Math.abs(MathUtils.getShortestRotation(aim, VectorUtils.getAngle(loc, shipTarget.getLocation()))) < searchCone / 2
                    )
            ) {
                return shipTarget;
            }

            //POINTER TARGET
            for (ShipAPI s : engine.getShips()) {
                if (
                        s.isAlive()
                                && s.getOwner() != source.getOwner()
                                && CombatUtils.isVisibleToSide(s, source.getOwner())
                                && MathUtils.isWithinRange(s, source.getMouseTarget(), 100)
                                && (
                                allAspect
                                        || Math.abs(MathUtils.getShortestRotation(aim, VectorUtils.getAngle(loc, s.getLocation()))) < searchCone / 2
                        )
                ) {
                    return s;
                }
            }
        }

        //nothing fits
        return null;
    }

    private static ShipAPI getClosestTargetInCone(CombatEngineAPI engine, CombatEntityAPI source, Integer maxRange, Integer searchCone, boolean failsafe) {
        ShipAPI candidate = null;
        ShipAPI backup = null;
        boolean allAspect = (searchCone >= 360);
        Integer range = maxRange * maxRange;

        for (ShipAPI s : engine.getShips()) {

            if (s.isAlive() && s.getOwner() != source.getOwner() && WEIGHT.get(s.getHullSize()) > 0) { //is the ship targetable

                if (CombatUtils.isVisibleToSide(s, source.getOwner())) {

                    if (MathUtils.getDistanceSquared(source, s) < range) { //is it closer

                        if (allAspect || Math.abs(MathUtils.getShortestRotation(source.getFacing(), VectorUtils.getAngle(source.getLocation(), s.getLocation()))) < searchCone / 2) { //is it in cone
                            candidate = s;
                            range = (int) MathUtils.getDistanceSquared(source, s);
                        } else {
                            backup = s;
                        }
                    } else if (backup == null && MathUtils.getDistanceSquared(source, s) < 4 * range) {
                        backup = s;
                    }
                }
            }
        }
        if (failsafe && candidate == null) {
            return backup;
        }
        return candidate;
    }

    private static ShipAPI getRandomTargetInCone(CombatEngineAPI engine, CombatEntityAPI source, Vector2f lookAround, Integer maxRange, Integer searchCone, boolean failsafe) {
        ShipAPI candidate = null;
        ShipAPI backup = null;
        boolean allAspect = (searchCone >= 360);

        Integer range = 4 * maxRange * maxRange;

        WeightedRandomPicker<ShipAPI> targetPicker = new WeightedRandomPicker<>();

        for (ShipAPI s : engine.getShips()) {

            if (s.isAlive() && s.getOwner() != source.getOwner() && WEIGHT.get(s.getHullSize()) > 0) { //is the ship targetable

                if (CombatUtils.isVisibleToSide(s, source.getOwner())) {
                    if (MathUtils.isWithinRange(lookAround, s.getLocation(), maxRange)) { //is it close

                        if (allAspect || Math.abs(MathUtils.getShortestRotation(source.getFacing(), VectorUtils.getAngle(source.getLocation(), s.getLocation()))) < searchCone / 2) { //is it in cone

                            targetPicker.add(s, WEIGHT.get(s.getHullSize()));

                        } else if (backup == null || MathUtils.getDistanceSquared(lookAround, s.getLocation()) < range) {
                            backup = s;
                            range = (int) MathUtils.getDistanceSquared(lookAround, s.getLocation());
                        }
                    } else if (backup == null && MathUtils.isWithinRange(lookAround, s.getLocation(), maxRange * 2)) {
                        backup = s;
                        range = (int) MathUtils.getDistanceSquared(lookAround, s.getLocation());
                    }
                }
            }
        }

        if (!targetPicker.isEmpty()) {
            candidate = targetPicker.pick();
        }

        if (failsafe && candidate == null) {
            return backup;
        }
        return candidate;
    }
}