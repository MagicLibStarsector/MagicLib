package org.magiclib.bounty;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Tells the game engine to use {@link MagicBountyFleetInteractionDialogPlugin} for battles with bounty fleets.
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
        if (getBountyKeyForInteractionTarget(interactionTarget) != null) {
            return new PluginPick<InteractionDialogPlugin>(new MagicBountyFleetInteractionDialogPlugin(), PickPriority.MOD_SPECIFIC);
        } else {
            return super.pickInteractionDialogPlugin(interactionTarget);
        }
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        String bountyKey = getBountyKeyForInteractionTarget(opponent);

        if (bountyKey != null) {
            return new PluginPick<BattleCreationPlugin>(new MagicBountyBattleCreationPlugin(bountyKey), PickPriority.MOD_SPECIFIC);
        } else {
            return super.pickBattleCreationPlugin(opponent);
        }
    }

    /**
     * Whether the player is interacting with a fleet whose flagship has an active bounty.
     * Returns the bounty key if there is one.
     */
    @Nullable
    private static String getBountyKeyForInteractionTarget(SectorEntityToken interactionTarget) {
        Collection<ActiveBounty> bounties = MagicBountyCoordinator.getInstance().getActiveBounties().values();

        if (!bounties.isEmpty() && interactionTarget instanceof CampaignFleetAPI) {
            for (ActiveBounty bounty : bounties) {
                if (bounty.getFlagshipId() != null) {
                    FleetMemberAPI flagship = ((CampaignFleetAPI) interactionTarget).getFlagship();
                    if (flagship != null && bounty.getFlagshipId().equals(flagship.getId())) {
                        return bounty.getKey();
                    }
                }
            }
        }

        return null;
    }
}
