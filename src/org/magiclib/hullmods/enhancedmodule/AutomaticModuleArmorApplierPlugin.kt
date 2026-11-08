package org.magiclib.hullmods.enhancedmodule

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.input.InputEventAPI
import org.magiclib.util.taskscheduler.CombatTaskScheduler
import java.util.*

internal class AutomaticModuleArmorApplierPlugin: BaseEveryFrameCombatPlugin() {
    private val seen: MutableSet<ShipAPI> = Collections.newSetFromMap(IdentityHashMap<ShipAPI, Boolean>())

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        val engine = Global.getCombatEngine() ?: return

        for (ship in engine.ships) {
            if (ship.isStationModule || ship.isHulk || ship.isFighter || ship.isDrone || ship.isShuttlePod) continue
            if (!engine.isEntityInPlay(ship)) continue
            if (!seen.add(ship)) continue // Already handled

            // Give each ship a single tick of delay after being spawned in so they can load their stuff first.
            //CombatTaskScheduler.performLater { // In testing without this, it didn't appear to be necessary, but it shouldn't cause much issue to wait a tick just in case.
                onShipSpawnedWithTickDelay(ship)
            //}
        }
    }

    private fun onShipSpawnedWithTickDelay(ship: ShipAPI) {
        if (ship.childModulesCopy.isEmpty()) return

        // Is a similar effect already applied to the ship?
        if (ship.getListeners(Any::class.java).any {
                val className = it.javaClass.simpleName.lowercase()
                className.contains("occlusion") || className.contains("raycast")
        })
            return

        // Is a similar effect already applied to the modules?
        for (module in ship.childModulesCopy) {
            if (module.isHulk)
                return
            if (module.getListeners(Any::class.java).any {
                val className = it.javaClass.simpleName.lowercase()
                    className.contains("occlusion") || className.contains("raycast") || className.contains("armor")
            })
                return
        }

        for (module in ship.childModulesCopy) {
            val hull = module.variant.hullSpec
            if (!hull.hints.contains(ShipHullSpecAPI.ShipTypeHints.MODULE))
                continue
            if (hull.fluxCapacity != 0f)
                continue
            if (!hull.shipSystemId.isNullOrEmpty())
                continue
            if (hull.allWeaponSlotsCopy.isNotEmpty())
                continue
            if (hull.fighterBays != 0)
                continue
            if (!hull.hasTag(Tags.MODULE_HULL_BAR_ONLY))
                continue
            if (hull.builtInMods.size != 2 || !hull.builtInMods.contains("always_detaches") || !hull.builtInMods.contains("reduced_explosion"))
                continue

            if (!module.variant.hasHullMod(EnhancedModuleArmor.HULL_MOD_ID))
                module.variant.addMod(EnhancedModuleArmor.HULL_MOD_ID)
        }
    }
}