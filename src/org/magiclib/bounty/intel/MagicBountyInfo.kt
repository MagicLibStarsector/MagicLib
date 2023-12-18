package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.MapParams
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.bounty.ActiveBounty
import org.magiclib.bounty.MagicBountyCoordinator
import org.magiclib.bounty.MagicBountyLoader.JobType
import org.magiclib.bounty.MagicBountySpec
import org.magiclib.bounty.MagicBountyUtilsInternal
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.kotlin.setAlpha
import org.magiclib.util.MagicMisc
import org.magiclib.util.MagicTxt
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

open class MagicBountyInfo(val bountyKey: String, val bountySpec: MagicBountySpec) : BountyInfo {
    var activeBounty: ActiveBounty? = null
    var holdingPanel: CustomPanelAPI? = null
    var panelThatCanBeRemoved: CustomPanelAPI? = null

    override fun getBountyId(): String {
        return bountyKey
    }

    override fun getBountyName(): String {
        return bountySpec.job_name
    }

    override fun getBountyType(): String {
        return bountySpec.job_type.name
    }

    override fun getBountyPayout(): Int {
        return activeBounty?.calculateCreditReward()?.toInt() ?: bountySpec.job_credit_reward
    }

    override fun getJobIcon(): String? {
        return activeBounty?.fleet?.commander?.portraitSprite ?: bountySpec.target_portrait
    }

    override fun getLocationIfBountyIsActive(): LocationAPI? {
        return activeBounty?.fleetSpawnLocation?.containingLocation
    }

    override fun notifyWhenAvailable(): Boolean {
        return true
    }

    override fun notifiedUserThatBountyIsAvailable() {
        val activeBountyLocal: ActiveBounty = activeBounty
            ?: MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey)
            ?: MagicBountyCoordinator.getInstance().createActiveBounty(bountyKey, bountySpec)
        activeBounty = activeBountyLocal
    }

    override fun addNotificationBulletpoints(info: TooltipMakerAPI) {
        activeBounty!!.givingFaction?.let {
            info.addPara(
                MagicTxt.getString("mb_intel_offeredBy").format(it.displayName),
                2f,
                activeBounty!!.givingFactionTextColor,
                it.displayName
            )
        }

        val creditReward = getBountyPayout().toFloat()
        if (creditReward > 0f) {
            val rewardText = Misc.getDGSCredits(creditReward)
            info.addPara(MagicTxt.getString("mb_credits").format(rewardText), 1f, Misc.getHighlightColor(), rewardText)
        }
    }

    override fun shouldShow(): Boolean {
        if (activeBounty != null) {
            when (activeBounty!!.stage) {
                ActiveBounty.Stage.Accepted -> return true
                ActiveBounty.Stage.Succeeded -> return false
                ActiveBounty.Stage.ExpiredAfterAccepting -> return false
                ActiveBounty.Stage.ExpiredWithoutAccepting -> return false
                ActiveBounty.Stage.FailedSalvagedFlagship -> return false
                ActiveBounty.Stage.EndedWithoutPlayerInvolvement -> return false
                ActiveBounty.Stage.Dismissed -> return false
                else -> { /* do nothing */ } //NotAccepted
            }
        }

        var shouldShow = true
        if (bountySpec.trigger_min_days_elapsed > 0 && bountySpec.trigger_min_days_elapsed > MagicMisc.getElapsedDaysSinceGameStart()) {
            shouldShow = false
        }
        if (bountySpec.trigger_player_minLevel > 0 && bountySpec.trigger_player_minLevel > Global.getSector().playerStats.level) {
            shouldShow = false
        }

        if (bountySpec.trigger_min_fleet_size > 0) {
            val playerFleet = Global.getSector().playerFleet
            val effectiveFP = playerFleet.fleetPoints.toFloat()
            if (bountySpec.trigger_min_fleet_size > effectiveFP) {
                shouldShow = false
            }
        }

        val memory = Global.getSector().memoryWithoutUpdate
        //checking trigger_memKeys_all
        if (bountySpec.trigger_memKeys_all.isNotEmpty() && shouldShow) {
            shouldShow = checkAllMemKeys(memory)
        }

        //checking memKeys_none
        if (bountySpec.trigger_memKeys_none.isNotEmpty() && shouldShow) {
            shouldShow = checkNoneMemKeys(memory)
        }

        //checking trigger_memKeys_any
        if (bountySpec.trigger_memKeys_any.isNotEmpty() && shouldShow) {
            shouldShow = checkAnyMemKeys(memory)
        }

        //CHECK FOR EXISTING FLEET
        if (bountySpec.existing_target_memkey != null && shouldShow) {
            var targetFleetGone = true
            for (s in Global.getSector().starSystems) {
                for (f in s.fleets) {
                    if (f.memoryWithoutUpdate.contains(bountySpec.existing_target_memkey)) {
                        targetFleetGone = false
                        break
                    }
                }
                if (!targetFleetGone) break
            }
            shouldShow = !targetFleetGone
        }

        //check if close enough to receive from faction
        val rangeToShowBounties = 10f
        if (bountySpec.job_forFaction != null && shouldShow) {
            var withinRange = false
            for (s in Misc.getNearbyStarSystems(Global.getSector().playerFleet, rangeToShowBounties)) {
                if (Misc.getMarketsInLocation(s, bountySpec.job_forFaction).isNotEmpty()) {
                    withinRange = true
                    break
                }
            }

            shouldShow = withinRange
        }

        if (shouldShow && activeBounty == null) {
            val activeBountyLocal: ActiveBounty = MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey)
                ?: MagicBountyCoordinator.getInstance().createActiveBounty(bountyKey, bountySpec)
            activeBounty = activeBountyLocal
        }

        return shouldShow
    }

    private fun checkAllMemKeys(memory: MemoryAPI): Boolean {
        var allMemKeysFound = true
        for (f in bountySpec.trigger_memKeys_all.keys) {
            //check if the memKey exists
            if (!memory.keys.contains(f) || memory[f] == null) {
                allMemKeysFound = false
                break
            }
            //check if it has the proper value
            if (bountySpec.trigger_memKeys_all[f] != memory.getBoolean(f)) {
                allMemKeysFound = false
                break
            }
        }
        return allMemKeysFound
    }

    private fun checkNoneMemKeys(memory: MemoryAPI): Boolean {
        var noKeysFound = true
        for ((key, value) in bountySpec.trigger_memKeys_none.entries) {
            if (memory.contains(key) && memory[key] != null) {
                if (memory.getBoolean(key) == value) {
                    noKeysFound = false
                    break
                }
            }
        }
        return noKeysFound
    }

    private fun checkAnyMemKeys(memory: MemoryAPI): Boolean {
        var anyKeyFound = false
        for (key in bountySpec.trigger_memKeys_any.keys) {
            //check if the memKey exists
            if (memory.keys.contains(key)) {
                //check if it has the proper value
                if (bountySpec.trigger_memKeys_any[key] == memory.getBoolean(key)) {
                    anyKeyFound = true
                    break
                }
            }
        }
        return anyKeyFound
    }

    override fun decorateListItem(
        plugin: BountyListPanelPlugin.BountyItemPanelPlugin,
        tooltip: TooltipMakerAPI,
        width: Float,
        height: Float
    ) {
        val textTooltip = tooltip.beginImageWithText(
            this.getJobIcon() ?: "graphics/portraits/portrait_generic_grayscale.png",
            64f,
            width,
            true
        )
        textTooltip.addPara(this.getBountyName(), 0f)
        textTooltip.addPara(this.getBountyType(), Misc.getGrayColor(), 0f)
        textTooltip.addPara(
            "Base reward: ${Misc.getDGSCredits(this.getBountyPayout().toFloat())}",
            Misc.getHighlightColor(),
            0f
        )
        tooltip.addImageWithText(2f)

        val activeBountyLocal: ActiveBounty = activeBounty
            ?: MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey)
            ?: MagicBountyCoordinator.getInstance().createActiveBounty(bountyKey, bountySpec)
            ?: return
        activeBounty = activeBountyLocal

        if (activeBountyLocal.stage == ActiveBounty.Stage.Accepted) {
            plugin.baseBgColor = Misc.getDarkHighlightColor().setAlpha(45)
            plugin.hoveredColor = Misc.getDarkHighlightColor().setAlpha(75)
            plugin.selectedColor = Misc.getDarkHighlightColor().setAlpha(125)
        }
    }

    override fun layoutPanel(tooltip: TooltipMakerAPI, width: Float, height: Float) {
        if (panelThatCanBeRemoved != null) {
            holdingPanel!!.removeComponent(panelThatCanBeRemoved)
        }

        if (holdingPanel == null) {
            holdingPanel = Global.getSettings().createCustom(width / 2, height, null)
        }

        tooltip.addCustom(holdingPanel, 0f)
        panelThatCanBeRemoved = holdingPanel!!.createCustomPanel(width / 2, height, null)

        val width = width - 16f
        val height = height - 16f
        val activeBountyLocal: ActiveBounty = activeBounty
            ?: MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey)
            ?: MagicBountyCoordinator.getInstance().createActiveBounty(bountyKey, bountySpec)
            ?: return
        activeBounty = activeBountyLocal

        val leftPanel = panelThatCanBeRemoved!!.createCustomPanel(width / 2, height, null)
        showBountyText(leftPanel, width / 2, height)
        panelThatCanBeRemoved!!.addComponent(leftPanel)

        val rightPanelPlugin = InteractiveUIPanelPlugin()
        val rightPanelWidth = width / 2 - 16f
        val rightPanel = panelThatCanBeRemoved!!.createCustomPanel(rightPanelWidth, height - 4f, rightPanelPlugin)

        val rightScrollPanel = rightPanel.createUIElement(rightPanelWidth, height - 36f, true)
        val rightChildPanel = rightPanel.createCustomPanel(rightPanelWidth, height - 36f, null)
        showTargetInfo(rightChildPanel, rightPanelWidth, height - 36f)
        rightScrollPanel.addCustom(rightChildPanel, 0f)
        rightPanel.addUIElement(rightScrollPanel).inTL(0f, 0f)

        val actionTooltip = rightPanel.createUIElement(rightPanelWidth, 32f, false)

        if (activeBountyLocal.stage == ActiveBounty.Stage.NotAccepted) {
            val acceptButton = actionTooltip.addButton(bountySpec.job_pick_option, null, rightPanelWidth, 24f, 0f)
            rightPanelPlugin.addButton(acceptButton) {
                acceptButton.isChecked = false
                activeBounty?.let {
                    it.acceptBounty(
                        Global.getSector().playerFleet,
                        it.calculateCreditReward(),
                        it.spec.job_reputation_reward,
                        it.spec.job_forFaction
                    )
                }
                BountyBoardIntelPlugin.refreshPanel(this)
            }
        } else if (activeBountyLocal.stage == ActiveBounty.Stage.Accepted) {
            val courseButton =
                actionTooltip.addButton(MagicTxt.getString("mb_plot_course"), null, rightPanelWidth, 24f, 0f)
            rightPanelPlugin.addButton(courseButton) {
                courseButton.isChecked = false
                Global.getSector().layInCourseFor(
                    Misc.getDistressJumpPoint(activeBounty!!.fleet.containingLocation as StarSystemAPI)
                )
            }
        }

        rightPanel.addUIElement(actionTooltip).inBL(0f, 0f)
        panelThatCanBeRemoved!!.addComponent(rightPanel).rightOfTop(leftPanel, 2f)

        holdingPanel!!.addComponent(panelThatCanBeRemoved)
    }

    open fun showBountyText(panel: CustomPanelAPI, width: Float, height: Float): TooltipMakerAPI {
        val textTooltip = panel.createUIElement(width, height, true)
        val bountyFactionId = "ML_bounty"

        if (activeBounty!!.stage == ActiveBounty.Stage.Accepted) {
            textTooltip.addPara(MagicTxt.getString("mb_descAccepted"), Misc.getHighlightColor(), 0f)
            textTooltip.addSpacer(12f)
        }

        bountySpec.job_description.split("\n")
            .map {
                MagicTxt.MagicDisplayableText(MagicBountyUtilsInternal.replaceStringVariables(activeBounty, it))
            }
            .forEach { bountyText ->
                textTooltip.addPara(bountyText.format, 3f, Misc.getHighlightColor(), *bountyText.highlights)
            }
        textTooltip.addSpacer(10f)

        when (activeBounty!!.getSpec().job_type) {
            JobType.Assassination -> if (activeBounty!!.targetFaction == null || activeBounty!!.targetFaction?.id == bountyFactionId) {
                textTooltip.addPara(
                    MagicTxt.getString("mb_intelType"),
                    4f,
                    Misc.getTextColor(),
                    Misc.getHighlightColor(),
                    MagicTxt.getString("mb_type_assassination1")
                )
            } else {
                val label: LabelAPI = textTooltip.addPara(
                    MagicTxt.getString("mb_intelType0") +
                            MagicTxt.getString("mb_type_assassination1") +
                            MagicTxt.getString("mb_intelType1") +
                            activeBounty!!.targetFaction?.displayName +
                            MagicTxt.getString("mb_intelType2"),
                    4f
                )
                label.setHighlight(
                    MagicTxt.getString("mb_type_assassination1"),
                    activeBounty!!.targetFaction?.displayName
                )
                label.setHighlightColors(Misc.getHighlightColor(), activeBounty!!.targetFactionTextColor)
            }

            JobType.Destruction -> if (activeBounty!!.targetFaction == null || activeBounty!!.targetFaction?.id == bountyFactionId) {
                textTooltip.addPara(
                    MagicTxt.getString("mb_intelType"),
                    4f,
                    Misc.getTextColor(),
                    Misc.getHighlightColor(),
                    MagicTxt.getString("mb_type_destruction1")
                )
            } else {
                val label: LabelAPI = textTooltip.addPara(
                    (MagicTxt.getString("mb_intelType0") +
                            MagicTxt.getString("mb_type_destruction1") +
                            MagicTxt.getString("mb_intelType1") +
                            activeBounty!!.targetFaction?.displayName +
                            MagicTxt.getString("mb_intelType2")),
                    4f
                )
                label.setHighlight(
                    MagicTxt.getString("mb_type_destruction1"),
                    activeBounty!!.targetFaction?.displayName
                )
                label.setHighlightColors(Misc.getHighlightColor(), activeBounty!!.targetFactionTextColor)
            }

            JobType.Obliteration -> if (activeBounty!!.targetFaction == null || activeBounty!!.targetFaction?.id == bountyFactionId) {
                textTooltip.addPara(
                    MagicTxt.getString("mb_intelType"),
                    4f,
                    Misc.getTextColor(),
                    Misc.getHighlightColor(),
                    MagicTxt.getString("mb_type_obliteration1")
                )
            } else {
                val label: LabelAPI = textTooltip.addPara(
                    (MagicTxt.getString("mb_intelType0") +
                            MagicTxt.getString("mb_type_obliteration1") +
                            MagicTxt.getString("mb_intelType1") +
                            activeBounty!!.targetFaction?.displayName +
                            MagicTxt.getString("mb_intelType2")),
                    4f
                )
                label.setHighlight(
                    MagicTxt.getString("mb_type_obliteration1"),
                    activeBounty!!.targetFaction?.displayName
                )
                label.setHighlightColors(Misc.getHighlightColor(), activeBounty!!.targetFactionTextColor)
            }

            JobType.Neutralization -> if (activeBounty!!.targetFaction == null || activeBounty!!.targetFaction?.id == bountyFactionId) {
                textTooltip.addPara(
                    MagicTxt.getString("mb_intelType"),
                    4f,
                    Misc.getTextColor(),
                    Misc.getHighlightColor(),
                    MagicTxt.getString("mb_type_neutralisation1")
                )
            } else {
                val label: LabelAPI = textTooltip.addPara(
                    (MagicTxt.getString("mb_intelType0") +
                            MagicTxt.getString("mb_type_neutralisation1") +
                            MagicTxt.getString("mb_intelType1") +
                            activeBounty!!.targetFaction?.displayName +
                            MagicTxt.getString("mb_intelType2")),
                    4f
                )
                label.setHighlight(
                    MagicTxt.getString("mb_type_neutralisation1"),
                    activeBounty!!.targetFaction?.displayName
                )
                label.setHighlightColors(Misc.getHighlightColor(), activeBounty!!.targetFactionTextColor)
            }
        }

        val reward = activeBounty!!.rewardCredits ?: activeBounty!!.calculateCreditReward()
        val givingFaction = activeBounty!!.givingFaction
        if (reward != null && givingFaction != null) {
            val rewardText = Misc.getDGSCredits(reward)
            textTooltip.addPara(
                MagicTxt.getString("mb_descFactionReward").format(givingFaction.displayNameWithArticle, rewardText),
                2f,
                arrayOf(givingFaction.color, Misc.getHighlightColor()),
                givingFaction.displayNameWithArticle, rewardText
            )
        } else if (givingFaction != null) {
            textTooltip.addPara(
                MagicTxt.getString("mb_descFactionNoReward").format(givingFaction.displayNameWithArticle),
                2f,
                givingFaction.color,
                givingFaction.displayNameWithArticle
            )
        } else if (reward != null) {
            val rewardText = Misc.getDGSCredits(reward)
            textTooltip.addPara(
                MagicTxt.getString("mb_descAnonymousReward").format(rewardText),
                2f,
                Misc.getHighlightColor(),
                rewardText
            )
        }

        panel.addUIElement(textTooltip)

        return textTooltip
    }

    open fun showTargetInfo(panel: CustomPanelAPI, width: Float, height: Float): TooltipMakerAPI {
        val targetInfoTooltip = panel.createUIElement(width, height, true)
        val childPanelWidth = width - 16f

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
            targetInfoTooltip.addCustom(map, 2f)

            targetInfoTooltip.addPara(
                MagicTxt.getString("mb_descLocation").format(location.name),
                3f,
                location.lightColor,
                location.name
            )
        } else {
            targetInfoTooltip.setButtonFontOrbitron20Bold()
            targetInfoTooltip.addPara(MagicTxt.getString("mb_descLocationUnknown"), 3f, Color.RED).position.inTMid(2f)
        }

        val ships = activeBounty!!.fleet.fleetData.membersInPriorityOrder
        val iconSize = 64f
        val columns = floor(childPanelWidth / iconSize).toInt()
        val rows = ceil(ships.size / columns.toDouble()).toInt()
        targetInfoTooltip.addPara(MagicTxt.getString("mb_fleet2"), 8f)
        targetInfoTooltip.addShipList(columns, rows, iconSize, Color.white, ships, 2f)

        targetInfoTooltip.addPara(MagicTxt.getString("mb_hvb_skillsHeader"), 8f)
        targetInfoTooltip.addSkillPanel(activeBounty!!.captain, 2f)

        panel.addUIElement(targetInfoTooltip)

        return targetInfoTooltip
    }

    fun acceptBounty() {
        activeBounty?.let {
            it.acceptBounty(
                Global.getSector().playerFleet,
                it.calculateCreditReward(),
                it.spec.job_reputation_reward,
                it.spec.job_forFaction
            )
        }
        BountyBoardIntelPlugin.refreshPanel(this)
    }
}

fun ActiveBounty.calculateCreditReward(): Float? {
    return this.calculateCreditReward(
        MagicBountyCoordinator.getInstance().preScalingCreditRewardMultiplier,
        MagicBountyCoordinator.getInstance().postScalingCreditRewardMultiplier
    )
}