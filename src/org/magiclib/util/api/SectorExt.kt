@file:JvmName("SectorUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI

/**
 * Returns the actual CoreUITabId of the campaign UI.
 *
 * This function is necessary because the campaign UI can report that the player is still in a CoreUITab even if they are not.
 * This can happen when the player enters an interaction dialog, opens any CoreUITab such as the crew/cargo tab, then escapes that CoreUITab back to the interaction dialog. It will still report that they are in the crew/cargo tab when they are not.
 *
 * This function checks if the player is in a ghost interaction dialog and if so, returns null, indicating that the player is not in a CoreUITab
 */
fun CampaignUIAPI.getActualCurrentTab(): CoreUITabId? {
    val sector = Global.getSector() ?: return null
    if (!sector.isPaused) return null
    if (this.currentInteractionDialog != null && this.currentInteractionDialog.interactionTarget != null) {
        // Validate that we're not stuck in a ghost interaction dialog. (Happens when you escape out of a CoreUITab while in an interaction dialog. It reports that the player is still in that CoreUITab, which is false)
        if (this.currentInteractionDialog.optionPanel != null && this.currentInteractionDialog.optionPanel.savedOptionList.isNotEmpty()) return null
    }

    return this.currentCoreTab
}


/**
 * Returns all entities across all locations in the sector.
 */
fun SectorAPI.getAllEntities(): List<SectorEntityToken> {
    return this.allLocations
        .flatMap { it.allEntities }
}

/**
 * Returns all unique markets in the sector.
 *
 * Markets are filtered from [getAllEntities] and deduplicated by market ID.
 *
 * This function can be expensive. Avoid calling it frequently.
 */
fun SectorAPI.getMarkets(): List<MarketAPI> {
    return this.getMarkets(this.getAllEntities())
}

/**
 * Returns all unique markets in an entity list.
 *
 * Markets are deduplicated by market ID.
 */
fun SectorAPI.getMarkets(entities: List<SectorEntityToken>): List<MarketAPI> {
    return entities
        .mapNotNull { it.market }
        .distinctBy { it.id }
}

/**
 * Returns all unique submarkets in the sector.
 *
 * Submarkets are filtered from [getMarkets] and deduplicated by reference.
 *
 * This function can be expensive. Avoid calling it frequently.
 */
fun SectorAPI.getSubmarkets(): List<SubmarketAPI> {
    return this.getSubmarkets(this.getMarkets())
}

/**
 * Returns all unique submarkets in a market list.
 *
 * Submarkets are deduplicated by reference.
 */
fun SectorAPI.getSubmarkets(markets: List<MarketAPI>): List<SubmarketAPI> {
    return markets
        .flatMap { it.submarketsCopy }
        .distinctBy { it }
}

/**
 * Returns all unique cargo instances from sector submarkets.
 *
 * Cargo instances are filtered from [getSubmarkets] and deduplicated by reference.
 *
 * This function can be expensive. Avoid calling it frequently.
 */
fun SectorAPI.getCargoFromSubmarkets(): List<CargoAPI> {
    return this.getCargoFromSubmarkets(this.getSubmarkets())
}

/**
 * Returns all unique cargo instances from a submarket list.
 *
 * Cargo instances are deduplicated by reference.
 */
fun SectorAPI.getCargoFromSubmarkets(submarkets: List<SubmarketAPI>): List<CargoAPI> {
    return submarkets
        .mapNotNull { it.cargo }
        .distinctBy { it }
}