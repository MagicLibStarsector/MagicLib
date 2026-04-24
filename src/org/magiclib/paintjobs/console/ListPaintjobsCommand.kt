package org.magiclib.paintjobs.console

import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.magiclib.paintjobs.MagicPaintjobManager

class ListPaintjobsCommand : BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        val all = MagicPaintjobManager.getPaintjobs(includeShiny = true, includeHidden = true)

        val normalPjs = all.filterTo(mutableSetOf()) { !it.isShiny && !it.isHidden }
        val shinyPjs  = all.filterTo(mutableSetOf()) { it.isShiny && !it.isHidden }
        val hiddenPjs = all.filterTo(mutableSetOf()) { it.isHidden }

        Console.showMessage("Regular Paintjobs")
        for (pj in normalPjs) {
            Console.showMessage("\t$pj")
        }

        Console.showMessage("Shiny Paintjobs")
        for (pj in shinyPjs) {
            Console.showMessage("\t$pj")
        }

        Console.showMessage("Hidden Paintjobs")
        for (pj in hiddenPjs) {
            Console.showMessage("\t$pj")
        }

        return BaseCommand.CommandResult.SUCCESS
    }
}