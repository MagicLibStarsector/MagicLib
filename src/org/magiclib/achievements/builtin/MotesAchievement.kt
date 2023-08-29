package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager
import org.json.JSONArray
import org.magiclib.achievements.MagicAchievement
import org.magiclib.kotlin.toStringList

/**
 * Encountered eight different hyperspace ghosts.
 */
class MotesAchievement : MagicAchievement() {
    var key = "motesSeen"

    override fun advanceAfterInterval(amount: Float) {
        super.advanceAfterInterval(amount)
        if (Global.getSector().playerFleet == null) return

        val motesSeen = ((memory[key] as? JSONArray)?.toStringList() ?: emptyList()).toSet()

        val newMotesSeen = motesSeen + SensorGhostManager.getGhostManager().ghosts.map { it.javaClass.simpleName }

        if (motesSeen.size != newMotesSeen.size) {
            memory[key] = JSONArray(newMotesSeen)
            saveChanges()
        }

        if (newMotesSeen.size >= maxProgress) {
            completeAchievement()
            saveChanges()
        }
    }

    override fun getProgress(): Float = ((memory[key] as? JSONArray)?.toStringList() ?: emptyList()).size.toFloat()
    override fun getMaxProgress(): Float = 8f
}
