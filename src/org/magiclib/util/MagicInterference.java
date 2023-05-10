
package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Tartiflette
 */
public class MagicInterference {

    private static final String INTERFERENCE_HULLMOD = "ML_interferenceWarning";

    //////////////////////////////
    //                          //
    //   INTERFERENCE EFFECT    //
    //                          //
    //////////////////////////////

    /**
     * Reduces a ship's passive dissipation if more than one weapon causing interferences is installed. The strength of the effect has a quadratic growth with the number of such weapons installed.
     *
     * @param shipVariant Variant of the ship affected by the interference debuff
     */
    public static void applyInterference(ShipVariantAPI shipVariant) {
        ApplyInterference(shipVariant);
    }

    /**
     * Reduces a ship's passive dissipation if more than one weapon causing interferences is installed. The strength of the effect has a quadratic growth with the number of such weapons installed.
     *
     * @param shipVariant Variant of the ship affected by the interference debuff
     * @deprecated use applyInterference instead (follows proper naming convention)
     */
    public static void ApplyInterference(ShipVariantAPI shipVariant) {
        //get interference data
        if (RATES.isEmpty() || Global.getSettings().isDevMode()) {
            loadInterference();
        }

        if (getTotalInterference(shipVariant) > 0) {
            if (!shipVariant.getHullMods().contains(INTERFERENCE_HULLMOD)) {
                shipVariant.getHullMods().add(INTERFERENCE_HULLMOD);
            }
        } else if (shipVariant.getHullMods().contains(INTERFERENCE_HULLMOD)) {
            shipVariant.getHullMods().remove(INTERFERENCE_HULLMOD);
        }
    }


    //////////////////////////////
    //                          //
    //    INTERFERENCE DATA     //
    //                          //
    //////////////////////////////            

    private static final Logger LOG = Global.getLogger(MagicInterference.class);
    private static final Map<String, Float> WEAPONS = new HashMap<>();
    private static Map<String, Float> RATES = new HashMap<>();
    private static float RFC_MULT = 0;

    public static void loadInterference() {

        WEAPONS.clear();
        RATES.clear();

        RATES = MagicSettings.getFloatMap(MagicVariables.MAGICLIB_ID, "interferences_rates");
        RFC_MULT = MagicSettings.getFloat(MagicVariables.MAGICLIB_ID, "interference_RFCmult");
        Map<String, String> rawWeapons = MagicSettings.getStringMap(MagicVariables.MAGICLIB_ID, "interferences_weapons");
        for (Entry<String, String> w : rawWeapons.entrySet()) {
            WEAPONS.put(w.getKey(), RATES.get(w.getValue()));
        }
    }


    //////////////////////////////
    //                          //
    // INTERFERENCE COMPUTATION //
    //                          //
    //////////////////////////////  

    /**
     * @param shipVariant Variant of the ship affected by the interferences
     * @return Uncapped reduction of the flux dissipation caused by the interfering weapons
     */

    public static Float getTotalInterference(ShipVariantAPI shipVariant) {

        float total = 0;
        Map<String, Float> theDebuffs = getDebuffs(shipVariant);

        //compute total of the interferences
        if (theDebuffs.size() > 1) {
            for (String w : theDebuffs.keySet()) {
                total += theDebuffs.get(w);
            }
        }

        return total;
    }

    public static Map<String, Float> getRates() {
        if (RATES.isEmpty()) {
            loadInterference();
        }
        return RATES;
    }

    public static float getRFC() {
        if (RFC_MULT == 0) {
            loadInterference();
        }
        return RFC_MULT;
    }

    /**
     * @param shipVariant Variant of the ship affected by the interferences
     * @return Map of the mounts fitted with a weapon with interference, with their current individual effects
     */
    public static Map<String, Float> getDebuffs(ShipVariantAPI shipVariant) {

        //double check if all the values were loaded
        if (RATES.isEmpty()) {
            loadInterference();
        }

        if (Global.getSettings().isDevMode()) {

            Map<String, Float> theDebuffs = new HashMap<>();

            if (MagicVariables.verbose) {
                LOG.info("computing interference debuff");
            }

            //scan all weapons for interference sources
            for (String w : shipVariant.getNonBuiltInWeaponSlots()) {
                if (shipVariant.getFittedWeaponSlots().contains(w) && WEAPONS.containsKey(shipVariant.getWeaponId(w))) {
                    theDebuffs.put(w, 0f);

                    if (MagicVariables.verbose) {
                        LOG.info("added interference source: " + shipVariant.getWeaponId(w));
                    }
                }
            }

            float hullmod = 1;
            //scan for interference-reducing hullmod
            if (shipVariant.getHullMods().contains("fluxbreakers")) {
                hullmod = RFC_MULT;

                if (MagicVariables.verbose) {
                    LOG.info("Resistant Flux Conduits installed, debuff reduced.");
                }
            }

            //compute all the debuff
            //LOG.info("found "+theDebuffs.size()+" interference sources");

            if (theDebuffs.size() > 1) {
                for (String w : theDebuffs.keySet()) {

                    theDebuffs.put(
                            w,
                            (float) (theDebuffs.size() - 1) //interference sources
                                    * WEAPONS.get(shipVariant.getWeaponId(w)) //
                                    * hullmod
                    );
                    if (MagicVariables.verbose) {
                        LOG.info(shipVariant.getWeaponSpec(w).getWeaponName() + " debuff: " + WEAPONS.get(shipVariant.getWeaponId(w)));
                    }
                }
            }
            return theDebuffs;
        } else {
            //Same without any log for performance saving
            Map<String, Float> theDebuffs = new HashMap<>();
            //scan all weapons for interference sources
            for (String w : shipVariant.getNonBuiltInWeaponSlots()) {
                if (shipVariant.getFittedWeaponSlots().contains(w) && WEAPONS.containsKey(shipVariant.getWeaponId(w))) {
                    theDebuffs.put(w, 0f);
                }
            }
            float hullmod = 1;
            //scan for interference-reducing hullmod
            if (shipVariant.getHullMods().contains("fluxbreakers")) {
                hullmod = RFC_MULT;
            }
            //compute all the debuff
            if (theDebuffs.size() > 1) {
                for (String w : theDebuffs.keySet()) {

                    theDebuffs.put(
                            w,
                            (float) (theDebuffs.size() - 1) //interference sources
                                    * WEAPONS.get(shipVariant.getWeaponId(w)) //
                                    * hullmod
                    );
                }
            }
            return theDebuffs;
        }
    }
}