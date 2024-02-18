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
    var keyList: List<Int> = mutableListOf()

    @JvmStatic
    fun initialize() {
        reloadKeys()
        LunaSettings.addSettingsListener(LunaKeybindSettingsListener())
    }

    @JvmStatic
    fun addActivator(ship: ShipAPI, activator: CombatActivator) {
        var activatorData: MutableMap<Class<out CombatActivator>, CombatActivator>? = getActivatorMap(ship)
        if (activatorData == null) {
            activatorData = LinkedHashMap()
            ship.setCustomData("combatActivators", activatorData)
        }

        activatorData.let { activators ->
            if (!activators.containsKey(activator.javaClass)) {
                activators[activator.javaClass] = activator

                if (!activator.canAssignKey() || activators.size > keyList.size) {
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

    @JvmStatic
    fun removeActivator(ship: ShipAPI, activatorClass: Class<out CombatActivator>) {
        val activatorData: MutableMap<Class<out CombatActivator>, CombatActivator> = getActivatorMap(ship) ?: return
        activatorData.remove(activatorClass)
    }

    @JvmStatic
    fun advanceActivators(amount: Float) {
        val ships: List<ShipAPI> = Global.getCombatEngine().ships
        for (ship in ships) {
            getActivators(ship)?.forEach {
                if (!Global.getCombatEngine().isPaused) {
                    it.advanceInternal(amount * ship.mutableStats.timeMult.modifiedValue)
                }

                it.advanceEveryFrame()
            }
        }
    }

    @JvmStatic
    fun drawActivatorsUI(viewport: ViewportAPI) {
        if (Global.getCombatEngine().combatUI == null || Global.getCombatEngine().combatUI.isShowingCommandUI || Global.getCombatEngine().combatUI.isShowingDeploymentDialog || !Global.getCombatEngine().isUIShowingHUD) {
            return
        }

        Global.getCombatEngine().playerShip?.let { ship ->
            getActivators(ship)?.let {
                var lastVec = MagicUI.getHUDRightOffset(ship)
                for (activator in it) {
                    activator.drawHUDBar(viewport, lastVec)
                    lastVec = Vector2f.add(lastVec, Vector2f(0f, 22f), null)
                }
            }
        }
    }

    @JvmStatic
    fun drawActivatorsWorld(viewport: ViewportAPI) {
        val ships: List<ShipAPI> = Global.getCombatEngine().ships
        for (ship in ships) {
            getActivators(ship)?.forEach {
                it.renderWorld(viewport)
            }
        }
    }

    @JvmStatic
    fun getActivators(ship: ShipAPI): List<CombatActivator>? {
        val map = getActivatorMap(ship)
        if (map != null) {
            return ArrayList(map.values)
        }
        return null
    }

    @JvmStatic
    fun getActivatorMap(ship: ShipAPI): MutableMap<Class<out CombatActivator>, CombatActivator>? {
        return ship.customData["combatActivators"] as MutableMap<Class<out CombatActivator>, CombatActivator>?
            ?: return null
    }

    fun reloadKeys() {
        keyList = mutableListOf(
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind1")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind2")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind3")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind4")!!,
            LunaSettings.getInt("combatactivators", "combatActivators_KeyBind5")!!,
        ).filter { it != 0 }
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