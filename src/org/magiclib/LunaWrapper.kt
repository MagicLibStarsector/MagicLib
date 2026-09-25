package org.magiclib

import com.fs.starfarer.api.Global
import lunalib.backend.ui.settings.LunaSettingsLoader
import lunalib.lunaSettings.LunaSettings
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.optFloat
import org.magiclib.kotlin.optColor
import java.awt.Color

object LunaWrapper {
    val lunaLibEnabled = Global.getSettings().modManager.isModEnabled("lunalib")

    /**
     * Adds a listener to be notified when LunaLib settings change.
     *
     * This listener gets called when the settings from any mod get changed.
     * @param listener The listener to add.
     */
    @JvmStatic
    @Deprecated("Use addSettingsListener(modId, listener) instead.")
    fun addSettingsListener(listener: LunaWrapperSettingsListener) {
        if (!lunaLibEnabled) return
        LunaSettings.addSettingsListener(object : lunalib.lunaSettings.LunaSettingsListener {
            override fun settingsChanged(modID: String) {
                listener.settingsChanged(modID)
            }
        })
    }

    /**
     * Adds a listener to be notified when LunaLib settings change and optionally once upon creation.
     *
     * If [invokeImmediately] is true, the listener will be called once irregardless of if LunaLib is enabled or not.
     *
     * This listener only gets called when the input [modId] matches the modId of the mod which had their settings changed.
     * @param modId The mod ID to listen to.
     * @param invokeImmediately If true, the listener will be invoked immediately after creation.
     * @param listener The listener to add.
     */
    @JvmStatic
    @JvmOverloads
    fun addSettingsListener(modId: String, invokeImmediately: Boolean = true, listener: LunaWrapperSettingsListener) {
        if (invokeImmediately)
            listener.settingsChanged(modId)

        if (!lunaLibEnabled) return

        LunaSettings.addSettingsListener(object : lunalib.lunaSettings.LunaSettingsListener {
            override fun settingsChanged(modID: String) {
                if (modID != modId)
                    return
                listener.settingsChanged(modID)
            }
        })
    }

    private var modSettings: JSONObject? = null
    private fun loadModFileSettings(modID: String): JSONObject? {
        if(modSettings == null) {
            try {
                modSettings = Global.getSettings().loadJSON("userSettings.json", modID)
            } catch (e: Exception) {
                Global.getLogger(this::class.java).error("Failed to load mod settings for $modID", e)
            }
        }
        
        return modSettings
    }

    private fun settingExistsInLunaLib(modID: String, fieldID: String): Boolean {
        if (!lunaLibEnabled) return false
        if (!LunaSettingsLoader.hasLoaded) LunaSettingsLoader.load()

        val modSettings = LunaSettingsLoader.Settings[modID] ?: return false
        return modSettings.has(fieldID)
    }

    @JvmStatic
    @JvmOverloads
    fun getBoolean(modID: String, fieldID: String, default: Boolean = false): Boolean {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getBoolean(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optBoolean(fieldID, default)
    }

    @JvmStatic
    @JvmOverloads
    fun getInt(modID: String, fieldID: String, default: Int = 0): Int {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getInt(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optInt(fieldID, default)
    }

    @JvmStatic
    @JvmOverloads
    fun getDouble(modID: String, fieldID: String, default: Double = 0.0): Double {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getDouble(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optDouble(fieldID, default)
    }

    @JvmStatic
    @JvmOverloads
    fun getFloat(modID: String, fieldID: String, default: Float = 0f): Float {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getFloat(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optFloat(fieldID, default)
    }

    @JvmStatic
    @JvmOverloads
    fun getString(modID: String, fieldID: String, default: String = ""): String {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getString(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optString(fieldID, default)
    }

    @JvmStatic
    @JvmOverloads
    fun getColor(modID: String, fieldID: String, default: Color = Color.WHITE): Color {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getColor(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        try {
            return modSettings.optColor(fieldID, default)
        } catch (e: Exception) {
            Global.getLogger(this::class.java).error("Failed to load color setting for modID '$modID' fieldID '$fieldID'", e)
            return default
        }
    }
}

fun interface LunaWrapperSettingsListener {
    fun settingsChanged(modID: String)
}