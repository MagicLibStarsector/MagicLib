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

/**
 * Checks if [filePath] exists
 *
 * Without [modID], first checks the root of each enabled mod (e.g. /mods/MagicLib/[filePath]), then checks the working directory (e.g. /starsector-core/[filePath])
 *
 * With [modID], only checks the specified mod's folder and nowhere else.
 *
 * @param filePath Path using `/` separators, e.g. `data/config/settings.json`.
 * @param modID If set, searches only that mod's folder and nowhere else.
 * @return `true` if the file exists and can be loaded, `false` otherwise.
 */
@JvmOverloads
fun SettingsAPI.doesFileExist(
    filePath: String,
    modID: String? = null,
): Boolean = try {
    if (modID == null) {
        openStream(filePath).use { true } // Opens and closes, no read. Much faster.
    } else {
        loadText(filePath, modID) // No mod-scoped openStream exists, have to use the slower version.
        true
    }
} catch (_: Exception) {
    false
}