package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import org.magiclib.achievements.MagicAchievement;
import org.magiclib.util.MagicMisc;

public class LuckyStartAchievement extends MagicAchievement implements DiscoverEntityListener {
    int days = 30;

    @Override
    public void onSaveGameLoaded() {
        super.onSaveGameLoaded();
        if (MagicMisc.getElapsedDaysSinceGameStart() > days)
            return;

        Global.getSector().getListenerManager().addListener(this, true);
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
        Global.getSector().getListenerManager().removeListener(this);
    }

    @Override
    public void reportEntityDiscovered(SectorEntityToken entity) {
        if (MagicMisc.getElapsedDaysSinceGameStart() <= days) {
            if (entity instanceof CampaignFleetAPI) {
                CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

                if (!fleet.getFaction().getId().equals(Factions.DERELICT)) {
                    for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
                        if (member.isCapital()) {
                            completeAchievement();
                            saveChanges();
                            Global.getLogger(this.getClass()).info("Lucky start achievement: found ship '" + member.getShipName() + "'.");
                            onDestroyed();
                            return;
                        }
                    }
                }
            }
        }
    }
}
