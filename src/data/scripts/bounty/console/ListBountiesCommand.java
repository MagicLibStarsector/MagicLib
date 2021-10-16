package data.scripts.bounty.console;

import data.scripts.bounty.ActiveBounty;
import data.scripts.bounty.MagicBountyCoordinator;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListBountiesCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        Map<String, ActiveBounty> activeBounties = MagicBountyCoordinator.getInstance().getActiveBounties();

        if (args.isEmpty()) {
            Set<Map.Entry<String, ActiveBounty>> entries = activeBounties.entrySet();
            if (entries.isEmpty()) {
                Console.showMessage("No active bounties");
            } else {
                Console.showMessage("Active bounties");

                for (Map.Entry<String, ActiveBounty> entry : entries) {
                    ActiveBounty bounty = entry.getValue();
                    Console.showMessage(String.format("  Id: %s, Stage: %s\n  %s\n", entry.getKey(), bounty.getStage().name(), bounty));
                }
            }

            List<String> completedBounties = MagicBountyCoordinator.getInstance().getCompletedBounties();

            if (completedBounties.isEmpty()) {
                Console.showMessage("No completed bounties");
            } else {
                Console.showMessage("Completed bounties");

                for (String completedBounty : completedBounties) {
                    Console.showMessage(String.format(" %s \n", completedBounty));
                }
            }
        }

        return CommandResult.SUCCESS;
    }
}
