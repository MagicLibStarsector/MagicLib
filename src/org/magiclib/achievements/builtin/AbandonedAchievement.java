package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener;
import org.magiclib.achievements.MagicAchievement;

/**
 * Abandoned a colony.
 */
public class AbandonedAchievement extends MagicAchievement implements PlayerColonizationListener {
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
    public void reportPlayerAbandonedColony(MarketAPI colony) {
        completeAchievement();
        saveChanges();
    }

    @Override
    public void reportPlayerColonizedPlanet(PlanetAPI planet) {

    }
}
