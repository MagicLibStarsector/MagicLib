package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ends bounties based on battle results.
 *
 * @author Wisp
 */
public final class MagicBountyBattleListener implements FleetEventListener {
    /**
     * Whether this listener is finished listening to battles that this bounty fleet gets into.
     */
    private boolean isDone = false;

    @NotNull
    private Set<String> bountyKeys;

    @Deprecated
    private String bountyKey;

    public MagicBountyBattleListener(@NotNull Set<String> bountyKeys) {
        this.bountyKeys = bountyKeys;
    }

    /**
     * Use `MagicBountyBattleListener(Set<String> bountyKeys)` instead.
     */
    @Deprecated
    public MagicBountyBattleListener(@NotNull String bountyKey) {
        this.bountyKeys = new HashSet<>();
        this.bountyKeys.add(bountyKey);
    }

    /**
     * If a key is already present, does nothing.
     */
    public void addBountyKey(@NotNull String bountyKey) {
        handleBackwardsCompat();
        this.bountyKeys.add(bountyKey);
    }

    /**
     * If the fleet has despawned, end all bounties on the fleet as [EndedWithoutPlayerInvolvement].
     */
    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        if (isDone) {
            return;
        }

        // Backwards compatibility.
        handleBackwardsCompat();

        fleet.removeEventListener(this);

        ActiveBounty firstAcceptedBountyOnFleet = null;

        for (String key : bountyKeys) {
            firstAcceptedBountyOnFleet = MagicBountyCoordinator.getInstance().getActiveBounty(key);
            if (firstAcceptedBountyOnFleet != null && firstAcceptedBountyOnFleet.getStage() == ActiveBounty.Stage.Accepted) {
                break;
            }
        }

        if (firstAcceptedBountyOnFleet == null) return;

        if (fleet.getId().equals(firstAcceptedBountyOnFleet.getFleet().getId())) {
            fleet.setCommander(fleet.getFaction().createRandomPerson());

            for (String key : bountyKeys) {
                ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(key);
                if (bounty != null) {
                    bounty.endBounty(new ActiveBounty.BountyResult.EndedWithoutPlayerInvolvement());
                }
            }

            Global.getSector().addScript(new AutoDespawnScript(fleet));
        }
    }

    /**
     * "bountyFleet" will be null if the listener is registered with the ListenerManager, and non-null
     * if the listener is added directly to a fleet.
     * We attach it to the bounty fleet, so `bountyFleet` will always be the bounty fleet.
     */
    @Override
    public void reportBattleOccurred(CampaignFleetAPI bountyFleet, CampaignFleetAPI winningFleet, BattleAPI battle) {
        handleBackwardsCompat();

        ActiveBounty firstAcceptedBountyOnFleet = null;

        for (String key : bountyKeys) {
            firstAcceptedBountyOnFleet = MagicBountyCoordinator.getInstance().getActiveBounty(key);
            if (firstAcceptedBountyOnFleet != null && firstAcceptedBountyOnFleet.getStage() == ActiveBounty.Stage.Accepted) {
                break;
            }
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        if (firstAcceptedBountyOnFleet == null) return;

        /////////// Below is copied (and heavily modified) from PersonBountyIntel.reportBattleOccurred.
        if (isDone) return;

        boolean playerInvolved = battle.isPlayerInvolved();

        if (bountyFleet.getId().equals(firstAcceptedBountyOnFleet.getFleet().getId())) {
            PersonAPI bountyCommander = firstAcceptedBountyOnFleet.getCaptain();

            if (battle.isInvolved(bountyFleet) && !playerInvolved) {
                if (bountyFleet.getFlagship() == null || bountyFleet.getFlagship().getCaptain() != bountyCommander) {
                    bountyFleet.setCommander(bountyFleet.getFaction().createRandomPerson());
                    //Global.getSector().reportEventStage(this, "other_end", market.getPrimaryEntity(), messagePriority);
                    for (String key : bountyKeys) {
                        ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(key);
                        if (bounty != null) {
                            bounty.endBounty(new ActiveBounty.BountyResult.EndedWithoutPlayerInvolvement());
                        }
                    }
                    // Quietly despawn the fleet when player goes away, since they can't complete the bounty.
                    Global.getSector().addScript(new AutoDespawnScript(bountyFleet));
//                        result = new PersonBountyIntel.BountyResult(PersonBountyIntel.BountyResultType.END_OTHER, 0, null);
//                        sendUpdateIfPlayerHasIntel(result, true);
//                        cleanUpFleetAndEndIfNecessary();
                    return;
                }
            }

            if (!playerInvolved || !battle.isInvolved(bountyFleet) || battle.onPlayerSide(bountyFleet)) {
                return;
            }

            boolean didDisableOrDestroyOriginalFlagship = bountyFleet.getFlagship() == null || bountyFleet.getFlagship().getCaptain() != bountyCommander;
            boolean didPlayerSalvageFlagship = false;
            List<FleetMemberAPI> bountyFleetBeforeBattle = bountyFleet.getFleetData().getSnapshot();

            if (firstAcceptedBountyOnFleet.getFlagshipId() != null) {
                for (FleetMemberAPI fleetMember : playerFleet.getFleetData().getMembersListCopy()) {

                    for (FleetMemberAPI ship : bountyFleetBeforeBattle) {
                        // Look for the flagship of the bounty fleet's presence in the player fleet.
                        if (fleetMember.getId().equals(firstAcceptedBountyOnFleet.getFlagshipId()) && fleetMember.getId().equals(ship.getId())) {
                            Global.getLogger(MagicBountyBattleListener.class).info(String.format("Player salvaged flagship %s (%s)", ship.getShipName(), ship.getId()));
                            didPlayerSalvageFlagship = true;
                        }
                    }
                }
            }

            for (String key : bountyKeys) {
                ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(key);
                if (bounty == null) {
                    continue;
                }

                // Skip bounties that have already been completed (not in "ready to accept" or "accepted" stages)
                if (bounty.getStage() != ActiveBounty.Stage.NotAccepted && bounty.getStage() != ActiveBounty.Stage.Accepted) {
                    continue;
                }

                // Go through each bounty's win conditions and end the bounty if met.
                switch (bounty.getSpec().job_type) {
                    case Assassination:
                        if (didDisableOrDestroyOriginalFlagship) {
                            bounty.endBounty(new ActiveBounty.BountyResult.Succeeded(true));
                        }

                        break;
                    case Destruction:
                        if (didDisableOrDestroyOriginalFlagship)
                            if (!didPlayerSalvageFlagship) {
                                bounty.endBounty(new ActiveBounty.BountyResult.Succeeded(true));
                            } else {
                                // If the bounty required destroying the target, but player salvaged their ship, they don't get credits.
                                bounty.endBounty(new ActiveBounty.BountyResult.FailedSalvagedFlagship());
                            }

                        break;
                    case Obliteration:
                        if (bountyFleet.getFleetSizeCount() <= 0) {
                            bounty.endBounty(new ActiveBounty.BountyResult.Succeeded(true));
                        }

                        break;
                    case Neutralization:
                    case Neutralisation:
                        float fpPostFight = bountyFleet.getFleetPoints();

                        if ((fpPostFight / bounty.getInitialBountyFleetPoints()) <= (1f / 3f)) {
                            bounty.endBounty(new ActiveBounty.BountyResult.Succeeded(true));
                        }

                        break;
                }
            }

            if (bountyFleet.getFleetSizeCount() <= 0) {
                // Fleet is totally dead, player is not gonna be starting another battle to finish another bounty on this fleet, so cancel out the listener.
                isDone = true;
            } else {
                // Handle the case where a battle occurred but player didn't complete all bounties.
                // We keep the BattleListener running until all bounties on the fleet have been completed (or the fleet is defeated, in reportFleetDespawnedToListener).
                boolean hasUnfinishedBounty = false;

                for (String key : bountyKeys) {
                    ActiveBounty bounty = MagicBountyCoordinator.getInstance().getActiveBounty(key);
                    if (bounty != null) {
                        if (bounty.getStage() == ActiveBounty.Stage.NotAccepted || bounty.getStage() == ActiveBounty.Stage.Accepted) {
                            hasUnfinishedBounty = true;
                        }
                    }
                }

                if (!hasUnfinishedBounty) {
                    isDone = true;
                }
            }
        }
    }

    /**
     * If restoring a save from before `bountyKeys` was added, migrate `bountyKey` to `bountyKeys`.
     */
    private void handleBackwardsCompat() {
        if (bountyKeys == null) {
            bountyKeys = new HashSet<>();
        }

        if (bountyKey != null) {
            bountyKeys.add(bountyKey);
        }
    }
}
