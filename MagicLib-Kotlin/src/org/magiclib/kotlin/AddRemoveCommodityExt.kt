package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity

fun TextPanelAPI.addStackGainText(stack: CargoStackAPI) = AddRemoveCommodity.addStackGainText(stack, this)
fun TextPanelAPI.addStackGainText(stack: CargoStackAPI, lowerCase: Boolean) =
    AddRemoveCommodity.addStackGainText(stack, this)

fun TextPanelAPI.addCommodityGainText(commodityId: String, quantity: Int) =
    AddRemoveCommodity.addCommodityGainText(commodityId, quantity, this)

fun TextPanelAPI.addCommodityLossText(commodityId: String, quantity: Int) =
    AddRemoveCommodity.addCommodityLossText(commodityId, quantity, this)

fun TextPanelAPI.addCreditsGainText(credits: Int) = AddRemoveCommodity.addCreditsGainText(credits, this)
fun TextPanelAPI.addCreditsLossText(credits: Int) = AddRemoveCommodity.addCreditsLossText(credits, this)
fun TextPanelAPI.addAbilityGainText(abilityId: String) = AddRemoveCommodity.addAbilityGainText(abilityId, this)
fun TextPanelAPI.addOfficerGainText(officer: PersonAPI) = AddRemoveCommodity.addOfficerGainText(officer, this)
fun TextPanelAPI.addOfficerLossText(officer: PersonAPI) = AddRemoveCommodity.addOfficerLossText(officer, this)
fun TextPanelAPI.addAdminGainText(admin: PersonAPI) = AddRemoveCommodity.addAdminGainText(admin, this)
fun TextPanelAPI.addFleetMemberGainText(member: FleetMemberAPI) =
    AddRemoveCommodity.addFleetMemberGainText(member, this)

fun TextPanelAPI.addFleetMemberLossText(member: FleetMemberAPI) =
    AddRemoveCommodity.addFleetMemberLossText(member, this)

fun TextPanelAPI.addFleetMemberGainText(variant: ShipVariantAPI) =
    AddRemoveCommodity.addFleetMemberGainText(variant, this)

fun TextPanelAPI.addCRLossText(member: FleetMemberAPI, crLoss: Float) =
    AddRemoveCommodity.addCRLossText(member, this, crLoss)