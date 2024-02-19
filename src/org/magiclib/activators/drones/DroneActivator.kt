package org.magiclib.activators.drones

import org.magiclib.activators.advanceAndCheckElapsed
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.ui.LazyFont
import org.lwjgl.util.vector.Vector2f
import org.magiclib.activators.CombatActivator
import org.magiclib.util.MagicUI

abstract class DroneActivator(ship: ShipAPI) : CombatActivator(ship) {
    var activeWings: MutableMap<ShipAPI, PIDController> = LinkedHashMap()
    var formation: DroneFormation = SpinningCircleFormation()
    var droneCreationInterval: IntervalUtil = IntervalUtil(0f, 0f)
    var droneCharges: Int = 0
    val droneDeployInterval: IntervalUtil = IntervalUtil(0f, 0f)

    var dronesToSpawn: Int = 0

    override fun init() {
        super.init()

        formation = getDroneFormation()
        droneDeployInterval.setInterval(getDroneDeployDelay(), getDroneDeployDelay())

        if (hasSeparateDroneCharges()) {
            droneCreationInterval.setInterval(getDroneCreationTime(), getDroneCreationTime())
        } else {
            droneCreationInterval.setInterval(chargeGenerationDuration, chargeGenerationDuration)
        }

        dronesToSpawn = getMaxDeployedDrones()
    }

    /**
     * Formation object that controls drones. The two implemented by this library are HoveringFormation and
     * SpinningCircleFormation. To create your own, you should extend the DroneFormation class.
     */
    open fun getDroneFormation(): DroneFormation {
        return SpinningCircleFormation()
    }

    /**
     * How many drones can be deployed at once.
     */
    open fun getMaxDeployedDrones(): Int {
        return droneCharges
    }

    /**
     * This is useful if you want a charged activator to use while allowing drones to have their own set of charges.
     */
    open fun hasSeparateDroneCharges(): Boolean {
        return false
    }

    /**
     * Maximum amount of stored drone charges. This is only used if the hasSeparateDroneCharges method returns true.
     */
    open fun getMaxDroneCharges(): Int {
        return 0
    }

    /**
     * Time to store a single drone charge. This is only used if the hasSeparateDroneCharges method returns true.
     */
    open fun getDroneCreationTime(): Float {
        return 0f
    }

    /**
     * Delay between subsequent drone deployments if multiple need to be deployed at once.
     */
    open fun getDroneDeployDelay(): Float {
        return 0.1f
    }

    open fun generatesDroneChargesWhileShipIsDead(): Boolean {
        return false
    }

    /**
     * All drones will explode when the ship dies.
     */
    open fun dronesExplodeWhenShipDies(): Boolean {
        return true
    }

    /**
     * Drones will deploy when ship is dead only if dronesExplodeWhenShipDies returns false.
     * Otherwise, they would explode in the next frame.
     */
    open fun dronesDeployWhenShipIsDead(): Boolean {
        return false
    }

    /**
     * Drones will still advance while the ship is dead no matter what.
     */
    override fun getAdvancesWhileDead(): Boolean {
        return super.getAdvancesWhileDead()
    }

    abstract fun getDroneVariant(): String

    open fun spawnDrone(): ShipAPI {
        Global.getCombatEngine().getFleetManager(ship.owner).isSuppressDeploymentMessages = true
        val fleetSide = FleetSide.values()[ship.owner]
        val fighter = CombatUtils.spawnShipOrWingDirectly(
            getDroneVariant(),
            FleetMemberType.FIGHTER_WING,
            fleetSide,
            0.7f,
            ship.location,
            ship.facing
        )
        activeWings[fighter] = getPIDController()
        fighter.shipAI = null
        fighter.giveCommand(ShipCommand.SELECT_GROUP, null, 99)
        Global.getCombatEngine().getFleetManager(ship.owner).isSuppressDeploymentMessages = false

        return fighter
    }

    open fun getPIDController(): PIDController {
        return PIDController(2f, 2f, 6f, 0.5f)
    }

    private val droneWeaponGroupCheckInterval = IntervalUtil(0.5f, 1f)
    override fun advanceInternal(amount: Float) {
        super.advanceInternal(amount)

        val alive = ship.isAlive && !ship.isHulk && ship.owner != 100
        if (!alive) {
            if (dronesExplodeWhenShipDies()) {
                activeWings.forEach { (drone, _) ->
                    var damageFrom: Vector2f? = Vector2f(drone.location)
                    damageFrom = Misc.getPointWithinRadius(damageFrom, 20f)
                    Global.getCombatEngine()
                        .applyDamage(drone, damageFrom, 1000000f, DamageType.ENERGY, 0f, true, false, drone, false)
                }
            }
        }

        if (hasSeparateDroneCharges() && (alive || generatesDroneChargesWhileShipIsDead())) {
            if (droneCharges < getMaxDroneCharges() || (getMaxDroneCharges() == 0 && activeWings.size < getMaxDeployedDrones())) {
                if (droneCreationInterval.intervalElapsed()) {
                    if (getMaxDroneCharges() >= 0) {
                        droneCharges++
                        droneCreationInterval.advance(0f) //reset
                    }
                    //this interval will be left elapsed if getMaxDroneCharges returns 0.
                    //this means that killing a drone will effectively create one after a specific amount of time, like normal fighters.
                } else {
                    droneCreationInterval.advance(amount)
                }
            }
        } else if (!hasSeparateDroneCharges()) {
            if (maxCharges == 0 && activeWings.size < getMaxDeployedDrones() && !droneCreationInterval.intervalElapsed()) {
                droneCreationInterval.advance(amount)
                //this interval will be left elapsed if getMaxDroneCharges returns 0.
                //this means that killing a drone will effectively create one after a specific amount of time, like normal fighters.
            }
        }

        if (!droneDeployInterval.intervalElapsed()) {
            droneDeployInterval.advance(amount)
        }

        activeWings = activeWings.filter { it.key.isAlive && !it.key.isHulk }.toMutableMap()

        if (alive || (!dronesExplodeWhenShipDies() && dronesDeployWhenShipIsDead())) {
            if (activeWings.size < getMaxDeployedDrones()) {
                if (shouldSpawnDrone()) {
                    var shouldSpawnDrone = dronesToSpawn > 0
                    if (!shouldSpawnDrone) {
                        shouldSpawnDrone =
                            if ((hasSeparateDroneCharges() && getMaxDroneCharges() == 0) || maxCharges == 0) {
                                droneCreationInterval.intervalElapsed()
                            } else if (hasSeparateDroneCharges()) {
                                droneCharges > 0
                            } else {
                                charges > 0
                            }
                    }

                    while (shouldSpawnDrone && activeWings.size < getMaxDeployedDrones() && (droneDeployInterval.intervalDuration == 0f || droneDeployInterval.intervalElapsed())) {
                        spawnDrone()
                        droneDeployInterval.advance(0f)

                        if (dronesToSpawn > 0) {
                            dronesToSpawn--
                        } else if ((hasSeparateDroneCharges() && getMaxDroneCharges() == 0) || maxCharges == 0) {
                            droneCreationInterval.advance(0f) //reset interval
                        } else if (hasSeparateDroneCharges()) {
                            droneCharges--
                        } else {
                            charges--
                        }
                    }
                }
            }
        }

        if (activeWings.isEmpty()) return

        if (droneWeaponGroupCheckInterval.advanceAndCheckElapsed(amount)) {
            activeWings
                .forEach { (ship, _) ->
                    ship.weaponGroupsCopy.forEachIndexed { index, group ->
                        if (!group.isAutofiring) {
                            ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, null, index);
                        }
                    }
                }
        }

        formation.advance(ship, activeWings, amount)
    }

    /**
     * Whether a drone should be spawned on the next deployment interval, if the amount of drones is below the limit and we have charges to spawn the drone.
     * To force spawn a drone, set dronesToSpawn.
     * @return should spawn drone
     */
    open fun shouldSpawnDrone(): Boolean {
        return true
    }

    /**
     * Override to not display charge filling in favor of separate drone bar if system doesn't use charges.
     */
    override fun getBarFill(): Float {
        if (activeWings.size < getMaxDeployedDrones()) {
            if (hasSeparateDroneCharges()) {
                if (droneCharges == 0) {
                    return (droneCreationInterval.elapsed / droneCreationInterval.intervalDuration).coerceIn(0f..1f)
                }
            } else if (maxCharges > 0 && charges == 0 && activeWings.size < getMaxDeployedDrones()) {
                return (chargeInterval.elapsed / chargeInterval.intervalDuration).coerceIn(0f..1f)
            } else {
                return (droneCreationInterval.elapsed / droneCreationInterval.intervalDuration).coerceIn(0f..1f)
            }
        }


        if (!usesChargesOnActivate() && !hasSeparateDroneCharges()) {
            var fill = when (state) {
                State.READY -> 0f
                State.IN -> stateInterval.elapsed / (inDuration + activeDuration)
                State.ACTIVE -> if (isToggle && activeElapsed) {
                    1f
                } else {
                    (inDuration + stateInterval.elapsed) / (inDuration + activeDuration)
                }

                State.OUT -> 1f - stateInterval.elapsed / (outDuration + cooldownDuration)
                State.COOLDOWN -> 1f - (outDuration + stateInterval.elapsed) / (outDuration + cooldownDuration)
            }

            return fill.coerceIn(0f..1f)
        }

        return super.getBarFill()
    }

    override fun drawHUDBar(viewport: ViewportAPI, barLoc: Vector2f) {
        var barLoc = barLoc
        MagicUI.setTextAligned(LazyFont.TextAlignment.LEFT)
        val nameText: String = if (canAssignKey() || key != BLANK_KEY) {
            val keyText = keyText
            String.format("%s (%s)", displayText, keyText)
        } else {
            String.format("%s", displayText)
        }
        val nameWidth = MagicUI.getTextWidth(nameText)
        MagicUI.addText(ship, nameText, hudColor, Vector2f.add(barLoc, Vector2f(0f, 10f), null), false)
        barLoc = Vector2f.add(barLoc, Vector2f(nameWidth + nameTextPadding + 2f, 0f), null)
        MagicUI.setTextAligned(LazyFont.TextAlignment.RIGHT)
        MagicUI.addText(ship, ammoText, hudColor, Vector2f.add(barLoc, Vector2f(0f, 10f), null), false)
        MagicUI.setTextAligned(LazyFont.TextAlignment.LEFT)

        if (stateText !=  null && stateText.isNotEmpty()) {
            MagicUI.addText(
                ship, stateText,
                hudColor, Vector2f.add(barLoc, Vector2f((12 + 4 + 59).toFloat(), 10f), null), false
            )
        }

        MagicUI.addBar(
            ship,
            barFill, hudColor, hudColor, 0f, Vector2f.add(barLoc, Vector2f(12f, 0f), null)
        )

        if ((hasSeparateDroneCharges() && getMaxDroneCharges() > 0) || (!usesChargesOnActivate() && maxCharges > 0)) {
            val droneBarPadding = 1 * MagicUI.UI_SCALING
            val droneBarWidth =
                ((59 * MagicUI.UI_SCALING - droneBarPadding * (getMaxDroneCharges() - 1)) / getMaxDroneCharges()).coerceAtLeast(
                    1f
                )

            val max = (droneCharges + 1).coerceAtMost(getMaxDroneCharges())
            for (i in 0 until max) {
                val droneBarPos =
                    Vector2f.add(barLoc, Vector2f(12f + droneBarWidth * i + droneBarPadding * i, -2 * MagicUI.UI_SCALING), null)
                val droneBarFill =
                    if (droneCharges < getMaxDroneCharges() && i == max - 1) droneCreationInterval.elapsed / droneCreationInterval.intervalDuration else 1f
                MagicUI.addBar(
                    ship,
                    droneBarFill, hudColor, hudColor, 0f, droneBarPos, 2 * MagicUI.UI_SCALING, droneBarWidth, false
                )
            }
        }
    }
}