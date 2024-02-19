package org.magiclib.activators

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.util.IntervalUtil
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicUI

object ActivatorManager {
    var hotkeyList: List<Int> = mutableListOf()

    /**
     * Call in your [onApplicationLoad] to initialize hotkeys and settings.
     */
    @JvmStatic
    fun initialize() {
        reloadKeys()
        LunaSettings.addSettingsListener(LunaKeybindSettingsListener())
    }

    /**
     * Add an activator to a ship.
     * @param ship The ship to add the activator to.
     * @param activator The activator to add.
     */
    @JvmStatic
    fun addActivator(ship: ShipAPI, activator: CombatActivator) {
        var shipActivatorData = getActivatorMapForShip(ship)

        if (shipActivatorData == null) {
            shipActivatorData = LinkedHashMap()
            ship.setCustomData("combatActivators", shipActivatorData)
        }

        shipActivatorData.let { activators ->
            if (!activators.containsKey(activator.javaClass)) {
                activators[activator.javaClass] = activator

                if (!activator.canAssignKey() || activators.size > hotkeyList.size) {
                    activator.key = CombatActivator.BLANK_KEY
                } else {
                    activator.keyIndex = activators
                        .filterValues { it.canAssignKey() }
                        .count()
                }

                activator.init()
            }
        }
    }

    /**
     * Remove an activator from a ship.
     */
    @JvmStatic
    fun removeActivator(ship: ShipAPI, activatorClass: Class<out CombatActivator>) {
        getActivatorMapForShip(ship)?.remove(activatorClass)
    }

    /**
     * Gets a shallow copy of the list of activators for a ship.
     */
    @JvmStatic
    fun getActivatorsForShipCopy(ship: ShipAPI): List<CombatActivator>? {
        val map = getActivatorMapForShip(ship) ?: return null
        return ArrayList(map.values)
    }

    /**
     * Gets a map of activators for a ship, where the key is the activator's class.
     */
    @JvmStatic
    fun getActivatorMapForShip(ship: ShipAPI): MutableMap<Class<out CombatActivator>, CombatActivator>? {
        return ship.customData["combatActivators"] as? MutableMap<Class<out CombatActivator>, CombatActivator>?
            ?: return null
    }

    fun reloadKeys() {
        hotkeyList = mutableListOf(
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind1")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind2")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind3")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind4")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind5")!!,
        )
            .filter { it != 0 }
    }
}

class LunaKeybindSettingsListener : LunaSettingsListener {
    override fun settingsChanged(modID: String) {
        ActivatorManager.reloadKeys()
    }
}

fun IntervalUtil.advanceAndCheckElapsed(amount: Float): Boolean {
    this.advance(amount)
    return this.intervalElapsed()
}