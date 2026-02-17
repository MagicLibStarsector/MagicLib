package org.magiclib.bounty.intel.sorters

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.magiclib.bounty.intel.BountyInfo
import org.magiclib.bounty.intel.MagicBountyInfo
import org.magiclib.bounty.ui.InteractiveUIPanelPlugin
import org.magiclib.bounty.ui.lists.sorted.ListSorter
import org.magiclib.bounty.ui.lists.sorted.Sortable
import org.magiclib.internalextensions.addTooltip
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.util.MagicTxt
import java.awt.Color

class TogglePrimarySorter : ListSorter<BountyInfo, LocationAPI> {

    private var nonEnemyToBottom = true

    enum class Order {
        ASCENDING,
        DESCENDING,
    }
    private var orderBy = Order.ASCENDING
    fun getOrderBy(): Order = orderBy
    fun setOrderBy(value: Order) {
        orderBy = value
    }

    enum class SortingMethod {
        ALPHABETICAL,
        CREDITS,
        KNOWNDISTANCE,
        FIRSTCREATED,
    }
    private var sortBy = SortingMethod.FIRSTCREATED
    fun getSortBy(): SortingMethod = sortBy
    fun setSortBy(value: SortingMethod) {
        sortBy = value
    }

    override fun createPanel(
        tooltip: TooltipMakerAPI,
        width: Float,
        lastItems: List<Sortable<BountyInfo>>
    ): CustomPanelAPI {
        val sorterPlugin = InteractiveUIPanelPlugin()
        val sorterPanel = Global.getSettings().createCustom(width, tooltip.heightSoFar, sorterPlugin)

        //checkbox tooltip
        val toggleGroupTooltip = sorterPanel.createUIElement(width, sorterPanel.position.height, false)


        val orderTogglesData = listOf(
            MagicTxt.getString("mb_sort_Ascending") to Order.ASCENDING,
            MagicTxt.getString("mb_sort_Descending") to Order.DESCENDING
        )

        var currentOrderSelected: ButtonAPI? = null

        orderTogglesData.forEachIndexed { index, (label, order) ->
            val checkbox = toggleGroupTooltip.addCheckbox(20f, 16f, label, null, ButtonAPI.UICheckboxSize.SMALL, if(index == 0) 0f else 4f)

            // Check the current order by default
            if (orderBy == order) {
                checkbox.isChecked = true
                currentOrderSelected = checkbox
            }

            sorterPlugin.addCheckbox(checkbox) { checked ->
                if (checked) {
                    currentOrderSelected?.let { if (it != checkbox) it.isChecked = false }
                    currentOrderSelected = checkbox
                    orderBy = order
                } else {
                    currentOrderSelected?.isChecked = true
                }
            }
        }

        toggleGroupTooltip.addSpacer(12f)

        val togglesData = listOf(
            MagicTxt.getString("mb_sort_Alphabetical") to SortingMethod.ALPHABETICAL,
            MagicTxt.getString("mb_sort_Distance") to SortingMethod.KNOWNDISTANCE,
            MagicTxt.getString("mb_sort_Credits") to SortingMethod.CREDITS,
            MagicTxt.getString("mb_sort_FirstCreated") to SortingMethod.FIRSTCREATED,
        )

        var currentSelected: ButtonAPI? = null

        togglesData.forEachIndexed { index, (label, method) ->
            val checkbox = toggleGroupTooltip.addCheckbox(20f, 16f, label, null, ButtonAPI.UICheckboxSize.SMALL, if(index == 0) 0f else 4f)
            if (sortBy == method) {
                checkbox.isChecked = true
                currentSelected = checkbox
            }
            sorterPlugin.addCheckbox(checkbox) { checked ->
                if (checked) {
                    currentSelected?.let { if (it != checkbox) it.isChecked = false }
                    currentSelected = checkbox
                    sortBy = method
                } else {
                    currentSelected?.isChecked = true
                }
            }
        }


        val nonEnemyToBottomButton = toggleGroupTooltip.addCheckbox(20f, 16f, MagicTxt.getString("mb_sort_DropNonHostiles"), null, ButtonAPI.UICheckboxSize.SMALL, toggleGroupTooltip.heightSoFar - toggleGroupTooltip.position.height - 16f - 4f)
        toggleGroupTooltip.addTooltip(nonEnemyToBottomButton, TooltipMakerAPI.TooltipLocation.BELOW, 600f) {
            it.addPara(MagicTxt.getString("mb_sort_DropNonHostilesTooltip"), 0f)
        }
        nonEnemyToBottomButton.isChecked = nonEnemyToBottom
        sorterPlugin.addCheckbox(nonEnemyToBottomButton) { checked ->
            nonEnemyToBottom = checked
        }

        sorterPanel.addUIElement(toggleGroupTooltip).inTMid(2f)
        tooltip.addCustomDoNotSetPosition(sorterPanel)

        return sorterPanel
    }

    override fun saveToPersistentData() {
        Global.getSector().persistentData["MagicLib.LocationSorter.sortBy"] = sortBy
        Global.getSector().persistentData["MagicLib.LocationSorter.orderBy"] = orderBy
        Global.getSector().persistentData["MagicLib.LocationSorter.nonEnemyToBottom"] = nonEnemyToBottom
    }

    override fun loadFromPersistentData(members: List<BountyInfo>) {
        if (Global.getSector().persistentData.containsKey("MagicLib.LocationSorter.sortBy"))
            sortBy = Global.getSector().persistentData["MagicLib.LocationSorter.sortBy"] as SortingMethod
        if (Global.getSector().persistentData.containsKey("MagicLib.LocationSorter.orderBy"))
            orderBy = Global.getSector().persistentData["MagicLib.LocationSorter.orderBy"] as Order
        if (Global.getSector().persistentData.containsKey("MagicLib.LocationSorter.nonEnemyToBottom"))
            nonEnemyToBottom = Global.getSector().persistentData["MagicLib.LocationSorter.nonEnemyToBottom"] as Boolean

        sortMembers(members)
    }

    fun sortMembers(items: List<BountyInfo>) {
        items.forEach { it.setSortIndexOffset(0) }

        val sorted = when (getSortBy()) {
            SortingMethod.CREDITS ->
                items.sortedBy { it.getBountyPayout() }

            SortingMethod.KNOWNDISTANCE ->
                items.sortedBy { it.getPlayerKnownDistanceIfBountyIsActive() ?: Float.MAX_VALUE }

            SortingMethod.FIRSTCREATED ->
                items.sortedBy { (it as? MagicBountyInfo)?.activeBounty?.bountyCreatedTimestamp }.reversed()

            SortingMethod.ALPHABETICAL ->
                items.sortedBy { it.getBountyName() }
        }.toMutableList()

        // Reverse if descending
        if (getOrderBy() == Order.DESCENDING) {
            sorted.reverse()
        }

        sorted.forEach { it.setCustomPanelColor(null) }
        if (nonEnemyToBottom) {
            val sector = Global.getSector()
            val playerFaction = sector.playerFaction

            val (keep, moveToBottom) = sorted.partition { entry ->
                val bounty = (entry as? MagicBountyInfo)?.activeBounty
                    ?: return@partition true

                val faction = bounty.targetFaction ?: return@partition true
                val system = bounty.fleetSpawnLocation.starSystem ?: return@partition true

                val isCoreFaction = faction.isShowInIntelTab
                val isNotEnemy = !faction.isHostileTo(playerFaction)

                val hasLargeMarketInSystem = system.getMarketsInLocation(faction.id)?.any { market ->
                    market.factionId == faction.id && market.size >= 4
                } == true

                // keep everything that does NOT match all conditions
                !(isCoreFaction && isNotEnemy && hasLargeMarketInSystem)
            }

            sorted.clear()
            sorted.addAll(keep)
            sorted.addAll(moveToBottom)
            moveToBottom.forEach { it.setCustomPanelColor(Color(255, 0, 0, 102)) }
        }

        // Assign sortIndexOffset
        sorted.forEachIndexed { index, item ->
            item.setSortIndexOffset(index)
        }

    }

    override fun isActive(): Boolean {
        return true
    }
}