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

class ExplosionOcclusionRaycast(): DamageTakenModifier {
    companion object {
        const val EXPLOSION_RAYCAST_MAPS = "explosion_raycast"
        const val OCCLUSION_MODIFIER = "occlusion_modifier"
        const val DELETE_TIME = "delete_time"
        const val PASS_THROUGH_OCCLUSION = "pass_through_occlusion"
        const val NO_OCCLUSION = "no_occlusion" // Damage is always 1f, no raycasts are performed, raycasts cannot see this. This is simply excluded.
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

        val (blockingModules, passthroughModules) = allInRange.partition {
            !it.hasTag(PASS_THROUGH_OCCLUSION)
        }

        val rayEndpoints = MathUtils.getPointsAlongCircumference(projectile.location, radius, NUM_RAYCASTS, 0f)

        // For each ray, find the closest blocking/parent occluder (if any) and how far away it hit -
        // that's the distance a IGNORE_OCCLUSION module has to beat to count as exposed on that ray.
        val rayBlockDistSq = FloatArray(NUM_RAYCASTS) { Float.POSITIVE_INFINITY }
        val hitsMap = mutableMapOf<ShipAPI, Int>()
        var totalRayHits = 0

        if (blockingModules.isNotEmpty()) {
            for (i in rayEndpoints.indices) {
                val endpoint = rayEndpoints[i]
                var closestTarget: ShipAPI? = null
                var closestDistSq = Float.POSITIVE_INFINITY
                for (module in blockingModules) {  // for each ray loop past all blocking occlusions
                    val pointOnBounds = CollisionUtils.getCollisionPoint(projectile.location, endpoint, module)
                    if (pointOnBounds != null) {
                        val occlusionDistanceSq = Misc.getDistanceSq(projectile.location, pointOnBounds)
                        if (occlusionDistanceSq < closestDistSq) { // check the distance, if its shorter remember it
                            closestTarget = module
                            closestDistSq = occlusionDistanceSq
                        }
                    }
                }
                rayBlockDistSq[i] = closestDistSq
                if (closestTarget != null) {
                    totalRayHits++
                    hitsMap[closestTarget] = hitsMap.getOrDefault(closestTarget, 0) + 1
                }
            }
        }

        // resolve how much damage each blocking module / the parent hull itself takes
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

        // Non-blocking modules never occlude anything,
        // but they can still be shielded by a blocker that's closer to the explosion than they are on a given ray.
        for (module in passthroughModules) {
            var reachableRays = 0
            var unobstructedRays = 0
            for ((rayIndex, endpoint) in rayEndpoints.withIndex()) {
                val pointOnBounds = CollisionUtils.getCollisionPoint(projectile.location, endpoint, module) ?: continue
                reachableRays++
                if (Misc.getDistanceSq(projectile.location, pointOnBounds) < rayBlockDistSq[rayIndex]) unobstructedRays++
            }

            if (reachableRays == 0) {
                explosionMap[module.id] = 0f // no comparison to make here
                continue
            }

            val rayBudget = totalRayHits + reachableRays // Tally amount of reached rays across blocking and un-blocking modules
            explosionMap[module.id] = unobstructedRays / rayBudget.toFloat()
        }

        return explosionMap
    }
}