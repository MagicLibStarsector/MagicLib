package org.magiclib.kotlin

import com.fs.starfarer.api.campaign.VisualPanelAPI
import com.fs.starfarer.api.characters.PersonAPI

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