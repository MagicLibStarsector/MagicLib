package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.magiclib.util.MagicMisc;

public class TestAchievement extends MagicAchievement {

    @Override
    public void advance(float amount) {
        if (MagicMisc.getElapsedDaysSinceGameStart() > 5) {
            completeAchievement(Global.getSector().getPlayerPerson());
            saveChanges();
        }
    }

    @Override
    public @Nullable Float getProgress() {
        return Math.min(MagicMisc.getElapsedDaysSinceGameStart(), getMaxProgress());
    }

    @Override
    public @NotNull Float getMaxProgress() {
        return 5f;
    }
}
