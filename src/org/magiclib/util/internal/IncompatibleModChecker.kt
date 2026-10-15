package org.magiclib.util.internal

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.json.JSONObject
import org.lwjgl.Sys
import org.magiclib.Magic_modPlugin
import org.magiclib.kotlin.forEach
import org.magiclib.kotlin.toStringList
import org.magiclib.util.MagicTxt

internal object IncompatibleModChecker {
    val log: Logger = Global.getLogger(IncompatibleModChecker::class.java)

    internal fun check() {
        try {
            val settings = Global.getSettings()
            val modManager = settings.modManager
            val enabledMods = modManager.enabledModsCopy
            for (mod in enabledMods) {
                val modInfo = try {
                    settings.loadJSON("mod_info.json", mod.id)
                } catch (e: Exception) {
                    log.error("Failed to load mod_info.json for " + mod.id, e)
                    continue
                } ?: continue
                val incompModsArray = modInfo.optJSONArray("magiclib_modConflicts") ?: continue
                incompModsArray.forEach<JSONObject> { incompModObject ->
                    fun JSONObject.requireString(key: String): String? =
                        optString(key, null).also { if (it == null) log.error("Invalid mod_info.json for ${mod.id}") }

                    val incompModId = incompModObject.requireString("id") ?: return@forEach
                    val incompMod = modManager.getModSpec(incompModId) ?: return@forEach // If the mod is not enabled, it will stop here.
                    val incompModReason = incompModObject.requireString("reason")

                    val title = MagicTxt.getString("imc_title")
                    val message = if (incompModReason == null) MagicTxt.getString("imc_message", mod.name, mod.id, incompMod.name, incompMod.id)
                    else MagicTxt.getString("imc_message_with_reason", mod.name, mod.id, incompMod.name, incompMod.id, incompModReason)
                    fatalNow(
                        title,
                        message
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Failed to check for incompatible mods", e)
        }
    }

    private fun fatalNow(title: String, message: String) {
        log.error(message)

        try {
            val currentThread = Thread.currentThread()
            if (currentThread.name != "main") {
                for (thread in Thread.getAllStackTraces().keys) {
                    if (currentThread === thread || thread.name != "main") continue
                    // Throws UnsupportedOperationException as of JDK 20, but is caught by try/catch.
                    // This only really does anything if it is called early, it might do nothing at all.
                    // This is not vitally important to run, all it does is pause the loading in the background. It may not work for all setups.
                    // The game halts at the end of loading due to Sys.alert anyway, this just does it earlier.
                    @Suppress("removal", "DEPRECATION")
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