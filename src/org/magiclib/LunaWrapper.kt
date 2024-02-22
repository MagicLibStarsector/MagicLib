package org.magiclib

import lunalib.lunaSettings.LunaSettings

/**
 * There's some bug where having even a soft dependency on LunaLib becomes a hard dependency.
 * Creating a wrapper class that's onlyl instantiated if LunaLib is present seems to fix it.
 */
object LunaWrapper {
    @JvmStatic
    fun addSettingsListener(listener: LunaWrapperSettingsListener) {
        LunaSettings.addSettingsListener(object : lunalib.lunaSettings.LunaSettingsListener {
            override fun settingsChanged(modID: String) {
                listener.settingsChanged(modID)
            }
        })
    }

    @JvmStatic
    fun getBoolean(modID: String, fieldID: String): Boolean? = LunaSettings.getBoolean(modID, fieldID)

    @JvmStatic
    fun getInt(modID: String, fieldID: String): Int? = LunaSettings.getInt(modID, fieldID)
}

interface LunaWrapperSettingsListener {
    fun settingsChanged(modID: String)
}