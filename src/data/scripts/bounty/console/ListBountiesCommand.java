package data.scripts.bounty.console;

import data.scripts.bounty.ActiveBounty;
import data.scripts.bounty.MagicBountyCoordinator;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.Map;

public class ListBountiesCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        Map<String, ActiveBounty> activeBounties = MagicBountyCoordinator.getInstance().getActiveBounties();

        if (args.isEmpty()) {
            for (Map.Entry<String, ActiveBounty> entry : activeBounties.entrySet()) {
                ActiveBounty bounty = entry.getValue();
                Console.showMessage(String.format("Id: %s, Stage: %s\n%s\n", entry.getKey(), bounty.getStage().name(), bounty));
            }
        }

        return CommandResult.SUCCESS;
    }
}
