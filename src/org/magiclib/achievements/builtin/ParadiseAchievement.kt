package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.listeners.SurveyPlanetListener
import org.magiclib.achievements.MagicAchievement

class ParadiseAchievement : MagicAchievement(), SurveyPlanetListener {
    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()
        Global.getSector().listenerManager.addListener(this, true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun reportPlayerSurveyedPlanet(planet: PlanetAPI?) {
        if ((planet?.market?.hazardValue ?: 0f) <= 0.75f) {
            completeAchievement()
            saveChanges()
        }
    }
}