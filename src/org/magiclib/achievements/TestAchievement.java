package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import org.magiclib.util.MagicMisc;

public class TestAchievement extends MagicAchievement {

    @Override
    public void advance(float amount) {
        if (MagicMisc.getElapsedDaysSinceGameStart() > 5) {
            completeAchievement(Global.getSector().getPlayerPerson());
            saveChanges();
        }
    }
}
