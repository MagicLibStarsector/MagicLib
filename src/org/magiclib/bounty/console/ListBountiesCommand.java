package org.magiclib.bounty.console;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ListBountiesCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        List<String> bountyKeys = new ArrayList<>(MagicBountyData.BOUNTIES.keySet());
        String trimmedArgs = args.trim();

        if (trimmedArgs.isEmpty()) {
            showBounties(bountyKeys);
        } else {
            if (bountyKeys.contains(trimmedArgs)) {
                String loadedBounty = bountyKeys.get(bountyKeys.indexOf(trimmedArgs));
                showBounties(Collections.singletonList(loadedBounty));
            } else {
                Console.showMessage(String.format("Couldn't find '%s'", trimmedArgs));
            }
        }

        return CommandResult.SUCCESS;
    }

    private void showBounties(List<String> loadedBounties) {
        MagicBountyCoordinator mbc = MagicBountyCoordinator.getInstance();
        Map<String, ActiveBounty> activeBounties = mbc.getActiveBounties();
        List<Map.Entry<String, ActiveBounty>> entries = new ArrayList<>(activeBounties.entrySet());
        Console.showMessage("-------");

        if (loadedBounties.isEmpty()) {
            Console.showMessage("No loaded bounties");
        } else {
            Console.showMessage("Loaded Bounties");
            for (String entry : loadedBounties) {
                Console.showMessage(String.format("  %s", entry));
            }
        }

        Console.showMessage("-------");
        if (entries.isEmpty()) {
            Console.showMessage("No active bounties");
        } else {
            Console.showMessage("Active bounties");

            for (Map.Entry<String, ActiveBounty> entry : entries) {
                ActiveBounty bounty = entry.getValue();
                Console.showMessage(
                        String.format(
                                //"  Id: %s, Stage: %s\n  %s\n"
                                "  Id: %s, Stage: %s\n"
                                , entry.getKey()
                                , bounty.getStage().name()
                                //,bounty
                        )
                );
            }
        }

        List<String> completedBounties = mbc.getCompletedBounties();
        Console.showMessage("-------");

        if (completedBounties.isEmpty()) {
            Console.showMessage("No completed bounties");
        } else {
            Console.showMessage("Completed bounties");

            for (String completedBounty : completedBounties) {
                Console.showMessage(String.format(" %s", completedBounty));
            }
        }
    }
}
