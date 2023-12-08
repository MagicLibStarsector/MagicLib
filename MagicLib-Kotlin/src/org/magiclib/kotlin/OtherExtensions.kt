package org.magiclib.kotlin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.VisualPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.LabelAPI

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