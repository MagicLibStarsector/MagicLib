package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import org.magiclib.util.MagicTxt
import org.magiclib.util.ui.MagicRefreshableBaseIntelPlugin
import java.awt.Color

class BountyBoardIntelPlugin : MagicRefreshableBaseIntelPlugin() {
    @Transient
    private var lastWidth: Float = 0f

    @Transient
    private var lastHeight: Float = 0f

    //    @Transient
    private var bountiesThatUserHasBeenNotifiedFor = mutableSetOf<String>()

    /**
     * One day between bounty board refreshes.
     */
    @Transient
    private val interval: IntervalUtil = IntervalUtil(1f, 1f)

    @Transient
    private var tempBountyInfo: BountyInfo? = null

    @Transient
    private var scrollPos: Float? = null

    init {
        // Add this as a transient script if it's not already there.
        if (!Global.getSector().hasTransientScript(BountyBoardIntelPlugin::class.java)) {
            Global.getSector().addTransientScript(this)
        }

        loadNotifiedBounties()
    }

    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false

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

    fun layoutPanel(panel: CustomPanelAPI, width: Float = lastWidth, height: Float = lastHeight) {
        val bountyList = BountyListPanelPlugin(panel)
        bountyList.panelWidth = 300f
        bountyList.panelHeight = height - 8f
        doBeforeRefresh { scrollPos = bountyList.scroller?.yOffset }
        doAfterRefresh { bountyList.scroller?.yOffset = scrollPos ?: 0f }

        val availableBounties: MutableList<BountyInfo> = PROVIDERS
            .flatMap { it.getBounties() }
            .toMutableList()

        val bountyListPanel = bountyList.layoutPanels(availableBounties)

        val textPanelWidth = width - bountyList.panelWidth
        val textPanelHeight = height - 8f
        var textPanel = panel.createCustomPanel(textPanelWidth, textPanelHeight, null)
        var descriptionTooltip = textPanel.createUIElement(350f, 150f, true)
        MagicTxt.addPara(
            descriptionTooltip,
            MagicTxt.getString("mb_intelTutorial"),
            10f,
            Misc.getTextColor(),
            Misc.getHighlightColor()
        )

        textPanel.addUIElement(descriptionTooltip).inMid()
        panel.addComponent(textPanel).rightOfTop(bountyListPanel, 4f)

        bountyList.addListener { bountyInfo ->
            panel.removeComponent(textPanel)

            lastSelectedBountyId = bountyInfo.getBountyId()
            textPanel = panel.createCustomPanel(textPanelWidth, textPanelHeight, null)
            descriptionTooltip = textPanel.createUIElement(textPanelWidth, textPanelHeight, false)

            bountyInfo.layoutPanel(descriptionTooltip, textPanelWidth, textPanelHeight)

            textPanel.addUIElement(descriptionTooltip).inTL(0f, 0f)
            panel.addComponent(textPanel).rightOfTop(bountyListPanel, 4f)
        }

        lastSelectedBountyId?.let { desiredItem ->
            //find matching item in available bounties and pick it
            availableBounties
                .firstOrNull { desiredItem == it.getBountyId() }
                ?.let {
                    bountyList.itemClicked(it)
                }
        }
    }

    override fun createLargeDescriptionImpl(panel: CustomPanelAPI, width: Float, height: Float) {
        super.createLargeDescriptionImpl(panel, width, height)
        lastWidth = width
        lastHeight = height
        layoutPanel(panel, width, height)
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
        var lastSelectedBountyId: String? = null
        const val NOTIFIED_BOUNTY_KEY = "ml_notifiedBountyKeys"
        val PROVIDERS = mutableListOf<BountyBoardProvider>()

        fun addProvider(provider: BountyBoardProvider) {
            PROVIDERS.add(provider)
        }

        fun refreshPanel(desiredItem: BountyInfo) {
            lastSelectedBountyId = desiredItem.getBountyId()
            (Global.getSector().intelManager.getFirstIntel(BountyBoardIntelPlugin::class.java) as BountyBoardIntelPlugin).apply {
                refreshPanel()
            }
        }
    }
}