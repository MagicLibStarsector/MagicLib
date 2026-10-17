package org.magiclib.hullmods

import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.combat.HullModEffect
import com.fs.starfarer.api.combat.ShipAPI

class IncompatibleHullModEffect(
    private val base: HullModEffect
) : HullModEffect by base {
    class Incompatibility(
        val reason: String,
        val predicate: (ShipAPI) -> Boolean
    )

    val blockers: MutableList<Incompatibility> = ArrayList()

    override fun isApplicableToShip(ship: ShipAPI): Boolean =
        blockers.none { it.predicate(ship) } && base.isApplicableToShip(ship)

    override fun getUnapplicableReason(ship: ShipAPI): String? {
        val reasons = blockers.filter { it.predicate(ship) }
        return if (reasons.isEmpty()) {
            base.getUnapplicableReason(ship)
        } else {
            reasons.joinToString("\n") { it.reason }
        }
    }

    override fun getRequiredItem(): CargoStackAPI? = base.requiredItem
    override fun applyEffectsAfterShipAddedToCombatEngine(ship: ShipAPI, id: String) = base.applyEffectsAfterShipAddedToCombatEngine(ship, id)
}