package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI

object SectorUtils {
    /**
     * Returns the actual CoreUITabId of the campaign UI.
     *
     * This function is necessary because the campaign UI can report that the player is still in a CoreUITab even if they are not.
     * This can happen when the player enters an interaction dialog, opens any CoreUITab such as the crew/cargo tab, then escapes that CoreUITab back to the interaction dialog. It will still report that they are in the crew/cargo tab when they are not.
     *
     * This function checks if the player is in a ghost interaction dialog and if so, returns null, indicating that the player is not in a CoreUITab]
     */
    @JvmStatic
    fun getActualCurrentTab(ui: CampaignUIAPI): CoreUITabId? {
        val sector = Global.getSector() ?: return null
        if (!sector.isPaused) return null
        if (ui.currentInteractionDialog != null && ui.currentInteractionDialog.interactionTarget != null) {
            // Validate that we're not stuck in a ghost interaction dialog. (Happens when you escape out of a CoreUITab while in an interaction dialog. It reports that the player is still in that CoreUITab, which is false)
            if (ui.currentInteractionDialog.optionPanel != null && ui.currentInteractionDialog.optionPanel.savedOptionList.isNotEmpty()) return null
        }

        return ui.currentCoreTab
    }

    /**
     * Returns all entities across all locations in the sector.
     */
    @JvmStatic
    fun getSectorEntities(): List<SectorEntityToken> {
        val sector = Global.getSector() ?: return emptyList()

        return sector.allLocations
            .flatMap { it.allEntities }
    }

    /**
     * Returns all unique markets in the sector.
     *
     * Markets are collected from all [getSectorEntities] and deduplicated by market ID.
     *
     * This function can be expensive. Avoid calling it frequently.
     */
    @JvmStatic
    fun getSectorMarkets(): List<MarketAPI> {
        return getMarkets(getSectorEntities())
    }

    /**
     * Returns markets from a precomputed entity list.
     */
    @JvmStatic
    fun getMarkets(entities: List<SectorEntityToken>): List<MarketAPI> {
        return entities
            .mapNotNull { it.market }
            .distinctBy { it.id }
    }

    /**
     * Returns all unique submarkets in the sector.
     *
     * Submarkets are collected from all [getMarkets] and deduplicated by reference.
     *
     * This function can be expensive. Avoid calling it frequently.
     */
    @JvmStatic
    fun getSectorSubmarkets(): List<SubmarketAPI> {
        return getSubmarkets(getSectorMarkets())
    }

    /**
     * Returns submarkets from a precomputed market list.
     */
    @JvmStatic
    fun getSubmarkets(markets: List<MarketAPI>): List<SubmarketAPI> {
        return markets
            .flatMap { it.submarketsCopy }
            .distinctBy { it }
    }

    /**
     * Returns all unique cargo instances from sector submarkets.
     *
     * Cargo is collected from all [getSubmarkets] and deduplicated by reference.
     *
     * This function can be expensive. Avoid calling it frequently.
     */
    @JvmStatic
    fun getCargoFromSectorSubmarkets(): List<CargoAPI> {
        return getCargoFromSubmarkets(getSectorSubmarkets())
    }

    /**
     * Returns cargo from a precomputed submarket list.
     */
    @JvmStatic
    fun getCargoFromSubmarkets(submarkets: List<SubmarketAPI>): List<CargoAPI> {
        return submarkets
            .mapNotNull { it.cargo }
            .distinctBy { it }
    }
}