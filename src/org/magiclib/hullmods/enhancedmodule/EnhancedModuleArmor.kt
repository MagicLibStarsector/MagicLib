package org.magiclib.hullmods.enhancedmodule

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import org.lwjgl.util.vector.Vector2f

/**
 * A hull-mod that makes armor modules act like armor, not separate targets, and fixes
 * explosions bypassing modular armor via occlusion raycasting.
 *
 * If this hull-mod is applied to a hull with modules, it will be applied to all modules present on that hull.
 *
 * If this hull-mod is applied to a module, it will only function on that specific module.
 */
class EnhancedModuleArmor: BaseHullMod() {
    companion object {
        const val MODULE_DEAD = "module_dead"
        const val MODULE_HULKED = "module_hulked"
        const val MODULE_LISTENERS_ADDED = "module_listeners_added"

        const val HULL_MOD_ID = "ML_enhancedModuleArmor"
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if((ship.childModulesCopy.isEmpty() && ship.parentStation == null) || ship.hasTag(MODULE_LISTENERS_ADDED)) return

        ship.addTag(MODULE_LISTENERS_ADDED)

        ship.parentStation?.let { parent -> // Apply to parent if module
            if (!ship.hasListenerOfClass(ArmorModuleChild::class.java)) ship.addListener(ArmorModuleChild(ship))

            if (!parent.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) {
                parent.addListener(ExplosionOcclusionRaycast())

                parent.childModulesCopy.forEach {
                    if (!it.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) it.addListener(ExplosionOcclusionRaycast())
                    if (!it.hullSpec.isBuiltInMod(HULL_MOD_ID)) it.addTag(ExplosionOcclusionRaycast.NO_BLOCK_OCCLUSION)
                }
            }
        }
        if(ship.childModulesCopy.isNotEmpty()) {
            if (!ship.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) ship.addListener(ExplosionOcclusionRaycast())

            ship.childModulesCopy.forEach { module -> // Apply to children if parent
                if (!module.hasListenerOfClass(ArmorModuleChild::class.java)) module.addListener(ArmorModuleChild(module))
                if (!module.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) module.addListener(ExplosionOcclusionRaycast())
            }
        }
    }

    class ArmorModuleChild(val module: ShipAPI): DamageListener, HullDamageAboutToBeTakenListener, AdvanceableListener {
        override fun advance(amount: Float) {
            val engine = Global.getCombatEngine()
            // return if we are sure the module is no longer in play
            if (Global.getCurrentState() != GameState.COMBAT || engine == null || !Global.getCombatEngine().isEntityInPlay(module) ||
                module.parentStation?.isAlive != true || module.hitpoints <= 0 || module.hasTag(MODULE_DEAD)) return

            /*
            Enemy AI prioritizes targeting modules over the base hull. While logical for ship/station sections, this creates issues with armor modules.
            To prevent this, I set the module to a 'hulk' state, causing the AI to ignore it.
            However, this triggers a distracting visual whiteout ship explosion the first time it is done.
            My solution is to teleport the module offscreen (currently, a map corner) before marking it as a hulk.
            This must be done after the ship is loaded into the map and within its borders to prevent the game from despawning it.
            */

            val pad = 50f
            val moduleInMap = (module.location.x in (pad - engine.mapWidth / 2)..(engine.mapWidth / 2 - pad)) &&
                    (module.location.y in (pad - engine.mapHeight / 2)..(engine.mapHeight / 2 - pad))

            if (!module.hasTag(MODULE_HULKED) && moduleInMap) {
                // only teleport to inside the map border
                val borderEdgeX = if (module.location.getX() > 0) engine.mapWidth / 2 else -engine.mapWidth / 2
                val borderEdgeY = if (module.location.getY() > 0) engine.mapHeight / 2 else -engine.mapHeight / 2

                module.location.set(borderEdgeX, borderEdgeY)
                module.isHulk = true

                /*
                I set the modules to be station drones, this makes the enemy AI not see the ship as a group of ships. (ie: a capital with 4 frigate escorts)
                Without this enemies AI would not try to fight module ships as they think it is a many vs 1.
                This can also be avoided by setting the module hullsize to be a fighter, but that has other rendering issues. (fighters always render over everything else)
                */
                module.isDrone = true
                module.addTag(MODULE_HULKED)

                // set module captain to parent captain, no clue if this is already done or not, but best be safe.
                module.captain = module.parentStation.captain
            }

            // re-set the module to be a hulk if it's not, this happens after hulk is unset for vanilla damage fx's
            if (!module.isHulk && module.hasTag(MODULE_HULKED)) {
                module.isHulk = true
            }

            // sync hardflux level with parent hull for polarized armor purposes
            //val moduleFlux = module.parentStation.fluxLevel * module.maxFlux
            //module.fluxTracker.currFlux = moduleFlux
            //module.fluxTracker.hardFlux = moduleFlux
        }

        // unset hulk for right before any damage gets dealt to the module, this allows for normal processing of hit explosions
        override fun reportDamageApplied(source: Any?, target: CombatEntityAPI, result: ApplyDamageResultAPI) {
            val module = target as ShipAPI
            if (module.isHulk && module.hitpoints > 0 && !module.hasTag(MODULE_DEAD)) module.isHulk = false
        }

        // for some reason the above listener doesn't catch when the module is actually going to be dead.
        override fun notifyAboutToTakeHullDamage(param: Any?, module: ShipAPI, point: Vector2f, damageAmount: Float): Boolean {
            if (module.hitpoints <= damageAmount && !module.hasTag(MODULE_DEAD)) {
                module.isHulk = false
                module.isDrone = false
                module.addTag(MODULE_DEAD)
            }
            return false
        }
    }
}

