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
import org.magiclib.kotlin.getMarketsInLocation

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
        //val validBounties = lastItems
        //    .map { it as BountyInfo }

        val filterPlugin = InteractiveUIPanelPlugin()
        val filterPanel = Global.getSettings().createCustom(width, 64f, filterPlugin)

        //checkbox tooltip
        val toggleGroupTooltip = filterPanel.createUIElement(width, 64f, false)


        val orderTogglesData = listOf(
            "Ascending" to Order.ASCENDING,
            "Descending" to Order.DESCENDING
        )

        var currentOrderSelected: ButtonAPI? = null

        orderTogglesData.forEachIndexed { index, (label, order) ->
            val checkbox = toggleGroupTooltip.addCheckbox(20f, 16f, label, null, ButtonAPI.UICheckboxSize.SMALL, if(index == 0) 0f else 4f)

            // Check the current order by default
            if (orderBy == order) {
                checkbox.isChecked = true
                currentOrderSelected = checkbox
            }

            filterPlugin.addCheckbox(checkbox) { checked ->
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
            "Alphabetical" to SortingMethod.ALPHABETICAL,
            "Distance" to SortingMethod.KNOWNDISTANCE,
            "Credits" to SortingMethod.CREDITS,
            "Time Posted" to SortingMethod.FIRSTCREATED,
        )

        var currentSelected: ButtonAPI? = null

        togglesData.forEachIndexed { index, (label, method) ->
            val checkbox = toggleGroupTooltip.addCheckbox(20f, 16f, label, null, ButtonAPI.UICheckboxSize.SMALL, if(index == 0) 0f else 4f)
            if (sortBy == method) {
                checkbox.isChecked = true
                currentSelected = checkbox
            }
            filterPlugin.addCheckbox(checkbox) { checked ->
                if (checked) {
                    currentSelected?.let { if (it != checkbox) it.isChecked = false }
                    currentSelected = checkbox
                    sortBy = method
                } else {
                    currentSelected?.isChecked = true
                }
            }
        }


        toggleGroupTooltip.addSpacer(12f)

        val nonEnemyToBottomButton = toggleGroupTooltip.addCheckbox(20f, 16f, "Send non-enemies in civilized systems to the bottom", null, ButtonAPI.UICheckboxSize.SMALL, 4f)
        nonEnemyToBottomButton.isChecked = nonEnemyToBottom
        filterPlugin.addCheckbox(nonEnemyToBottomButton) { checked ->
            nonEnemyToBottom = checked
        }

        filterPanel.addUIElement(toggleGroupTooltip).inTMid(2f)
        tooltip.addCustomDoNotSetPosition(filterPanel)

        return filterPanel
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