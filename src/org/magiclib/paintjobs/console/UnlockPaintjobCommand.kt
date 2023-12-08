package org.magiclib.paintjobs.console

import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.magiclib.paintjobs.MagicPaintjobManager

class UnlockPaintjobCommand : BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        val trimmedArgs = args.trim { it <= ' ' }

        val paintjobs = MagicPaintjobManager.getPaintjobs()
        if (trimmedArgs.isEmpty()) {
            Console.showMessage("Missing arg ('all' or a specific paintjob id).")
            Console.showMessage(paintjobs.joinToString(separator = "\n") { "  Name: ${it.name}, Id: ${it.id}" })
            return BaseCommand.CommandResult.BAD_SYNTAX
        } else {
            try {
                if (trimmedArgs == "all") {
                    paintjobs.forEach { MagicPaintjobManager.unlockPaintjob(it.id) }
                } else {
                    if (paintjobs.none { it.id == trimmedArgs }) {
                        Console.showMessage("No paintjob with id '$trimmedArgs' found.")
                        return BaseCommand.CommandResult.ERROR
                    } else {
                        MagicPaintjobManager.unlockPaintjob(trimmedArgs)
                        Console.showMessage("Paintjob '$trimmedArgs' unlocked.")
                    }
                }
            } catch (e: Exception) {
                Console.showMessage("Error unlocking paintjob: ${e.message}.")
            }
        }
        return BaseCommand.CommandResult.SUCCESS
    }
}
