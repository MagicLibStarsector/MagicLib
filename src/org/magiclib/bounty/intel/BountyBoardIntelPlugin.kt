package org.magiclib.bounty.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.util.MagicTxt

class BountyBoardIntelPlugin : BaseIntelPlugin() {
    @Transient
    private var parentPanel: CustomPanelAPI? = null
    @Transient
    private var holdingPanel: CustomPanelAPI? = null
    @Transient
    private var lastWidth: Float = 0f
    @Transient
    private var lastHeight: Float = 0f

    override fun hasLargeDescription(): Boolean = true
    override fun hasSmallDescription(): Boolean = false
    override fun isImportant(): Boolean = true

    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
        //intel is being recreated
        parentPanel = null
        holdingPanel = null

        info.addPara(MagicTxt.getString("mb_intelTitle"), 0f)
    }

    fun layoutPanel(width: Float = lastWidth, height: Float = lastHeight, desiredItem: BountyInfo? = null) {
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
        var descriptionTooltip = textPanel.createUIElement(textPanelWidth, textPanelHeight, true)

        textPanel.addUIElement(descriptionTooltip).inTL(0f, 0f)
        panel.addComponent(textPanel).rightOfTop(bountyListPanel, 4f)

        bountyList.addListener {
            panel.removeComponent(textPanel)

            textPanel = panel.createCustomPanel(textPanelWidth, textPanelHeight, null)
            descriptionTooltip = textPanel.createUIElement(textPanelWidth, textPanelHeight, false)

            it.layoutPanel(descriptionTooltip, textPanelWidth, textPanelHeight)

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
        val PROVIDERS = mutableListOf<BountyBoardProvider>()

        fun addProvider(provider: BountyBoardProvider) {
            PROVIDERS.add(provider)
        }

        fun refreshPanel(desiredItem: BountyInfo) {
            (Global.getSector().intelManager.getFirstIntel(BountyBoardIntelPlugin::class.java) as BountyBoardIntelPlugin).layoutPanel(desiredItem = desiredItem)
        }
    }
}