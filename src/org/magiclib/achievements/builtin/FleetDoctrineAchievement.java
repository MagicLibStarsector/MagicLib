package org.magiclib.achievements.builtin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.magiclib.achievements.MagicAchievement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FleetDoctrineAchievement extends MagicAchievement {
    private final Set<String> vanillaFactions = new HashSet<>();
    private final Set<String> allTechTypes = new HashSet<>();

    @Override
    public void onApplicationLoaded() {
        super.onApplicationLoaded();
        vanillaFactions.add(Factions.INDEPENDENT);
        vanillaFactions.add(Factions.PIRATES);
        vanillaFactions.add(Factions.PERSEAN);
        vanillaFactions.add(Factions.TRITACHYON);
        vanillaFactions.add(Factions.HEGEMONY);
        vanillaFactions.add(Factions.LIONS_GUARD);
        vanillaFactions.add(Factions.DIKTAT);
        vanillaFactions.add(Factions.KOL);
        vanillaFactions.add(Factions.LUDDIC_CHURCH);
        vanillaFactions.add(Factions.LUDDIC_PATH);
        vanillaFactions.add(Factions.DERELICT);
        vanillaFactions.add(Factions.REMNANTS);
    }

    @Override
    public void onGameLoaded() {
        super.onGameLoaded();
        allTechTypes.clear();

        for (String factionId : vanillaFactions) {
            for (String alwaysKnownShipId : Global.getSector().getFaction(factionId).getAlwaysKnownShips()) {
                try {
                    ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(alwaysKnownShipId);

                    if (hullSpec.getManufacturer() == null || hullSpec.getManufacturer().trim().isEmpty())
                        continue;
                    if (hullSpec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)) continue;
                    if (hullSpec.getHullSize() == ShipAPI.HullSize.FIGHTER) continue;

                    allTechTypes.add(hullSpec.getManufacturer());
                } catch (Exception e) {
                    Global.getLogger(this.getClass()).error("Failed to load hull spec for " + alwaysKnownShipId, e);
                }
            }
        }

        Global.getLogger(this.getClass()).info("Loaded " + Misc.getJoined(", ", new ArrayList<>(allTechTypes)) + " tech types.");
    }

    @Override
    public void advanceInCombat(float amount, List<InputEventAPI> events, boolean isSimulation) {
        if (isSimulation) return;
        if (allTechTypes.isEmpty()) return;
        CombatEngineAPI combatEngine = Global.getCombatEngine();

        Set<String> techTypesDeployed = new HashSet<>();

        for (DeployedFleetMemberAPI ship : combatEngine.getFleetManager(FleetSide.PLAYER).getAllEverDeployedCopy()) {
            // Skip allied ships.
            if (ship.getMember().isAlly())
                continue;

            techTypesDeployed.add(ship.getMember().getHullSpec().getManufacturer());
        }

        for (String reqType : allTechTypes) {
            if (!techTypesDeployed.contains(reqType)) {
                return;
            }
        }

        completeAchievement();
        saveChanges();
    }

    @Override
    public @NotNull String getTooltip() {
        return Misc.getJoined(", ", new ArrayList<>(allTechTypes));
    }
}
