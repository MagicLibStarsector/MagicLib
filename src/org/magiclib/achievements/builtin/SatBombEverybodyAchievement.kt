package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.json.JSONArray
import org.magiclib.achievements.MagicAchievement
import org.magiclib.kotlin.toStringList

class SatBombEverybodyAchievement : MagicAchievement(), ColonyPlayerHostileActListener {
    companion object {
        private const val key = "factionsBombed"
    }

    private val targets: MutableSet<String> = HashSet()

    val targetsBombardedSoFar: Set<String>
        get() = ((memory[key] as? JSONArray)?.toStringList() ?: emptyList()).toSet()

    override fun onApplicationLoaded() {
        super.onApplicationLoaded()
        targets.add("volturn")
        targets.add("gilead")
        targets.add("jangala")
        targets.add("hesperus") // TODO ?
        targets.add("chicomoztoc")
    }

    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()

        Global.getSector().listenerManager.addListener(this, true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        market ?: return
        actionData ?: return

        if (market.id !in targets)
            return

        val existing: MutableSet<String> = targetsBombardedSoFar.toMutableSet()
        val initialCount = existing.size
        existing += market.id

        if (existing.size != initialCount) {
            memory[key] = existing
            saveChanges()
        }

        if (existing.containsAll(targets)) {
            completeAchievement()
            saveChanges()
            onDestroyed()
        }
    }

    override fun getProgress(): Float = targetsBombardedSoFar.size.toFloat()
    override fun getMaxProgress(): Float = targets.size.toFloat()

    override fun hasTooltip() = true

    override fun createTooltip(tooltipMakerAPI: TooltipMakerAPI, isExpanded: Boolean, width: Float) {
        createTooltipHeader(tooltipMakerAPI)
        val allPlanets = Global.getSector().starSystems.flatMap { it.planets }
        targets.mapNotNull { planetId -> allPlanets.first { it.id == planetId } }
            .sortedBy { it.name ?: "" }
            .forEach { spec ->
                val hasItem = spec.id in targetsBombardedSoFar
                tooltipMakerAPI.addPara(
                    spec.name,
                    if (hasItem) Misc.getTextColor() else Misc.getNegativeHighlightColor(),
                    3f
                )
            }
    }

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) = Unit

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) = Unit

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) = Unit
}