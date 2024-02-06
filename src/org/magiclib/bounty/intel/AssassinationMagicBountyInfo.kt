package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.MapParams
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.bounty.MagicBountyLoader
import org.magiclib.bounty.MagicBountySpec
import org.magiclib.kotlin.ucFirst
import org.magiclib.util.MagicTxt
import java.awt.Color
import kotlin.math.roundToInt

class AssassinationMagicBountyInfo(bountyKey: String, bountySpec: MagicBountySpec) :
    MagicBountyInfo(bountyKey, bountySpec) {
    override fun showTargetInfo(panel: CustomPanelAPI, width: Float, height: Float): TooltipMakerAPI {
        val targetInfoTooltip = panel.createUIElement(width, height, true)
        val childPanelWidth = width - 16f
        val activeBountyLocal = activeBounty ?: return targetInfoTooltip

        if (bountySpec.job_show_captain) {
            val portrait = targetInfoTooltip.beginImageWithText(getJobIcon(), 64f)
            var displayName = activeBountyLocal.fleet.commander.nameString
            val targetFirstName = activeBountyLocal.captain.name.first
            val targetLastName = activeBountyLocal.captain.name.last
            if (targetFirstName != null || targetLastName != null) {
                displayName = "$targetFirstName $targetLastName"
                if (targetFirstName == null || targetFirstName.isEmpty())
                    displayName = targetLastName
                else if (targetLastName == null || targetLastName.isEmpty())
                    displayName = targetFirstName
            }
            portrait.addPara(displayName, activeBountyLocal.targetFactionTextColor, 0f)
            portrait.addPara(activeBountyLocal.fleet.commander.rank.ucFirst(), 2f)
            targetInfoTooltip.addImageWithText(0f)
        }

        val location = getLocationIfBountyIsActive()
        if (location is StarSystemAPI) {
            val params = MapParams()
            params.showSystem(location)
            val w = targetInfoTooltip.widthSoFar
            val h = (w / 1.6f).roundToInt().toFloat()
            params.positionToShowAllMarkersAndSystems(false, w.coerceAtMost(h))
            params.filterData.fuel = true
            params.arrows.add(IntelInfoPlugin.ArrowData(Global.getSector().playerFleet, location.center))

            val map = targetInfoTooltip.createSectorMap(childPanelWidth, 200f, params, null)
            targetInfoTooltip.addCustom(map, 4f)

            if (bountySpec.job_show_distance != MagicBountyLoader.ShowDistance.None) {
                when (bountySpec.job_show_distance) {
                    MagicBountyLoader.ShowDistance.Exact -> targetInfoTooltip.addPara(
                        createLocationPreciseText(activeBounty!!),
                        10f,
                        location.lightColor,
                        activeBounty!!.fleetSpawnLocation.starSystem.nameWithLowercaseType
                    )

                    MagicBountyLoader.ShowDistance.System -> targetInfoTooltip.addPara(
                        MagicTxt.getString("mb_distance_system"),
                        10f,
                        arrayOf(Misc.getTextColor(), location.lightColor),
                        MagicTxt.getString("mb_distance_they"),
                        activeBounty!!.fleetSpawnLocation.starSystem.nameWithLowercaseType
                    )

                    else -> targetInfoTooltip.addPara(
                        createLocationEstimateText(activeBounty!!),
                        10f,
                        location.lightColor,
                        BreadcrumbSpecial.getLocationDescription(activeBounty!!.fleetSpawnLocation, false)
                    )
                }
            }
        } else {
            targetInfoTooltip.setButtonFontOrbitron20Bold()
            targetInfoTooltip.addPara(MagicTxt.getString("mb_descLocationUnknown"), 3f, Color.RED).position.inTMid(2f)
        }

        activeBounty?.let {
            showFleet(targetInfoTooltip, childPanelWidth, it)

            if (it.spec.job_show_captain) {
                targetInfoTooltip.addPara(MagicTxt.getString("mb_hvb_skillsHeader"), 8f)
                targetInfoTooltip.addSkillPanel(it.captain, 2f)
            }
        }

        panel.addUIElement(targetInfoTooltip).inTL(0f, 0f)

        return targetInfoTooltip
    }
}