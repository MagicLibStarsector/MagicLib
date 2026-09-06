package org.magiclib

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import org.lazywizard.lazylib.LazyLib
import org.magiclib.util.MagicVariables

/**
 * There's some bug where having even a soft dependency on LunaLib becomes a hard dependency.
 * Creating a wrapper class that's onlyl instantiated if LunaLib is present seems to fix it.
 */
object LunaWrapper {
    /**
     * Adds a listener to be notified when LunaLib settings change.
     *
     * This listener gets called when the settings from any mod get changed.
     * @param listener The listener to add.
     */
    @JvmStatic
    fun addSettingsListener(listener: LunaWrapperSettingsListener) {
        if (!Global.getSettings().modManager.isModEnabled("lunalib"))
            return

        LunaSettings.addSettingsListener(object : lunalib.lunaSettings.LunaSettingsListener {
            override fun settingsChanged(modID: String) {
                listener.settingsChanged(modID)
            }
        })
    }

    /**
     * Adds a listener to be notified when LunaLib settings change and optionally once upon creation.
     *
     * This listener only gets called when the input [modId] matches the modId of the mod which had their settings changed.
     * @param modId The mod ID to listen to.
     * @param invokeImmediately If true, the listener will be invoked immediately after creation.
     * @param listener The listener to add.
     */
    @JvmStatic
    @JvmOverloads
    internal fun addSettingsListener(modId: String, invokeImmediately: Boolean = true, listener: LunaWrapperSettingsListener) {
        if (!Global.getSettings().modManager.isModEnabled("lunalib"))
            return

        LunaSettings.addSettingsListener(object : lunalib.lunaSettings.LunaSettingsListener {
            override fun settingsChanged(modID: String) {
                if (modID != modId)
                    return
                listener.settingsChanged(modID)
            }
        })

        if (invokeImmediately)
            listener.settingsChanged(MagicVariables.MAGICLIB_ID)
    }

    @JvmStatic
    fun getBoolean(modID: String, fieldID: String): Boolean? = LunaSettings.getBoolean(modID, fieldID)

    @JvmStatic
    fun getInt(modID: String, fieldID: String): Int? = LunaSettings.getInt(modID, fieldID)
}

fun interface LunaWrapperSettingsListener {
    fun settingsChanged(modID: String)
}