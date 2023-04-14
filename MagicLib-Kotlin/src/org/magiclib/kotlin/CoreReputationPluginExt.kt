@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color


/**
 * @since 0.46.1
 */
inline fun TextPanelAPI.addDeltaMessage(
    delta: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    info: TooltipMakerAPI?,
    textColor: Color?,
    withCurrent: Boolean,
    padding: Float
) = CoreReputationPlugin.addDeltaMessage(
    delta, faction, person, this, info, textColor, withCurrent, padding
)

/**
 * @since 0.46.1
 */
inline fun TooltipMakerAPI.addDeltaMessage(
    delta: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    panel: TextPanelAPI?,
    textColor: Color?,
    withCurrent: Boolean,
    padding: Float
) = CoreReputationPlugin.addDeltaMessage(
    delta, faction, person, panel, this, textColor, withCurrent, padding
)

/**
 * @since 0.46.1
 */
inline fun TooltipMakerAPI.addNoChangeMessage(
    deltaSign: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    message: CommMessageAPI?,
    panel: TextPanelAPI?,
    textColor: Color?,
    withCurrent: Boolean,
    padding: Float,
    reason: String? = null
) = CoreReputationPlugin.addNoChangeMessage(
    deltaSign, faction, person, message, panel, this, textColor, withCurrent, padding, reason
)

/**
 * @since 0.46.1
 */
inline fun TextPanelAPI.addAdjustmentMessage(
    delta: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    message: CommMessageAPI? = null,
    info: TooltipMakerAPI? = null,
    textColor: Color,
    withCurrent: Boolean,
    padding: Float,
    reason: String? = null
) = CoreReputationPlugin.addAdjustmentMessage(
    delta, faction, person, message, this, info, textColor, withCurrent, padding, reason
)

/**
 * @since 0.46.1
 */
inline fun TextPanelAPI.addCurrentStanding(
    faction: FactionAPI?,
    person: PersonAPI?,
    info: TooltipMakerAPI? = null,
    textColor: Color,
    padding: Float
) = CoreReputationPlugin.addCurrentStanding(
    faction, person, this, info, textColor, padding
)

/**
 * @since 0.46.1
 */
inline fun TooltipMakerAPI.addCurrentStanding(
    faction: FactionAPI?,
    person: PersonAPI?,
    panel: TextPanelAPI? = null,
    textColor: Color,
    padding: Float
) = CoreReputationPlugin.addCurrentStanding(
    faction, person, panel, this, textColor, padding
)

/**
 * @since 0.46.1
 */
inline fun TextPanelAPI.addRequiredStanding(
    faction: FactionAPI?,
    requiredStanding: RepLevel,
    person: PersonAPI?,
    info: TooltipMakerAPI? = null,
    textColor: Color,
    padding: Float,
    orBetter: Boolean
) = CoreReputationPlugin.addRequiredStanding(
    faction, requiredStanding, person, this, info, textColor, padding, orBetter
)

/**
 * @since 0.46.1
 */
inline fun TooltipMakerAPI.addRequiredStanding(
    faction: FactionAPI?,
    requiredStanding: RepLevel,
    person: PersonAPI?,
    panel: TextPanelAPI? = null,
    textColor: Color,
    padding: Float,
    orBetter: Boolean
) = CoreReputationPlugin.addRequiredStanding(
    faction, requiredStanding, person, panel, this, textColor, padding, orBetter
)

/**
 * @since 0.46.1
 */
inline fun TextPanelAPI.addNoChangeMessage(
    deltaSign: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    message: CommMessageAPI?,
    info: TooltipMakerAPI?,
    textColor: Color?,
    withCurrent: Boolean,
    padding: Float,
    reason: String? = null
) = CoreReputationPlugin.addNoChangeMessage(
    deltaSign, faction, person, message, this, info, textColor, withCurrent, padding, reason
)

/**
 * @since 0.46.1
 */
inline fun TooltipMakerAPI.addAdjustmentMessage(
    delta: Float,
    faction: FactionAPI?,
    person: PersonAPI?,
    message: CommMessageAPI? = null,
    panel: TextPanelAPI? = null,
    textColor: Color,
    withCurrent: Boolean,
    padding: Float,
    reason: String? = null
) = CoreReputationPlugin.addAdjustmentMessage(
    delta, faction, person, panel, this, textColor, withCurrent, padding, reason
)