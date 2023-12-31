package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import org.magiclib.util.MagicTxt
import org.magiclib.util.MagicTxt.MagicDisplayableText
import java.awt.Color

class BountyBoardIntelPlugin : BaseIntelPlugin() {
    @Transient
    private var parentPanel: CustomPanelAPI? = null

    @Transient
    private var holdingPanel: CustomPanelAPI? = null

    @Transient
    private var lastWidth: Float = 0f

    @Transient
    private var lastHeight: Float = 0f

    private var bountiesThatUserHasBeenNotifiedFor = mutableSetOf<String>()
    private var interval: IntervalUtil = IntervalUtil(1f, 1f)
    private var tempBountyInfo: BountyInfo? = null

    init {
        // Add this as a transient script if it's not already there.
        if (!Global.getSector().hasTransientScript(BountyBoardIntelPlugin::class.java)) {
            Global.getSector().addTransientScript(this)
        }

        loadNotifiedBounties()
    }

    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isImportant(): Boolean = true

    override fun getName(): String {
        if (tempBountyInfo != null) {
            return "${MagicTxt.getString("mb_intelTitle")} - ${tempBountyInfo!!.getBountyName()}"
        }
        return MagicTxt.getString("mb_intelTitle")
    }

    override fun getIcon(): String =
        tempBountyInfo?.getJobIcon() ?: Global.getSettings().getSpriteName("intel", "magicBoard")

    override fun addBulletPoints(
        info: TooltipMakerAPI,
        mode: ListInfoMode,
        isUpdate: Boolean,
        tc: Color,
        initPad: Float
    ) {
        tempBountyInfo?.addNotificationBulletpoints(info)
    }

    fun saveNotifiedBounties() {
        Global.getSector().persistentData[NOTIFIED_BOUNTY_KEY] = bountiesThatUserHasBeenNotifiedFor
    }

    fun loadNotifiedBounties() {
        if (Global.getSector().persistentData.containsKey(NOTIFIED_BOUNTY_KEY)) {
            bountiesThatUserHasBeenNotifiedFor =
                Global.getSector().persistentData[NOTIFIED_BOUNTY_KEY] as MutableSet<String>
        }
    }

    fun notifyUserThatBountyIsAvailable(bountyInfo: BountyInfo) {
        bountiesThatUserHasBeenNotifiedFor.add(bountyInfo.getBountyId())
        saveNotifiedBounties()

        bountyInfo.notifiedUserThatBountyIsAvailable()

        this.tempBountyInfo = bountyInfo
        this.sendUpdateIfPlayerHasIntel(null, false, false)
        this.tempBountyInfo = null
    }

    override fun advance(amount: Float) {
        // Don't show bounties until the player has been playing for a few days.
        // This prevents the player from being overwhelmed with bounties right at the start of the game.
        if (Global.getSector().clock.elapsedDaysSinceGameStart() < 3) return

        interval.advance(Global.getSector().clock.convertToDays(amount))
        if (interval.intervalElapsed()) {
            PROVIDERS
                .flatMap { it.getBounties() }
                .filter { !bountiesThatUserHasBeenNotifiedFor.contains(it.getBountyId()) }
                .firstOrNull { it.shouldShow() }
                ?.let {
                    notifyUserThatBountyIsAvailable(it)
                }
        }
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode) {
        if (mode == ListInfoMode.INTEL) {
            //intel is being recreated
            parentPanel = null
            holdingPanel = null
        }

        super.createIntelInfo(info, mode)
    }

    fun layoutPanel(width: Float = lastWidth, height: Float = lastHeight, desiredItem: BountyInfo? = null) {
        parentPanel ?: return // somehow, "apocrita_belcher" is getting here with a null parentPanel

        if (holdingPanel != null) {
            parentPanel!!.removeComponent(holdingPanel)
        }

        holdingPanel = parentPanel!!.createCustomPanel(width, height, null)
        parentPanel!!.addComponent(holdingPanel).inTL(0f, 0f)

        val panel: CustomPanelAPI = holdingPanel!!
        val bountyList = BountyListPanelPlugin(panel)
        bountyList.panelWidth = 300f
        bountyList.panelHeight = height - 8f

        val availableBounties: MutableList<BountyInfo> = PROVIDERS
            .flatMap { it.getBounties() }
            .toMutableList()

        val bountyListPanel = bountyList.layoutPanels(availableBounties)

        val textPanelWidth = width - bountyList.panelWidth
        val textPanelHeight = height - 8f
        var textPanel = panel.createCustomPanel(textPanelWidth, textPanelHeight, null)
        var descriptionTooltip = textPanel.createUIElement(350f, 150f, true)
        MagicTxt.addPara(descriptionTooltip, MagicTxt.getString("mb_intelTutorial"), 10f, Misc.getTextColor(), Misc.getHighlightColor())

        textPanel.addUIElement(descriptionTooltip).inMid()
        panel.addComponent(textPanel).rightOfTop(bountyListPanel, 4f)

        bountyList.addListener { bountyInfo ->
            panel.removeComponent(textPanel)

            textPanel = panel.createCustomPanel(textPanelWidth, textPanelHeight, null)
            descriptionTooltip = textPanel.createUIElement(textPanelWidth, textPanelHeight, false)

            bountyInfo.layoutPanel(descriptionTooltip, textPanelWidth, textPanelHeight)

            textPanel.addUIElement(descriptionTooltip).inTL(0f, 0f)
            panel.addComponent(textPanel).rightOfTop(bountyListPanel, 4f)
        }

        desiredItem?.let { desiredItem ->
            //find matching item in available bounties and pick it
            availableBounties
                .firstOrNull { desiredItem.getBountyId() == it.getBountyId() }
                ?.let {
                    bountyList.itemClicked(it)
                }
        }
    }

    override fun createLargeDescription(panel: CustomPanelAPI, width: Float, height: Float) {
        parentPanel = panel
        lastWidth = width
        lastHeight = height
        layoutPanel(width, height)
    }

    override fun getSortString(): String {
        return "00000Bounties"
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_BOUNTY)
        return tags
    }

    companion object {
        const val NOTIFIED_BOUNTY_KEY = "ml_notifiedBountyKeys"
        val PROVIDERS = mutableListOf<BountyBoardProvider>()

        fun addProvider(provider: BountyBoardProvider) {
            PROVIDERS.add(provider)
        }

        fun refreshPanel(desiredItem: BountyInfo) {
            (Global.getSector().intelManager.getFirstIntel(BountyBoardIntelPlugin::class.java) as BountyBoardIntelPlugin).layoutPanel(
                desiredItem = desiredItem
            )
        }
    }
}