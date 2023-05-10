package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicTxt;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.Locale;

import static data.scripts.util.MagicTxt.getString;

@Deprecated
public class MagicIncompatibleWarning extends BaseHullMod {

    private static final Logger LOG = Global.getLogger(MagicIncompatibleWarning.class);
    private final String DESC0 = MagicTxt.getString("conflictWarning");

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return DESC0;
        return null;
    }


    private final String POST0 = getString("conflictTitle");
    private final String POST1 = getString("conflictTxt1");
    private final String POST2 = getString("conflictTxt2");
    private final String POST3 = getString("conflictTxt3");
    private final Color HL = Global.getSettings().getColor("hColor");

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        //do nothing if there isn't an entry for that ship.
        if (MagicIncompatibleHullmods.getReason(ship.getVariant()) != null) {

            String removed = MagicIncompatibleHullmods.getReason(ship.getVariant()).get(0);
            String cause = MagicIncompatibleHullmods.getReason(ship.getVariant()).get(1);

            try {
                removed = Global.getSettings().getHullModSpec(removed).getDisplayName();
                cause = Global.getSettings().getHullModSpec(cause).getDisplayName();
            } catch (Exception e) {
                throw new RuntimeException(
                        "MagicLib was unable to find one of the following hull mod specs: " + removed + " or " + cause + ".",
                        e);
            }

            Locale.setDefault(Locale.ENGLISH);

            //title
            tooltip.addSectionHeading(POST0, Alignment.MID, 15);

            //effect
            tooltip.addPara(POST1 + removed + POST2 + cause + POST3,
                    10,
                    HL,
                    removed,
                    cause);
        }
    }
}
