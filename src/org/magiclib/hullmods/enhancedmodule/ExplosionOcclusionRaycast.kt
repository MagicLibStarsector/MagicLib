package org.magiclib.hullmods.enhancedmodule

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.entities.DamagingExplosion
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.internal.MiscellaneousUtil.damageAfterArmor
import org.magiclib.util.internal.MiscellaneousUtil.isCloseTo
import kotlin.collections.iterator
import kotlin.math.max
import kotlin.math.min

/**
 * Scales explosion damage across a ship and its modules based on occlusion.
 *
 * Casts [NUM_RAYCASTS] rays from the blast center and converts each module's share of ray hits
 * into a damage multiplier, so modules shielded by others take less damage. Per-module behavior
 * is controlled by the [PASS_THROUGH_OCCLUSION], [DEDUCT_FIRST_HIT_RAYCAST], and [NO_OCCLUSION]
 * hull tags. Results are cached briefly per explosion.
 */
class ExplosionOcclusionRaycast(): DamageTakenModifier {
    companion object {
        const val EXPLOSION_RAYCAST_MAPS = "explosion_raycast"
        const val OCCLUSION_MODIFIER = "occlusion_modifier"
        const val DELETE_TIME = "delete_time"
        const val PASS_THROUGH_OCCLUSION = "pass_through_occlusion" // Rays hit this module but continue through it.
            // This is intended for non-armor modules that explosions should pass through. Damage is scaled by how much of the module the blast hits relative to other modules.
        const val DEDUCT_FIRST_HIT_RAYCAST = "deduct_first_hit_raycast" // Rays that hit this module first don't count toward total hits.
            // This is intended for use on modules that take no damage (e.g. station vast bulk modules), so damage is not 'wasted' on invincible modules.
        const val NO_OCCLUSION = "no_occlusion" // Ignore occlusion entirely; No raycasts, always takes full damage.
            // Base-game behavior, this module is effectively invisible to this class.
        const val NUM_RAYCASTS = 36;
    }

    override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI, damage: DamageAPI, point: Vector2f, shieldHit: Boolean): String? {
        if (shieldHit) return null // Shields are not accounted for in raycasts, so hits may incorrectly pass through to blocking modules behind them. Thus, ignore shield hits.

        if (param !is DamagingProjectileAPI) return null
        val ship = target as? ShipAPI ?: return null

        val parent = ship.parentStation ?: ship
        if(parent.customData[EXPLOSION_RAYCAST_MAPS] == null)
            parent.setCustomData(EXPLOSION_RAYCAST_MAPS, mutableMapOf<DamagingProjectileAPI, Map<String, Float>>())

        if (param is DamagingExplosion || param is MissileAPI){
            @Suppress("UNCHECKED_CAST")
            val explosionMaps = parent.customData[EXPLOSION_RAYCAST_MAPS] as MutableMap<DamagingProjectileAPI, Map<String, Float>>
            val explosionMap = explosionMaps.firstNotNullOfOrNull { (dp, em) ->
                if (dp === param) em
                else if (dp.damageAmount.isCloseTo(param.damageAmount, 1e-6f) && Misc.getDistanceSq(dp.location, param.location) < 25f) em
                else null
            } ?: generateExplosionRayhitMap(param, damage, parent)

            damage.modifier.modifyMult(OCCLUSION_MODIFIER, explosionMap.getOrDefault(target.id, 0f))
            return OCCLUSION_MODIFIER
        }
        return null
    }


    fun generateExplosionRayhitMap(projectile: DamagingProjectileAPI, damage: DamageAPI, parent: ShipAPI): Map<String, Float>{
        if (projectile !is DamagingExplosion && projectile !is MissileAPI) return mapOf() // should never happen

        @Suppress("UNCHECKED_CAST")
        val explosionMaps = parent.customData[EXPLOSION_RAYCAST_MAPS] as MutableMap<DamagingProjectileAPI, Map<String, Float>>
        if (projectile in explosionMaps) return explosionMaps[projectile]!! // should also never happen, just in case

        val currentTime = Global.getCombatEngine().getTotalElapsedTime(false)
        // remove all stale values
        explosionMaps.entries.retainAll { (_, em) -> em[DELETE_TIME]!! >= currentTime }

        // make new entry
        val explosionMap = mutableMapOf<String, Float>()
        explosionMaps[projectile] = explosionMap
        explosionMap[DELETE_TIME] = currentTime + 0.1f

        val radius = when (projectile) {
            is DamagingExplosion -> projectile.explosionSpecIfExplosion?.radius ?: 0f
            is MissileAPI -> projectile.spec.explosionRadius
            else -> 0f
        }

        val allInRange = (parent.childModulesCopy + listOf(parent)).filter {
            if(it.hasTag(NO_OCCLUSION)) {
                explosionMap[it.id] = 1f
                false
            } else {
                val maxDistance = radius + Misc.getTargetingRadius(projectile.location, it, false)
                Misc.getDistanceSq(it.location, projectile.location) < maxDistance * maxDistance
            }
        }

        // easy cases
        if (allInRange.isEmpty()) return explosionMap
        if (allInRange.size == 1) {
            explosionMap[allInRange.first().id] = 1f
            return explosionMap
        }

        // blockingModules stop a ray outright. All other modules still take a hit when a ray crosses them, but the ray isn't blocked and continues on to whatever's behind it.
        val blockingModules = allInRange.filterNot { it.hasTag(PASS_THROUGH_OCCLUSION) }.toSet()

        val rayEndpoints = MathUtils.getPointsAlongCircumference(projectile.location, radius, NUM_RAYCASTS, 0f)

        val hitsMap = mutableMapOf<ShipAPI, Int>()
        var totalRayHits = 0

        for (endpoint in rayEndpoints) {
            // every module this ray crosses, nearest first
            val collisions = allInRange
                .mapNotNull { module ->
                    val pointOnBounds = CollisionUtils.getCollisionPoint(projectile.location, endpoint, module)
                    pointOnBounds?.let { module to Misc.getDistanceSq(projectile.location, it) }
                }
                .sortedBy { it.second }

            if (collisions.isEmpty()) continue

            totalRayHits++

            for ((index, pair) in collisions.withIndex()) {
                val module = pair.first

                hitsMap[module] = hitsMap.getOrDefault(module, 0) + 1 // Hit!
                if(index == 0 && module.hasTag(DEDUCT_FIRST_HIT_RAYCAST)) totalRayHits--
                if (module in blockingModules) break // blocked here, ray goes no further
                // else: pass-through module took a hit, but the ray keeps traveling
            }
        }

        // Task: overkill damage should affect modules too, not just the parent.

        // resolve how much damage each hit module / the parent hull itself takes
        if (hitsMap.size == 1) {
            explosionMap[hitsMap.keys.first().id] = 1f
        } else if (hitsMap.isNotEmpty()) {
            // Note: this implementation does not account for overkill damage for any other module than the parent.
            var overkillDamage = 0f
            for ((occlusion, rayHits) in hitsMap) {
                if (occlusion === parent) continue // special case the parent

                val damageMult = min(1f, max(rayHits / totalRayHits.toFloat(), rayHits / (NUM_RAYCASTS/2).toFloat()))
                explosionMap[occlusion.id] = damageMult
                val armor = occlusion.getAverageArmorInSlice(Misc.getAngleInDegrees(occlusion.location, projectile.location), 30f)
                val (_, hullDamage) = damageAfterArmor(projectile.damageType, projectile.damageAmount * damageMult, projectile.damageAmount, armor, occlusion)
                overkillDamage += max(0f, hullDamage - occlusion.hitpoints)
            }

            // do the same mult calc for the parent, except also subtract overkill from the reduction
            val parentDamageMult = if (parent !in hitsMap) 0f
            else min(1f, max(hitsMap[parent]!! / totalRayHits.toFloat(), hitsMap[parent]!! / (NUM_RAYCASTS/2).toFloat()))
            explosionMap[parent.id] = min(((projectile.damageAmount * parentDamageMult) + overkillDamage) / projectile.damageAmount, 1f)
        }

        return explosionMap
    }
}