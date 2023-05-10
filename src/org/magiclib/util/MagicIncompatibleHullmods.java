package org.magiclib.util;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proposes a universal solution for hullmod incompatibilities when they can't be avoided through isApplicableToShip.
 * When called, it will remove the offending hullmod, and add a "WARNING" hullmod detailing which hullmod was removed and why.
 * Please note that it is best practice to remove the hullmod that was just added due to the presence of a previously installed one rather than the opposite.
 * <br />
 * <br />
 * Weapons must be added to data/config/modSetting.json in the MagicLib.interferences_weapons section.
 * <p>
 * - id: weapon id from weapon_data.csv
 * - intensity: defines the amount of interference generated. By default it can be "WEAK", "MILD" or "STRONG, but mods can add new ratings in modSettings.json, although those will not be mentioned in the hullmod description.
 * <br />
 * <br />
 * By default, weak = -20 flux dissipation for each other interference weapon installed, mild = -40 and strong = -80
 * <br />
 * <br />
 * Then the weapon must have an everyframeWeaponScript to trigger the interference check:
 * <pre>
 *     if(weapon.getShip().getOriginalOwner()<0 && !weapon.getSlot().isBuiltIn()){
 *     MagicInterference.ApplyInterference(weapon.getShip().getVariant());
 * }
 * </pre>
 * <p>
 * A default script is provided: {@link org.magiclib.weapons.MagicBasicInterferenceEffect}
 * <br />
 * <br />
 * Please note that the interference trait should be mentioned in the weapon extra effect part of the tooltip card.
 *
 * @author Tartiflette
 */
public class MagicIncompatibleHullmods {

    private static final Map<ShipVariantAPI, List<String>> INCOMPATIBILITIES = new HashMap<>();
    private static final String WARNING = "ML_incompatibleHullmodWarning";

    public static void removeHullmodWithWarning(ShipVariantAPI variant, String toRemove, String removeCause) {

        //cleanup of the other variants just to avoid bloat
        if (!INCOMPATIBILITIES.isEmpty()) {
            for (ShipVariantAPI v : INCOMPATIBILITIES.keySet()) {
                if (v != variant && v.getHullMods().contains(WARNING)) {
                    v.removeMod(WARNING);
                }
            }
        }
        INCOMPATIBILITIES.clear();

        //remove conflicting hullmod if that's not already done
//        if(variant.getHullMods().contains(toRemove)){
//            variant.removeMod(toRemove);
//        }
        if (variant.getHullMods().contains(toRemove)) {
            if (variant.getSMods().contains(toRemove)) {
                variant.removeMod(removeCause);
                //add the info for the tooltip
                List<String> removed = new ArrayList<>();
                removed.add(0, removeCause);
                removed.add(1, toRemove);
                INCOMPATIBILITIES.put(variant, removed);
            } else {
                variant.removeMod(toRemove);
                //add the info for the tooltip
                List<String> removed = new ArrayList<>();
                removed.add(0, toRemove);
                removed.add(1, removeCause);
                INCOMPATIBILITIES.put(variant, removed);
            }
        }
        //add the warning hullmod
        if (!variant.getHullMods().contains(WARNING)) {
            variant.addMod(WARNING);
        }
        //add the info for the tooltip
        List<String> removed = new ArrayList<>();
        removed.add(0, toRemove);
        removed.add(1, removeCause);
        INCOMPATIBILITIES.put(variant, removed);

    }

    public static List<String> getReason(ShipVariantAPI variant) {
        if (INCOMPATIBILITIES.containsKey(variant)) {
            return INCOMPATIBILITIES.get(variant);
        } else {
            INCOMPATIBILITIES.clear();
            return null;
        }
    }

    public static void clearData() {
        INCOMPATIBILITIES.clear();
    }
}