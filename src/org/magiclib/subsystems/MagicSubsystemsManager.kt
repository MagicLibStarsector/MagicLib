package org.magiclib.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.apache.log4j.Logger
import org.lazywizard.lazylib.ext.logging.i
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Vector2f
import org.magiclib.LunaWrapper
import org.magiclib.LunaWrapperSettingsListener
import org.magiclib.util.MagicSettings

object MagicSubsystemsManager {
    val log: Logger = Global.getLogger(MagicSubsystemsManager::class.java)
    val lunaLibEnabled = Global.getSettings().modManager.isModEnabled("lunalib")
    const val CUSTOM_DATA_KEY = "magicSubsystems"

    var widgetOffsetX = 0
    var widgetOffsetY = 0
    var infoHotkey: Int = -1
    var infoByDefault: Boolean = true
    var infoAlphaFadeout: Int = 40
    var hotkeyList: List<Int> = mutableListOf()

    /**
     * Call in your [onApplicationLoad] to initialize hotkeys and settings.
     */
    @JvmStatic
    fun initialize() {
        reloadSettings()

        if (lunaLibEnabled) {
            LunaWrapper.addSettingsListener(SubsystemSettingsListener())
        }
    }

    /**
     * Add a subsystem to a ship.
     * @param ship The ship to add the subsystem to.
     * @param subsystem The subsystem to add.
     */
    @JvmStatic
    fun addSubsystemToShip(ship: ShipAPI, subsystem: MagicSubsystem) {
        var shipSubsystemData = getSubsystemMapForShip(ship)

        if (shipSubsystemData == null) {
            shipSubsystemData = LinkedHashMap()
            ship.setCustomData(CUSTOM_DATA_KEY, shipSubsystemData)
        }

        shipSubsystemData.let { subsystems ->
            if (!subsystems.containsKey(subsystem.javaClass)) {
                subsystems[subsystem.javaClass] = subsystem

                if (subsystem.canAssignKey()) {
                    reassignKeys(ship)
                }

                subsystem.init()
            }
        }
    }

    /**
     * Remove a subsystem from a ship.
     */
    @JvmStatic
    fun removeSubsystemFromShip(ship: ShipAPI, subsystemClass: Class<out MagicSubsystem>) {
        getSubsystemMapForShip(ship)?.remove(subsystemClass)
    }

    /**
     * Gets a shallow copy of the list of subsystems for a ship.
     */
    @JvmStatic
    fun getSubsystemsForShipCopy(ship: ShipAPI): List<MagicSubsystem>? {
        val map = getSubsystemMapForShip(ship) ?: return null
        return ArrayList(map.values)
    }

    /**
     * Gets a map of subsystems for a ship, where the key is the subsystem's class.
     */
    @JvmStatic
    private fun getSubsystemMapForShip(ship: ShipAPI): MutableMap<Class<out MagicSubsystem>, MagicSubsystem>? {
        return ship.customData[CUSTOM_DATA_KEY] as? MutableMap<Class<out MagicSubsystem>, MagicSubsystem>?
            ?: return null
    }

    @JvmStatic
    fun getKeyForIndex(index: Int): Int {
        if (index < 0 || index >= hotkeyList.size) return -1
        return hotkeyList[index]
    }

    @JvmStatic
    fun reassignKeys(ship: ShipAPI) {
        val shipSubsystemData = getSubsystemMapForShip(ship) ?: return
        var skippedIndexes = 0

        sortSubsystems(shipSubsystemData.values)
            .filter { it.canAssignKey() }
            .filter { it.key == MagicSubsystem.BLANK_KEY } //do not mess with keys assigned by the subsystem
            .forEachIndexed { index, subsystem ->
                var actualIndex = index + skippedIndexes
                //skip over keys used by subsystems with static keys
                while (actualIndex < hotkeyList.size && shipSubsystemData.values.any { Keyboard.getKeyIndex(it.key) == hotkeyList[actualIndex] })
                    actualIndex = index + (++skippedIndexes)

                if (actualIndex >= hotkeyList.size) {
                    subsystem.key = MagicSubsystem.BLANK_KEY
                    subsystem.keyIndex = -1
                } else {
                    subsystem.keyIndex = actualIndex
                }
            }
    }

    fun sortSubsystems(subsystems: Collection<MagicSubsystem>): Collection<MagicSubsystem> {
        return subsystems.sortedWith { a, b ->
            if (a.order == b.order) {
                a.displayText.compareTo(b.displayText) //sort by alphabet
            } else {
                b.order.compareTo(a.order)  //sort by descending order
            }
        }
    }

    fun reloadSettings() {
        if (lunaLibEnabled) {
            infoByDefault = LunaWrapper.getBoolean("MagicLib", "magiclib_subsystems_showInfoDefault") ?: true
            infoHotkey = LunaWrapper.getInt("MagicLib", "magiclib_subsystems_InfoKeyBind") ?: 23
            hotkeyList = mutableListOf(
                LunaWrapper.getInt("MagicLib", "magiclib_subsystems_KeyBind1") ?: 0,
                LunaWrapper.getInt("MagicLib", "magiclib_subsystems_KeyBind2") ?: 0,
                LunaWrapper.getInt("MagicLib", "magiclib_subsystems_KeyBind3") ?: 0,
                LunaWrapper.getInt("MagicLib", "magiclib_subsystems_KeyBind4") ?: 0,
                LunaWrapper.getInt("MagicLib", "magiclib_subsystems_KeyBind5") ?: 0,
            )
                .filter { it > 0 }

            widgetOffsetX = LunaWrapper.getInt("MagicLib", "magiclib_subsystems_widgetOffsetX") ?: 0
            widgetOffsetY = LunaWrapper.getInt("MagicLib", "magiclib_subsystems_widgetOffsetY") ?: 0
            infoAlphaFadeout = LunaWrapper.getInt("MagicLib", "magiclib_subsystems_infoTextFadeout") ?: 0
        } else {
            infoHotkey = MagicSettings.getInteger("MagicLib", "subsystemInfoKey") ?: 23
            hotkeyList = MagicSettings.getList("MagicLib", "subsystemKeys")
                .map { Keyboard.getKeyIndex(it) }
                .filter { it != 0 }
        }

        log.i({ "Loaded hotkey list ${hotkeyList.joinToString { "," }}" })
    }

    @JvmStatic
    fun getWidgetOffsetVector(): Vector2f {
        return Vector2f(widgetOffsetX.toFloat(), widgetOffsetY.toFloat())
    }

    @JvmStatic
    fun getInfoTextMaxFadeout(): Int {
        return infoAlphaFadeout
    }
}

class SubsystemSettingsListener : LunaWrapperSettingsListener {
    override fun settingsChanged(modID: String) {
        MagicSubsystemsManager.reloadSettings()
    }
}

fun IntervalUtil.advanceAndCheckElapsed(amount: Float): Boolean {
    this.advance(amount)
    return this.intervalElapsed()
}

/**
 * Add a subsystem to a ship.
 * @param subsystem The subsystem to add.
 */
fun ShipAPI.addSubsystem(subsystem: MagicSubsystem) =
    MagicSubsystemsManager.addSubsystemToShip(this, subsystem)

/**
 * Remove a subsystem.
 */
fun ShipAPI.removeSubsystem(subsystemClass: Class<out MagicSubsystem>) =
    MagicSubsystemsManager.removeSubsystemFromShip(this, subsystemClass)

/**
 * Gets a shallow copy of the list of subsystems.
 */
val ShipAPI.subsystemsCopy: List<MagicSubsystem>?
    get() = MagicSubsystemsManager.getSubsystemsForShipCopy(this)