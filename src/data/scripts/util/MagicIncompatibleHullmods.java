/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
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