package org.magiclib.util.internal

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import org.magiclib.ReflectionUtils.getFieldsMatching
import org.magiclib.kotlin.doesFileExist
import org.magiclib.util.api.kotlin.HullExt.getActualHullId
import org.magiclib.util.api.kotlin.HullExt.isSkin

internal object ShipSkinSourceMod {
    fun setShipSkinSourceMods() {
        Global.getLogger(this.javaClass).info("Setting modded ship skin source mods (if present)...")
        var count = 0
        val allHullSpecs = Global.getSettings().allShipHullSpecs
        allHullSpecs.forEach { hull ->
            if (hull.sourceMod == null && hull.isSkin()) {
                val sourceMod = getSourceModFromSkin(hull.getActualHullId())
                if (sourceMod != null) {
                    hull.getFieldsMatching(type = ModSpecAPI::class.java).getOrNull(0)?.set(hull, sourceMod)
                    Global.getLogger(this.javaClass).info("Set modded ship skin source mod for ${hull.hullId} to modID ${sourceMod.id}.")
                    count++
                } else if (hull.shipFilePath.startsWith("data/hulls/") // Base game skins typically have a full file path. Mod skins starts with "data/hulls/"
                    ) {
                    // This is not a base-game hull, yet no sourceMod was found.
                    Global.getLogger(this.javaClass).info("Could not set the modded ship skin source mod for ${hull.hullId} at path ${hull.shipFilePath}. Make sure the skinId and file name are equal.")
                }
            }
        }

        if (count > 0)
            Global.getLogger(this.javaClass).info("Set modded ship skins source mods for $count hulls.")
        else
            Global.getLogger(this.javaClass).info("None present, no changes made.")
    }

    fun getSourceModFromSkin(hullId: String?): ModSpecAPI? {
        val settings = Global.getSettings()

        val filename = "data/hulls/skins/$hullId.skin"
        settings.modManager.enabledModsCopy.forEach { mod ->
            if (settings.doesFileExist(filename, mod.id))
                return mod
        }

        return null
    }
}