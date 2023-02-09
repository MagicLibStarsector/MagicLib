package org.magiclib.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicVariables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Creates a fleet with a defined flagship and optional escort.
 *
 * @author Wisp
 * @since 0.46.0
 */
@SuppressWarnings("unused")
public class MagicFleetBuilder {
    private @Nullable String fleetName;
    private @NotNull String fleetFaction = MagicVariables.BOUNTY_FACTION;
    private @Nullable String fleetType;
    private @Nullable String flagshipName;
    private @Nullable String flagshipVariant;
    private boolean flagshipAlwaysRecoverable;
    private boolean flagshipAutofit;
    private @Nullable PersonAPI captain;
    private @Nullable Map<String, Integer> supportFleet;
    private boolean supportAutofit;
    private int minFP = Global.getSector().getPlayerFleet().getFleetPoints();
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
                flagshipAlwaysRecoverable,
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
     * Default: MagicVariables.BOUNTY_FACTION ("ML_bounty").
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

    /**
     * Default: automatically picked by the game.
     */
    public MagicFleetBuilder setFlagshipName(@Nullable String flagshipName) {
        this.flagshipName = flagshipName;
        return this;
    }

    /**
     * Default: picks the first ship in the fleet after it has been sorted.
     */
    public MagicFleetBuilder setFlagshipVariant(String flagshipVariant) {
        this.flagshipVariant = flagshipVariant;
        return this;
    }

    /**
     * Default: false.
     */
    public MagicFleetBuilder setFlagshipAlwaysRecoverable(boolean flagshipAlwaysRecoverable) {
        this.flagshipAlwaysRecoverable = flagshipAlwaysRecoverable;
        return this;
    }

    /**
     * Whether the flagship will be autofitted. When false, the Flagship does not receive D-mods or S-mods from the quality override.
     * Default: false.
     */
    public MagicFleetBuilder setFlagshipAutofit(boolean flagshipAutofit) {
        this.flagshipAutofit = flagshipAutofit;
        return this;
    }

    /**
     * Can be null for a random captain, otherwise use MagicCampaign.createCaptain()
     */
    public MagicFleetBuilder setCaptain(@Nullable PersonAPI captain) {
        this.captain = captain;
        return this;
    }

    /**
     * Map<variantId, number> Optional escort ship variants and how many of each to create.
     * Default: no preset support fleet.
     */
    public MagicFleetBuilder setSupportFleet(@Nullable Map<String, Integer> supportFleet) {
        this.supportFleet = supportFleet;
        return this;
    }

    /**
     * Whether the preset ships will be autofitted. When false, the preset ships do not receive D-mods or S-mods from the quality override.
     * Default: false.
     */
    public MagicFleetBuilder setSupportAutofit(boolean supportAutofit) {
        this.supportAutofit = supportAutofit;
        return this;
    }

    /**
     * Minimal fleet size, can be used to adjust to the player's power. Set to -1 to ignore.
     * Default: current player fleet FP.
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
     * Default: assignmentTarget.
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
     * Default: spawnLocation.
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