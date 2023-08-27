package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.magiclib.achievements.MagicAchievement
import java.util.*

class RevisionistAchievement : MagicAchievement() {
    companion object {
        private const val FLEET_IDS_HASHED_KEY = "magicachievement_revisionist_fleetIdsInBattleHash"
        private const val LOADED_SAVE_KEY = "magicachievement_revisionist_loadedSave"
        private val interval = IntervalUtil(1f, 1f)
    }

    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()
        if (isComplete) return
        memory[LOADED_SAVE_KEY] = true
        saveChanges()
    }

    override fun advanceInCombat(amount: Float, events: List<InputEventAPI>, isSimulation: Boolean) {
        super.advanceInCombat(amount, events, isSimulation)
        if (isSimulation || isComplete) return

        // No need to check every frame.
        interval.advance(amount)

        if (interval.intervalElapsed()) {
            // This line is why I'm using Kotlin instead of Java.
            val allFleetIdsInBattle =
                Global.getCombatEngine().ships.mapNotNull { it.fleetMember?.fleetData?.fleet?.id }.distinct()
            val fleetIdsHashed = Objects.hash(allFleetIdsInBattle)
            if (allFleetIdsInBattle.size < 2) return // During deployment, the other fleet is not in the battle yet.

            // If the fleet ids hash is the same as what was stored, then the player is either
            // in the same battle as last `advanceInCombat`, or in a reloaded battle with the same fleets.
            if (memory[FLEET_IDS_HASHED_KEY] == fleetIdsHashed) {
                // If the "loaded save" is true, then they are fighting the same fleets after (re)loading a save.
                if (memory[LOADED_SAVE_KEY] == true) {
                    completeAchievement()
                    saveChanges()
                }
            } else {
                // If the fleet ids hash is different, then the player is in a different battle.
                // Store the new hash and reset the "loaded save" flag.
                memory[FLEET_IDS_HASHED_KEY] = fleetIdsHashed
                memory[LOADED_SAVE_KEY] = false
                saveChanges()
            }


//            BarEventManager.getInstance().active.items
//                .filterIsInstance<HubMissionBarEventWrapper>()
//                .forEach { event -> BarEventManager.getInstance().active.items.remove(event) }
//            BarEventManager.getInstance().creators
//                .filterIsInstance<SpecBarEventCreator>()
//                .forEach { creator -> BarEventManager.getInstance().creators.remove(creator) }
        }
    }
}
