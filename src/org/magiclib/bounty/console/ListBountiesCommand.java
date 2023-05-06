package org.magiclib.bounty.console;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.magiclib.bounty.ActiveBounty;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyLoader;

import java.util.*;

public class ListBountiesCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        List<String> bountyKeys = new ArrayList<>(MagicBountyLoader.BOUNTIES.keySet());
        bountyKeys.addAll(MagicBountyCoordinator.getInstance().getActiveBounties().keySet());
        bountyKeys = new ArrayList<>(new HashSet<>(bountyKeys)); // Remove duplicates.

        Collections.sort(bountyKeys);
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
        } else if (loadedBounties.size() == 1) {
            String bountyKey = loadedBounties.get(0);
            if (mbc.getActiveBounty(bountyKey) != null) {
                Console.showMessage(String.format("  %s", mbc.getActiveBounty(bountyKey)));
            } else if (MagicBountyLoader.BOUNTIES.get(bountyKey) != null) {
                Console.showMessage(String.format("  %s", MagicBountyLoader.BOUNTIES.get(bountyKey)));
            }
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
