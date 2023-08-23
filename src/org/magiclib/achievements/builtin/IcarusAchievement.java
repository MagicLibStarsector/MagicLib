package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.achievements.MagicAchievement;

import java.util.ArrayList;
import java.util.List;

/**
 * Flew into a sun.
 */
public class IcarusAchievement extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        if (Global.getSector() == null
                || Global.getCurrentState() != GameState.CAMPAIGN
                || Global.getSector().getCurrentLocation() == Global.getSector().getHyperspace()
        ) return;

        List<PlanetAPI> stars = new ArrayList<>();

        for (PlanetAPI planet : Global.getSector().getPlayerFleet().getContainingLocation().getPlanets()) {
            if (planet.isStar()) {
                stars.add(planet);
            }
        }

        Vector2f playerLocation = Global.getSector().getPlayerFleet().getLocation();

        // Check if player is inside a system star's radius.
        for (PlanetAPI star : stars) {
            if (star.getRadius() > Vector2f.sub(playerLocation, star.getLocation(), null).length()) {
                completeAchievement();
                saveChanges();
                return;
            }
        }
    }
}
