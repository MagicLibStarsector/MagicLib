package org.magiclib.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.lwjgl.util.vector.Vector2f

// SettingsAPI.java

/**
 * Bit of a hack to have this method here [in SettingsAPI]. It's just a way to call into some unexposed utility code.
 * @param from
 * @param target
 * @param considerShield
 * @return
 *
 * @since 1.3.0
 */
fun Vector2f.getTargetingRadius(target: CombatEntityAPI, considerShield: Boolean) =
    Global.getSettings().getTargetingRadius(this, target, considerShield)

/**
 * @since 1.3.0
 */
fun ShipAPI.createDefaultShipAI(config: ShipAIConfig) = Global.getSettings().createDefaultShipAI(this, config)

/**
 * @since 1.3.0
 */
fun Vector2f.getAngleInDegreesFast() = Global.getSettings().getAngleInDegreesFast(this)

/**
 * @since 1.3.0
 */
fun Vector2f.getAngleInDegreesFast(to: Vector2f) = Global.getSettings().getAngleInDegreesFast(this, to)

/**
 * @since 1.3.0
 */
fun ShipVariantAPI.computeNumFighterBays() = Global.getSettings().computeNumFighterBays(this)

/**
 * @since 1.3.0
 */
fun TooltipMakerAPI.addCommodityInfoToTooltip(
    initPad: Float,
    spec: CommoditySpecAPI?,
    max: Int,
    withText: Boolean,
    withSell: Boolean,
    withBuy: Boolean
) = Global.getSettings().addCommodityInfoToTooltip(this, initPad, spec, max, withText, withSell, withBuy)

/**
 * @since 1.3.0
 */
fun FleetMemberAPI.pickShipAIPlugin(ship: ShipAPI?) = Global.getSettings().pickShipAIPlugin(this, ship)