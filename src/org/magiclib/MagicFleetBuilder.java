package org.magiclib;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicVariables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Creates a fleet with a defined flagship and optional escort.
 * @author Wisp
 * @since 0.45.3
 */
@SuppressWarnings("unused")
public class MagicFleetBuilder {
    private @Nullable String fleetName;
    private @NotNull String fleetFaction = MagicVariables.BOUNTY_FACTION;
    private @Nullable String fleetType;
    private @Nullable String flagshipName;
    private @Nullable String flagshipVariant;
    private boolean flagshipRecovery;
    private boolean flagshipAutofit;
    private @Nullable PersonAPI captain;
    private @Nullable Map<String, Integer> supportFleet;
    private boolean supportAutofit;
    private int minFP = 50;
    @Nullable
    private String reinforcementFaction;
    private @Nullable Float qualityOverride;
    private @Nullable SectorEntityToken spawnLocation;
    private @Nullable FleetAssignment assignment;
    private @Nullable SectorEntityToken assignmentTarget;
    private boolean isImportant = false;
    private boolean transponderOn = false;
    private @Nullable String variantsPath;

    public MagicFleetBuilder() {
    }

    public CampaignFleetAPI create() {
        return MagicCampaign.createFleet(
                fleetName,
                fleetFaction,
                fleetType,
                flagshipName,
                flagshipVariant,
                flagshipRecovery,
                flagshipAutofit,
                captain,
                supportFleet,
                supportAutofit,
                minFP,
                reinforcementFaction,
                qualityOverride,
                spawnLocation,
                assignment,
                assignmentTarget,
                isImportant,
                transponderOn,
                variantsPath);
    }

    /**
     * Default: "<faction name> Fleet".
     */
    public MagicFleetBuilder setFleetName(String fleetName) {
        this.fleetName = fleetName;
        return this;
    }

    /**
     * Default: MagicVariables.BOUNTY_FACTION.
     */
    public MagicFleetBuilder setFleetFaction(String fleetFaction) {
        this.fleetFaction = fleetFaction;
        return this;
    }

    /**
     * campaign.ids.FleetTypes, defaults to FleetTypes.PERSON_BOUNTY_FLEET
     */
    public MagicFleetBuilder setFleetType(@Nullable String fleetType) {
        this.fleetType = fleetType;
        return this;
    }

    public MagicFleetBuilder setFlagshipName(@Nullable String flagshipName) {
        this.flagshipName = flagshipName;
        return this;
    }

    public MagicFleetBuilder setFlagshipVariant(String flagshipVariant) {
        this.flagshipVariant = flagshipVariant;
        return this;
    }

    public MagicFleetBuilder setFlagshipRecovery(boolean flagshipRecovery) {
        this.flagshipRecovery = flagshipRecovery;
        return this;
    }

    public MagicFleetBuilder setFlagshipAutofit(boolean flagshipAutofit) {
        this.flagshipAutofit = flagshipAutofit;
        return this;
    }

    /**
     * Can be NULL for random captain, otherwise use MagicCampaign.createCaptain()
     */
    public MagicFleetBuilder setCaptain(@Nullable PersonAPI captain) {
        this.captain = captain;
        return this;
    }

    /**
     * Map<variantId, number> Optional escort ship VARIANTS and their NUMBERS
     */
    public MagicFleetBuilder setSupportFleet(@Nullable Map<String, Integer> supportFleet) {
        this.supportFleet = supportFleet;
        return this;
    }

    public MagicFleetBuilder setSupportAutofit(boolean supportAutofit) {
        this.supportAutofit = supportAutofit;
        return this;
    }

    /**
     * Minimal fleet size, can be used to adjust to the player's power. Set to 0 to ignore
     */
    public MagicFleetBuilder setMinFP(int minFP) {
        this.minFP = minFP;
        return this;
    }

    /**
     * Reinforcement faction, if the fleet faction is a "neutral" faction without ships
     * Default: fleetFaction.
     */
    public MagicFleetBuilder setReinforcementFaction(String reinforcementFaction) {
        this.reinforcementFaction = reinforcementFaction;
        return this;
    }

    /**
     * Ship quality override.
     * Default: 2 (no D-mods) if null or <0.
     */
    public MagicFleetBuilder setQualityOverride(@Nullable Float qualityOverride) {
        this.qualityOverride = qualityOverride;
        return this;
    }

    /**
     * Where the fleet will spawn.
     * Defaults to assignmentTarget.
     * If assignmentTarget is also not set, the fleet will not be spawned.
     */
    public MagicFleetBuilder setSpawnLocation(@Nullable SectorEntityToken spawnLocation) {
        this.spawnLocation = spawnLocation;
        return this;
    }

    /**
     * com.fs.starfarer.api.campaign.FleetAssignment.
     * Default: orbit aggressive.
     */
    public MagicFleetBuilder setAssignment(@Nullable FleetAssignment assignment) {
        this.assignment = assignment;
        return this;
    }

    /**
     * Where the fleet will go to execute its order.
     * Defaults to spawnLocation.
     * If spawnLocation is also not set, the fleet will not be spawned.
     */
    public MagicFleetBuilder setAssignmentTarget(@Nullable SectorEntityToken assignmentTarget) {
        this.assignmentTarget = assignmentTarget;
        return this;
    }

    /**
     * Default: false.
     */
    public MagicFleetBuilder setIsImportant(boolean isImportant) {
        this.isImportant = isImportant;
        return this;
    }

    /**
     * Default: false.
     */
    public MagicFleetBuilder setTransponderOn(boolean transponderOn) {
        this.transponderOn = transponderOn;
        return this;
    }

    /**
     * If not null, the script will try to find missing variant files there.
     * Used to generate fleets using cross-mod variants that won't be loaded otherwise to avoid crashes.
     * The name of the variant files must match the ID of the variant.
     */
    public MagicFleetBuilder setVariantsPath(@Nullable String variantsPath) {
        this.variantsPath = variantsPath;
        return this;
    }
}