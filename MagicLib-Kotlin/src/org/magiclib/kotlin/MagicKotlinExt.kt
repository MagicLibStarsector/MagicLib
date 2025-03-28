@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.*
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import java.awt.Color
import kotlin.random.Random
import kotlin.system.measureTimeMillis

// Extensions that are one-offs or two-offs and don't justify an entire file to themselves.

/**
 * @since 0.46.0
 */
inline fun FleetParamsV3.createFleet() = FleetFactoryV3.createFleet(this)

/**
 * @since 0.46.1
 */
inline fun Color.modify(red: Int = this.red, green: Int = this.green, blue: Int = this.blue, alpha: Int = this.alpha) =
    Color(red, green, blue, alpha)

/**
 * Returns the red component in the range 0f-1f in the default sRGB space.
 */
inline val Color.redf: Float
    get() = this.red / 255f

/**
 * Returns the green component in the range 0f-1f in the default sRGB space.
 */
inline val Color.greenf: Float
    get() = this.green / 255f

/**
 * Returns the blue component in the range 0f-1f in the default sRGB space.
 */
inline val Color.bluef: Float
    get() = this.blue / 255f

/**
 * Returns the alpha component in the range 0f-1f in the default sRGB space.
 */
inline val Color.alphaf : Float
    get() = this.alpha / 255f

/**
 * Time how long it takes to run [func] and run [onFinished] afterwards.
 * If `onlyRunTraceInDevMode` is true and dev mode is disabled, `onFinished` will still run but `millis` will be 0.
 *
 * @since 0.46.1
 */
inline fun <T> trace(
    onlyRunTraceInDevMode: Boolean = true,
    onFinished: (result: T, millis: Long) -> Unit,
    func: () -> T
): T {
    var result: T
    val millis =
        if (!onlyRunTraceInDevMode || Global.getSettings().isDevMode)
            measureTimeMillis { result = func() }
        else {
            result = func()
            0
        }
    onFinished(result, millis)
    return result
}

/**
 * Time how long it takes to run [func].
 * @since 0.46.1
 */
inline fun <T> trace(
    onlyRunTraceInDevMode: Boolean = true,
    func: () -> T
): T =
    trace(
        onlyRunTraceInDevMode,
        onFinished = { _, _ -> },
        func = func
    )

/**
 * @since 0.46.1
 * @see Misc.adjustRep
 */
inline fun FactionAPI.adjustReputationWithPlayer(
    repChange: Float, textPanel: TextPanelAPI? = null, limit: RepLevel? = null
) {
    if (repChange != 0f) {
        Global.getSector().adjustPlayerReputation(
            RepActionEnvelope(
                /* action = */ RepActions.CUSTOM,
                /* param = */ CustomRepImpact().apply {
                    this.delta = repChange
                    this.limit = limit
                },
                /* message = */ null,
                /* textPanel = */ textPanel,
                /* addMessageOnNoChange = */ true
            ), this.id
        )
    }
}

/**
 * @since 0.46.1
 * @see Misc.adjustRep
 */
inline fun PersonAPI.adjustReputationWithPlayer(
    repChange: Float, textPanel: TextPanelAPI? = null, limit: RepLevel? = null
) {
    if (repChange != 0f) {
        Global.getSector().adjustPlayerReputation(
            RepActionEnvelope(
                /* action = */ RepActions.CUSTOM,
                /* param = */ CustomRepImpact().apply {
                    this.delta = repChange
                    this.limit = limit
                },
                /* message = */ null,
                /* textPanel = */ textPanel,
                /* addMessageOnNoChange = */ true
            ), this
        )
    }
}

/**
 * @since 0.46.1
 * @see Misc.adjustRep
 */
inline fun TextPanelAPI.adjustReputationWithPlayer(
    factionId: String, repChange: Float, limit: RepLevel? = null
) = Global.getSector().getFaction(factionId).adjustReputationWithPlayer(repChange, this, limit)

/**
 * @since 0.46.1
 * @see Misc.adjustRep
 */
inline fun TextPanelAPI.adjustReputationWithPlayer(
    person: PersonAPI, repChange: Float, limit: RepLevel? = null
) = person.adjustReputationWithPlayer(repChange, this, limit)

fun ClosedFloatingPointRange<Float>.random(): Float =
    Random.nextDouble(this.start.toDouble(), this.endInclusive.toDouble()).toFloat()

fun CampaignClockAPI.elapsedDaysSinceGameStart(): Float =
    Global.getSector().clock.getElapsedDaysSince(-55661245698000L);