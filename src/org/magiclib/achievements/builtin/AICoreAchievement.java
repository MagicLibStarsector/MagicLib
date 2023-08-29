package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.RogueAICore;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievement;

/**
 * Had an AI core go rogue.
 */
public class AICoreAchievement extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        super.advanceAfterInterval(amount);

        for (MarketAPI playerMarket : Misc.getPlayerMarkets(false)) {
            if (RogueAICore.get(playerMarket) != null) {
                completeAchievement();
                saveChanges();
            }
        }
    }
}
