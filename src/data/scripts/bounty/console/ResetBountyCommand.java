package data.scripts.bounty.console;

import data.scripts.bounty.MagicBountyCoordinator;
import data.scripts.bounty.MagicBountyData;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Please replace `data.scripts` with `org.magiclib`.
 */
public class ResetBountyCommand implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        MagicBountyCoordinator mbc = MagicBountyCoordinator.getInstance();
        String trimmedArgs = args.trim();

        if (trimmedArgs.isEmpty()) {
            Console.showMessage("Missing bounty id.");
            return CommandResult.BAD_SYNTAX;
        } else {
            try {
                mbc.resetBounty(trimmedArgs);
                Console.showMessage(String.format("'%s' reset.", trimmedArgs));
            } catch (Exception e) {
                Console.showMessage(String.format("Error resetting '%s': %s", trimmedArgs, e.getMessage()));
            }
        }

        return CommandResult.SUCCESS;
    }
}
