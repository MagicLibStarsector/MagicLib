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
        //if(ship.isPiece) return null

        val parent = ship.parentStation ?: ship
        if(parent.customData[EXPLOSION_RAYCAST_MAPS] == null)
            parent.setCustomData(EXPLOSION_RAYCAST_MAPS, mutableMapOf<DamagingProjectileAPI, Map<String, Float>>())

        if (param is DamagingExplosion || param is MissileAPI) {
            @Suppress("UNCHECKED_CAST")
            val explosionMaps = parent.customData[EXPLOSION_RAYCAST_MAPS] as MutableMap<DamagingProjectileAPI, Map<String, Float>>

            val currentTime = Global.getCombatEngine().getTotalElapsedTime(false)
            explosionMaps.entries.retainAll { (_, em) -> em[DELETE_TIME]!! >= currentTime } // remove all stale values

            val explosionMap = explosionMaps.firstNotNullOfOrNull { (dp, em) ->
                // Sometimes a DamagingExplosion can apply itself twice on what could be considered the same target.
                // This is presumed to occur as a module is destroyed and is made into pieces. Each ship piece is considered a 'different ship' to the explosion, so they are exploded again.
                // Did you know, DamagingExplosion moves! It seems it inherits its creator's velocity, then moves along it during its lifetime. So here we use .spawnLocation instead of .location to make it easier to match it with the MissieAPI that made it.
                val paramLoc = if(param is DamagingExplosion) param.spawnLocation else param.location

                if (dp === param)
                    em
                else if (//dp.damageAmount.isCloseTo(param.damageAmount, 1e-6f) && // Commented out as some missiles have different damage amounts at different radius's within the same DamagingExplosion due to damage falloff from coreRadius to radius.
                    (dp.location == paramLoc || Misc.getDistanceSq(dp.location,paramLoc) < 25f))
                    em
                else
                    null
            } ?: generateExplosionRayhitMap(param, damage, parent, currentTime)

            damage.modifier.modifyMult(OCCLUSION_MODIFIER, explosionMap.getOrDefault(target.id, 0f))
            return OCCLUSION_MODIFIER
        }
        return null
    }

    private fun generateExplosionRayhitMap(projectile: DamagingProjectileAPI, damage: DamageAPI, parent: ShipAPI, currentTime: Float): Map<String, Float> {
        @Suppress("UNCHECKED_CAST")
        val explosionMaps = parent.customData[EXPLOSION_RAYCAST_MAPS] as MutableMap<DamagingProjectileAPI, Map<String, Float>>
        if (projectile in explosionMaps) return explosionMaps[projectile]!! // should also never happen, just in case

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
        val origin = projectile.location
        val moduleCount = allInRange.size

        // Cast every ray once. Collisions don't change with destruction, so cache each ray's hits sorted by distance.
        val distSq = FloatArray(moduleCount)
        val scratch = IntArray(moduleCount)
        val rayHits = ArrayList<IntArray>(NUM_RAYCASTS)
        var totalRayHits = 0

        for (endpoint in rayEndpoints) {
            var count = 0
            for (i in 0 until moduleCount) {
                val p = CollisionUtils.getCollisionPoint(origin, endpoint, allInRange[i]) ?: continue
                distSq[i] = Misc.getDistanceSq(origin, p)
                scratch[count++] = i
            }
            if (count == 0) continue

            // Stable insertion sort by distance
            for (a in 1 until count) {
                val v = scratch[a]
                var b = a - 1
                while (b >= 0 && distSq[scratch[b]] > distSq[v]) { scratch[b + 1] = scratch[b]; b-- }
                scratch[b + 1] = v
            }

            if (!allInRange[scratch[0]].hasTag(DEDUCT_FIRST_HIT_RAYCAST)) totalRayHits++
            rayHits += scratch.copyOf(count)
        }

        if (rayHits.isEmpty()) return explosionMap

        val isBlocker = BooleanArray(moduleCount) { allInRange[it] in blockingModules }
        // Parent dying ends the fight, and DEDUCT_FIRST_HIT_RAYCAST modules are typically invincible, so neither can be "destroyed"
        val canDie = BooleanArray(moduleCount) { allInRange[it] !== parent && !allInRange[it].hasTag(DEDUCT_FIRST_HIT_RAYCAST) }

        val pass = FloatArray(moduleCount) { if (isBlocker[it]) 0f else 1f }
        val weights = FloatArray(moduleCount)
        val mults = FloatArray(moduleCount)
        val total = max(1, totalRayHits).toFloat()

        // pass[i] is how much damage continues past module i once it's destroyed.
        // Blockers start opaque (pass=0); destroying one exposes what's behind it, so we recompute each round until nothing new dies.
        // The loop bound (moduleCount) is just a safety cap.
        for (iteration in 0..moduleCount) {
            weights.fill(0f)
            for (hits in rayHits) {
                var w = 1f
                for (i in hits) {
                    weights[i] += w
                    w *= pass[i]
                    if (w <= 0f) break
                }
            }

            var changed = false
            for (i in 0 until moduleCount) {
                val mult = min(1f, max(weights[i] / total, weights[i] / (NUM_RAYCASTS / 2f)))
                mults[i] = mult // never reduced: a module that would die takes its full original damage

                if (canDie[i] && weights[i] > 0f) {
                    val module = allInRange[i]
                    val damageTaken = hullDamageTaken(projectile, module, mult)
                    if (damageTaken > module.hitpoints) {
                        val overkillFraction = (damageTaken - module.hitpoints) / damageTaken
                        val newPass = if (isBlocker[i]) overkillFraction else 1f
                        if (newPass > pass[i] + 0.001f) { pass[i] = newPass; changed = true }
                    }
                }
            }
            if (!changed) break
        }

        val hitCount = weights.count { it > 0f }
        for (i in 0 until moduleCount) {
            if (weights[i] > 0f) explosionMap[allInRange[i].id] = if (hitCount == 1) 1f else mults[i]
        }

        return explosionMap
    }

    private fun hullDamageTaken(projectile: DamagingProjectileAPI, module: ShipAPI, mult: Float): Float {
        val armor = module.getAverageArmorInSlice(Misc.getAngleInDegrees(module.location, projectile.location), 30f)
        val (_, hullDamage) = damageAfterArmor(projectile.damageType, projectile.damageAmount * mult, projectile.damageAmount, armor, module)
        return hullDamage
    }
}