package org.magiclib.util

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.VersionInfoAPI
import com.fs.starfarer.launcher.ModManager

internal class MagicModVersionTracker: EveryFrameScript {
    var init = false
    override fun isDone(): Boolean = init
    override fun runWhilePaused(): Boolean = true

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
            val versionInfo = ModManager.VersionInfo(mod.version)
            //copying version info may not be necessary as the above should already do so, but for safety sake do it anyway.
            versionInfo.major = mod.versionInfo.major
            versionInfo.minor = mod.versionInfo.minor
            versionInfo.patch = mod.versionInfo.patch
            mlModVersions[mod.id] = versionInfo as VersionInfoAPI
        }

        persistentData["ml_modVersions"] = mlModVersions
        init = true
    }
}