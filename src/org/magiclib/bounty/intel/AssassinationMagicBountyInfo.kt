package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.MapParams
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.MagicBountySpec
import org.magiclib.bounty.MagicBountyUtilsInternal
import org.magiclib.kotlin.ucFirst
import org.magiclib.util.MagicTxt
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class AssassinationMagicBountyInfo(bountyKey: String, bountySpec: MagicBountySpec): MagicBountyInfo(bountyKey, bountySpec) {
    override fun showTargetInfo(panel: CustomPanelAPI, width: Float, height: Float): TooltipMakerAPI {
        val targetInfoTooltip = panel.createUIElement(width, height, true)
        val childPanelWidth = width - 16f

        val portrait = targetInfoTooltip.beginImageWithText(activeBounty!!.fleet.commander.portraitSprite, 64f)
        var displayName = activeBounty!!.fleet.commander.nameString
        if (bountySpec.target_first_name != null || bountySpec.target_last_name != null) {
            displayName = "${bountySpec.target_first_name} ${bountySpec.target_last_name}"
            if (bountySpec.target_first_name == null || bountySpec.target_first_name.isEmpty())
                displayName = bountySpec.target_last_name
            else if (bountySpec.target_last_name == null || bountySpec.target_last_name.isEmpty())
                displayName = bountySpec.target_first_name
        }
        portrait.addPara(displayName, Global.getSettings().getFactionSpec(bountySpec.fleet_faction).baseUIColor, 0f)
        portrait.addPara(activeBounty!!.fleet.commander.rank.ucFirst(), 2f)

        targetInfoTooltip.addImageWithText(0f)

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
            targetInfoTooltip.addPara(MagicTxt.getString("mb_descLocation").format(location.name), 0f, location.lightColor, location.name)
        } else {
            targetInfoTooltip.setButtonFontOrbitron20Bold()
            targetInfoTooltip.addPara(MagicTxt.getString("mb_descLocationUnknown"), 0f, Color.RED).position.inTMid(2f)
        }

        val ships = activeBounty!!.fleet.fleetData.membersInPriorityOrder
        val iconSize = 64f
        val columns = floor(childPanelWidth / iconSize).toInt()
        val rows = ceil(ships.size / columns.toDouble()).toInt()
        targetInfoTooltip.addPara(MagicTxt.getString("mb_fleet2"), 8f)
        targetInfoTooltip.addShipList(columns, rows, iconSize, Color.white, ships, 2f)

        targetInfoTooltip.addPara(MagicTxt.getString("mb_hvb_skillsHeader"), 8f)
        targetInfoTooltip.addSkillPanel(activeBounty!!.captain, 2f)

        panel.addUIElement(targetInfoTooltip).inTL(0f, 0f)

        return targetInfoTooltip
    }
}