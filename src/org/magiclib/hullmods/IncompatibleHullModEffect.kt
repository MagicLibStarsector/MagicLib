package org.magiclib.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.HullModEffect
import com.fs.starfarer.api.combat.ShipAPI

class IncompatibleHullModEffect(
    val unwrapped: HullModEffect
) : HullModEffect by unwrapped {
    class Incompatibility(
        val reason: String,
        val predicate: (ShipAPI) -> Boolean
    )
    private var loggedError = false // To avoid log spam in isApplicableToShip. It gets called a lot.

    val blockers: MutableList<Incompatibility> = ArrayList()

    override fun isApplicableToShip(ship: ShipAPI): Boolean =
        unwrapped.isApplicableToShip(ship) && blockers.none {
            try { it.predicate(ship) }
            catch (e: Exception) {
                if(!loggedError) {
                    Global.getLogger(this::class.java).error("Incompatibility predicate failed for '${it.reason}'", e)
                    loggedError = true
                }
                true // Not compatible if an error occurs.
            }
        }

    override fun getUnapplicableReason(ship: ShipAPI): String? {
        val reasons = blockers.filter {
            try { it.predicate(ship) }
            catch (e: Exception) {
                Global.getLogger(this::class.java).error("Incompatibility predicate failed for '${it.reason}'", e)
                false // Exclude if an error occurs.
            }
        }

        val reason = if(reasons.isNotEmpty())
            reasons.joinToString("\n") { it.reason }
        else
            unwrapped.getUnapplicableReason(ship)

        return reason + if(loggedError) "\n(Failed to check compatibility. See starsector.log)" else ""
    }

    override fun getRequiredItem(): CargoStackAPI? = unwrapped.requiredItem
    override fun applyEffectsAfterShipAddedToCombatEngine(ship: ShipAPI?, id: String?) = unwrapped.applyEffectsAfterShipAddedToCombatEngine(ship, id)
}