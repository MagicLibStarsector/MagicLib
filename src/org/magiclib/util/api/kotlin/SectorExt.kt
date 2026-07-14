package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import org.magiclib.util.api.SectorUtils

/**
 * Returns the actual CoreUITabId of the campaign UI.
 *
 * This extension is necessary because the campaign UI can report that the player is still in a CoreUITab even if they are not.
 * This can happen when the player enters an interaction dialog, opens any CoreUITab such as the crew/cargo tab, then escapes that CoreUITab back to the interaction dialog. It will still report that they are in the crew/cargo tab when they are not.
 *
 * This extension checks if the player is in a ghost interaction dialog and if so, returns null, indicating that the player is not in a CoreUITab.
 *
 * This delegates to [SectorUtils.getActualCurrentTab]
 */
fun CampaignUIAPI.getActualCurrentTab(): CoreUITabId? {
    return SectorUtils.getActualCurrentTab(this)
}
