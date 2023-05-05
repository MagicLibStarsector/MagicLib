package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.magiclib.util.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Tartiflette
 */
public class MagicBountyLoader {

    public static Map<String, MagicBountySpec> BOUNTIES = new HashMap<>();
    public static boolean JSONfailed = false;
    public static String BOUNTY_FLEET_TAG = "MagicLib_Bounty_target_fleet";
    private static JSONObject bounty_data;
    private static final Logger LOG = Global.getLogger(MagicBountyLoader.class);
    private static final String BOUNTY_BOARD = "bounty_board", PATH = "data/config/modFiles/magicBounty_data.json";

    /**
     * @param id        bounty unique id
     * @param data      all the data
     * @param overwrite overwrite existing bounty with same id
     */
    public static void addBountyData(String id, MagicBountySpec data, boolean overwrite) {
        if (overwrite || !BOUNTIES.containsKey(id)) {
            BOUNTIES.put(id, data);
        }
    }

    public static MagicBountySpec getBountyData(String id) {
        if (BOUNTIES.containsKey(id)) {
            return BOUNTIES.get(id);
        } else return null;
    }

    public static void deleteBountyData(String id) {
        if (BOUNTIES.containsKey(id)) {
            BOUNTIES.remove(id);
        }
    }

    public static void clearBountyData() {
        if (MagicVariables.verbose) {
            LOG.info("Cleaning bounty board");
        }
        for (Iterator<Map.Entry<String, MagicBountySpec>> iterator = BOUNTIES.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, MagicBountySpec> entry = iterator.next();
            //cleanly remove the bounties that are NOT taken
            if (!Global.getSector().getMemoryWithoutUpdate().contains(entry.getValue().job_memKey)) {
                iterator.remove();
            } else {
                if (MagicVariables.verbose) {
                    LOG.info("Keeping active bounty: " + entry.getKey());
                }
            }
        }
    }

    public static void loadBountiesFromJSON(boolean appendOnly) {

        if (MagicVariables.verbose) {
            LOG.info("\n ######################\n\n MAGIC BOUNTIES LOADING\n\n ######################");
        }

        //load MagicBounty_data.json
        bounty_data = loadBountyData();

        if (bounty_data == null) return;

        int x = 0;
        //time to sort that stuff
        for (Iterator<String> iterator = bounty_data.keys(); iterator.hasNext(); ) {
            String bountyId = iterator.next();

            if (bountyId.isEmpty()) continue;

            if (MagicVariables.verbose) {
                LOG.info("Reading " + bountyId + " from file ");
            }

            boolean missingMod = false;
            List<String> requiredMods = getStringList(bountyId, "required_mods_id");
            if (requiredMods != null && !requiredMods.isEmpty()) {
                for (String id : requiredMods) {
                    if (!Global.getSettings().getModManager().isModEnabled(id)) {
                        if (MagicVariables.verbose) {
                            LOG.info("Skipping bounty due to missing mod: " + id);
                        }
                        missingMod = true;
                    }
                }
            }
            if (missingMod) {
                continue;
            }

            String genderString = getString(bountyId, "target_gender");
            FullName.Gender gender = null;
            if (genderString != null) {
                switch (genderString) {
                    case "MALE":
                        gender = FullName.Gender.MALE;
                        break;
                    case "FEMALE":
                        gender = FullName.Gender.FEMALE;
                        break;
                    case "UNDEFINED":
                        gender = FullName.Gender.ANY;
                        break;
                    default:
                        break;
                }
            }

            String fleet_behavior = getString(bountyId, "fleet_behavior");
            FleetAssignment order = FleetAssignment.ORBIT_AGGRESSIVE;
            if (fleet_behavior != null) {
                switch (fleet_behavior) {
                    case "PASSIVE": {
                        order = FleetAssignment.ORBIT_PASSIVE;
                        break;
                    }
                    case "AGGRESSIVE": {
                        order = FleetAssignment.DEFEND_LOCATION;
                        break;
                    }
                    case "ROAMING": {
                        order = FleetAssignment.PATROL_SYSTEM;
                        break;
                    }
                }
            }

            String target_skill_pref = getString(bountyId, "target_skill_preference");
            SkillPickPreference skillPref = SkillPickPreference.GENERIC;
            if (target_skill_pref != null && !target_skill_pref.isEmpty()) {
                switch (target_skill_pref) {
                    case "CARRIER": {
                        skillPref = SkillPickPreference.CARRIER;
                        break;
                    }
                    case "PHASE": {
                        skillPref = SkillPickPreference.PHASE;
                        break;
                    }
                }
            }

            String memKey = "$" + bountyId;
            if (getString(bountyId, "job_memKey") != null && !getString(bountyId, "job_memKey").isEmpty()) {
                memKey = getString(bountyId, "job_memKey");
            }

            float reputation = 0.05f;
            if (getInt(bountyId, "job_reputation_reward") != null) {
                reputation = (float) getInt(bountyId, "job_reputation_reward") / 100f;
            }

            String reply = MagicTxt.getString("mb_comm_reply");
            if (getString(bountyId, "job_comm_reply") == null) {
                reply = null;
            } else if (!getString(bountyId, "job_comm_reply").isEmpty()) {
                reply = getString(bountyId, "job_comm_reply");
            }

            /*
            List<String> themes = new ArrayList<>();
            if(getStringList(bountyId, "location_themes")!=null && !getStringList(bountyId, "location_themes").isEmpty()){
                themes = getStringList(bountyId, "location_themes");
                if(themes.contains("procgen_no_theme") || themes.contains("procgen_no_theme_pulsar_blackhole")){
                    themes.add("theme_misc_skip");
                    themes.add("theme_misc");
                    themes.add("theme_core_unpopulated");
                }
            }
            */

            String origin_faction = getString(bountyId, "fleet_composition_faction");
            if (origin_faction == null || origin_faction.isEmpty()) {
                origin_faction = getString(bountyId, "fleet_faction");
            }

            //Random flagship variant:                
            String flagship = getString(bountyId, "fleet_flagship_variant");
            List<String> flagshipList = getStringList(bountyId, "fleet_flagship_variant");
            if (flagshipList != null && !flagshipList.isEmpty()) {
                int i = MathUtils.getRandomNumberInRange(0, flagshipList.size() - 1);
                flagship = flagshipList.get(i);
            }

            //fixes for my own mistakes
            List<String> locations = new ArrayList<>();
            if (getStringList(bountyId, "location_marketIDs") != null) {
                locations = getStringList(bountyId, "location_marketIDs");
            } else if (getStringList(bountyId, "location_entitiesID") != null) {
                locations = getStringList(bountyId, "location_entitiesID");
            }

            //fixes for my own mistakes
            Integer minSize = getInt(bountyId, "fleet_min_FP", getInt(bountyId, "fleet_min_DP"));

            MagicBountySpec this_bounty = new MagicBountySpec(
                    getStringList(bountyId, "trigger_market_id"),
                    getStringList(bountyId, "trigger_marketFaction_any"),
                    getBoolean(bountyId, "trigger_marketFaction_alliedWith"),
                    getStringList(bountyId, "trigger_marketFaction_none"),
                    getBoolean(bountyId, "trigger_marketFaction_enemyWith"),
                    getInt(bountyId, "trigger_market_minSize"),
                    getInt(bountyId, "trigger_player_minLevel"),
                    getInt(bountyId, "trigger_min_days_elapsed"),
                    getInt(bountyId, "trigger_min_fleet_size", 0),
                    getFloat(bountyId, "trigger_weight_mult", 1f),
                    getBooleanMap(bountyId, "trigger_memKeys_all"),
                    getBooleanMap(bountyId, "trigger_memKeys_any"),
                    getBooleanMap(bountyId, "trigger_memKeys_none"),
                    getFloatMap(bountyId, "trigger_playerRelationship_atLeast"),
                    getFloatMap(bountyId, "trigger_playerRelationship_atMost"),
                    getFloat(bountyId, "trigger_giverTargetRelationship_atLeast", -99f),
                    getFloat(bountyId, "trigger_giverTargetRelationship_atMost", 99f),

                    getString(bountyId, "job_name", MagicTxt.getString("mb_unnamed")), //"Unnamed job"
                    getString(bountyId, "job_description"),
                    reply,
                    getString(bountyId, "job_intel_success"),
                    getString(bountyId, "job_intel_failure"),
                    getString(bountyId, "job_intel_expired"),
                    getString(bountyId, "job_forFaction"),
                    getString(bountyId, "job_difficultyDescription"),
                    getInt(bountyId, "job_deadline"),
                    getInt(bountyId, "job_credit_reward"),
                    getFloat(bountyId, "job_reward_scaling"),
                    reputation,
                    getIntMap(bountyId, "job_item_reward"),
                    getString(bountyId, "job_type"),
                    getBooleanDefaultTrue(bountyId, "job_show_type"),
                    getBooleanDefaultTrue(bountyId, "job_show_captain"),
                    getString(bountyId, "job_show_fleet"),
                    getString(bountyId, "job_show_distance"),
                    getBoolean(bountyId, "job_show_arrow"),
                    getString(bountyId, "job_pick_option"),
                    getString(bountyId, "job_pick_script"),
                    memKey,
                    getString(bountyId, "job_conclusion_script"),

                    getString(bountyId, "existing_target_memkey", null),

                    getString(bountyId, "target_first_name"),
                    getString(bountyId, "target_last_name"),
                    getString(bountyId, "target_portrait"),
                    gender,
                    getString(bountyId, "target_rank"),
                    getString(bountyId, "target_post"),
                    getString(bountyId, "target_personality"),
                    getString(bountyId, "target_aiCoreId"),
                    getInt(bountyId, "target_level"),
                    getInt(bountyId, "target_elite_skills", -1),
                    skillPref,
                    getIntMap(bountyId, "target_skills"),

                    getString(bountyId, "fleet_name"),
                    getString(bountyId, "fleet_faction"),
                    flagship,
                    getString(bountyId, "fleet_flagship_name"),
                    // Renamed to fleet_flagship_alwaysRecoverable, keeping previous for backwards compat with older bounties.
                    // If either is true, the flagship is always recoverable.
                    getBoolean(bountyId, "fleet_flagship_recoverable") || getBoolean(bountyId, "fleet_flagship_alwaysRecoverable"),
                    getBoolean(bountyId, "fleet_flagship_autofit"),
                    getIntMap(bountyId, "fleet_preset_ships"),
                    getBoolean(bountyId, "fleet_preset_autofit"),
                    getFloat(bountyId, "fleet_scaling_multiplier"),
                    minSize,
                    origin_faction,
                    getFloat(bountyId, "fleet_composition_quality", 1),
                    getBoolean(bountyId, "fleet_transponder"),
                    getBoolean(bountyId, "fleet_no_retreat"),
                    order,
                    getString(bountyId, "fleet_musicSetId"),

                    locations,
                    getStringList(bountyId, "location_marketFactions"),
                    getString(bountyId, "location_distance"),
                    getStringList(bountyId, "location_themes"),
                    getStringList(bountyId, "location_themes_blacklist"),
                    getStringList(bountyId, "location_entities"),
                    getBoolean(bountyId, "location_prioritizeUnexplored"),
                    //getBoolean(bountyId, "location_defaultToAnySystem"),                    
                    getBoolean(bountyId, "location_defaultToAnyEntity")
            );

            //add the bounty if it doesn't exist and hasn't been taken already or if the script is redoing the whole thing
            if ((!appendOnly || (!BOUNTIES.containsKey(bountyId) && !Global.getSector().getMemoryWithoutUpdate().contains(this_bounty.job_memKey)))) {
                BOUNTIES.put(bountyId, this_bounty);
                if (MagicVariables.verbose) {
                    LOG.info("SUCCESS");
                }
                x++;
            } else if (MagicVariables.verbose) {
                LOG.info("SKIPPED");
            }
        }

        if (MagicVariables.verbose) {
            LOG.info("Successfully loaded " + x + " bounties");
        }

        validateAndCullLoadedBounties();

        if (MagicVariables.verbose) {
            LOG.info("\n ######################\n\n MAGIC BOUNTIES LOADING COMPLETE\n\n ######################");
        }
    }

    static void validateAndCullLoadedBounties() {
        LOG.info("\n ######################\n\n VALIDATING BOUNTIES \n\n ######################");
        int valid = 0;
        List<String> culling = new ArrayList<>();
        for (Map.Entry<String, MagicBountySpec> dataEntry : MagicBountyLoader.BOUNTIES.entrySet()) {
            boolean isValid = validateAndCorrectIds(dataEntry.getKey(), dataEntry.getValue());
            if (isValid) {
                valid++;
            } else {
                culling.add(dataEntry.getKey());
            }
        }
        if (!culling.isEmpty()) {
            for (String id : culling) {
                MagicBountyLoader.BOUNTIES.remove(id);
            }
        }
        LOG.info("Successfully validated " + valid + " bounties!");
        LOG.info("\n ######################\n\n  VALIDATING BOUNTIES COMPLETED  \n\n ######################");
    }

    private static boolean validateAndCorrectIds(String bountyId, MagicBountySpec this_bounty) {
        try {
            // fleet_faction
            if (MagicTxt.nullStringIfEmpty(this_bounty.fleet_faction) != null) {
                String fleetFactionId = this_bounty.fleet_faction;
                FactionAPI faction = MagicStringMatcher.findBestFactionMatch(fleetFactionId);

                if (faction == null) {
                    //that faction couldn't be found, invalidating the bounty
                    LOG.info(String.format("Unable to find faction '%s' from bounty %s. Bounty is INVALID!", fleetFactionId, bountyId));
                    return false;
                } else if (!Objects.equals(faction.getId(), fleetFactionId)) {
                    LOG.info(String.format("Corrected faction id '%s' to '%s' in bounty %s.", fleetFactionId, faction.getId(), bountyId));
                    this_bounty.fleet_faction = faction.getId();
                }
            } else
                //this is a mandatory value if the bounty is not placed on an existing fleet
                if (MagicTxt.nullStringIfEmpty(this_bounty.existing_target_memkey) == null) {
                    LOG.info(String.format("Invalid '%s' bounty, missing fleet_faction", bountyId));
                    return false;
                }

            // fleet_composition_faction
            if (MagicTxt.nullStringIfEmpty(this_bounty.fleet_composition_faction) != null) {
                String compositionFactionId = this_bounty.fleet_composition_faction;
                FactionAPI fleetCompositionFaction = MagicStringMatcher.findBestFactionMatch(compositionFactionId);

                if (fleetCompositionFaction == null) {
                    //that faction couldn't be found, invalidating the bounty
                    LOG.info(String.format("Unable to find fleet_composition_faction '%s' from bounty %s. Bounty is INVALID!", compositionFactionId, bountyId));
                    return false;
                } else if (!Objects.equals(fleetCompositionFaction.getId(), compositionFactionId)) {
                    LOG.info(String.format("Corrected fleet_composition_faction '%s' to '%s' in bounty %s.", compositionFactionId, fleetCompositionFaction.getId(), bountyId));
                    this_bounty.fleet_composition_faction = fleetCompositionFaction.getId();
                }
            }

            // job_forFaction
            if (MagicTxt.nullStringIfEmpty(this_bounty.job_forFaction) != null) {
                String job_forFactionId = this_bounty.job_forFaction;
                FactionAPI job_forFaction = MagicStringMatcher.findBestFactionMatch(job_forFactionId);

                if (job_forFaction == null) {
                    //that faction couldn't be found, invalidating the bounty
                    LOG.info(String.format("Unable to find job_forFaction '%s' from bounty %s. Bounty is INVALID!", job_forFactionId, bountyId));
                    return false;
                } else if (!Objects.equals(job_forFaction.getId(), job_forFactionId)) {
                    LOG.info(String.format("Corrected job_forFactionId '%s' to '%s' in bounty %s.", job_forFactionId, job_forFaction.getId(), bountyId));
                    this_bounty.job_forFaction = job_forFaction.getId();
                }
            }

            // location_marketFactions
            if (this_bounty.location_marketFactions != null && !this_bounty.location_marketFactions.isEmpty()) {
                List<String> location_marketFactions = this_bounty.location_marketFactions;

                for (int i = location_marketFactions.size() - 1; i >= 0; i--) {
                    String location_marketFactionId = location_marketFactions.get(i);
                    FactionAPI location_marketFaction = MagicStringMatcher.findBestFactionMatch(location_marketFactionId);

                    if (location_marketFaction == null) {
                        //that faction couldn't be found, invalidating the bounty
                        LOG.info(String.format("Unable to find location_marketFactions '%s' from bounty %s. Bounty is INVALID!", location_marketFactionId, bountyId));
                        return false;
                    } else if (!Objects.equals(location_marketFaction.getId(), location_marketFactionId)) {
                        LOG.info(String.format("Corrected location_marketFactionId '%s' to '%s' in bounty %s.", location_marketFactionId, location_marketFaction.getId(), bountyId));
                        this_bounty.location_marketFactions.remove(i);
                        this_bounty.location_marketFactions.add(location_marketFaction.getId());
                    }
                }
            }

            // trigger_marketFaction_any
            if (this_bounty.trigger_marketFaction_any != null && !this_bounty.trigger_marketFaction_any.isEmpty()) {
                List<String> trigger_marketFaction_any = this_bounty.trigger_marketFaction_any;

                for (int i = trigger_marketFaction_any.size() - 1; i >= 0; i--) {
                    String trigger_marketFaction_anyId = trigger_marketFaction_any.get(i);
                    FactionAPI trigger_marketFaction = MagicStringMatcher.findBestFactionMatch(trigger_marketFaction_anyId);

                    if (trigger_marketFaction == null) {
                        //that faction couldn't be found, invalidating the bounty
                        LOG.info(String.format("Unable to find trigger_marketFaction_any '%s' from bounty %s. Bounty is INVALID!", trigger_marketFaction_anyId, bountyId));
                        return false;
                    } else if (!Objects.equals(trigger_marketFaction.getId(), trigger_marketFaction_anyId)) {
                        LOG.info(String.format("Corrected trigger_marketFaction_any '%s' to '%s' in bounty %s.", trigger_marketFaction_anyId, trigger_marketFaction.getId(), bountyId));
                        this_bounty.trigger_marketFaction_any.remove(i);
                        this_bounty.trigger_marketFaction_any.add(trigger_marketFaction.getId());
                    }
                }
            }

            // trigger_marketFaction_none
            if (this_bounty.trigger_marketFaction_none != null && !this_bounty.trigger_marketFaction_none.isEmpty()) {
                List<String> trigger_marketFaction_none = this_bounty.trigger_marketFaction_none;

                for (int i = trigger_marketFaction_none.size() - 1; i >= 0; i--) {
                    String trigger_marketFaction_noneId = trigger_marketFaction_none.get(i);
                    FactionAPI trigger_marketFaction = MagicStringMatcher.findBestFactionMatch(trigger_marketFaction_noneId);

                    if (trigger_marketFaction == null) {
                        //that faction couldn't be found, invalidating the bounty
                        LOG.info(String.format("Unable to find trigger_marketFaction_none '%s' from bounty %s. Bounty is INVALID!", trigger_marketFaction_noneId, bountyId));
                        return false;
                    } else if (!Objects.equals(trigger_marketFaction.getId(), trigger_marketFaction_noneId)) {
                        LOG.info(String.format("Corrected trigger_marketFaction_none '%s' to '%s' in bounty %s.", trigger_marketFaction_noneId, trigger_marketFaction.getId(), bountyId));
                        this_bounty.trigger_marketFaction_none.remove(i);
                        this_bounty.trigger_marketFaction_none.add(trigger_marketFaction.getId());
                    }
                }
            }

            // trigger_playerRelationship_atLeast
            if (this_bounty.trigger_playerRelationship_atLeast != null && !this_bounty.trigger_playerRelationship_atLeast.isEmpty()) {
                Map<String, Float> trigger_playerRelationship_atLeast = this_bounty.trigger_playerRelationship_atLeast;
                Map<String, Float> validIDs = new HashMap<>();
                for (String f : trigger_playerRelationship_atLeast.keySet()) {
                    FactionAPI trigger_playerRelationship_faction = MagicStringMatcher.findBestFactionMatch(f);

                    if (trigger_playerRelationship_faction == null) {
                        //that faction couldn't be found, invalidating the bounty
                        LOG.info(String.format("Unable to find trigger_playerRelationship_atLeast '%s' from bounty %s. Bounty is INVALID!", f, bountyId));
                        return false;
                    } else {
                        if (!Objects.equals(trigger_playerRelationship_faction.getId(), f)) {
                            LOG.info(String.format("Corrected trigger_playerRelationship_atLeast '%s' to '%s' in bounty %s.", f, trigger_playerRelationship_faction.getId(), bountyId));
                        }
                        validIDs.put(trigger_playerRelationship_faction.getId(), trigger_playerRelationship_atLeast.get(f));
                    }

                }
                this_bounty.trigger_playerRelationship_atLeast = validIDs;
            }

            // trigger_playerRelationship_atMost
            if (this_bounty.trigger_playerRelationship_atMost != null && !this_bounty.trigger_playerRelationship_atMost.isEmpty()) {
                Map<String, Float> trigger_playerRelationship_atMost = this_bounty.trigger_playerRelationship_atMost;
                Map<String, Float> validIDs = new HashMap<>();
                for (String f : trigger_playerRelationship_atMost.keySet()) {
                    FactionAPI trigger_playerRelationship_faction = MagicStringMatcher.findBestFactionMatch(f);

                    if (trigger_playerRelationship_faction == null) {
                        //that faction couldn't be found, invalidating the bounty
                        LOG.info(String.format("Unable to find trigger_playerRelationship_atMost '%s' from bounty %s. Bounty is INVALID!", f, bountyId));
                        return false;
                    } else {
                        if (!Objects.equals(trigger_playerRelationship_faction.getId(), f)) {
                            LOG.info(String.format("Corrected trigger_playerRelationship_atMost '%s' to '%s' in bounty %s.", f, trigger_playerRelationship_faction.getId(), bountyId));
                        }
                        validIDs.put(trigger_playerRelationship_faction.getId(), trigger_playerRelationship_atMost.get(f));
                    }

                }
                this_bounty.trigger_playerRelationship_atMost = validIDs;
            }

            //OTHER REQUIREMENT CHECKS
            if (MagicTxt.nullStringIfEmpty(this_bounty.existing_target_memkey) == null) {
                if (MagicTxt.nullStringIfEmpty(this_bounty.fleet_flagship_variant) == null) {
                    //No flagship variant, invalidating the bounty
                    LOG.info(String.format("Missing fleet_flagship_variant from bounty %s. Bounty is INVALID!", bountyId));
                    return false;
                }

                if (Global.getSettings().getVariant(this_bounty.fleet_flagship_variant) == null && MagicCampaign.loadVariant(MagicVariables.VARIANT_PATH + this_bounty.fleet_flagship_variant + ".variant") == null) {
                    //that flagship variant couldn't be found, invalidating the bounty
                    LOG.info(String.format("Missing fleet_flagship_variant '%s' from bounty %s. Bounty is INVALID!", this_bounty.fleet_flagship_variant, bountyId));
                    return false;
                }

                if (this_bounty.fleet_preset_ships != null && !this_bounty.fleet_preset_ships.isEmpty()) {
                    //check all reinforcement variants
                    for (String v : this_bounty.fleet_preset_ships.keySet()) {
                        if (Global.getSettings().getVariant(v) == null && MagicCampaign.loadVariant(MagicVariables.VARIANT_PATH + v + ".variant") == null) {
                            //that reinforcement variant couldn't be found, invalidating the bounty
                            LOG.info(String.format("Missing fleet_preset_ships variant '%s' from bounty %s. Bounty is INVALID!", v, bountyId));
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Something went wrong when validating " + bountyId, e);
        }

        LOG.info(bountyId + " : VALID");

        return true;
    }

    /**
     * Loads a bounty list from modSettings.json while respecting their mod requirements
     */
    private static List<String> readBountyList(boolean verbose) {

        //load the list of bounties that should be loaded, as well as their mod requirements
        Map<String, List<String>> bountiesWithRequirements = new HashMap<>();

        JSONObject localCopy = MagicSettings.modSettings;

        try {
            JSONObject reqSettings = localCopy.getJSONObject(MagicVariables.MAGICLIB_ID);
            //try to get the requested value
            if (reqSettings.has(BOUNTY_BOARD)) {
                JSONObject bountiesList = reqSettings.getJSONObject(BOUNTY_BOARD);
                if (bountiesList.length() > 0) {
                    for (Iterator<?> iter = bountiesList.keys(); iter.hasNext(); ) {
                        //bounty id
                        String key = (String) iter.next();
                        //bounty requirements
                        List<String> values = new ArrayList<>();
                        JSONArray requirementList = bountiesList.getJSONArray(key);
                        if (requirementList.length() > 0) {
                            for (int i = 0; i < requirementList.length(); i++) {
                                values.add(requirementList.getString(i));
                            }
                        }
                        bountiesWithRequirements.put(key, values);
                    }
                }
            } else {
                LOG.error("MagicBountyData is unable to find " + BOUNTY_BOARD + " within " + MagicVariables.MAGICLIB_ID + " in modSettings.json");
            }
        } catch (JSONException ex) {
            LOG.error("MagicBountyData is unable to read the content of " + MagicVariables.MAGICLIB_ID + " in modSettings.json", ex);
        }

        List<String> bountiesAvailable = new ArrayList<>();

        for (String id : bountiesWithRequirements.keySet()) {
            if (bountiesWithRequirements.get(id).isEmpty()) {
                //no requirement
                bountiesAvailable.add(id);
                if (verbose) {
                    LOG.info("Bounty " + id + " will be loaded.");
                }
            } else {
                //check if all the required mods are active
                boolean missingRequirement = false;
                for (String required : bountiesWithRequirements.get(id)) {
                    if (!Global.getSettings().getModManager().isModEnabled(required)) {
                        missingRequirement = true;
                        if (verbose) {
                            LOG.info("Bounty " + id + " is unavailable, missing " + required);
                        }
                        break;
                    }
                }
                if (!missingRequirement) {
                    bountiesAvailable.add(id);
                    //log if devMode is active
                    if (verbose) {
                        LOG.info("Bounty " + id + " will be loaded.");
                    }
                }
            }
        }
        //only return bounties that have no other mod requirement, or all of them are present
        return bountiesAvailable;
    }

    /**
     * Load the bounty data file
     */
    private static JSONObject loadBountyData() {
        JSONObject this_bounty_data = null;
        try {
            this_bounty_data = Global.getSettings().getMergedJSONForMod(PATH, MagicVariables.MAGICLIB_ID);
        } catch (IOException | JSONException ex) {
            LOG.fatal("MagicBountyData is unable to read magicBounty_data.json", ex);
            JSONfailed = true;
        }
        return this_bounty_data;
    }

    private static boolean getBoolean(String bountyId, String key) {
        boolean value = false;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                value = reqSettings.getBoolean(key);
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static boolean getBooleanDefaultTrue(String bountyId, String key) {
        boolean value = true;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                value = reqSettings.getBoolean(key);
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static String getString(String bountyId, String key) {
        return getString(bountyId, key, null);
    }

    private static String getString(String bountyId, String key, String defaultValue) {
        String value = defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                value = reqSettings.getString(key);
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Integer getInt(String bountyId, String key) {
        return getInt(bountyId, key, -1);
    }

    private static Integer getInt(String bountyId, String key, int defaultValue) {
        int value = defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                value = reqSettings.getInt(key);
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Float getFloat(String bountyId, String key) {
        return getFloat(bountyId, key, -1);
    }

    private static Float getFloat(String bountyId, String key, float defaultValue) {
        float value = defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                value = (float) reqSettings.getDouble(key);
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static List<String> getStringList(String bountyId, String key) {
        List<String> value = new ArrayList<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                JSONArray list = reqSettings.getJSONArray(key);
                if (list.length() > 0) {
                    for (int i = 0; i < list.length(); i++) {
                        value.add(list.getString(i));
                    }
                }
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Map<String, Boolean> getBooleanMap(String bountyId, String key) {
        Map<String, Boolean> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                JSONObject list = reqSettings.getJSONObject(key);
                if (list.length() > 0) {
                    for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                        String this_key = (String) iter.next();
                        boolean this_data = list.getBoolean(this_key);
                        value.put(this_key, this_data);
                    }
                }
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Map<String, Float> getFloatMap(String bountyId, String key) {
        Map<String, Float> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                JSONObject list = reqSettings.getJSONObject(key);
                if (list.length() > 0) {
                    for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                        String this_key = (String) iter.next();
                        float this_data = (float) list.getDouble(this_key);
                        value.put(this_key, this_data);
                    }
                }
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Map<String, Integer> getIntMap(String bountyId, String key) {
        Map<String, Integer> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if (reqSettings.has(key)) {
                JSONObject list = reqSettings.getJSONObject(key);
                if (list.length() > 0) {
                    for (Iterator<?> iter = list.keys(); iter.hasNext(); ) {
                        String this_key = (String) iter.next();
                        int this_data = (int) list.getDouble(this_key);
                        value.put(this_key, this_data);
                    }
                }
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    private static Map<String, List<String>> getListMap(String bountyId, String key) {
        Map<String, List<String>> value = new HashMap<>();
        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            //get object
            if (reqSettings.has(key)) {
                //get key list
                JSONObject keyList = reqSettings.getJSONObject(key);
                if (keyList.length() > 0) {
                    for (Iterator<?> iter = keyList.keys(); iter.hasNext(); ) {
                        String this_key = (String) iter.next();
                        //get list of values for each key
                        JSONArray data_list = keyList.getJSONArray(key);
                        List<String> parsed_list = new ArrayList<>();
                        if (data_list.length() > 0) {
                            for (int i = 0; i < data_list.length(); i++) {
                                //turn json list into array list
                                parsed_list.add(data_list.getString(i));
                            }
                        }
                        value.put(this_key, parsed_list);
                    }
                }
            }
        } catch (JSONException ex) {
        }

        return value;
    }

    /**
     * The type of bounty; defines criteria for successful completion.
     */
    public enum JobType {
        /**
         * Required only to disable the flagship.
         */
        Assassination,
        /**
         * Requires the complete destruction of the flagship without recovery.
         */
        Destruction,
        /**
         * Requires the complete destruction or disabling of the enemy fleet
         */
        Obliteration,
        /**
         * Requires the destruction or disabling of 2/3rd of the enemy fleet.
         */
        Neutralisation,
    }

    public enum ShowFleet {
        None,
        Text,
        Flagship,
        FlagshipText,
        Preset,
        PresetText,
        Vanilla,
        All
    }

    public enum ShowDistance {
        None,
        Vague,
        Vanilla,
        Distance,
        System,
        VanillaDistance,
        Exact
    }

    public enum Gender {
        Undefined,
        Any,
        Male,
        Female
    }

}
