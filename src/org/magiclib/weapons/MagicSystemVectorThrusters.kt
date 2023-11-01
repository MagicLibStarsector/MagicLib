package org.magiclib.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.EngineSlotAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipEngineControllerAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.loading.specs.EngineSlot
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * Vectors a ship's system-activated (e.g. Plasma Jets) engines when the ship system is active.
 *
 * Usage:
 *
 * ```java
 * class MyShipSystem extends BaseShipSystemScript {
 *   private final MagicSystemVectorThrusters vectorThrusters = new MagicSystemVectorThrusters();
 *
 *   @Override
 *   public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
 * 	   vectorThrusters.apply((ShipAPI) stats.getEntity(), state, effectLevel);
 * 	   // your code
 * 	 }
 * }
 * ```
 *
 * @author Nia Tahl, Dark Revenant
 * @since 1.3.0
 */
@Suppress("MemberVisibilityCanBePrivate")
open class MagicSystemVectorThrusters {
    protected val engState: MutableMap<Int, EngineStats> = HashMap()
    protected var hasRun = false

    protected open fun init(ship: ShipAPI) {
        val engList = ship.engineController.shipEngines
        for (i in engList.indices) {
            val eState = EngineStats(
                length = engList[i].engineSlot.length,
                width = engList[i].engineSlot.width,
                state = 0f
            )
            engState[i] = eState
        }
    }

    /**
     * Call in your ship system's `apply(`) method.
     *
     * For `ship`, use `(ShipAPI) stats.getEntity()`. You may want to do a `stats.getEntity() instanceOf ShipAPI` check first.
     */
    open fun apply(ship: ShipAPI, state: ShipSystemStatsScript.State, effectLevel: Float) {
        if (!hasRun) {
            init(ship)
            hasRun = true
        }

        val combatEngine = Global.getCombatEngine() ?: return
        val amount = combatEngine.elapsedInLastFrame
        var objectiveAmount = amount * combatEngine.timeMult.getModifiedValue()

        if (combatEngine.isPaused) {
            objectiveAmount = 0f
        }

        ship.engineController.forceShowAccelerating()

        if (state != ShipSystemStatsScript.State.COOLDOWN && state != ShipSystemStatsScript.State.IDLE) {
            val direction = Vector2f()
            var visualDir = 0f
            var maneuvering = true
            var cwTurn = false
            var ccwTurn = false

            if (ship.engineController.isAccelerating) {
                direction.y += 1f
            } else if (ship.engineController.isAcceleratingBackwards) {
                direction.y -= 1f
            }

            if (ship.engineController.isStrafingLeft) {
                direction.x -= 1f
            } else if (ship.engineController.isStrafingRight) {
                direction.x += 1f
            }

            if (direction.length() > 0f) {
                visualDir = MathUtils.clampAngle(VectorUtils.getFacing(direction) - 90f)
            } else if (ship.engineController.isDecelerating && ship.velocity.length() > 0f) {
                visualDir = MathUtils.clampAngle(VectorUtils.getFacing(ship.velocity) + 180f - ship.facing)
            } else {
                maneuvering = false
            }

            if (ship.engineController.isTurningRight) {
                cwTurn = true
            }

            if (ship.engineController.isTurningLeft) {
                ccwTurn = true
            }

            val engList = ship.engineController.shipEngines
            val engineScaleMap: MutableMap<Int, Float> = HashMap()

            for (i in engList.indices) {
                val eng = engList[i]

                if (eng.isSystemActivated) {
                    engineScaleMap[i] = getSystemEngineScale(ship, eng, visualDir, maneuvering, cwTurn, ccwTurn, null)
                }
            }

            for (i in engList.indices) {
                val eng = engList[i]

                if (eng.isSystemActivated) {
                    var targetLevel =
                        getSystemEngineScale(ship, eng, visualDir, maneuvering, cwTurn, ccwTurn, engineScaleMap)

                    if (state == ShipSystemStatsScript.State.OUT) {
                        targetLevel *= effectLevel
                    }
                    var currLevel = engState[i]?.state

                    if (currLevel == null) {
                        currLevel = 0f
                    }

                    currLevel = if (currLevel > targetLevel) {
                        max(targetLevel, (currLevel - objectiveAmount / EXTEND_TIME[ship.hullSize]!!))
                    } else {
                        min(targetLevel, (currLevel + objectiveAmount / EXTEND_TIME[ship.hullSize]!!))
                    }

                    if (ship.engineController.isFlamedOut) {
                        currLevel = 0f
                    }

                    engState[i]?.state = currLevel
                    setEngineValues(ship, eng.engineSlot, currLevel, i)
                }
            }
        } else {
            val engList = ship.engineController.shipEngines

            for (i in engList.indices) {
                val eng = engList[i]

                if (eng.isSystemActivated) {
                    engState[i]?.state = 0f
                    setEngineValues(ship, eng.engineSlot, 0f, i)
                }
            }
        }
    }

    data class EngineStats(
        var length: Float,
        var width: Float,
        var state: Float
    )

    protected open fun setEngineValues(ship: ShipAPI, slot: EngineSlotAPI, level: Float, index: Int) {
        ship.engineController.setFlameLevel(slot, level)
        val scalar = if ((ship.engineController.isStrafingLeft
                    || ship.engineController.isStrafingRight
                    || ship.engineController.isAcceleratingBackwards
                    || ship.engineController.isDecelerating)
            && !ship.engineController.isAccelerating
        ) 0.5f else 1f
        (slot as EngineSlot).setGlowParams(engState[index]!!.width * scalar, engState[index]!!.length * level, 1f, 1f)
    }

    protected open fun getSystemEngineScale(
        ship: ShipAPI,
        engine: ShipEngineControllerAPI.ShipEngineAPI,
        direction: Float,
        maneuvering: Boolean,
        cwTurn: Boolean,
        ccwTurn: Boolean,
        engineScaleMap: Map<Int, Float>?
    ): Float {
        var target = 0f
        val engineRelLocation = Vector2f(engine.location)

        Vector2f.sub(
            engineRelLocation,
            ship.location,
            engineRelLocation
        ) // Example -- (20, 20) ship facing forwards, engine on upper right quadrant
        engineRelLocation.normalise(engineRelLocation) // (0.7071, 0.7071)
        VectorUtils.rotate(
            engineRelLocation,
            -ship.facing,
            engineRelLocation
        ) // (0.7071, -0.7071) - engine past centerline (x) on right side (y)

        val engineAngleVector =
            VectorUtils.rotate(Vector2f(1f, 0f), engine.engineSlot.angle) // 270 degrees into (0, -1)
        val torque = VectorUtils.getCrossProduct(
            engineRelLocation,
            engineAngleVector
        ) // 0.7071*-1 - -0.7071*0 = -0.7071 (70.71% strength CCW torque)

        if (
            MathUtils.getShortestRotation(engine.engineSlot.angle, direction)
                .toDouble().absoluteValue > 100f && maneuvering
        ) {
            target = 1f
        } else {
            if (torque <= -0.4f && ccwTurn) {
                target = 1f
            } else if (torque >= 0.4f && cwTurn) {
                target = 1f
            }
        }

        /* Engines that are firing directly against each other should shut off */
        if (engineScaleMap != null) {
            val engineList = ship.engineController.shipEngines

            for (i in engineList.indices) {
                val otherEngine = engineList[i]

                if (otherEngine.isSystemActivated && engineScaleMap[i]!! >= 0.5f) {
                    val otherEngineRelLocation = Vector2f(otherEngine.location)

                    Vector2f.sub(
                        otherEngineRelLocation,
                        ship.location,
                        otherEngineRelLocation
                    ) // Example -- (20, 20) ship facing forwards, engine on upper right quadrant
                    otherEngineRelLocation.normalise(otherEngineRelLocation) // (0.7071, 0.7071)
                    VectorUtils.rotate(
                        otherEngineRelLocation,
                        -ship.facing,
                        otherEngineRelLocation
                    ) // (0.7071, -0.7071) - engine past centerline (x) on right side (y)

                    val otherEngineAngleVector = VectorUtils.rotate(
                        Vector2f(1f, 0f),
                        otherEngine.engineSlot.angle
                    ) // 270 degrees into (0, -1)
                    val otherTorque = VectorUtils.getCrossProduct(
                        otherEngineRelLocation,
                        otherEngineAngleVector
                    ) // 0.7071*-1 - -0.7071*0 = -0.7071 (70.71% strength CCW torque)

                    if (MathUtils.getShortestRotation(engine.engineSlot.angle, otherEngine.engineSlot.angle)
                            .toDouble().absoluteValue > 155f
                        && (torque + otherTorque).toDouble().absoluteValue <= 0.2f
                    ) {
                        target = 0f
                        break
                    }
                }
            }
        }

        return target
    }

    protected companion object {
        @JvmStatic
        val EXTEND_TIME: MutableMap<ShipAPI.HullSize, Float> = EnumMap(ShipAPI.HullSize::class.java)

        init {
            EXTEND_TIME[ShipAPI.HullSize.FRIGATE] = 0.1f
            EXTEND_TIME[ShipAPI.HullSize.DESTROYER] = 0.125f
            EXTEND_TIME[ShipAPI.HullSize.CRUISER] = 0.15f
            EXTEND_TIME[ShipAPI.HullSize.CAPITAL_SHIP] = 0.175f
        }
    }
}