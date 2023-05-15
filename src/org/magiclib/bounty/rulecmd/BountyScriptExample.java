package org.magiclib.bounty.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyIntel;

import java.util.List;
import java.util.Map;

/**
 * Not used. This is an example of a bounty script that can run on accept and on completion.
 * This is triggered by a trigger defined in rules.csv.
 *
 * @author Wisp
 */
public class BountyScriptExample extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String your_bounty_id = "your_bounty_id";
        ActiveBounty bounty;

        // Try to get the bounty. The bounty id is the key of the json object that defines the bounty (eg "test_bounty").
        try {
            bounty = MagicBountyCoordinator.getInstance().getActiveBounty(your_bounty_id);

            if (bounty == null) {
                throw new NullPointerException();
            }
        } catch (Exception ex) {
            Global.getLogger(BountyScriptExample.class).error("Unable to get MagicBounty: " + your_bounty_id, ex);
            return true;
        }

        ActiveBounty.Stage bountyStage = bounty.getStage();
        MagicBountyIntel intel = bounty.getIntel();

        throw new RuntimeException("success!");

//        return true;
    }
}
