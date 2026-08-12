@file:JvmName("OtherUtils")

package org.magiclib.util.api

import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI

/**
 * Delegates to [ShipHullSpecAPI.createHullVariant].
 */
fun SettingsAPI.createHullVariant(hull: ShipHullSpecAPI): ShipVariantAPI =
    hull.createHullVariant()
