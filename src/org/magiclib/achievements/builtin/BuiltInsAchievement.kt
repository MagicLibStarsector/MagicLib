package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.plog.PlaythroughLog
import org.magiclib.achievements.MagicAchievement

class BuiltInsAchievement : MagicAchievement() {
    private val key = "magiclib_builtInsAchievement_playerBuiltSmods"
    private val numberNeeded = 20

    override fun advanceAfterInterval(amount: Float) {
        super.advanceAfterInterval(amount)

        val existing: MutableMap<String, MutableSet<String>> =
            (memory[key] as? MutableMap<String, MutableSet<String>>?) ?: mutableMapOf()
        val countBeforeChange = existing.values.flatten().size

        PlaythroughLog.getInstance().sModsInstalled
            .groupBy({ it.member.id }, { it.sMods })
            .forEach { (shipId, smodIds) ->
                existing[createId(shipId)] =
                    (existing[createId(shipId)] ?: mutableSetOf()).apply { addAll(smodIds.flatten()) }
            }

        memory[key] = existing

        // If the count didn't change, no reason to continue further.
        if (existing.values.flatten().size == countBeforeChange) {
            return
        }

        if (existing.values.flatten().size >= numberNeeded) {
            completeAchievement()
        }

        saveChanges()
    }

    private fun createId(shipId: String) = "${Global.getSector().playerPerson.id}-$shipId"
}