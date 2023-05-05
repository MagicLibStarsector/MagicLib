package org.magiclib.bounty.console;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.magiclib.bounty.MagicBountyCoordinator;

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
