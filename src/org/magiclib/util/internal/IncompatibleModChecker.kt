package org.magiclib.util.internal

import com.fs.starfarer.api.Global
import org.lwjgl.Sys
import org.magiclib.Magic_modPlugin
import org.magiclib.kotlin.toStringList
import org.magiclib.util.MagicTxt

internal object IncompatibleModChecker {
    internal fun check() {
        val settings = Global.getSettings()
        val modManager = settings.modManager
        val enabledMods = modManager.enabledModsCopy
        for (mod in enabledMods) {
            val modInfo = try {
                settings.loadJSON("mod_info.json", mod.id)
            } catch (e: Exception) {
                Global.getLogger(Magic_modPlugin::class.java).error("Failed to load mod_info.json for " + mod.id, e)
                continue
            }
            if (modInfo == null || !modInfo.has("conflicts")) continue

            val incompModsList = modInfo.optJSONArray("conflicts")?.toStringList() ?: continue

            for (incompModId in incompModsList) {
                val incompMod = modManager.getModSpec(incompModId) ?: continue
                fatalNow(
                    MagicTxt.getString("imc_title"),
                    MagicTxt.getString("imc_message", mod.name, mod.id, incompMod.name, incompMod.id)
                )
            }
        }
    }

    private fun fatalNow(title: String, message: String) {
        Global.getLogger(this::class.java).error(message)

        try {
            val currentThread = Thread.currentThread()
            if (currentThread.name != "main") {
                for (thread in Thread.getAllStackTraces().keys) {
                    if (currentThread === thread || thread.name != "main") continue
                    @Suppress("removal", "DEPRECATION")
                    // Throws UnsupportedOperationException as of JDK 20, but is caught by try/catch.
                    // This is not vitally important to run, all it does is pause the loading thread right away. The game halts at the end of loading due to Sys.alert anyway, this just does it earlier.
                    thread.suspend()
                }
            }
        } catch (_: Throwable) {
        } finally {
            Sys.alert(title, message)
            Runtime.getRuntime().halt(1)
        }
    }
}