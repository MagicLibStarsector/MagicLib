package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
//import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import data.scripts.util.MagicSettings;
import data.scripts.util.MagicTxt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static boolean verbose=false;
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
                FullName.Gender gender = FullName.Gender.ANY;
                if(genderString!=null){
                    if(genderString.equals("MALE")){
                        gender = FullName.Gender.MALE;
                    } else if(genderString.equals("FEMALE")){
                        gender = FullName.Gender.FEMALE;
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
                if(target_skill_pref!=null && !target_skill_pref.equals("")){
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
                if(getString(bountyId, "job_memKey")!=null && !getString(bountyId, "job_memKey").equals("")){
                    memKey = getString(bountyId, "job_memKey");
                }
                
                bountyData this_bounty = new bountyData(
                        getStringList(bountyId, "trigger_market_id"),
                        getStringList(bountyId, "trigger_marketFaction_any"),
                        getBoolean(bountyId, "trigger_marketFaction_alliedWith"),
                        getStringList(bountyId, "trigger_marketFaction_none"),
                        getBoolean(bountyId, "trigger_marketFaction_enemyWith"),
                        getInt(bountyId, "trigger_market_minSize"),
                        getInt(bountyId, "trigger_player_minLevel"),
                        getInt(bountyId, "trigger_min_days_elapsed"),
                        getFloat(bountyId, "trigger_weight_mult"),
                        getBooleanMap(bountyId, "trigger_memKeys_all"),
                        getBooleanMap(bountyId, "trigger_memKeys_any"),
                        getFloatMap(bountyId, "trigger_playerRelationship_atLeast"),
                        getFloatMap(bountyId, "trigger_playerRelationship_atMost"),
                                                
                        getString(bountyId, "job_name"),
                        getString(bountyId, "job_description"),  
                        getString(bountyId, "job_forFaction"),
                        getString(bountyId, "job_difficultyDescription"),
                        getInt(bountyId, "job_deadline"),
                        getInt(bountyId, "job_credit_reward"),
                        getFloat(bountyId, "job_reward_scaling"),
                        getString(bountyId, "job_type"),
                        getBooleanDefaultTrue(bountyId, "job_show_type"),
                        getBoolean(bountyId, "job_show_captain"),
                        getString(bountyId, "job_show_fleet"),
                        getString(bountyId, "job_show_distance"),
                        getBooleanDefaultTrue(bountyId, "job_show_arrow"),
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
                        getBoolean(bountyId, "target_isAI"),
                        getInt(bountyId, "target_level"),
                        getInt(bountyId, "target_elite_skills"),
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
                if(!appendOnly || (!BOUNTIES.containsKey(bountyId) && !Global.getSector().getMemoryWithoutUpdate().contains(this_bounty.job_memKey))){
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
        public List <String> trigger_market_id;                                 //will default to the other preferences if those are defined and the location doesn't exists due to Nexerelin random mode
        public List <String> trigger_marketFaction_any;
        public boolean trigger_marketFaction_alliedWith;                        //visited market is at least neutral with one those factions
        public List <String> trigger_marketFaction_none;
        public boolean trigger_marketFaction_enemyWith;                         //visited market is at best inhospitable with all those factions
        public int trigger_market_minSize;
        public int trigger_player_minLevel;
        public int trigger_min_days_elapsed;
        public float trigger_weight_mult;                                       //simple frequency multiplier
        public Map <String,Boolean> trigger_memKeys_all;       
        public Map <String,Boolean> trigger_memKeys_any;    
        public Map <String,Float> trigger_playerRelationship_atLeast;           //minimal player relationship with those factions
        public Map <String,Float> trigger_playerRelationship_atMost;            //maximum player relationship with those factions
        //job description
        public String job_name;                                                 //job name in the dialog pick list
        public String job_description;                                          //not sure how the description will handle text variables and highlights, will it work with variables such as "$he_or_she"?
        public String job_forFaction;                                           //successfully completing this mission with give a small reputation reward with this faction
        @Nullable public String job_difficultyDescription;                      // "none": no description, "auto": bounty board describes how dangerous the bounty is, any other text: bounty board displays the text
        public int job_deadline;
        public int job_credit_reward;
        public float job_reward_scaling;                                        //only used with fleet scaling: total reward = job_credits_reward * (job_reward_scaling * (bounty fleet DP / fleet_minimal_DP) )
        public JobType job_type;                                                //assassination, destruction, obliteration, neutralisation. see magicBounty_data_example.json 
        public boolean job_show_type;
        public boolean job_show_captain;
        public ShowFleet job_show_fleet;                                        // none, flagship, preset, or all: how much of the fleet to show on the bounty board. default: none
        public ShowDistance job_show_distance;                                  // none, vague or exact: how precisely the distance to the target is shown on the bounty board. default: none
        public boolean job_show_arrow;
        public String job_pick_option;                                          //dialog text to pick the job
        public String job_pick_script;                                          //optional, can be used to trigger further scripts when the mission is taken, for example you may want to have competing bounty hunters
        public String job_memKey;                                               //MemKey set to false is added when accepting the job, set to true if the job is sucessful
        public String job_conclusion_script;                                    //optional, can be used to give additional rewards or add further consequences in case of failure using memkeys to check the outcome
        //bounty target                                                         //ALL OPTIONAL
        public String target_first_name;
        public String target_last_name;
        public String target_portrait;                                          //id of the sprite in settings.json/graphics/characters
        public FullName.Gender target_gender;                                   //MALE, FEMALE, ANY
        public String target_rank;                                              //rank from campaign.ids.Ranks
        public String target_post;                                              //post from campaign.ids.Ranks
        public String target_personality;                                       //personality from campaign.ids.Personalities
        public boolean target_isAI;                                             //Makes the target drop AI cores
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
        public List<String> location_themes;                                    //campaign.ids.Tags + "PROCGEN_NO_THEME" + "PROCGEN_NO_THEME_NO_PULSAR_NO_BLACKHOLE"
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
            float trigger_weight_mult,                       
            Map <String,Boolean> trigger_memKeys_all,          
            Map <String,Boolean> trigger_memKeys_any,
            Map <String,Float> trigger_playerRelationship_atLeast,  
            Map <String,Float> trigger_playerRelationship_atMost,  
            String job_name,                         
            String job_description,        
            String job_forFaction,
            String job_difficultyDescription,
            int job_deadline,
            int job_credit_reward,
            float job_reward_scaling,    
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
            boolean target_isAI,                     
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
            this.trigger_weight_mult = trigger_weight_mult;                       
            this.trigger_memKeys_all = trigger_memKeys_all;                     
            this.trigger_memKeys_any = trigger_memKeys_any;
            this.trigger_playerRelationship_atLeast = trigger_playerRelationship_atLeast;
            this.trigger_playerRelationship_atMost = trigger_playerRelationship_atMost;
            this.job_name = job_name;                         
            this.job_description = job_description;  
            this.job_forFaction = job_forFaction;
            this.job_difficultyDescription = job_difficultyDescription;
            this.job_deadline = job_deadline;
            this.job_credit_reward = job_credit_reward;
            this.job_reward_scaling = job_reward_scaling;  
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
                } else if (job_show_fleet.equalsIgnoreCase("flagship")) {
                    this.job_show_fleet = ShowFleet.Flagship;
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
            this.target_isAI = target_isAI;                     
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
            LOG.error("MagicBountyData is unable to read the content of "+MOD+" in modSettings.json");
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
            LOG.fatal("MagicBountyData is unable to read magicBounty_data.json");
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
        String value=null;
          
        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);   
            if(reqSettings.has(key)){
                value = reqSettings.getString(key);
            }
        } catch (JSONException ex){}
                
        return value;
    }
    
    private static Integer getInt(String bountyId, String key){
        int value=-1;
          
        try {
            JSONObject reqSettings = bounty_data.getJSONObject(bountyId);   
            if(reqSettings.has(key)){
                value = reqSettings.getInt(key);
            }
        } catch (JSONException ex){}
                 
        return value;
    }
    
    private static Float getFloat(String bountyId, String key){
        float value=-1;
          
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

    public enum JobType {
        Assassination,
        Destruction,
        Obliteration,
        Neutralisation,
    }
    public enum ShowFleet {
        None,
        Flagship,
        Preset,
        All
    }
    public enum ShowDistance {
        None,
        Vague,
        Vanilla,
        Distance,
        VanillaDistance
    }
}
