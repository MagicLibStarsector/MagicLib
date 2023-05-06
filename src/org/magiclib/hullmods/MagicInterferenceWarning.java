package org.magiclib.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicInterference;
import org.magiclib.util.MagicTxt;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MagicInterferenceWarning extends BaseHullMod {

    //apply the effect
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float debuff = Math.min(stats.getFluxDissipation().getModifiedValue(), MagicInterference.getTotalInterference(stats.getVariant()));
        stats.getFluxDissipation().modifyFlat(id, -debuff);
    }

    //maintain status for the player ship in combat        
    private final Map<String, Integer> SHIPS = new HashMap<>();
    private final String UI1 = MagicTxt.getString("interferenceUItitle");
    private final String UI2 = MagicTxt.getString("-");
    private final String UI3 = MagicTxt.getString("interferenceUItxt");

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().getTotalElapsedTime(false) == 0) {
            SHIPS.clear();
        }

        if (!SHIPS.containsKey(ship.getId())) {
            SHIPS.put(ship.getId(), Math.round(MagicInterference.getTotalInterference(ship.getVariant())));
        }

        if (Global.getCombatEngine().getPlayerShip() == ship) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "Interference",
                    "graphics/SEEKER/icons/hullsys/SKR_interference.png",
                    UI1,
                    UI2 + SHIPS.get(ship.getId()) + UI3,
                    true
            );
        }
    }

    //description
    private final String DESC0 = MagicTxt.getString("interferenceWarning");
    private final String DESC1 = MagicTxt.getString("interferenceWeak");
    private final String DESC2 = MagicTxt.getString("interferenceMild");
    private final String DESC3 = MagicTxt.getString("interferenceStrong");
    private final String DESC4 = "" + MagicInterference.getRates().get("WEAK");
    private final String DESC5 = "" + MagicInterference.getRates().get("MILD");
    private final String DESC6 = "" + MagicInterference.getRates().get("STRONG");
    private final String DESC7 = Math.round(100 * (1 - MagicInterference.getRFC())) + MagicTxt.getString("%");
    private final String DESC8 = Global.getSettings().getHullModSpec("fluxbreakers").getDisplayName();

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {

        if (index == 0) return DESC0;
        if (index == 1) return DESC1;
        if (index == 2) return DESC2;
        if (index == 3) return DESC3;
        if (index == 4) return DESC4;
        if (index == 5) return DESC5;
        if (index == 6) return DESC6;
        if (index == 7) return DESC7;
        if (index == 8) return DESC8;
        return null;
    }

    //detailed description    
    private final Color HL = Global.getSettings().getColor("hColor");
    private final Color BAD = Misc.getNegativeHighlightColor();
    private final String TTIP0 = MagicTxt.getString("interferenceTitle");
    private final String TTIP1 = MagicTxt.getString("interferenceTxt1");
    private final String TTIP2 = MagicTxt.getString("interferenceTxt2");
    private final String TTIP3 = MagicTxt.getString("interferenceTxt3");
    private final String TTIP4 = MagicTxt.getString("interferenceSource");
    private final String TTIP5 = MagicTxt.getString("interferenceEffect");
    private final String TTIP6 = MagicTxt.getString("interferenceHullmod1");
    private final String TTIP7 = MagicTxt.getString("interferenceHullmod2");
    private final String TTIP8 = MagicTxt.getString("interferenceHullmod3");

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        Map<String, Float> sources = MagicInterference.getDebuffs(ship.getVariant());
        float total = 0;
        for (String s : sources.keySet()) {
            total += sources.get(s);
        }
        boolean reducedEffect = false;
        float mult = 1;
        if (ship.getVariant().getHullMods().contains("fluxbreakers")) {
            reducedEffect = true;
            mult = MagicInterference.getRFC();
        }

        //title
        tooltip.addSectionHeading(TTIP0, Alignment.MID, 15);

        //total effect
        LabelAPI global = tooltip.addPara(
                TTIP1
                        + sources.size()
                        + TTIP2
                        + Math.round(total) / mult
                        + TTIP3
                , 10);
        global.setHighlightColors(HL, BAD);
        global.setHighlight("" + sources.size(), "" + (Math.round(total) / mult));

        if (reducedEffect) {
            LabelAPI hullmod = tooltip.addPara(
                    TTIP6
                            + Math.round(total)
                            + TTIP7
                            + DESC8
                            + TTIP8,
                    10
            );
            hullmod.setHighlightColors(BAD, HL);
            hullmod.setHighlight("" + Math.round(total), DESC8);
        }

        //detailed breakdown
        tooltip.setBulletedListMode("    - ");
        for (String s : sources.keySet()) {

            String weapon = ship.getVariant().getWeaponSpec(s).getWeaponName();
            int effect = Math.round(sources.get(s));

            tooltip.addPara(weapon
                            + TTIP4
                            + (-effect)
                            + TTIP5,
                    3,
                    HL,
                    weapon,
                    "" + (-effect * mult)
            );
        }
        tooltip.setBulletedListMode(null);
    }
}
