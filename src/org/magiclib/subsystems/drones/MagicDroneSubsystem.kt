package org.magiclib.subsystems.drones

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
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.CombatUI
import org.magiclib.subsystems.CombatUI.SpriteDimWrapper
import org.magiclib.subsystems.MagicSubsystem
import org.magiclib.subsystems.advanceAndCheckElapsed
import org.magiclib.util.MagicTxt
import org.magiclib.util.MagicUI
import org.magiclib.subsystems.drones.PIDController
import java.awt.Color

abstract class MagicDroneSubsystem(ship: ShipAPI) : MagicSubsystem(ship) {
    var activeWings: MutableMap<ShipAPI, PIDController> = LinkedHashMap()
    var formation: DroneFormation = SpinningCircleFormation()
    var droneCreationInterval: IntervalUtil = IntervalUtil(0f, 0f)
    var droneCharges: Int = 0
    val droneDeployInterval: IntervalUtil = IntervalUtil(0f, 0f)
    private var dronesToSpawn: Int = 0

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
     * Count of drones waiting to be force-spawned.
     */
    fun getDronesToSpawn(): Int {
        return dronesToSpawn
    }

    /**
     * Set count of drones waiting to be spawned with no flux or charge cost.
     * @param drones number of drones
     */
    fun setDronesToSpawn(drones: Int) {
        dronesToSpawn = drones
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
     * This is useful if you want a charged subsystem to use while allowing drones to have their own set of charges that recharge in parallel.
     */
    open fun hasSeparateDroneCharges(): Boolean {
        return false
    }

    /**
     * Maximum amount of stored drone charges. This is only used if the hasSeparateDroneCharges method returns true.
     */
    protected open fun getMaxDroneCharges(): Int {
        return 0
    }

    /**
     * Uses system stats to calculate the actual drone charges based on {@link MagicSubsystem#scaleSystemStat(float, float)}
     * @return drone charges
     */
    open fun calcMaxDroneCharges(): Int {
        val baseValue = getMaxDroneCharges()
        if (baseValue == 0) {
            return 0
        }

        return scaleSystemStat(baseValue.toFloat(), ship.mutableStats.systemUsesBonus.computeEffective(baseValue.toFloat())).toInt()
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
     * Flux cost on drone deployment.
     * @return flat amount of flux
     */
    open fun getFluxCostFlatOnDroneDeployment(): Float {
        return 0f
    }

    /**
     * Flux cost on drone deployment as percent of base flux capacity of the ship.
     * @return percent of base flux
     */
    open fun getFluxCostPercentOnDroneDeployment(): Float {
        return 0f
    }

    /**
     * Set flux cost on drone deployment to be hard flux.
     * @return is hard flux?
     */
    open fun isHardFluxForDroneDeployment(): Boolean {
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
            if (droneCharges < calcMaxDroneCharges() || (calcMaxDroneCharges() == 0 && activeWings.size < getMaxDeployedDrones())) {
                if (droneCreationInterval.intervalElapsed()) {
                    if (calcMaxDroneCharges() >= 0) {
                        droneCharges++
                        droneCreationInterval.advance(0f) //reset
                    }
                    //this interval will be left elapsed if calcMaxDroneCharges returns 0.
                    //this means that killing a drone will effectively create one after a specific amount of time, like normal fighters.
                } else {
                    if (calcMaxDroneCharges() > 0) {
                        droneCreationInterval.advance(scaleSystemStat(amount, ship.mutableStats.systemRegenBonus.computeEffective(amount)))
                    } else {
                        droneCreationInterval.advance(scaleSystemStat(amount, ship.mutableStats.systemCooldownBonus.computeEffective(amount)))
                    }
                }
            }
        } else if (!hasSeparateDroneCharges()) {
            if (calcMaxCharges() == 0 && activeWings.size < getMaxDeployedDrones() && !droneCreationInterval.intervalElapsed()) {
                droneCreationInterval.advance(scaleSystemStat(amount, ship.mutableStats.systemCooldownBonus.computeEffective(amount)))

                //this interval will be left elapsed if getMaxDroneCharges returns 0.
                //this means that killing a drone will effectively create one after a specific amount of time, like normal fighters.
            }
        }

        if (!droneDeployInterval.intervalElapsed()) {
            droneDeployInterval.advance(amount)
        }

        activeWings = activeWings.filterKeys { it.isAlive && !it.isHulk }.toMutableMap()

        if (alive || (!dronesExplodeWhenShipDies() && dronesDeployWhenShipIsDead())) {
            if (activeWings.size < getMaxDeployedDrones()) {
                if (shouldSpawnDrone()) {
                    var shouldSpawnDrone = dronesToSpawn > 0
                    if (!shouldSpawnDrone) {
                        shouldSpawnDrone =
                            if (hasSeparateDroneCharges()) {
                                if (calcMaxDroneCharges() == 0) {
                                    droneCreationInterval.intervalElapsed()
                                } else {
                                    droneCharges > 0
                                }
                            } else if (hasCharges()) {
                                charges > 0
                            } else {
                                droneCreationInterval.intervalElapsed()
                            }
                    }

                    while (shouldSpawnDrone && activeWings.size < getMaxDeployedDrones() && (droneDeployInterval.intervalDuration == 0f || droneDeployInterval.intervalElapsed())) {
                        spawnDrone()
                        droneDeployInterval.advance(0f)

                        if (dronesToSpawn > 0) {
                            dronesToSpawn--
                        } else {
                            if (getFluxCostFlatOnDroneDeployment() > 0f) {
                                ship.fluxTracker.increaseFlux(
                                    getFluxCostFlatOnDroneDeployment(),
                                    isHardFluxForDroneDeployment()
                                )
                            }

                            if (getFluxCostPercentOnDroneDeployment() > 0f) {
                                ship.fluxTracker.increaseFlux(
                                    getFluxCostPercentOnDroneDeployment() * ship.hullSpec.fluxCapacity,
                                    isHardFluxForDroneDeployment()
                                )
                            }

                            if (hasSeparateDroneCharges()) {
                                if (calcMaxDroneCharges() == 0) {
                                    droneCreationInterval.advance(0f) //reset interval
                                } else {
                                    droneCharges--
                                }
                            } else if (hasCharges()) {
                                charges--
                            } else {
                                droneCreationInterval.advance(0f) //reset interval
                            }
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
            } else if (calcMaxCharges() > 0 && charges == 0 && activeWings.size < getMaxDeployedDrones()) {
                return (chargeInterval.elapsed / chargeInterval.intervalDuration).coerceIn(0f..1f)
            } else {
                return (droneCreationInterval.elapsed / droneCreationInterval.intervalDuration).coerceIn(0f..1f)
            }
        }


        if (!usesChargesOnActivate() && !hasSeparateDroneCharges()) {
            val fill = when (state!!) {
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

    open fun getDroneDimWrapper(): SpriteDimWrapper {
        return SpriteDimWrapper(Global.getSettings().getSprite("ui", "ship_arrow"))
    }

    override fun getNumHUDBars(): Int {
        return super.getNumHUDBars() + 2
    }

    override fun drawHUDBar(
        viewport: ViewportAPI,
        rootLoc: Vector2f,
        barLoc: Vector2f,
        displayAdditionalInfo: Boolean,
        longestNameWidth: Float
    ) {
        super.drawHUDBar(viewport, rootLoc, barLoc, displayAdditionalInfo, longestNameWidth)

        val colour = if (ship.isAlive) hudColor else MagicUI.BLUCOLOR

        var forgeCooldown: Float
        val reserveCharges: Int
        val reserveMaxCharges: Int

        if (hasSeparateDroneCharges()) {
            forgeCooldown = droneCreationInterval.elapsed / droneCreationInterval.intervalDuration
            reserveCharges = droneCharges
            reserveMaxCharges = getMaxDroneCharges()
        } else if (hasCharges()) {
            forgeCooldown = chargeInterval.elapsed / chargeInterval.intervalDuration
            reserveCharges = charges
            reserveMaxCharges = calcMaxCharges()
        } else {
            forgeCooldown = droneCreationInterval.elapsed / droneCreationInterval.intervalDuration
            reserveCharges = 0
            reserveMaxCharges = 0
        }

        val forgeText = if (reserveMaxCharges > 0) {
            MagicTxt.getString("subsystemDroneForgeText", reserveCharges.toString())
        } else {
            MagicTxt.getString("subsystemDroneForgeNoChargesText")
        }
        var textOnLeft: String? = null
        if (reserveCharges == reserveMaxCharges && reserveMaxCharges != 0) {
            forgeCooldown = 0f
            textOnLeft = MagicTxt.getString("subsystemDroneReservesFullText")
        }

        val chevronRow = if (hasCharges()) 2 else 1
        val chevronRowPos = getBarLocationForBarNum(barLoc, chevronRow)

        val additionalBarPadding = (longestNameWidth - CombatUI.STATUS_BAR_PADDING).coerceAtLeast(0f)
        CombatUI.renderAuxiliaryStatusBar(
            ship,
            CombatUI.INFO_TEXT_PADDING,
            false,
            CombatUI.STATUS_BAR_PADDING - CombatUI.INFO_TEXT_PADDING + additionalBarPadding,
            CombatUI.STATUS_BAR_WIDTH,
            forgeCooldown,
            forgeText,
            textOnLeft,
            true,
            chevronRowPos
        )

        // chevrons for alive wings
        var aliveDrones = booleanArrayOf()
        for (i in 0 until getMaxDeployedDrones()) {
            aliveDrones = aliveDrones.plus(i < activeWings.size)
        }

        Vector2f.add(chevronRowPos, Vector2f(CombatUI.INFO_TEXT_PADDING, 6f * MagicUI.UI_SCALING - CombatUI.BAR_HEIGHT * 2f), chevronRowPos)
        val tileDim = Vector2f(CombatUI.BAR_HEIGHT, CombatUI.BAR_HEIGHT)

        CombatUI.dimRender(
            Color.BLACK,
            chevronRowPos,
            Vector2f(MagicUI.UI_SCALING, -MagicUI.UI_SCALING),
            getDroneDimWrapper(),
            tileDim,
            aliveDrones,
            -1,
            null,
            true
        )

        CombatUI.dimRender(
            colour,
            chevronRowPos,
            Vector2f(0f, 0f),
            getDroneDimWrapper(),
            tileDim,
            aliveDrones,
            -1,
            null,
            true)

    }
}