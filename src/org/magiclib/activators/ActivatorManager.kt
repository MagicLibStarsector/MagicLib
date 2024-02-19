package org.magiclib.activators

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.IntervalUtil
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import org.apache.log4j.Logger
import org.lazywizard.lazylib.ext.logging.i
import org.lwjgl.input.Keyboard
import org.magiclib.util.MagicSettings

object ActivatorManager {
    val log: Logger = Global.getLogger(ActivatorManager::class.java)
    val lunaLibEnabled = Global.getSettings().modManager.isModEnabled("lunalib")
    var hotkeyList: List<Int> = mutableListOf()

    /**
     * Call in your [onApplicationLoad] to initialize hotkeys and settings.
     */
    @JvmStatic
    fun initialize() {
        reloadKeys()

        if (lunaLibEnabled) {
            LunaSettings.addSettingsListener(LunaKeybindSettingsListener())
        }
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

                if (activator.canAssignKey()) {
                    reassignKeys(ship)
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
    private fun getActivatorMapForShip(ship: ShipAPI): MutableMap<Class<out CombatActivator>, CombatActivator>? {
        return ship.customData["combatActivators"] as? MutableMap<Class<out CombatActivator>, CombatActivator>?
            ?: return null
    }

    @JvmStatic
    fun getKeyForIndex(index: Int): Int {
        if (index < 0 || index >= hotkeyList.size) return -1
        return hotkeyList[index]
    }

    @JvmStatic
    fun reassignKeys(ship: ShipAPI) {
        val shipActivatorData = getActivatorMapForShip(ship) ?: return
        var skippedIndexes = 0

        shipActivatorData.values
            .filter { it.canAssignKey() }
            .filter { it.key == CombatActivator.BLANK_KEY } //do not mess with keys assigned by the activator
            .sortedWith { a, b ->
                if (a.order == b.order) {
                    a.displayText.compareTo(b.displayText) //sort by alphabet
                } else {
                    b.order.compareTo(a.order)  //sort by descending order
                }
            }
            .forEachIndexed { index, combatActivator ->
                var actualIndex = index + skippedIndexes
                //skip over keys used by activators with static keys
                while (actualIndex < hotkeyList.size && shipActivatorData.values.any { Keyboard.getKeyIndex(it.key) == hotkeyList[actualIndex] })
                    actualIndex = index + (++skippedIndexes)

                if (actualIndex >= hotkeyList.size) {
                    combatActivator.key = CombatActivator.BLANK_KEY
                    combatActivator.keyIndex = -1
                } else {
                    combatActivator.keyIndex = actualIndex
                }
            }
    }

    fun reloadKeys() {
        if (lunaLibEnabled) {
            hotkeyList = mutableListOf(
                LunaSettings.getInt("combatactivators", "combatActivators_KeyBind1")!!,
                LunaSettings.getInt("combatactivators", "combatActivators_KeyBind2")!!,
                LunaSettings.getInt("combatactivators", "combatActivators_KeyBind3")!!,
                LunaSettings.getInt("combatactivators", "combatActivators_KeyBind4")!!,
                LunaSettings.getInt("combatactivators", "combatActivators_KeyBind5")!!,
            )
                .filter { it != 0 }
        } else {
            hotkeyList = MagicSettings.getList("MagicLib", "subsystemKeys")
                .map { Keyboard.getKeyIndex(it) }
                .filter { it != 0 }
        }

        log.i({ "Loaded hotkey list ${hotkeyList.joinToString{ "," }}" })
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