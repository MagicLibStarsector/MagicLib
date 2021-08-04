package data.scripts.util.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;

/**
 * This EveryFrameScript will
 */
public final class MagicBountyBattleListener implements FleetEventListener {
    private boolean isDone = false;

    @NotNull
    private final String bountyKey;

    public MagicBountyBattleListener(@NotNull String bountyKey) {
        this.bountyKey = bountyKey;
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        fleet.removeEventListener(this);
    }

    /**
     * "fleet" will be null if the listener is registered with the ListenerManager, and non-null
     * if the listener is added directly to a fleet.
     */
    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey);

        /////////// Below is copied (and modified) from PersonBountyIntel.reportBattleOccurred.
        if (isDone) return;

        // also credit the player if they're in the same location as the fleet and nearby
        float distToPlayer = Misc.getDistance(fleet, Global.getSector().getPlayerFleet());
        boolean playerInvolved = battle.isPlayerInvolved() || (fleet.isInCurrentLocation() && distToPlayer < 2000f);

        if (fleet.getId().equals(bounty.getFleet().getId())) {
            PersonAPI person = bounty.getFleet().getCommander();

            if (battle.isInvolved(fleet) && !playerInvolved) {
                if (fleet.getFlagship() == null || fleet.getFlagship().getCaptain() != person) {
                    fleet.setCommander(fleet.getFaction().createRandomPerson());
                    //Global.getSector().reportEventStage(this, "other_end", market.getPrimaryEntity(), messagePriority);
                    bounty.endBounty(ActiveBounty.BountyResult.EndedWithoutPlayerInvolvement);
                    // Quietly despawn the fleet when player goes away, since they can't complete the bounty.
                    Global.getSector().addScript(new AutoDespawnScript(fleet));
//                        result = new PersonBountyIntel.BountyResult(PersonBountyIntel.BountyResultType.END_OTHER, 0, null);
//                        sendUpdateIfPlayerHasIntel(result, true);
//                        cleanUpFleetAndEndIfNecessary();
                    return;
                }
            }

            if (!playerInvolved || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet)) {
                return;
            }

            // didn't destroy the original flagship
            if (fleet.getFlagship() != null && fleet.getFlagship().getCaptain() == person) return;

            bounty.endBounty(ActiveBounty.BountyResult.Succeeded);
            isDone = true;
        }
    }
}
