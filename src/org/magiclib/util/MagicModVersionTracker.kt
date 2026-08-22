package org.magiclib.util

import com.fs.starfarer.api.ModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModManagerAPI
import com.fs.starfarer.api.VersionInfoAPI
import com.fs.starfarer.launcher.ModManager

/**
 * Records the version of every currently-enabled mod into the sector's persistent data after onGameLoad,
 * under the key `"ml_modVersions"` (mod id -> [VersionInfoAPI]).
 *
 * This gives other mods a way to check, on a loaded save, which version of a mod was enabled last session onGameLoad.
 * e.g. for a mod to detect that it was updated since a save was last played and trigger one-off migration logic.
 *
 * Runs only once per session on the first advance of EveryFrameScript. After which the script is discarded.
 */
class MagicModVersionTracker: EveryFrameScript {
    companion object {
        /**
         * Returns a map of the mod versions last recorded in persistent data.
         *
         * As the map is updated only after [ModPlugin.onGameLoad], this is intended to check the mod versions of the last session inside [ModPlugin.onGameLoad].
         *
         * @return A map of mod IDs to their last recorded versions.
         */
        @JvmStatic
        fun getModVersions(): Map<String, VersionInfoAPI> {
            @Suppress("UNCHECKED_CAST")
            return Global.getSector().persistentData["ml_modVersions"] as? Map<String, VersionInfoAPI> ?: emptyMap()
        }
    }

    var init = false
    override fun isDone(): Boolean = init
    override fun runWhilePaused(): Boolean = true

    /**
     * One-time pass that rebuilds the `"ml_modVersions"` persistent-data entry on first advance:
     * Starting from whatever was previously recorded (so mods that are currently disabled keep their last-known version),
     * then overwrites the entry for every mod that is currently enabled with its current version.
     */
    override fun advance(amount: Float) {
        val persistentData = Global.getSector().persistentData

        val mlModVersions = mutableMapOf<String, VersionInfoAPI>().apply {
            (persistentData["ml_modVersions"] as? Map<*, *>)?.forEach { (k, v) ->
                if (k is String && v is VersionInfoAPI) {
                    put(k, v)
                }
            }
        }

        Global.getSettings().modManager.enabledModsCopy.forEach { mod ->
            // Making a new versionInfo object instead of directly assigning mod.versionInfo may not be necessary, but for safety it is done this way.
            val versionInfo = ModManager.VersionInfo(mod.version)
            versionInfo.major = mod.versionInfo.major
            versionInfo.minor = mod.versionInfo.minor
            versionInfo.patch = mod.versionInfo.patch
            mlModVersions[mod.id] = versionInfo as VersionInfoAPI
        }

        persistentData["ml_modVersions"] = mlModVersions
        init = true
    }
}