package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievement;

/**
 * Using a fusion lamp.
 */
public class PowerOfTheSunAchievement extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        super.advanceAfterInterval(amount);

        for (MarketAPI market : Misc.getPlayerMarkets(false)) {
            for (CustomCampaignEntityAPI curr : market.getContainingLocation().getCustomEntities()) {
                if (curr.getCustomEntityType().equals(Entities.FUSION_LAMP) && curr.getOrbitFocus() == market.getPrimaryEntity()) {
                    completeAchievement();
                    saveChanges();
                }
            }
        }
    }
}
