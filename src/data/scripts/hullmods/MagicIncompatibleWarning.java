package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicTxt;
import java.awt.Color;

public class MagicIncompatibleWarning extends BaseHullMod {    
    
    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return MagicTxt.getString("conflictWarning");
        return null;
    }
    
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        
        Color HL=Global.getSettings().getColor("hColor");
        
        //do nothing if there isn't an entry for that ship.
        if(MagicIncompatibleHullmods.getReason(ship.getVariant())!=null){
            
            String removed=MagicIncompatibleHullmods.getReason(ship.getVariant()).get(0);
            String cause=MagicIncompatibleHullmods.getReason(ship.getVariant()).get(1);

            removed=Global.getSettings().getHullModSpec(removed).getDisplayName();
            cause=Global.getSettings().getHullModSpec(cause).getDisplayName();


            //title
            tooltip.addSectionHeading(MagicTxt.getString("conflictTitle"), Alignment.MID, 15);        

            //effect
            tooltip.addPara(
                    MagicTxt.getString("conflictTxt1")
                    + removed
                    + MagicTxt.getString("conflictTxt2")
                    + cause
                    + MagicTxt.getString("conflictTxt3")
                    , 10
                    ,HL
                    ,removed
                    ,cause
            );
        }
    }
}
