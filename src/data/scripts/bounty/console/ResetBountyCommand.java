package data.scripts.bounty.console;

import data.scripts.bounty.MagicBountyCoordinator;
import data.scripts.bounty.MagicBountyData;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

public class ResetBountyCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        List<String> bountyKeys = new ArrayList<>(MagicBountyData.BOUNTIES.keySet());
        MagicBountyCoordinator mbc = MagicBountyCoordinator.getInstance();
        String trimmedArgs = args.trim();

        if (trimmedArgs.isEmpty()) {
            Console.showMessage("Missing bounty id.");
            return CommandResult.BAD_SYNTAX;
        } else {
            if (bountyKeys.contains(trimmedArgs)) {
                String bountyKey = bountyKeys.get(bountyKeys.indexOf(trimmedArgs));
                mbc.resetBounty(bountyKey);
                Console.showMessage(String.format("'%s' reset.", bountyKey));
            } else {
                Console.showMessage(String.format("Couldn't find '%s'", trimmedArgs));
            }
        }

        return CommandResult.SUCCESS;
    }
}
