package org.magiclib.paintjobs.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.magiclib.paintjobs.MagicPaintjobManager


class DoesFleetHaveShiny : BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token?>?,
        memoryMap: MutableMap<String?, MemoryAPI?>?
    ): Boolean {
        val fleet = dialog?.interactionTarget as? CampaignFleetAPI

        return fleet?.fleetData?.membersListCopy
            ?.map { MagicPaintjobManager.getCurrentShipPaintjob(it) }
            ?.any { it?.isShiny == true } == true
    }
}
