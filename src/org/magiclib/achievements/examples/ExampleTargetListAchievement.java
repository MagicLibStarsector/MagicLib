package org.magiclib.achievements.examples;

import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.input.InputEventAPI;
import org.magiclib.achievements.MagicTargetListAchievement;

import java.util.Arrays;
import java.util.List;

public class ExampleTargetListAchievement extends MagicTargetListAchievement {
    @Override
    public void onApplicationLoaded(boolean isComplete) {
        super.onApplicationLoaded(isComplete);
        if (isComplete) return;
        for (String factionId : Arrays.asList(Factions.PIRATES, Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.LUDDIC_PATH)) {
            addTarget(factionId, "Destroy a " + factionId + " ship");
        }
        saveChanges();
    }

    @Override
    public void advanceInCombat(float amount, List<InputEventAPI> events, boolean isSimulation) {
        super.advanceInCombat(amount, events, isSimulation);
        // Implementation code left as an exercise for the reader.
        setTargetComplete("the faction id");
        saveChanges();
    }
}
