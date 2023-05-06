package org.magiclib.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicCampaign;
import org.magiclib.util.MagicStringMatcher;
import org.magiclib.util.MagicVariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Creates a fleet with a defined flagship and optional escort.
 * <p>
 * Not all fields are required. Each `set` method has a comment showing the default value for if it is not used.
 * <p>
 * Example usage:
 * <pre>
 * MagicCampaign.createFleetBuilder()
 *         .setFleetName("Hegemony Attack Fleet")
 *         .setFleetFaction(Factions.HEGEMONY)
 *         .setFleetType(FleetTypes.TASK_FORCE)
 *         .setFlagshipName("HSS Onslaught")
 *         .setFlagshipVariant("onslaught_xiv_Elite")
 *         .setFlagshipAlwaysRecoverable(false)
 *         .setFlagshipAutofit(true)
 *         .setCaptain(theCaptain)
 *         .setSupportAutofit(true)
 *         .setReinforcementFaction(Factions.HEGEMONY)
 *         .create()
 * </pre>
 *
 * @author Wisp
 * @since 0.46.0
 */
@SuppressWarnings("unused")
public class MagicFleetBuilder {
    protected static Logger log = Global.getLogger(MagicFleetBuilder.class);

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

    /**
     * Creates a fleet with a defined flagship and optional escort.
     *
     * @author Wisp
     * @since 0.46.0
     */
    public MagicFleetBuilder() {
    }

    public CampaignFleetAPI create() {
        return createFleet(
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
     * Can be null for a random captain, otherwise use MagicCampaign.createCaptainBuilder()
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


    /**
     * Creates a fleet with a defined flagship and optional escort
     *
     * @param fleetType            campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
     * @param flagshipName         Optional flagship name
     * @param captain              PersonAPI, can be NULL for random captain, otherwise use createCaptain()
     * @param supportFleet         map <variantId, number> Optional escort ship VARIANTS and their NUMBERS
     * @param minFP                Minimal fleet size, can be used to adjust to the player's power,         set to 0 to ignore
     * @param reinforcementFaction Reinforcement faction,                                                   if the fleet faction is a "neutral" faction without ships
     * @param qualityOverride      Optional ship quality override, default to 2 (no D-mods) if null or <0
     * @param spawnLocation        Where the fleet will spawn, default to assignmentTarget if NULL
     * @param assignment           campaign.FleetAssignment, default to orbit aggressive
     * @param assignmentTarget     where the fleet will go to execute its order, it will not spawn if NULL
     * @param variantsPath         If not null, the script will try to find missing variant files there.
     *                             Used to generate fleets using cross-mod variants that won't be loaded otherwise to avoid crashes.
     *                             The name of the variant files must match the ID of the variant.
     */
    private static CampaignFleetAPI createFleet(
            @Nullable String fleetName,
            @Nullable String fleetFaction,
            @Nullable String fleetType,
            @Nullable String flagshipName,
            @Nullable String flagshipVariant,
            boolean flagshipRecovery,
            boolean flagshipAutofit,
            @Nullable PersonAPI captain,
            @Nullable Map<String, Integer> supportFleet,
            boolean supportAutofit,
            int minFP,
            @Nullable String reinforcementFaction,
            @Nullable Float qualityOverride,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            @Nullable SectorEntityToken assignmentTarget,
            boolean isImportant,
            boolean transponderOn,
            @Nullable String variantsPath
    ) {
        // Clean up previous generation
        MagicVariables.presetShipIdsOfLastCreatedFleet.clear();

        if (fleetName == null) {
            FactionAPI faction = null;

            if (fleetFaction != null) {
                faction = MagicStringMatcher.findBestFactionMatch(fleetFaction);
            } else if (reinforcementFaction != null) {
                faction = MagicStringMatcher.findBestFactionMatch(reinforcementFaction);
            }

            if (faction != null) {
                fleetName = faction.getDisplayName() + " Fleet";
            } else {
                fleetName = "Unknown Fleet";
            }
        }

        if (MagicVariables.verbose) {
            log.info(" ");
            log.info("SPAWNING " + fleetName);
            log.info(" ");
        }

        //Setup defaults
        String type = FleetTypes.PERSON_BOUNTY_FLEET;
        if (fleetType != null && !fleetType.equals("")) {
            type = fleetType;
        } else if (MagicVariables.verbose) {
            log.info("No fleet type defined, defaulting to bounty fleet.");
        }

        String extraShipsFaction = fleetFaction;

        if (reinforcementFaction != null) {
            extraShipsFaction = reinforcementFaction;
        } else if (MagicVariables.verbose) {
            log.info("No reinforcement faction defined, defaulting to fleet faction.");
        }

        SectorEntityToken location = assignmentTarget;
        if (spawnLocation != null) {
            location = spawnLocation;
        } else if (MagicVariables.verbose) {
            log.info("No spawn location defined, defaulting to assignment target.");
        }

        FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;

        if (assignment != null) {
            order = assignment;
        } else if (MagicVariables.verbose) {
            log.info("No assignment defined, defaulting to aggressive orbit.");
        }

        float quality = 1f;

        if (qualityOverride != null && qualityOverride >= -1) {
            quality = qualityOverride;
        } else if (MagicVariables.verbose) {
            log.info("No quality override defined, defaulting to highest quality.");
        }

        // EMPTY FLEET
        CampaignFleetAPI newFleet = FleetFactoryV3.createEmptyFleet(extraShipsFaction, type, null);

        // ADDING FLAGSHIP
        FleetMemberAPI flagship = generateShip(flagshipVariant, variantsPath, flagshipAutofit, MagicVariables.verbose);

        if (flagship == null) {
            log.warn("Warning during " + fleetName + " generation." +
                    "\n\tReason: flagshipVariant " + flagshipVariant + " and variantsPath " + variantsPath + " was specified, but flagship could not be created." +
                    "\n\tWill try to fall back to the first ship in the fleet.");
        } else {
            newFleet.getFleetData().addFleetMember(flagship);
        }

        // ADDING PRESET SHIPS IF REQUIRED
        if (supportFleet != null && !supportFleet.isEmpty()) {
            List<FleetMemberAPI> support = generatePresetShips(supportFleet, variantsPath, supportAutofit, MagicVariables.verbose);
            for (FleetMemberAPI m : support) {
                newFleet.getFleetData().addFleetMember(m);
                MagicVariables.presetShipIdsOfLastCreatedFleet.add(m.getId());
            }
        }

        int coreFP = newFleet.getFleetPoints();

        // ADDING PROCGEN SHIPS IF REQUIRED
        if (minFP > 0) {
            if (MagicVariables.verbose) {
                if (minFP < coreFP) {
                    log.info("Preset FP: " + coreFP + ", requested FP: " + minFP + ". No reinforcements required.");
                } else {
                    log.info("Preset FP: " + coreFP + ", requested FP: " + minFP + ". Adding " + (minFP - coreFP) + " FP worth of " + Global.getSector().getFaction(extraShipsFaction).getDisplayName() + " reinforcements.");
                }
            }

            if (minFP > coreFP) {
                CampaignFleetAPI reinforcements = generateRandomFleet(extraShipsFaction, quality, type, (minFP - coreFP), 0.2f);
                if (reinforcements != null) {
                    //KEEP THOSE DMODS!
                    if (reinforcements.getInflater() != null) {
                        newFleet.setInflater(reinforcements.getInflater());
                    }
                    if (MagicVariables.verbose) {
                        log.info("Fleet quality set to " + newFleet.getInflater().getQuality());
                    }

                    //check for empty reinforcement fleet (the empty fleet is kept just for the quality stuff)
                    if (!reinforcements.isEmpty()) {
                        List<FleetMemberAPI> membersInPriorityOrder = reinforcements.getFleetData().getMembersInPriorityOrder();
                        if (membersInPriorityOrder != null && !membersInPriorityOrder.isEmpty()) {
                            for (FleetMemberAPI m : membersInPriorityOrder) {
                                m.setCaptain(null);
                                newFleet.getFleetData().addFleetMember(m);
                                if (MagicVariables.verbose) {
                                    log.info("adding " + m.getHullId());
                                }
                            }
                        } else {
                            log.warn("FAILED reinforcement generation");
                        }
                    }
                }
            }
        }

        // FINALIZE FLAGSHIP
        // Choose a flagship if one wasn't specified
        if (flagship == null) {
            newFleet.getFleetData().sort();
            // If there is no flagship, this will return the first ship in the sorted fleet.
            flagship = newFleet.getFlagship();
        }

        if (flagship == null) {
            log.warn("Aborting " + fleetName + " generation. Reason: no flagshipVariant was specified and none could be automatically chosen.");
            return null;
        }

        // Rename the flagship if needed
        if (flagshipName != null && !flagshipName.isEmpty()) {
            flagship.setShipName(flagshipName);
        }

        flagship.setFlagship(true);

        if (flagshipRecovery) {
            flagship.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        }

        MagicVariables.presetShipIdsOfLastCreatedFleet.add(flagship.getId());

        // Ensure the flagship is properly set
        newFleet.getFleetData().setFlagship(flagship);

        // ADDING OFFICERS
        FleetParamsV3 fleetParams = new FleetParamsV3(
                null,
                new Vector2f(),
                fleetFaction,
                quality,
                type,
                newFleet.getFleetPoints(),
                0f, 0f, 0f, 0f, 0f, 0f
        );
        FleetFactoryV3.addCommanderAndOfficersV2(newFleet, fleetParams, new Random());

        // Ensure the flagship is properly set AGAIN!
        newFleet.getFleetData().setFlagship(flagship);

        // I swear those sneaky officers are messing up the flagship tags
        if (MagicVariables.verbose) {
            log.warn("Fleet flagship is " + newFleet.getFlagship().getHullId());
            for (FleetMemberAPI m : newFleet.getMembersWithFightersCopy()) {
                if (m.isFlagship()) {
                    log.warn(m.getHullId() + " has the Flagship tag");
                }
            }
        }
        for (FleetMemberAPI m : newFleet.getMembersWithFightersCopy()) {
            if (m == flagship) {
                if (!m.isFlagship()) {
                    m.setFlagship(true);
                    if (MagicVariables.verbose) {
                        log.warn("Adding flagship tag to " + m.getHullId());
                    }
                }
            } else if (m.isFlagship()) {
                m.setFlagship(false);
                if (MagicVariables.verbose) {
                    log.warn("Removing flagship tag from " + m.getHullId());
                }
            }
        }

        // add the defined captain to the flagship if needed
        if (captain != null) {
            newFleet.getFlagship().setCaptain(captain);
            newFleet.setCommander(flagship.getCaptain());
            if (MagicVariables.verbose) {
                log.warn("Assigning " + captain.getNameString() + " to the Flagship");
            }
        } else {
            newFleet.getFlagship().setCaptain(newFleet.getCommander());
            if (MagicVariables.verbose) {
                log.warn("Moving random commander to the Flagship");
            }
        }

        // apply skills to the fleet
        FleetFactoryV3.addCommanderSkills(newFleet.getCommander(), newFleet, fleetParams, new Random());
        if (MagicVariables.verbose) {
            int admiral = 0;
            int elite = 0;

            for (MutableCharacterStatsAPI.SkillLevelAPI skill : newFleet.getCommander().getStats().getSkillsCopy()) {
                if (skill.getSkill().isAdmiralSkill()) {
                    admiral++;
                    if (skill.getLevel() > 1) {
                        elite++;
                    }
                }
            }

            log.info("Applied " + admiral + " admiral skills ( " + elite + " elite ones) to the fleet.");
        }

        //cleanup name and faction
        newFleet.setNoFactionInName(true);
        newFleet.setFaction(fleetFaction, true);
        if (fleetName != null && !fleetName.isEmpty()) newFleet.setName(fleetName);

        //set standard 70% CR
        List<FleetMemberAPI> members = newFleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            member.getRepairTracker().setCR(0.7f);
        }

        //FINISHING
        newFleet.getFleetData().sort();
        newFleet.getFleetData().setSyncNeeded();
        newFleet.getFleetData().syncIfNeeded();
//        newFleet.getFleetData().syncMemberLists();
//        newFleet.setInflated(true);
//        newFleet.inflateIfNeeded();

        // SPAWN if needed
        if (location != null) {
            if (assignmentTarget == null) {
                //prevent a crash when the fleet is spawned at a location but without a target
                MagicCampaign.spawnFleet(
                        newFleet,
                        location,
                        order,
                        location,
                        isImportant,
                        transponderOn,
                        MagicVariables.verbose
                );
            } else {
                MagicCampaign.spawnFleet(
                        newFleet,
                        location,
                        order,
                        assignmentTarget,
                        isImportant,
                        transponderOn,
                        MagicVariables.verbose
                );
            }
        }

        if (MagicVariables.verbose) {
            log.warn(fleetName + " creation completed.");
        }

        return newFleet;
    }

    /**
     * Creates a fleet with a defined flagship and optional escort
     *
     * @param fleetType            campaign.ids.FleetTypes, default to FleetTypes.PERSON_BOUNTY_FLEET
     * @param flagshipName         Optional flagship name
     * @param captain              PersonAPI, can be NULL for random captain, otherwise use createCaptain()
     * @param supportFleet         map <variantId, number> Optional escort ship VARIANTS and their NUMBERS
     * @param minFP                Minimal fleet size, can be used to adjust to the player's power,         set to 0 to ignore
     * @param reinforcementFaction Reinforcement faction,                                                   if the fleet faction is a "neutral" faction without ships
     * @param qualityOverride      Optional ship quality override, default to 2 (no D-mods) if null or <0
     * @param spawnLocation        Where the fleet will spawn, default to assignmentTarget if NULL
     * @param assignment           campaign.FleetAssignment, default to orbit aggressive
     * @param assignmentTarget     where the fleet will go to execute its order, it will not spawn if NULL
     */
    private static CampaignFleetAPI createFleet(
            @Nullable String fleetName,
            @Nullable String fleetFaction,
            @Nullable String fleetType,
            @Nullable String flagshipName,
            @Nullable String flagshipVariant,
            @Nullable PersonAPI captain,
            @Nullable Map<String, Integer> supportFleet,
            int minFP,
            @Nullable String reinforcementFaction,
            @Nullable Float qualityOverride,
            @Nullable SectorEntityToken spawnLocation,
            @Nullable FleetAssignment assignment,
            @Nullable SectorEntityToken assignmentTarget,
            boolean isImportant,
            boolean transponderOn
    ) {
        return createFleet(fleetName, fleetFaction, fleetType, flagshipName, flagshipVariant, false, false,
                captain, supportFleet, true, minFP, reinforcementFaction, qualityOverride,
                spawnLocation, assignment, assignmentTarget, isImportant, transponderOn, null);
    }

    private static FleetMemberAPI generateShip(@Nullable String variant, @Nullable String variantsPath, boolean autofit, boolean verbose) {
        ShipVariantAPI thisVariant = Global.getSettings().getVariant(variant);
        //if the variant doesn't exist but a custom variant path is defined, try loading it
        if (thisVariant == null && variantsPath != null) {
            thisVariant = MagicCampaign.loadVariant(variantsPath + variant + ".variant");
        }
        if (thisVariant == null) {
            return null;
        }
        FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, thisVariant);

        if (ship != null) {
            ship.getVariant().addTag(Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE);
            //attempt at keeping the variants intact
            if (!autofit) {
                ship.getVariant().addTag("no_autofit");
            }
            if (verbose) log.info("Created " + variant);
            return ship;
        }

        log.warn("Failed to create " + variant);
        return null;
    }

    private static List<FleetMemberAPI> generatePresetShips(Map<String, Integer> supportFleet, @Nullable String variantsPath, boolean autofit, boolean verbose) {
        List<FleetMemberAPI> fleetMemberList = new ArrayList<>();
        for (String shipVariantId : supportFleet.keySet()) {
            for (int i = 0; i < supportFleet.get(shipVariantId); i++) {
                FleetMemberAPI fleetMember = generateShip(shipVariantId, variantsPath, autofit, verbose);
                if (fleetMember != null) fleetMemberList.add(fleetMember);
            }
        }
        return fleetMemberList;
    }

    private static CampaignFleetAPI generateRandomFleet(String factionId, float qualityOverride, String fleetType, float fleetPoints, float freightersAndTankersFraction) {

        FleetParamsV3 params = new FleetParamsV3(
                null,
                //fakeMarket(factionId, qualityOverride), //Fake market are actually not needed, one will be created by the FleetFactory
                new Vector2f(),
                factionId,
                qualityOverride, //this is supposed to override the default fleet quality without market of 0.5
                fleetType,
                fleetPoints * (1 - freightersAndTankersFraction),
                fleetPoints * (freightersAndTankersFraction / 3),
                fleetPoints * (freightersAndTankersFraction / 3),
                fleetPoints * (freightersAndTankersFraction / 3),
                0f, 0f,
                0 //DO NOT SET A QUALITY MOD, it is added to the market quality
        );

        params.ignoreMarketFleetSizeMult = true;
        params.maxNumShips = 50;
        params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;

        //add S mods?
        if (qualityOverride > 1) {
            params.averageSMods = Math.round(qualityOverride - 1);
        } else {
            params.averageSMods = 0;
        }

        CampaignFleetAPI tempFleet = FleetFactoryV3.createFleet(params);
        if (tempFleet == null) {
            log.warn("Failed to create procedural Support-Fleet");
            return null;
        }

        if (tempFleet.isEmpty()) {
            log.warn("Procedural Support-Fleet is empty, requested fleet size is too small (" + fleetPoints + "fp)");
        }

        return tempFleet;
    }
}