package org.magiclib.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.VisualPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.LabelAPI
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shows the given people in the [VisualPanelAPI].
 *
 * Example usage:
 * ```kt
 * dialog.visualPanel.showPeople(listOf(personA, personB, personC))
 *
 * dialog.visualPanel.showPeople(
 *   people: listOf(personA, personB, personC),
 *   hideCurrentlyShownPeople = true,
 *   withRelationshipBar = true
 * )
 * ```
 */
fun VisualPanelAPI.showPeople(
    people: List<PersonAPI>,
    hideCurrentlyShownPeople: Boolean = true,
    withRelationshipBar: Boolean = true
) {
    if (hideCurrentlyShownPeople) {
        if (people.isEmpty())
            this.hideFirstPerson()
        if (people.size < 2)
            this.hideSecondPerson()
        if (people.size < 3)
            this.hideThirdPerson()
    }

    people.forEachIndexed { index, person ->
        when (index) {
            0 -> {
                // Must call showFirstPerson() or else if the person was hidden, their portrait will be invisible.
                this.showFirstPerson()
                this.showPersonInfo(person, true, withRelationshipBar)
            }

            1 -> this.showSecondPerson(person)
            2 -> this.showThirdPerson(person)
        }
    }
}

/**
 * Adds the given [IntelInfoPlugin] to the [Global.getSector]'s [com.fs.starfarer.api.campaign.comm.IntelManagerAPI].
 * @param shouldNotifyPlayer Whether or not the player should be notified in the bottom-left corner.
 */
fun IntelInfoPlugin.addToManager(shouldNotifyPlayer: Boolean = false) {
    Global.getSector().intelManager.addIntel(this, !shouldNotifyPlayer)
}

/**
 * Automatically sizes the [LabelAPI] to the given text, or to the label's text if no text is given.
 * Usage: tooltip.addPara().autoSizeToText().position.inMid()
 */
fun LabelAPI.autoSizeToText(text: String = this.text): LabelAPI {
    this.autoSizeToWidth(this.computeTextWidth(text))
    return this
}

/**
 * Returns the angle (in degrees) between the `originShip`'s forward vector and [otherShip]'s.
 *
 * Contributed by rksharkz.
 *
 * @return the difference in degrees
 * @see getForwardVector(ShipAPI)
 * @since 1.4.6
 */
fun ShipAPI.getAngleToAnotherShip(otherShip: ShipAPI): Float {
    val targetDirectionAngle = VectorUtils.getAngle(this.location, otherShip.location)
    val myForwardVector: Vector2f = this.getForwardVector()
    val myAngle = VectorUtils.getAngle(myForwardVector, otherShip.location)

    return myAngle - targetDirectionAngle
}

/**
 * Returns the absolute angle (in degrees) between this ship and [otherShip].
 *
 * Contributed by rksharkz.
 *
 * @return the difference in degrees, as absolute value
 * @see getAngleToAnotherShip
 * @since 1.4.6
 */
fun ShipAPI.getAbsoluteAngleToAnotherShip(otherShip: ShipAPI): Float {
    return abs(this.getAngleToAnotherShip(otherShip).toDouble()).toFloat()
}

/**
 * Returns the [Vector2f] of where the ship is looking (facing).
 *
 * Contributed by rksharkz.
 *
 * @return the ship's forward vector, similar to [com.fs.starfarer.api.util.Misc.getUnitVectorAtDegreeAngle] used with the ship's [ShipAPI.getFacing]
 * @since 1.4.6
 */
fun ShipAPI.getForwardVector(): Vector2f {
    val rotationRadians = Math.toRadians(this.facing.toDouble())

    // Calculate the components of the forward vector
    val x = cos(rotationRadians).toFloat()
    val y = sin(rotationRadians).toFloat()

    // Return the forward vector
    return Vector2f(x, y)
}