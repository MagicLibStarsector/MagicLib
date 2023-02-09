@file:Suppress("NOTHING_TO_INLINE")

package org.magiclib.kotlin

import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3

/**
 * @since 0.46.0
 */
inline fun FleetParamsV3.createFleet() = FleetFactoryV3.createFleet(this)
