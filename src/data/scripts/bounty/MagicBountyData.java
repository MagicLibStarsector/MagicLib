package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import data.scripts.util.MagicSettings;
import data.scripts.util.MagicTxt;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Tartiflette
 */
public class MagicBountyData {

    public static Map<String,bountyData> BOUNTIES = new HashMap<>();
    public static boolean JSONfailed=false;
    public static String BOUNTY_FLEET_TAG = "MagicLib_Bounty_target_fleet";
    private static JSONObject bounty_data;
    private static final Logger LOG = Global.getLogger(MagicSettings.class);
    public static boolean verbose = false;
    private static final String MOD = "MagicLib", BOUNTY_BOARD = "bounty_board", PATH = "data/config/modFiles/magicBounty_data.json";

    /**
     * @param id
     * bounty unique id
     * @param data
     * all the data
     * @param overwrite
     * overwrite existing bounty with same id
     */
    public static void addBountyData(String id, bountyData data, boolean overwrite){
        if(overwrite || !BOUNTIES.containsKey(id)){
            BOUNTIES.put(id, data);
        }
    }

    public static bountyData getBountyData(String id){
        if(BOUNTIES.containsKey(id)){
            return BOUNTIES.get(id);
        } else return null;
    }

    public static void deleteBountyData(String id){
        if(BOUNTIES.containsKey(id)){
            BOUNTIES.remove(id);
        }
    }

    public static void loadBountiesFromJSON(boolean appendOnly){

        if(Global.getSettings().isDevMode())verbose=true;
        if(verbose){
            LOG.info("\n ######################\n\n MAGIC BOUNTIES LOADING\n\n ######################");
        }

        //this will be a long one
        if(!appendOnly){
            BOUNTIES.clear();
            if(verbose){
                LOG.info("Clearing bounty board");
            }
        }

        //get the list of bounties that need to be created from modSettings.json        
        List<String> bountiesToLoad = readBountyList(verbose);
        if(verbose){
            LOG.info("      Loading " + bountiesToLoad.size() + " bounties.");
        }
        //load MagicBounty_data.json
        bounty_data = loadBountyData();

        int x=0;
        //time to sort that stuff
        for(String bountyId : bountiesToLoad){
            if(verbose){
                LOG.info("Reading "+bountyId+" from file ");
            }
            if(bounty_data.has(bountyId)){

                String genderString = getString(bountyId, "target_gender");
                FullName.Gender gender = null;
                if(genderString!=null){
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
                if(fleet_behavior!=null){
                    switch (fleet_behavior){
                        case "PASSIVE": {
                            order=FleetAssignment.ORBIT_PASSIVE;
                            break;
                        }
                        case "AGGRESSIVE":{
                            order=FleetAssignment.DEFEND_LOCATION;
                            break;
                        }
                        case "ROAMING":{
                            order=FleetAssignment.PATROL_SYSTEM;
                            break;
                        }
                    }
                }

                String target_skill_pref = getString(bountyId, "target_skill_preference");
                SkillPickPreference skillPref = SkillPickPreference.GENERIC;
                if(target_skill_pref!=null && !target_skill_pref.isEmpty()){
                    switch (target_skill_pref){
                        case "CARRIER" :{
                            skillPref=SkillPickPreference.CARRIER;
                            break;
                        }
                        case "PHASE" :{
                            skillPref=SkillPickPreference.PHASE;
                            break;
                        }
                    }
                }

                String memKey = "$"+bountyId;
                if(getString(bountyId, "job_memKey")!=null && !getString(bountyId, "job_memKey").isEmpty()){
                    memKey = getString(bountyId, "job_memKey");
                }

                float reputation = 0.05f;
                if(getInt(bountyId, "job_reputation_reward")!=null){
                    reputation = (float)getInt(bountyId, "job_reputation_reward")/100f;
                }

                String reply = MagicTxt.getString("mb_comm_reply");
                if(getString(bountyId,"job_comm_reply")!=null && !getString(bountyId,"job_comm_reply").isEmpty()){
                    reply = getString(bountyId,"job_comm_reply");
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
                
                bountyData this_bounty = new bountyData(
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
                        getFloatMap(bountyId, "trigger_playerRelationship_atLeast"),
                        getFloatMap(bountyId, "trigger_playerRelationship_atMost"),

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
                        getBoolean(bountyId, "job_show_captain"),
                        getString(bountyId, "job_show_fleet"),
                        getString(bountyId, "job_show_distance"),
                        getBoolean(bountyId, "job_show_arrow"),
                        getString(bountyId, "job_pick_option"),
                        getString(bountyId, "job_pick_script"),
                        memKey,
                        getString(bountyId, "job_conclusion_script"),

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
                        getString(bountyId, "fleet_flagship_variant"),
                        getString(bountyId, "fleet_flagship_name"),
                        getBoolean(bountyId, "fleet_flagship_recoverable"),
                        getIntMap(bountyId, "fleet_preset_ships"),
                        getFloat(bountyId, "fleet_scaling_multiplier"),
                        getInt(bountyId, "fleet_min_DP"),
                        getString(bountyId, "fleet_composition_faction"),
                        getFloat(bountyId, "fleet_composition_quality"),
                        getBoolean(bountyId, "fleet_transponder"),
                        order,

                        getStringList(bountyId, "location_marketIDs"),
                        getStringList(bountyId, "location_marketFactions"),
                        getString(bountyId, "location_distance"),
                        getStringList(bountyId, "location_themes"),
                        getStringList(bountyId, "location_themes_blacklist"),
                        getStringList(bountyId, "location_entities"),
                        getBoolean(bountyId, "location_prioritizeUnexplored"),
                        getBoolean(bountyId, "location_defaultToAnyEntity")
                );

                //add the bounty if it doesn't exist and hasn't been taken already or if the script is redoing the whole thing
                if((!appendOnly || (!BOUNTIES.containsKey(bountyId) && !Global.getSector().getMemoryWithoutUpdate().contains(this_bounty.job_memKey)))){
                    BOUNTIES.put(bountyId, this_bounty);
                    if(verbose){
                        LOG.info("SUCCESS");
                    }
                    x++;
                } else if(verbose){
                    LOG.info("SKIPPED");
                }
            }
        }

        if(verbose){
            LOG.info("Successfully loaded "+x+" bounties");
        }
    }

    /**
     * The code representation of a bounty json definition.
     */
    public static class bountyData {

        //trigger parameters                                                    //ALL OPTIONAL
        @NotNull public List <String> trigger_market_id;                        //will default to the other preferences if those are defined and the location doesn't exists due to Nexerelin random mode
        @NotNull public List <String> trigger_marketFaction_any;
        public boolean trigger_marketFaction_alliedWith;                        //visited market is at least neutral with one those factions
        @NotNull public List <String> trigger_marketFaction_none;
        public boolean trigger_marketFaction_enemyWith;                         //visited market is at best inhospitable with all those factions
        public int trigger_market_minSize;
        public int trigger_player_minLevel;
        public int trigger_min_days_elapsed;
        public int trigger_min_fleet_size;
        public float trigger_weight_mult;                                       //simple frequency multiplier
        @NotNull public Map <String,Boolean> trigger_memKeys_all;
        @NotNull public Map <String,Boolean> trigger_memKeys_any;
        @NotNull public Map <String,Float> trigger_playerRelationship_atLeast;  //minimal player relationship with those factions
        @NotNull public Map <String,Float> trigger_playerRelationship_atMost;   //maximum player relationship with those factions
        //job description
        public String job_name;                                                 //job name in the dialog pick list
        public String job_description;                                          //full text of the bounty offer, the description will handle some text variables such as "$he_or_she". See documentation for more details
        public String job_comm_reply;                                           //Reply of the enemy to your hail, default to "The other $shipOrFleet does not answer to you hails."
        public String job_intel_success;                                        //short text added to the Intel object after the job has been successfully completed
        public String job_intel_failure;                                        //short text added to the Intel object after the job has been failed
        public String job_intel_expired;                                        //short text added to the Intel object after the job has been left to expire
        public String job_forFaction;                                           //successfully completing this mission with give a small reputation reward with this faction
        @Nullable public String job_difficultyDescription;                      // "none": no description, "auto": bounty board describes how dangerous the bounty is, any other text: bounty board displays the text
        public int job_deadline;
        public int job_credit_reward;
        public float job_credit_scaling;                                        //only used with fleet scaling: total reward = job_credits_reward * (job_reward_scaling * (bounty fleet DP / fleet_minimal_DP) )
        public float job_reputation_reward;
        @NotNull public Map <String,Integer> job_item_reward;
        public JobType job_type;                                                // assassination, destruction, obliteration, neutralisation
                                                                                // assassination: requires only to disable the flagship
                                                                                // destruction: requires the complete destruction of the flagship without recovery
                                                                                // obliteration: requires the complete destruction or disabling of the enemy fleet
                                                                                // neutralisation: requires the destruction or disabling of 2/3rd of the enemy fleet
                                                                                // default to assassination
        public boolean job_show_type;
        public boolean job_show_captain;
        public ShowFleet job_show_fleet;                                        // "none", "text",  "flagship", "flagshipText", "preset", "presetText", "vanilla" or "all"
                                                                                // how much of the fleet to show on the bounty board. default: none
                                                                                // text: "the target has a massive fleet comprised of around 15 ships."
                                                                                // flagship: only shows an image of the flagship
                                                                                // flagshipText: shows an image of the flagships and a text with the number of other ships
                                                                                // preset: only show an image of the Flagship and the preset fleet
                                                                                // presetText: show an image of the Flagship and the preset fleet, plus a text with the number of other ships
                                                                                // vanilla: shows the Flagship and the 9 biggest ships of the fleet, plus a text with the number of other ships
                                                                                // all: show an image of all the ships in the fleet.
        public ShowDistance job_show_distance;                                  // "none", "vague", "distance", "vanilla", "vanillaDistance"
                                                                                // how precisely the distance to the target is shown on the bounty board. default: none
                                                                                // vague: "The target is located somewhere in the vicinity of the core worlds."
                                                                                // distance: "It is located roughly %s LY away from your current position."
                                                                                // vanilla: "The target is located near a giant in a system with a yellow primary star, in the Nebulon constellation."
                                                                                // vanillaDistance: "The target is located near a giant in a system with a yellow primary star, in the Nebulon constellation. It is located roughly %s LY away from your current position."
        public boolean job_show_arrow;
        public String job_pick_option;                                          //dialog text to pick the job
        public String job_pick_script;                                          //optional, can be used to trigger further scripts when the mission is taken, for example you may want to have competing bounty hunters
        public String job_memKey;                                               //optional, MemKey set to false is added when accepting the job, set to true if the job is sucessful
        public String job_conclusion_script;                                    //optional, can be used to give additional rewards or add further consequences in case of failure using memkeys to check the outcome
        //bounty target                                                         //ALL OPTIONAL
        public String target_first_name;
        public String target_last_name;
        public String target_portrait;                                          //id of the sprite in settings.json/graphics/characters
        public FullName.Gender target_gender;                                   //MALE, FEMALE, ANY
        public String target_rank;                                              //rank from campaign.ids.Ranks
        public String target_post;                                              //post from campaign.ids.Ranks
        public String target_personality;                                       //personality from campaign.ids.Personalities
        public String target_aiCoreId;                                          //if properly set, turn the target into a AI, makes it drop AI cores
        public int target_level;
        public int target_elite_skills;                                         //Overrides the regular number of elite skills, set to -1 to ignore.
        public SkillPickPreference target_skill_preference;                     //GENERIC, PHASE, CARRIER, ANY from OfficerManagerEvent.SkillPickPreference
        public Map <String,Integer> target_skills;                              //OVERRIDES ALL RANDOM SKILLS!
        //bounty fleet
        public String fleet_name;
        public String fleet_faction;                                            //faction of the fleet once it is generated, but not necessarily the faction of the ships inside
        public String fleet_flagship_variant;
        public String fleet_flagship_name;                                      //optional
        public boolean fleet_flagship_recoverable;
        public Map <String,Integer> fleet_preset_ships;                         //optional preset fleet generated with the flagship, [variantId:number_of_ships]
        public float fleet_scaling_multiplier;                                  //dynamic reinforcements to match that amount of player fleet DP, set to 0 to ignore
        public int fleet_min_DP;
        public String fleet_composition_faction;                                //Faction of the extra ship, can be different from the bounty faction (in case of pirate deserters for example)
        public float fleet_composition_quality;                                 //default to 2 (no Dmods) if <0
        public boolean fleet_transponder;
        public FleetAssignment fleet_behavior;                                  //PASSIVE, GUARDED, AGGRESSIVE, ROAMING, default to GUARDED (campaign.FleetAssignment.orbit_aggressive)
        //location
        public List<String> location_marketIDs;                                 //preset location, can default to the other preferences if those are defined and the location doesn't exists due to Nexerelin random mode
        public List<String> location_marketFactions;                            //takes precedence over all other parameters but market ids
        public String location_distance;                                        //prefered distance, "CORE", "CLOSE" or "FAR". Can be left empty to ignore.
        public List<String> location_themes;                                    //campaign.ids.Tags + "procgen_no_theme" + "procgen_no_theme_pulsar_blackhole"
        public List<String> location_themes_blacklist;
        public List<String> location_entities;                                  //PLANET, GATE, STATION, STABLE_LOCATION, DEBRIS, WRECK, PROBE.
        public boolean location_prioritizeUnexplored;                           //will pick in priority systems that have not been visited by the player yet, but won't override the distance requirements
        public boolean location_defaultToAnyEntity;                             //if true and no suitable entity is found in systems with required themes and distance, a random entity will be picked instead.
                                                                                //if false, the script will ignore the distance requirement to attempt to find a suitable system
        public bountyData(
            List <String> trigger_market_id,
            List <String> trigger_marketFaction_any,
            boolean trigger_marketFaction_alliedWith,
            List <String> trigger_marketFaction_none,
            boolean trigger_marketFaction_enemyWith,
            int trigger_market_minSize,
            int trigger_player_minLevel,
            int trigger_min_days_elapsed,
            int trigger_min_fleet_size,
            float trigger_weight_mult,
            Map <String,Boolean> trigger_memKeys_all,
            Map <String,Boolean> trigger_memKeys_any,
            Map <String,Float> trigger_playerRelationship_atLeast,
            Map <String,Float> trigger_playerRelationship_atMost,
            String job_name,
            String job_description,
            String job_comm_reply,
            String job_intel_success,
            String job_intel_failure,
            String job_intel_expired,
            String job_forFaction,
            String job_difficultyDescription,
            int job_deadline,
            int job_credit_reward,
            float job_credit_scaling,
            float job_reputation_reward,
            Map <String,Integer> job_item_reward,
            String job_type,
            boolean job_show_type,
            boolean job_show_captain,
            String job_show_fleet,
            String job_show_distance,
            boolean job_show_arrow,
            String job_pick_option,
            String job_pick_script,
            String job_memKey,
            String job_conclusion_script,
            String target_first_name,
            String target_last_name,
            String target_portrait,
            FullName.Gender target_gender,
            String target_rank,
            String target_post,
            String target_personality,
            String target_aiCoreId,
            int target_level,
            int target_elite_skills,
            SkillPickPreference target_skill_preference,
            Map <String,Integer> target_skills,
            String fleet_name,
            String fleet_faction,
            String fleet_flagship_variant,
            String fleet_flagship_name,
            boolean fleet_flagship_recoverable,
            Map <String,Integer> fleet_preset_ships,
            float fleet_scaling_multiplier,
            int fleet_min_DP,
            String fleet_composition_faction,
            float fleet_composition_quality,
            boolean fleet_transponder,
            FleetAssignment fleet_behavior,
            List<String> location_marketIDs,
            List<String> location_marketFactions,
            String location_distance,
            List<String> location_themes,
            List<String> location_themes_blacklist,
            List<String> location_entities,
            boolean location_prioritizeUnexplored,
            boolean location_defaultToAnyEntity
        ) {
            this.trigger_market_id = trigger_market_id;
            this.trigger_marketFaction_any = trigger_marketFaction_any;
            this.trigger_marketFaction_alliedWith = trigger_marketFaction_alliedWith;
            this.trigger_marketFaction_none = trigger_marketFaction_none;
            this.trigger_marketFaction_enemyWith = trigger_marketFaction_enemyWith;
            this.trigger_market_minSize = trigger_market_minSize;
            this.trigger_player_minLevel = trigger_player_minLevel;
            this.trigger_min_days_elapsed = trigger_min_days_elapsed;
            this.trigger_min_fleet_size = trigger_min_fleet_size;
            this.trigger_weight_mult = trigger_weight_mult;
            this.trigger_memKeys_all = trigger_memKeys_all;
            this.trigger_memKeys_any = trigger_memKeys_any;
            this.trigger_playerRelationship_atLeast = trigger_playerRelationship_atLeast;
            this.trigger_playerRelationship_atMost = trigger_playerRelationship_atMost;
            this.job_name = job_name;
            this.job_description = job_description;
            this.job_comm_reply = job_comm_reply;
            this.job_intel_success = job_intel_success;
            this.job_intel_failure = job_intel_failure;
            this.job_intel_expired = job_intel_expired;
            this.job_forFaction = job_forFaction;
            this.job_difficultyDescription = job_difficultyDescription;
            this.job_deadline = job_deadline;
            this.job_credit_reward = job_credit_reward;
            this.job_credit_scaling = job_credit_scaling;
            this.job_reputation_reward = job_reputation_reward;
            this.job_item_reward = job_item_reward;
            if(job_type !=null){
                if (job_type.equalsIgnoreCase("assassination")) {
                    this.job_type = JobType.Assassination;
                } else if (job_type.equalsIgnoreCase("destruction")) {
                    this.job_type = JobType.Destruction;
                } else if (job_type.equalsIgnoreCase("obliteration")) {
                    this.job_type = JobType.Obliteration;
                } else if (job_type.equalsIgnoreCase("neutralisation")) {
                    this.job_type = JobType.Neutralisation;
                } else {
                    this.job_type = JobType.Assassination;
                }
            } else {
                this.job_type = JobType.Assassination;
            }
            this.job_show_type = job_show_type;
            this.job_show_captain = job_show_captain;

            if (job_show_fleet != null) {
                if (job_show_fleet.equalsIgnoreCase("all")) {
                    this.job_show_fleet = ShowFleet.All;
                } else if (job_show_fleet.equalsIgnoreCase("preset")) {
                    this.job_show_fleet = ShowFleet.Preset;
                } else if (job_show_fleet.equalsIgnoreCase("presetText")) {
                    this.job_show_fleet = ShowFleet.PresetText;
                } else if (job_show_fleet.equalsIgnoreCase("flagship")) {
                    this.job_show_fleet = ShowFleet.Flagship;
                } else if (job_show_fleet.equalsIgnoreCase("flagshipText")) {
                    this.job_show_fleet = ShowFleet.FlagshipText;
                } else if (job_show_fleet.equalsIgnoreCase("vanilla")) {
                    this.job_show_fleet = ShowFleet.Vanilla;
                } else {
                    this.job_show_fleet = ShowFleet.None;
                }
            } else {
                this.job_show_fleet = ShowFleet.None;
            }

            if (job_show_distance != null) {
                if (job_show_distance.equalsIgnoreCase("vague")) {
                    this.job_show_distance = ShowDistance.Vague;
                } else if (job_show_distance.equalsIgnoreCase("distance")) {
                    this.job_show_distance = ShowDistance.Distance;
                } else if (job_show_distance.equalsIgnoreCase("vanilla")) {
                    this.job_show_distance = ShowDistance.Vanilla;
                } else if (job_show_distance.equalsIgnoreCase("vanillaDistance")) {
                    this.job_show_distance = ShowDistance.VanillaDistance;
                } else if (job_show_distance.equalsIgnoreCase("exact")) {
                    this.job_show_distance = ShowDistance.Exact;
                } else {
                    this.job_show_distance = ShowDistance.None;
                }
            } else {
                this.job_show_distance = ShowDistance.None;
            }

            this.job_show_arrow = job_show_arrow;

            if(job_pick_option!=null && !job_pick_option.equals("")){
                this.job_pick_option = job_pick_option;
            } else {
                this.job_pick_option = MagicTxt.getString("mb_accept");
            }

            this.job_pick_script = job_pick_script;
            this.job_memKey = job_memKey;
            this.job_conclusion_script = job_conclusion_script;
            this.target_first_name = target_first_name;
            this.target_last_name = target_last_name;
            this.target_portrait = target_portrait;
            this.target_gender = target_gender;
            this.target_rank = target_rank;
            this.target_post = target_post;
            this.target_personality = target_personality;
            this.target_aiCoreId = target_aiCoreId;
            this.target_level = target_level;
            this.target_elite_skills = target_elite_skills;
            this.target_skill_preference = target_skill_preference;
            this.target_skills = target_skills;
            this.fleet_name = fleet_name;
            this.fleet_faction = fleet_faction;
            this.fleet_flagship_variant = fleet_flagship_variant;
            this.fleet_flagship_name = fleet_flagship_name;
            this.fleet_flagship_recoverable = fleet_flagship_recoverable;
            this.fleet_preset_ships = fleet_preset_ships;
            this.fleet_scaling_multiplier = fleet_scaling_multiplier;
            this.fleet_min_DP = fleet_min_DP;
            this.fleet_composition_faction = fleet_composition_faction;
            this.fleet_composition_quality = fleet_composition_quality;
            this.fleet_transponder = fleet_transponder;
            this.fleet_behavior = fleet_behavior;
            this.location_marketIDs = location_marketIDs;
            this.location_marketFactions = location_marketFactions;
            this.location_distance = location_distance;
            this.location_themes = location_themes;
            this.location_themes_blacklist = location_themes_blacklist;
            this.location_entities = location_entities;
            this.location_prioritizeUnexplored = location_prioritizeUnexplored;
            this.location_defaultToAnyEntity = location_defaultToAnyEntity;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("bountyData{");
            sb.append("trigger_market_id=").append(trigger_market_id);
            sb.append(", trigger_marketFaction_any=").append(trigger_marketFaction_any);
            sb.append(", trigger_marketFaction_alliedWith=").append(trigger_marketFaction_alliedWith);
            sb.append(", trigger_marketFaction_none=").append(trigger_marketFaction_none);
            sb.append(", trigger_marketFaction_enemyWith=").append(trigger_marketFaction_enemyWith);
            sb.append(", trigger_market_minSize=").append(trigger_market_minSize);
            sb.append(", trigger_player_minLevel=").append(trigger_player_minLevel);
            sb.append(", trigger_min_days_elapsed=").append(trigger_min_days_elapsed);
            sb.append(", trigger_weight_mult=").append(trigger_weight_mult);
            sb.append(", trigger_memKeys_all=").append(trigger_memKeys_all);
            sb.append(", trigger_memKeys_any=").append(trigger_memKeys_any);
            sb.append(", trigger_playerRelationship_atLeast=").append(trigger_playerRelationship_atLeast);
            sb.append(", trigger_playerRelationship_atMost=").append(trigger_playerRelationship_atMost);
            sb.append(", job_name='").append(job_name).append('\'');
            sb.append(", job_description='").append(job_description).append('\'');
            sb.append(", job_comm_reply='").append(job_comm_reply).append('\'');
            sb.append(", job_intel_success='").append(job_intel_success).append('\'');
            sb.append(", job_intel_failure='").append(job_intel_failure).append('\'');
            sb.append(", job_intel_expired='").append(job_intel_expired).append('\'');
            sb.append(", job_forFaction='").append(job_forFaction).append('\'');
            sb.append(", job_difficultyDescription='").append(job_difficultyDescription).append('\'');
            sb.append(", job_deadline=").append(job_deadline);
            sb.append(", job_credit_reward=").append(job_credit_reward);
            sb.append(", job_credit_scaling=").append(job_credit_scaling);
            sb.append(", job_reputation_reward=").append(job_reputation_reward);
            sb.append(", job_item_reward=").append(job_item_reward);
            sb.append(", job_type=").append(job_type);
            sb.append(", job_show_type=").append(job_show_type);
            sb.append(", job_show_captain=").append(job_show_captain);
            sb.append(", job_show_fleet=").append(job_show_fleet);
            sb.append(", job_show_distance=").append(job_show_distance);
            sb.append(", job_show_arrow=").append(job_show_arrow);
            sb.append(", job_pick_option='").append(job_pick_option).append('\'');
            sb.append(", job_pick_script='").append(job_pick_script).append('\'');
            sb.append(", job_memKey='").append(job_memKey).append('\'');
            sb.append(", job_conclusion_script='").append(job_conclusion_script).append('\'');
            sb.append(", target_first_name='").append(target_first_name).append('\'');
            sb.append(", target_last_name='").append(target_last_name).append('\'');
            sb.append(", target_portrait='").append(target_portrait).append('\'');
            sb.append(", target_gender=").append(target_gender);
            sb.append(", target_rank='").append(target_rank).append('\'');
            sb.append(", target_post='").append(target_post).append('\'');
            sb.append(", target_personality='").append(target_personality).append('\'');
            sb.append(", target_aiCoreId='").append(target_aiCoreId).append('\'');
            sb.append(", target_level=").append(target_level);
            sb.append(", target_elite_skills=").append(target_elite_skills);
            sb.append(", target_skill_preference=").append(target_skill_preference);
            sb.append(", target_skills=").append(target_skills);
            sb.append(", fleet_name='").append(fleet_name).append('\'');
            sb.append(", fleet_faction='").append(fleet_faction).append('\'');
            sb.append(", fleet_flagship_variant='").append(fleet_flagship_variant).append('\'');
            sb.append(", fleet_flagship_name='").append(fleet_flagship_name).append('\'');
            sb.append(", fleet_flagship_recoverable=").append(fleet_flagship_recoverable);
            sb.append(", fleet_preset_ships=").append(fleet_preset_ships);
            sb.append(", fleet_scaling_multiplier=").append(fleet_scaling_multiplier);
            sb.append(", fleet_min_DP=").append(fleet_min_DP);
            sb.append(", fleet_composition_faction='").append(fleet_composition_faction).append('\'');
            sb.append(", fleet_composition_quality=").append(fleet_composition_quality);
            sb.append(", fleet_transponder=").append(fleet_transponder);
            sb.append(", fleet_behavior=").append(fleet_behavior);
            sb.append(", location_marketIDs=").append(location_marketIDs);
            sb.append(", location_marketFactions=").append(location_marketFactions);
            sb.append(", location_distance='").append(location_distance).append('\'');
            sb.append(", location_themes=").append(location_themes);
            sb.append(", location_themes_blacklist=").append(location_themes_blacklist);
            sb.append(", location_entities=").append(location_entities);
            sb.append(", location_prioritizeUnexplored=").append(location_prioritizeUnexplored);
            sb.append(", location_defaultToAnyEntity=").append(location_defaultToAnyEntity);
            sb.append('}');
            return sb.toString();
        }
    }

    //Loads a bounty list from modSettings.json while respecting their mod requirements
    private static List<String> readBountyList(boolean verbose){

        //load the list of bounties that should be loaded, as well as their mod requirements
        Map<String, List<String>> bountiesWithRequirements = new HashMap<>();

        JSONObject localCopy = MagicSettings.modSettings;

        try {
            JSONObject reqSettings = localCopy.getJSONObject(MOD);
            //try to get the requested value
            if(reqSettings.has(BOUNTY_BOARD)){
                JSONObject bountiesList = reqSettings.getJSONObject(BOUNTY_BOARD);
                if(bountiesList.length()>0){
                    for(Iterator<?> iter = bountiesList.keys(); iter.hasNext();){
                        //bounty id
                        String key = (String)iter.next();
                        //bounty requirements
                        List<String> values = new ArrayList<>();
                        JSONArray requirementList = bountiesList.getJSONArray(key);
                        if(requirementList.length()>0){
                            for(int i=0; i<requirementList.length(); i++){
                                values.add(requirementList.getString(i));
                            }
                        }
                        bountiesWithRequirements.put(key, values);
                    }
                }
            } else {
                LOG.error("MagicBountyData is unable to find "+BOUNTY_BOARD+" within " +MOD+ " in modSettings.json");
            }
        } catch (JSONException ex){
            LOG.error("MagicBountyData is unable to read the content of "+MOD+" in modSettings.json",ex);
        }

        List<String> bountiesAvailable = new ArrayList<>();

        for(String id : bountiesWithRequirements.keySet()){
            if(bountiesWithRequirements.get(id).isEmpty()){
                //no requirement
                bountiesAvailable.add(id);
                if(verbose){
                    LOG.info("Bounty " +id+ " will be loaded.");
                }
            } else {
                //check if all the required mods are active
                boolean missingRequirement=false;
                for(String required : bountiesWithRequirements.get(id)){
                    if(!Global.getSettings().getModManager().isModEnabled(required)){
                        missingRequirement=true;
                        if(verbose){
                            LOG.info("Bounty " +id+ " is unavailable, missing " +required);
                        }
                        break;
                    }
                }
                if(!missingRequirement){
                    bountiesAvailable.add(id);
                    //log if devMode is active
                    if(verbose){
                        LOG.info("Bounty " +id+ " will be loaded.");
                    }
                }
            }
        }
        //only return bounties that have no other mod requirement, or all of them are present
        return bountiesAvailable;
    }

    //Load the bounty data file
    private static JSONObject loadBountyData(){
        JSONObject this_bounty_data=null;
        try{
            this_bounty_data = Global.getSettings().getMergedJSONForMod(PATH,MOD);
        } catch (IOException | JSONException ex) {
            LOG.fatal("MagicBountyData is unable to read magicBounty_data.json",ex);
            JSONfailed=true;
        }
        return this_bounty_data;
    }

    private static boolean getBoolean(String bountyId, String key){
        boolean value=false;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                value = reqSettings.getBoolean(key);
            }
        } catch (JSONException ex){}

        return value;
    }

    private static boolean getBooleanDefaultTrue(String bountyId, String key){
        boolean value=true;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                value = reqSettings.getBoolean(key);
            }
        } catch (JSONException ex){}

        return value;
    }

    private static String getString(String bountyId, String key){
        return getString(bountyId, key, null);
    }

    private static String getString(String bountyId, String key, String defaultValue){
        String value=defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                value = reqSettings.getString(key);
            }
        } catch (JSONException ex){}

        return value;
    }

    private static Integer getInt(String bountyId, String key){
        return getInt(bountyId, key, -1);
    }

    private static Integer getInt(String bountyId, String key, int defaultValue){
        int value = defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                value = reqSettings.getInt(key);
            }
        } catch (JSONException ex){}

        return value;
    }

    private static Float getFloat(String bountyId, String key){
        return getFloat(bountyId, key, -1);
    }

    private static Float getFloat(String bountyId, String key, float defaultValue){
        float value= defaultValue;

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                value = (float)reqSettings.getDouble(key);
            }
        } catch (JSONException ex){}

        return value;
    }

    private static List<String> getStringList(String bountyId, String key){
        List<String> value=new ArrayList<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                JSONArray list = reqSettings.getJSONArray(key);
                if(list.length()>0){
                    for (int i = 0; i < list.length(); i++) {
                        value.add(list.getString(i));
                    }
                }
            }
        } catch (JSONException ex){}

        return value;
    }

    private static Map<String,Boolean> getBooleanMap(String bountyId, String key){
        Map<String,Boolean> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                JSONObject list = reqSettings.getJSONObject(key);
                if(list.length()>0){
                    for(Iterator<?> iter = list.keys(); iter.hasNext();){
                        String this_key = (String)iter.next();
                        boolean this_data = list.getBoolean(this_key);
                        value.put(this_key,this_data);
                    }
                }
            }
        } catch (JSONException ex){}

        return value;
    }

    private static Map<String,Float> getFloatMap(String bountyId, String key){
        Map<String,Float> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                JSONObject list = reqSettings.getJSONObject(key);
                if(list.length()>0){
                    for(Iterator<?> iter = list.keys(); iter.hasNext();){
                        String this_key = (String)iter.next();
                        float this_data = (float)list.getDouble(this_key);
                        value.put(this_key,this_data);
                    }
                }
            }
        } catch (JSONException ex){}

        return value;
    }

    private static Map<String,Integer> getIntMap(String bountyId, String key){
        Map<String,Integer> value = new HashMap<>();

        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            if(reqSettings.has(key)){
                JSONObject list = reqSettings.getJSONObject(key);
                if(list.length()>0){
                    for(Iterator<?> iter = list.keys(); iter.hasNext();){
                        String this_key = (String)iter.next();
                        int this_data = (int)list.getDouble(this_key);
                        value.put(this_key,this_data);
                    }
                }
            }
        } catch (JSONException ex){}

        return value;
    }
    
    private static Map<String,List<String>> getListMap(String bountyId, String key){
        Map<String,List<String>> value = new HashMap<>();
        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);
            //get object
            if(reqSettings.has(key)){
                //get key list
                JSONObject keyList = reqSettings.getJSONObject(key);
                if(keyList.length()>0){
                    for(Iterator<?> iter = keyList.keys(); iter.hasNext();){
                        String this_key = (String)iter.next();
                        //get list of values for each key
                        JSONArray data_list = keyList.getJSONArray(key);
                        List<String>parsed_list = new ArrayList<>();
                        if(data_list.length()>0){
                            for(int i=0; i<data_list.length();i++){
                                //turn json list into array list
                                parsed_list.add(data_list.getString(i));
                            }
                        }
                        value.put(this_key,parsed_list);
                    }
                }
            }
        } catch (JSONException ex){}

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
