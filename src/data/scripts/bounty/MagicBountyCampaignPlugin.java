package data.scripts.bounty;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.Collection;

/**
 * Tells the game engine to use {@link MagicBountyFleetInteractionDialogPlugin} for battles with bounty fleets.
 * @deprecated Please replace `data.scripts` with `org.magiclib`.
 */
public class MagicBountyCampaignPlugin extends BaseCampaignPlugin {
    @Override
    public String getId() {
        return "Magic_BountyCampaignPlugin";
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        Collection<ActiveBounty> bounties = MagicBountyCoordinator.getInstance().getActiveBounties().values();

        // If the player is interacting with a fleet whose flagship has an active bounty.
        if (bounties.size() > 0 && interactionTarget instanceof CampaignFleetAPI) {
            for (ActiveBounty bounty : bounties) {
                if (bounty.getFlagshipId() != null
                        && bounty.getFlagshipId().equals(((CampaignFleetAPI) interactionTarget).getFlagship().getId())) {
                    return new PluginPick<InteractionDialogPlugin>(new MagicBountyFleetInteractionDialogPlugin(), PickPriority.MOD_SPECIFIC);
                }
            }
        }

        return super.pickInteractionDialogPlugin(interactionTarget);
    }
}
