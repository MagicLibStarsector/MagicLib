package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel;
import org.magiclib.achievements.MagicAchievement;

/**
 * Defeated an inspection fleet.
 */
public class InspectionAchievement extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        super.advanceAfterInterval(amount);

        for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(HegemonyInspectionIntel.class)) {
            HegemonyInspectionIntel hegIntel = (HegemonyInspectionIntel) intel;

            if (hegIntel.isPlayerTargeted()
                    && hegIntel.getOutcome() == HegemonyInspectionIntel.HegemonyInspectionOutcome.TASK_FORCE_DESTROYED) {
                completeAchievement();
                saveChanges();
            }
        }
    }
}
