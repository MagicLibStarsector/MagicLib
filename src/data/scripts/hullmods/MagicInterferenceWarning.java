package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicInterference;
import data.scripts.util.MagicTxt;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static data.scripts.util.MagicTxt.getString;

@Deprecated
public class MagicInterferenceWarning extends BaseHullMod {

    //apply the effect
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float debuff = Math.min(stats.getFluxDissipation().getModifiedValue(), MagicInterference.getTotalInterference(stats.getVariant()));
        stats.getFluxDissipation().modifyFlat(id, -debuff);
    }

    //maintain status for the player ship in combat        
    private final Map<String, Integer> SHIPS = new HashMap<>();
    private final String UI1 = getString("interferenceUItitle");
    private final String UI2 = getString("-");
    private final String UI3 = getString("interferenceUItxt");

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
    private final String DESC0 = getString("interferenceWarning");
    private final String DESC1 = getString("interferenceWeak");
    private final String DESC2 = getString("interferenceMild");
    private final String DESC3 = getString("interferenceStrong");
    private final String DESC4 = String.valueOf(MagicInterference.getRates().get("WEAK"));
    private final String DESC5 = String.valueOf(MagicInterference.getRates().get("MILD"));
    private final String DESC6 = String.valueOf(MagicInterference.getRates().get("STRONG"));
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
    private final String TTIP0 = getString("interferenceTitle");
    private final String TTIP1 = getString("interferenceTxt1");
    private final String TTIP2 = getString("interferenceTxt2");
    private final String TTIP3 = getString("interferenceTxt3");
    private final String TTIP4 = getString("interferenceSource");
    private final String TTIP5 = getString("interferenceEffect");
    private final String TTIP6 = getString("interferenceHullmod1");
    private final String TTIP7 = getString("interferenceHullmod2");
    private final String TTIP8 = getString("interferenceHullmod3");

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
        global.setHighlight(String.valueOf(sources.size()), String.valueOf(Math.round(total) / mult));

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
            hullmod.setHighlight(String.valueOf(Math.round(total)), DESC8);
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
                    String.valueOf(-effect * mult)
            );
        }
        tooltip.setBulletedListMode(null);
    }
}
