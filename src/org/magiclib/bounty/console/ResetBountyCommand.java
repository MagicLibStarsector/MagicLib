package org.magiclib.bounty.console;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.Console;
import org.magiclib.bounty.MagicBountyCoordinator;
import org.magiclib.bounty.MagicBountyLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ResetBountyCommand implements BaseCommandWithSuggestion {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull BaseCommand.CommandContext context) {
        MagicBountyCoordinator mbc = MagicBountyCoordinator.getInstance();
        String trimmedArgs = args.trim();

        if (trimmedArgs.isEmpty()) {
            Console.showMessage("Missing bounty id.");
            return CommandResult.BAD_SYNTAX;
        } else {
            try {
                if(trimmedArgs.equalsIgnoreCase("all")) {
                    Set<String> allBountyKeys = new HashSet<>(MagicBountyLoader.BOUNTIES.keySet());
                    allBountyKeys.addAll(mbc.getActiveBounties().keySet());
                    allBountyKeys.forEach(bountyKey -> {
                        // It would be rather slow to use resetBounty over and over, this would repeatedly reload all bounties from the JSON file every single call and spam the logs in the process.
                        mbc.resetBounty(bountyKey); // But we do it anyway because it's easier.
                        Console.showMessage(String.format("'%s' reset.", bountyKey));
                    });
                } else {
                    mbc.resetBounty(trimmedArgs);
                    Console.showMessage(String.format("'%s' reset.", trimmedArgs));
                }

            } catch (Exception e) {
                Console.showMessage(String.format("Error resetting '%s': %s", trimmedArgs, e.getMessage()));
            }
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int i, List<String> list, CommandContext commandContext) {
        Set<String> allBountyKeys = new HashSet<>(MagicBountyLoader.BOUNTIES.keySet());
        allBountyKeys.addAll(MagicBountyCoordinator.getInstance().getActiveBounties().keySet());

        List<String> result = new ArrayList<>(1 + allBountyKeys.size());
        result.add("all");
        result.addAll(allBountyKeys);
        return result;
    }
}
