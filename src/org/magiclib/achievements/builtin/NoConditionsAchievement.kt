package org.magiclib.achievements.builtin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.listeners.SurveyPlanetListener
import org.magiclib.achievements.MagicAchievement

class NoConditionsAchievement : MagicAchievement(), SurveyPlanetListener {
    override fun onSaveGameLoaded() {
        super.onSaveGameLoaded()
        Global.getSector().listenerManager.addListener(this, true)
    }

    override fun onDestroyed() {
        super.onDestroyed()
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun reportPlayerSurveyedPlanet(planet: PlanetAPI?) {
        if (planet?.market?.conditions?.isEmpty() == true) {
            completeAchievement()
            saveChanges()
        }
    }
}

// fuck my life, i didn't know about SurveyPlanetListener

//    override fun advanceAfterInterval(amount: Float) {
//        if (Global.getSector() == null || Global.getCurrentState() != GameState.CAMPAIGN) return
//
//
//        if (memory[getSectorKey()] == null) {
//            memory[getSectorKey()] = findPlanets().map { it.id }.distinct()
//            saveChanges()
//            return
//        }
//
//        val preexistingMatchingPlanetsInSector = (memory[getSectorKey()] as? JSONArray)?.toStringList() ?: emptyList()
//
//        val newMatchingPlanets = findPlanets()
//            .filter { it.id !in preexistingMatchingPlanetsInSector }
//
//        if (newMatchingPlanets.isNotEmpty()) {
//            Global.getLogger(this::class.java)
//                .info("Completing $specId: player surveyed planet ${newMatchingPlanets.first().name} with hazard ${newMatchingPlanets.first().market.hazardValue * 100f}%.")
//            memory["${specId}Name"] =
//                "${newMatchingPlanets.first().fullName} in ${newMatchingPlanets.first().starSystem.name}"
//            completeAchievement()
//            saveChanges()
//        }
//    }
//
//    abstract fun findPlanets(): List<PlanetAPI>
//
//    open fun getSectorKey() = "preExisting${specId}inSector-${Global.getSector().playerPerson.id}"
//}
//
///**
// * Surveyed a planet with 75% hazard rating or better.
// */
//class ParadiseAchievement : PlanetMatchingAchievement() {
//    override fun findPlanets() = Global.getSector().starSystems
//        .asSequence()
//        .flatMap { it.planets }
//        .filter { !it.isStar && !it.isGasGiant && it.starSystem != null && !it.starSystem.tags.contains(Tags.THEME_CORE) }
//        .filter {
//            it.market?.surveyLevel == MarketAPI.SurveyLevel.FULL
//                    && it.market.faction.id == Factions.NEUTRAL // Exclude colonized planets that might have player-built modifiers.
//                    && it.market.hazard.base <= 0.75f
//        }
//        .toList()
//}
//
///**
// * Surveyed a planet with no conditions.
// */
//class NoConditionsAchievement : PlanetMatchingAchievement() {
//    override fun findPlanets() = Global.getSector().starSystems
//        .asSequence()
//        .flatMap { it.planets }
//        .filter { !it.isStar && !it.isGasGiant && it.starSystem != null && !it.starSystem.tags.contains(Tags.THEME_CORE) }
//        .filter { it.market?.surveyLevel == MarketAPI.SurveyLevel.FULL && it.market.conditions.isEmpty() }
//        .toList()
//}