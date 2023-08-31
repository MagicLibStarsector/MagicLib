package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.GateTransitListener;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievement;

/**
 * Jumped 60 LY.
 */
public class GateTravelAchievement extends MagicAchievement implements GateTransitListener {
    @Override
    public void onSaveGameLoaded() {
        super.onSaveGameLoaded();
        Global.getSector().getListenerManager().addListener(this, true);
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
        Global.getSector().getListenerManager().removeListener(this);
    }

    @Override
    public void reportFleetTransitingGate(CampaignFleetAPI fleet, SectorEntityToken gateFrom, SectorEntityToken gateTo) {
        if (fleet == Global.getSector().getPlayerFleet() && gateFrom != null && gateTo != null) {
            float distance = Misc.getDistance(gateFrom.getLocationInHyperspace(), gateTo.getLocationInHyperspace());

            if (distance >= 60) {
                completeAchievement();
                saveChanges();
                onDestroyed();
            }
        }
    }
}
