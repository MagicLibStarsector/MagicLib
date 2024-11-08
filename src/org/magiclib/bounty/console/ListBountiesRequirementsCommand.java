package org.magiclib.bounty.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyLoader;
import org.magiclib.bounty.MagicBountySpec;
import org.magiclib.util.MagicMisc;

import java.util.*;

public class ListBountiesRequirementsCommand implements BaseCommand {

    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        String trimmedArgs = args.trim();

        List<String> completed = MagicBountyCoordinator.getInstance().getCompletedBounties();
        Set<String> active = MagicBountyCoordinator.getInstance().getActiveBounties().keySet();
        List<String> bountyKeys = new ArrayList<>(MagicBountyLoader.BOUNTIES.keySet());
        Collections.sort(bountyKeys);

        if (trimmedArgs.isEmpty()) {
            //no specified ID, show only the available bounties requirements
            for (String bountyKey : bountyKeys) {
                if (!completed.contains(bountyKey) && !active.contains(bountyKey)) {
                    showRequirements(bountyKey, MagicBountyLoader.BOUNTIES.get(bountyKey));
                    Console.showMessage(" ");
                }
            }
        } else {
            if (bountyKeys.contains(trimmedArgs)) {
                showRequirements(trimmedArgs, MagicBountyLoader.BOUNTIES.get(trimmedArgs));
            } else {
                //bounty id does not exist
                Console.showMessage(String.format("Couldn't find '%s'", trimmedArgs));
            }
        }

        return CommandResult.SUCCESS;
    }

    private void showRequirements(String bountyId, MagicBountySpec bounty) {
        Console.showMessage(bounty.job_name + " ( " + bountyId + " ):");

        //WHICH MARKET
        boolean location = false;
        if (!bounty.trigger_market_id.isEmpty()) {

            for (String id : bounty.trigger_market_id) {
                if (Global.getSector().getEntityById(id) != null) {
                    location = true;
                    break;
                }
            }

            if (location) {
                Console.showMessage(" - " + "Offered at market(s):");
                for (String id : bounty.trigger_market_id) {
                    if (Global.getSector().getEntityById(id) != null) {
                        Console.showMessage("   . " + Global.getSector().getEntityById(id).getFullName());
                    }
                }
            }
        }

        //if there is no priority market ID (or none are present due to Nexerelin) check the other parameters
        if (!location) {
            //WHOSE MARKETS
            if (!bounty.trigger_marketFaction_any.isEmpty() || !bounty.trigger_marketFaction_none.isEmpty()) {
                //find all the factions whose market can display the bounty:
                // all the listed factions if any,
                // all the allied factions if required
                // none of the blacklisted factions if any
                // all the enemies  of the blacklisted factions if required
                List<FactionAPI> allFactions = new ArrayList<>();

                Console.showMessage(" - " + "Offered at market(s) from faction(s):");

                List<FactionAPI> factionAny = new ArrayList<>();
                for (String id : bounty.trigger_marketFaction_any) {
                    if (Global.getSector().getFaction(id) != null) {
                        factionAny.add(Global.getSector().getFaction(id));
                    }
                }
                List<FactionAPI> factionNone = new ArrayList<>();
                for (String id : bounty.trigger_marketFaction_none) {
                    if (Global.getSector().getFaction(id) != null) {
                        factionNone.add(Global.getSector().getFaction(id));
                    }
                }

                //add the initialy requested factions
                if (!factionAny.isEmpty()) {
                    allFactions = new ArrayList<>(factionAny);
                }

                //find all the allies
                if (bounty.trigger_marketFaction_alliedWith && !factionAny.isEmpty()) {
                    for (FactionAPI faction : factionAny) {
                        for (FactionAPI f : Global.getSector().getAllFactions()) {
                            if (
                                    f != faction //isn't one of the requested factions
                                            &&
                                            f.isAtWorst(faction, RepLevel.WELCOMING) //is allied
                                            &&
                                            !factionNone.contains(f) //isn't blacklisted
                                            &&
                                            !allFactions.contains(f) //wasn't already added
                            ) {
                                allFactions.add(f);
                            }
                        }
                    }
                }

                //find the enemies
                if (bounty.trigger_marketFaction_enemyWith && !factionNone.isEmpty()) {
                    for (FactionAPI faction : factionNone) {
                        for (FactionAPI f : Global.getSector().getAllFactions()) {
                            if (
                                    f != faction //isn't one of the blacklisted factions
                                            &&
                                            f.isAtBest(faction, RepLevel.HOSTILE) //is hostile
                                            &&
                                            !allFactions.contains(f) //wasn't already added
                            ) {
                                allFactions.add(f);
                            }
                        }
                    }
                }

                for (FactionAPI faction : allFactions) {
                    Console.showMessage("   . " + faction.getDisplayName());
                }
            }

            //WHAT MARKET
            if (bounty.trigger_market_minSize > 0) {
                Console.showMessage(" - " + "Offered at market(s) size " + bounty.trigger_market_minSize + " and above.");
            }
        }

        boolean valid = true;

        //WHEN
        if (bounty.trigger_min_days_elapsed > 0) {
            Console.showMessage(" - " + "Offered only after " + bounty.trigger_min_days_elapsed + " days.");
            int daysElapsed = (int) MagicMisc.getElapsedDaysSinceGameStart();

            if (daysElapsed >= bounty.trigger_min_days_elapsed) {
                Console.showMessage("   . " + "VALID ( day " + Global.getSector().getClock().getCal().get(Calendar.DAY_OF_YEAR) + " cycle " + Global.getSector().getClock().getCycle() + " : " + daysElapsed + " )");
            } else {
                Console.showMessage("   . " + "INVALID ( day " + Global.getSector().getClock().getCal().get(Calendar.DAY_OF_YEAR) + " cycle " + Global.getSector().getClock().getCycle() + " : " + daysElapsed + " )");
                valid = false;
            }
        }

        //TO WHO
        if (bounty.trigger_player_minLevel > 0) {
            Console.showMessage(" - " + "Offered past level " + bounty.trigger_player_minLevel);
            if (Global.getSector().getPlayerStats().getLevel() >= bounty.trigger_player_minLevel) {
                Console.showMessage("   . " + "VALID ( level " + Global.getSector().getPlayerStats().getLevel() + " )");
            } else {
                Console.showMessage("   . " + "INVALID ( level " + Global.getSector().getPlayerStats().getLevel() + " )");
                valid = false;
            }
        }

        //TO WHAT
        if (bounty.trigger_min_fleet_size > 0) {
            Console.showMessage(" - " + "Offered to fleets at least " + bounty.trigger_min_fleet_size + " Fleet Points large");
            if (Global.getSector().getPlayerFleet().getFleetPoints() >= bounty.trigger_min_fleet_size) {
                Console.showMessage("   . " + "VALID ( currently " + Global.getSector().getPlayerFleet().getFleetPoints() + " FPs )");
            } else {
                Console.showMessage("   . " + "INVALID ( currently " + Global.getSector().getPlayerFleet().getFleetPoints() + " FPs )");
                valid = false;
            }
        }

        //TO WHOSE FRIEND
        if (!bounty.trigger_playerRelationship_atLeast.isEmpty()) {
            Console.showMessage(" - " + "Offered at relationship(s) floor(s): ");
            boolean relationship = false;
            for (String f : bounty.trigger_playerRelationship_atLeast.keySet()) {
                if (Global.getSector().getFaction(f) != null) {
                    FactionAPI faction = Global.getSector().getFaction(f);
                    Console.showMessage("   . " + faction.getDisplayName() + " : " + bounty.trigger_playerRelationship_atLeast.get(f));
                    if (Global.getSector().getPlayerFaction().getRelationship(f) >= bounty.trigger_playerRelationship_atLeast.get(f)) {
                        Console.showMessage("     " + "VALID ( currently " + Global.getSector().getPlayerFaction().getRelationship(f) + " )");
                        relationship = true;
                    } else {
                        Console.showMessage("     " + "INVALID ( currently " + Global.getSector().getPlayerFaction().getRelationship(f) + " )");
                    }
                }
            }
            if (!relationship) valid = false;
        }

        //TO WHOSE ENEMY
        if (!bounty.trigger_playerRelationship_atMost.isEmpty()) {
            Console.showMessage(" - " + "Offered at relationship(s) ceiling(s): ");
            boolean relationship = false;
            for (String f : bounty.trigger_playerRelationship_atMost.keySet()) {
                if (Global.getSector().getFaction(f) != null) {
                    FactionAPI faction = Global.getSector().getFaction(f);
                    Console.showMessage("   . " + faction.getDisplayName() + " : " + bounty.trigger_playerRelationship_atMost.get(f));
                    if (Global.getSector().getPlayerFaction().getRelationship(f) <= bounty.trigger_playerRelationship_atMost.get(f)) {
                        Console.showMessage("     " + "VALID ( currently " + Global.getSector().getPlayerFaction().getRelationship(f) + " )");
                        relationship = true;
                    } else {
                        Console.showMessage("     " + "INVALID ( currently " + Global.getSector().getPlayerFaction().getRelationship(f) + " )");
                    }
                }
            }
            if (!relationship) valid = false;
        }

        //WHEN 'ALL' MEMKEYS ARE SET
        if (bounty.trigger_memKeys_all != null && !bounty.trigger_memKeys_all.isEmpty()) {
            Console.showMessage(" - " + "Requires all the following MemKey(s): ");
            for (String k : bounty.trigger_memKeys_all.keySet()) {
                Boolean value = bounty.trigger_memKeys_all.get(k);
                Console.showMessage("   . " + k + " : " + value);
                if (Global.getSector().getMemoryWithoutUpdate().contains(k) && Global.getSector().getMemoryWithoutUpdate().getBoolean(k) == value) {
                    Console.showMessage("     " + "PASS");
                } else {
                    Console.showMessage("     " + "FAIL");
                    valid = false;
                }
            }
        }

        //WHEN 'ANY' MEMKEYS ARE SET
        if (bounty.trigger_memKeys_any != null && !bounty.trigger_memKeys_any.isEmpty()) {
            Console.showMessage(" - " + "Requires one of the following MemKey(s): ");
            boolean key = false;
            for (String k : bounty.trigger_memKeys_any.keySet()) {
                Boolean value = bounty.trigger_memKeys_any.get(k);
                Console.showMessage("   . " + k + " : " + value);
                if (Global.getSector().getMemoryWithoutUpdate().contains(k) && Global.getSector().getMemoryWithoutUpdate().getBoolean(k) == value) {
                    Console.showMessage("     " + "PASS");
                    key = true;
                } else {
                    Console.showMessage("     " + "FAIL");
                }
            }
            if (!key) valid = false;
        }

        //WHEN 'NONE' MEMKEYS ARE SET
        if (bounty.trigger_memKeys_none != null && !bounty.trigger_memKeys_none.isEmpty()) {
            Console.showMessage(" - " + "Requires the following MemKey(s) to NOT exist with specific values: ");
            boolean key = false;
            for (String k : bounty.trigger_memKeys_none.keySet()) {
                Boolean value = bounty.trigger_memKeys_none.get(k);
                Console.showMessage("   . " + k + " : " + value);
                if (!Global.getSector().getMemoryWithoutUpdate().contains(k) || Global.getSector().getMemoryWithoutUpdate().getBoolean(k) != value) {
                    Console.showMessage("     " + "PASS");
                    key = true;
                } else {
                    Console.showMessage("     " + "FAIL");
                }
            }
            if (!key) valid = false;
        }

        if (bounty.existing_target_memkey != null && !bounty.existing_target_memkey.isEmpty()) {
            Console.showMessage(" - " + "Requires a fleet with the MemKey set on it: " + bounty.existing_target_memkey);
            boolean key = false;
            for (StarSystemAPI s : Global.getSector().getStarSystems()) {
                for (CampaignFleetAPI f : s.getFleets()) {
                    if (f.getMemoryWithoutUpdate().contains(bounty.existing_target_memkey)) {
                        key = true;
                        Console.showMessage("     " + "PASS");
                        break;
                    }
                }
            }

            if (!key) {
                valid = false;
                Console.showMessage("     " + "FAIL");
            }
        }

        if (valid) {
            Console.showMessage("This bounty CAN show up in the relevant markets");
        } else {
            Console.showMessage("This bounty CANNOT show up in the relevant markets yet");
        }
    }
}
