package org.magiclib

import com.fs.starfarer.api.Global
import lunalib.backend.ui.settings.LunaSettingsLoader
import lunalib.lunaSettings.LunaSettings
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.optFloat
import org.magiclib.kotlin.optColor
import java.awt.Color

/**
 * Utility for reading user configured mod settings in a way that supports both LunaLib and plain `userSettings.json` files.
 *
 * If LunaLib is not enabled, or the setting does not exist in LunaLib, the utility will fall back to reading from `userSettings.json`.
 * This makes having an optional dependency on LunaLib easier, as none of the functions in this file require LunaLib to be an enabled mod.
 */
object MagicUserSettings {
    val lunaLibEnabled = Global.getSettings().modManager.isModEnabled("lunalib")

    /**
     * Adds a listener to be notified for when LunaLib settings change, optionally invoking the listener once upon creation.
     *
     * If [invokeImmediately] is true, the listener will be called once regardless of if the mod LunaLib is enabled or not.
     *
     * The listener is only called for changes to [modId]'s own settings, not for other mods.
     * @param modId The mod ID to listen to.
     * @param invokeImmediately If true, the listener will be invoked immediately after creation.
     * @param listener The listener to add.
     */
    @JvmStatic
    @JvmOverloads
    fun addSettingsListener(modId: String, invokeImmediately: Boolean = true, listener: MagicSettingsListener) {
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

    /**
     * Gets a [Boolean] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
    @JvmStatic
    @JvmOverloads
    fun getBoolean(modID: String, fieldID: String, default: Boolean = false): Boolean {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getBoolean(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optBoolean(fieldID, default)
    }

    /**
     * Gets a [Int] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
    @JvmStatic
    @JvmOverloads
    fun getInt(modID: String, fieldID: String, default: Int = 0): Int {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getInt(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optInt(fieldID, default)
    }

    /**
     * Gets a [Double] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
    @JvmStatic
    @JvmOverloads
    fun getDouble(modID: String, fieldID: String, default: Double = 0.0): Double {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getDouble(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optDouble(fieldID, default)
    }

    /**
     * Gets a [Float] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
    @JvmStatic
    @JvmOverloads
    fun getFloat(modID: String, fieldID: String, default: Float = 0f): Float {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getFloat(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optFloat(fieldID, default)
    }

    /**
     * Gets a [String] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
    @JvmStatic
    @JvmOverloads
    fun getString(modID: String, fieldID: String, default: String = ""): String {
        if (lunaLibEnabled && settingExistsInLunaLib(modID, fieldID))
            return LunaSettings.getString(modID, fieldID) ?: default

        val modSettings = loadModFileSettings(modID) ?: return default

        return modSettings.optString(fieldID, default)
    }

    /**
     * Gets a [Color] setting: checks LunaLib first, if LunaLib is not enabled or does not have that setting it will look in `userSettings.json` instead.
     *
     * Returns [default] if the setting wasn't found in either.
     * @param modID The mod ID to retrieve the setting from.
     * @param fieldID The field ID of the setting.
     * @param default The default value to return if the setting is not found.
     */
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

fun interface MagicSettingsListener {
    fun settingsChanged(modID: String)
}