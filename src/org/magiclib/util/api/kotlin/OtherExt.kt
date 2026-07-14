package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import org.magiclib.util.api.HullUtils

/**
 * Delegates to [HullUtils.createHullVariant].
 */
fun SettingsAPI.createHullVariant(hull: ShipHullSpecAPI): ShipVariantAPI =
    HullUtils.createHullVariant(hull)
