@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.shared.PlayerTradeProfitabilityData.CommodityData
import org.lwjgl.util.vector.Vector2f


/**
 * @since 0.46.0
 */
inline fun StarSystemAPI.isNearCorona(loc: Vector2f) = HubMissionWithTriggers.isNearCorona(this, loc)

/**
 * @since 0.46.0
 */
inline fun CampaignTerrainAPI.getTerrainName() = BaseHubMission.getTerrainName(this)

/**
 * @since 0.46.0
 */
inline fun CampaignTerrainAPI.hasSpecialName() = BaseHubMission.hasSpecialName(this)

/**
 * @since 0.46.0
 */
inline fun CampaignTerrainAPI.getTerrainNameAOrAn() = BaseHubMission.getTerrainNameAOrAn(this)

/**
 * @since 0.46.0
 */
inline fun CampaignTerrainAPI.getTerrainType() = BaseHubMission.getTerrainType(this)

/**
 * @since 0.46.0
 */
inline fun CommodityData.playerHasEnough(quantity: Int) = BaseHubMission.playerHasEnough(this.commodityId, quantity)
