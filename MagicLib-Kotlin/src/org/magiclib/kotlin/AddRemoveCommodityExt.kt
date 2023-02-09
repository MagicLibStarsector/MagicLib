@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addStackGainText(stack: CargoStackAPI) = AddRemoveCommodity.addStackGainText(stack, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addStackGainText(stack: CargoStackAPI, lowerCase: Boolean) =
    AddRemoveCommodity.addStackGainText(stack, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addCommodityGainText(commodityId: String, quantity: Int) =
    AddRemoveCommodity.addCommodityGainText(commodityId, quantity, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addCommodityLossText(commodityId: String, quantity: Int) =
    AddRemoveCommodity.addCommodityLossText(commodityId, quantity, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addCreditsGainText(credits: Int) = AddRemoveCommodity.addCreditsGainText(credits, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addCreditsLossText(credits: Int) = AddRemoveCommodity.addCreditsLossText(credits, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addAbilityGainText(abilityId: String) = AddRemoveCommodity.addAbilityGainText(abilityId, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addOfficerGainText(officer: PersonAPI) = AddRemoveCommodity.addOfficerGainText(officer, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addOfficerLossText(officer: PersonAPI) = AddRemoveCommodity.addOfficerLossText(officer, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addAdminGainText(admin: PersonAPI) = AddRemoveCommodity.addAdminGainText(admin, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addFleetMemberGainText(member: FleetMemberAPI) =
    AddRemoveCommodity.addFleetMemberGainText(member, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addFleetMemberLossText(member: FleetMemberAPI) =
    AddRemoveCommodity.addFleetMemberLossText(member, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addFleetMemberGainText(variant: ShipVariantAPI) =
    AddRemoveCommodity.addFleetMemberGainText(variant, this)

/**
 * @since 0.46.0
 */
inline fun TextPanelAPI.addCRLossText(member: FleetMemberAPI, crLoss: Float) =
    AddRemoveCommodity.addCRLossText(member, this, crLoss)