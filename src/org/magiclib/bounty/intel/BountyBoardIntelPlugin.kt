package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.magiclib.bounty.ActiveBounty
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import org.magiclib.util.MagicTxt
import org.magiclib.util.ui.MagicRefreshableBaseIntelPlugin
import java.awt.Color

class BountyBoardIntelPlugin : MagicRefreshableBaseIntelPlugin() {

    @Transient
    private var lastWidth: Float = 0f

    @Transient
    private var lastHeight: Float = 0f

    //TODO: Remove on 0.98.5a
    @Transient
    @Deprecated("Use bountiesThatUserHasBeenNotifiedForV2 instead")
    private var bountiesThatUserHasBeenNotifiedFor = mutableSetOf<String>()

    /** One day between bounty board refreshes. */
    @Transient
    private val boardRefreshInterval: IntervalUtil = run {
        val key = "\$ML_boardRefreshInterval"
        val value = Global.getSector().memoryWithoutUpdate.get(key) as? IntervalUtil
            ?: IntervalUtil(1f, 1f).also { Global.getSector().memoryWithoutUpdate[key] = it }
        value
    }
    /** How often a personal bounty can spawn, roughly within ~%25 of one day. */
    @Transient
    private val personalBountyInterval = run {
        val key = "\$ML_personalBountyInterval"
        val value = Global.getSector().memoryWithoutUpdate.get(key) as? IntervalUtil
            ?: IntervalUtil(0.75f, 1.25f).also { Global.getSector().memoryWithoutUpdate[key] = it }
        value
    }
    /** How often to reconsider the personalbounty cap (currMaxPersonalBounties), roughly every 7.5-12.5 days. */
    @Transient
    private val personalBountyMaxUpdateInterval = run {
        val key = "\$ML_personalBountyMaxUpdateInterval"
        val value = Global.getSector().memoryWithoutUpdate.get(key) as? IntervalUtil
            ?: IntervalUtil(7.5f, 12.5f).also { Global.getSector().memoryWithoutUpdate[key] = it }
        value
    }
    /**
     * The current target number of concurrently-active personal bounties.
     * it wanders between MIN_MAGIC_PERSONAL_BOUNTIES and MAX_MAGIC_PERSONAL_BOUNTIES over time rather than sitting at a fixed value.
     * Backed by sector memory since this plugin instance isn't guaranteed to survive a save/load.
     */
    private var currMaxPersonalBounties: Int
        get() {
            (Global.getSector().memoryWithoutUpdate.get(CURR_MAX_PERSONAL_BOUNTIES_KEY) as? Int)?.let { return it }
            val initial = MIN_MAGIC_PERSONAL_BOUNTIES +
                    (Math.random() * (MAX_MAGIC_PERSONAL_BOUNTIES - MIN_MAGIC_PERSONAL_BOUNTIES + 1)).toInt()
            Global.getSector().memoryWithoutUpdate[CURR_MAX_PERSONAL_BOUNTIES_KEY] = initial
            return initial
        }
        set(value) {
            val clamped = value.coerceIn(MIN_MAGIC_PERSONAL_BOUNTIES, MAX_MAGIC_PERSONAL_BOUNTIES)
            Global.getSector().memoryWithoutUpdate[CURR_MAX_PERSONAL_BOUNTIES_KEY] = clamped
        }


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

    fun notifyUserThatBountyIsAvailable(bountyInfo: BountyInfo) {
        addNotifiedBounty(bountyInfo.getBountyId())

        bountyInfo.notifiedUserThatBountyIsAvailable()

        if(bountyInfo is MagicBountyInfo) {
            if(bountyInfo.bountySpec.job_auto_accept != null) return

            this.tempBountyInfo = bountyInfo
            this.sendUpdateIfPlayerHasIntel(null, false, false)
            this.tempBountyInfo = null
        }
    }

    override fun advance(amount: Float) {
        val days = Global.getSector().clock.convertToDays(amount)

        personalBountyInterval.advance(days)
        personalBountyMaxUpdateInterval.advance(days)

        fun getPersonalBounties(): List<BountyInfo> =
            PROVIDERS.flatMap { it.getBounties() }
                .filter { it.shouldShow() }
                .filter { (it as MagicBountyInfo).bountySpec.job_auto_accept == "personal" }
        fun getActiveCount(personalBounties: List<BountyInfo>) = personalBounties.count { (it as MagicBountyInfo).activeBounty!!.stage == ActiveBounty.Stage.Accepted }

        // Only reconsider the cap while we're not already sitting above it, then mostly nudge by +/-1 and occasionally reroll entirely.
        if (personalBountyMaxUpdateInterval.intervalElapsed()) {
            val personalBounties = getPersonalBounties()
            val activeCount = getActiveCount(personalBounties)

            if (activeCount <= currMaxPersonalBounties || activeCount == 0) {
                currMaxPersonalBounties = when {
                    Math.random() < 0.05 -> MIN_MAGIC_PERSONAL_BOUNTIES +
                            (Math.random() * (MAX_MAGIC_PERSONAL_BOUNTIES - MIN_MAGIC_PERSONAL_BOUNTIES + 1)).toInt()
                    Math.random() < 0.5 -> currMaxPersonalBounties - 1
                    else -> currMaxPersonalBounties + 1
                }
            }
        }

        // Once under the (wandering) cap there's a 25% chance per elapsed interval that a bounty actually spawns.
        if (personalBountyInterval.intervalElapsed() && Math.random().toFloat() >= 0.75f) {
            val personalBounties = getPersonalBounties()
            val activeCount = getActiveCount(personalBounties)

            if (activeCount < currMaxPersonalBounties) {
                val personalBountiesFree = personalBounties.filter { (it as MagicBountyInfo).activeBounty!!.stage == ActiveBounty.Stage.NotAccepted }
                personalBountiesFree.randomOrNull()?.let {
                    (it as MagicBountyInfo).activeBounty!!.acceptBounty(
                        Global.getSector().playerFleet,
                        it.bountySpec.job_reputation_reward,
                        it.bountySpec.job_forFaction
                    )
                    notifyUserThatBountyIsAvailable(it)
                }
            }
        }

        boardRefreshInterval.advance(days)
        if (!boardRefreshInterval.intervalElapsed()) return

        // Don't show bounties until the player has been playing for a few days.
        // This prevents the player from being overwhelmed with bounties right at the start of the game.
        if (Global.getSector().clock.elapsedDaysSinceGameStart() < 3) return

        PROVIDERS
            .flatMap { it.getBounties() }
            .filter { !bountiesThatUserHasBeenNotifiedForV2.contains(it.getBountyId()) }
            .firstOrNull { it.shouldShow() }
            ?.let {
                notifyUserThatBountyIsAvailable(it)
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
            .filter { it.shouldShow() || it.shouldAlwaysShow() }
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
                    if(bountyList.shouldMakePanelForItem(it))
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
        // These constant values should instead be a setting somewhere that can be modified by other mods.
        var MIN_MAGIC_PERSONAL_BOUNTIES = 1
        var MAX_MAGIC_PERSONAL_BOUNTIES = 2
        private const val CURR_MAX_PERSONAL_BOUNTIES_KEY = "\$ML_currMaxPersonalBounties"

        private val bountiesThatUserHasBeenNotifiedForV2 = mutableSetOf<String>()
        val bountiesThatUserHasBeenNotifiedFor: Set<String>
            get() = bountiesThatUserHasBeenNotifiedForV2
        fun hasNotifiedBounty(bountyID: String): Boolean =
            bountiesThatUserHasBeenNotifiedForV2.contains(bountyID)
        /**
         * Removes the bounty from the list of bounties that have been notified to the user.
         */
        fun removeNotifiedBounty(bountyID: String) {
            bountiesThatUserHasBeenNotifiedForV2.remove(bountyID)
            saveNotifiedBounties()
        }
        /**
         * Adds the bounty to the list of bounties that have been notified to the user. This does not notify the user with a message, it only adds it to the list as if it did.
         */
        fun addNotifiedBounty(bountyID: String) {
            bountiesThatUserHasBeenNotifiedForV2.add(bountyID)
            saveNotifiedBounties()
        }
        private fun saveNotifiedBounties() {
            Global.getSector().persistentData[NOTIFIED_BOUNTY_KEY] = bountiesThatUserHasBeenNotifiedForV2
        }
        private fun loadNotifiedBounties() {
            bountiesThatUserHasBeenNotifiedForV2.clear()
            if (Global.getSector().persistentData.containsKey(NOTIFIED_BOUNTY_KEY)) {
                bountiesThatUserHasBeenNotifiedForV2.addAll(Global.getSector().persistentData[NOTIFIED_BOUNTY_KEY] as MutableSet<String>)
            }
        }

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