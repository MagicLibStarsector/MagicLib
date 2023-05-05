package org.magiclib.bounty.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.bounty.*;
import org.magiclib.util.MagicTxt;

import java.util.List;
import java.util.Map;

/**
 * Part of the logic to show the bounty board in every single market.
 *
 * @author Wisp
 */
public class MagicBountyCommsReplyCmd extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        SectorEntityToken target = dialog.getInteractionTarget();

        MagicBountyCoordinator mbc = MagicBountyCoordinator.getInstance();
        if (target == null || !target.hasTag(MagicBountyLoader.BOUNTY_FLEET_TAG)) return false;
        ActiveBounty bounty = null;
        for (String tag : target.getTags()) {
            if (mbc.getActiveBounty(tag) != null) {
                bounty = mbc.getActiveBounty(tag);
                break;
            }
        }
        if (bounty == null) return false;

        MagicBountySpec spec = bounty.getSpec();

        if (MagicTxt.nullStringIfEmpty(spec.job_comm_reply) != null) {
            String commReply = MagicBountyUtilsInternal.replaceStringVariables(bounty, spec.job_comm_reply);
            MagicTxt.MagicDisplayableText text = new MagicTxt.MagicDisplayableText(commReply);
            dialog.getTextPanel().addPara(text.format, Misc.getHighlightColor(), text.highlights);
        }

        return true;
    }
}
