package org.magiclib.util.api.kotlin

import com.fs.starfarer.api.characters.PersonAPI
import org.magiclib.util.api.PersonUtils


/**
 * Delegate to [PersonUtils.getMaxOfficerLevel]
 */
fun PersonAPI.getMaxOfficerLevel(): Int {
    return PersonUtils.getMaxOfficerLevel(this)
}

/**
 * Delegate to [PersonUtils.getMaxOfficerEliteSkills]
 */
fun PersonAPI.getMaxOfficerEliteSkills(): Int {
    return PersonUtils.getMaxOfficerEliteSkills(this)
}
