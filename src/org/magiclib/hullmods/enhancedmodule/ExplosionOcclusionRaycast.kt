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

class ExplosionOcclusionRaycast: DamageTakenModifier {
    companion object {
        const val EXPLOSION_RAYCAST_MAPS = "explosion_raycast"
        const val OCCLUSION_MODIFIER = "occlusion_modifier"
        const val DELETE_TIME = "delete_time"
        const val NUM_RAYCASTS = 36;
    }

    override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI, damage: DamageAPI, point: Vector2f, shieldHit: Boolean): String? {
        if (param !is DamagingProjectileAPI) return null
        val ship = target as? ShipAPI ?: return null
        val parent = if (ship.parentStation == null) ship else ship.parentStation
        if(parent.customData[EXPLOSION_RAYCAST_MAPS] == null)
            parent.setCustomData(EXPLOSION_RAYCAST_MAPS, mutableMapOf<DamagingProjectileAPI, Map<String, Float>>())


        if (param is DamagingExplosion || param is MissileAPI){
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

        val explosionMaps = parent.customData[EXPLOSION_RAYCAST_MAPS] as MutableMap<DamagingProjectileAPI, Map<String, Float>>
        if (projectile in explosionMaps) return explosionMaps[projectile]!! // should also never happen, just in case

        val currentTime = Global.getCombatEngine().getTotalElapsedTime(false)
        // remove all stale values
        explosionMaps.entries.retainAll { (_, em) -> em[DELETE_TIME]!! < currentTime }

        // make new entry
        val explosionMap = mutableMapOf<String, Float>()
        explosionMaps[projectile] = explosionMap
        explosionMap[DELETE_TIME] = currentTime + 0.1f

        val radius = projectile.explosionSpecIfExplosion?.radius ?: (projectile as MissileAPI).spec.explosionRadius

        val potentialOcclusions = (parent.childModulesCopy + listOf(parent)).toMutableList()
        potentialOcclusions.retainAll {
            val maxDistance = radius + Misc.getTargetingRadius(projectile.location, it, false)
            Misc.getDistanceSq(it.location, projectile.location) < maxDistance*maxDistance
        }

        // easy cases
        if (potentialOcclusions.isEmpty()) return explosionMap
        if (potentialOcclusions.size == 1) {
            explosionMap[potentialOcclusions.first().id] = 1f
            return explosionMap
        }

        val hitsMap = mutableMapOf<ShipAPI, Int>()

        // if we have 2 things in range, then we need to do the raycast
        val rayEndpoints = MathUtils.getPointsAlongCircumference(projectile.location, radius, NUM_RAYCASTS, 0f);

        var totalRayHits = 0
        for (endpoint in rayEndpoints){
            var closestTarget: ShipAPI? = null
            var targetDistanceSq = Float.POSITIVE_INFINITY
            for (potentialOcclusion in potentialOcclusions) { // for each ray loop past all occlusions
                val pointOnBounds = CollisionUtils.getCollisionPoint(projectile.location, endpoint, potentialOcclusion)
                if (pointOnBounds != null) { // if one is hit
                    val occlusionDistance: Float = Misc.getDistanceSq(projectile.location, pointOnBounds)
                    if (occlusionDistance < targetDistanceSq) { // check the distance, if its shorter remember it
                        closestTarget = potentialOcclusion
                        targetDistanceSq = occlusionDistance
                    }
                }
            }
            if (closestTarget != null) { // only not null if something is hit, in that case inc TotalRayHits
                totalRayHits++
                hitsMap[closestTarget] = hitsMap.getOrDefault(closestTarget, 0) + 1
            }
        }
        if (hitsMap.isEmpty()) return explosionMap // should also be impossible?
        if (hitsMap.size == 1) { // simple case
            explosionMap[hitsMap.keys.first().id] = 1f
            return explosionMap
        }

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
        val damageMult = if(parent !in hitsMap) 0f
        else min(1f, max(hitsMap[parent]!! / totalRayHits.toFloat(), hitsMap[parent]!! / (NUM_RAYCASTS/2).toFloat()))
        explosionMap[parent.id] = min(((projectile.damageAmount * damageMult) + overkillDamage) / projectile.damageAmount, 1f)

        return explosionMap
    }
}