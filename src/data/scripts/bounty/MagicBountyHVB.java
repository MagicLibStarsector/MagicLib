package data.scripts.bounty;

/**
 *
 * @author Schaf-Unschaf, Tartiflette (and Vayra)
 */

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
//import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.bounty.MagicBountyData.BOUNTIES;
import data.scripts.util.MagicSettings;
import static data.scripts.util.MagicTxt.getString;
import data.scripts.util.MagicVariables;
import static data.scripts.util.MagicVariables.MAGICLIB_ID;
import static data.scripts.util.MagicVariables.verbose;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MagicBountyHVB {
    
    public static final String VAYRA_UNIQUE_BOUNTIES_FILE = "data/config/vayraBounties/unique_bounty_data.csv";
    private static final Logger LOG = Global.getLogger(MagicSettings.class);
                        
    private static final List<String> WHITELIST = new ArrayList<>();
    static {
        WHITELIST.add("theme_misc");
        WHITELIST.add("theme_misc_skip");
        WHITELIST.add("theme_interesting_minor");
        WHITELIST.add("theme_ruins_secondary");
        WHITELIST.add("theme_derelict_probes");
    }
    
    private static final List<String> BLACKLIST = new ArrayList<>();
    static {
        BLACKLIST.add("theme_already_colonized");
        BLACKLIST.add("theme_already_occupied");
        BLACKLIST.add("theme_hidden");
    }
    
    public static void convertHVBs(boolean appendOnly) {
        
        if(verbose){
            LOG.info("\n ######################\n\n   HVB CONVERSION  \n\n ######################");
        }
        
        try {
            JSONArray uniqueBountyDataJSON = Global.getSettings().getMergedSpreadsheetDataForMod("bounty_id", VAYRA_UNIQUE_BOUNTIES_FILE, MAGICLIB_ID);
            
            int hvb =0;
            
            for (int i = 0; i < uniqueBountyDataJSON.length(); ++i) {
                JSONObject row = uniqueBountyDataJSON.getJSONObject(i);
                if (row.has("bounty_id") && row.getString("bounty_id") != null && !row.getString("bounty_id").isEmpty()) {
                                        
                    String bountyId = row.getString("bounty_id");
                    
                    if(verbose){
                        LOG.info("loading HVB " + bountyId);
                    }
                    
                    String fleetListString = row.optString("fleetVariantIds");
                    List<String> fleetList = null;
                    if (fleetListString != null) {
                        fleetList = new ArrayList<>(Arrays.asList(fleetListString.split("\\s*(,\\s*)+")));
                        if (fleetList.isEmpty() || (fleetList.get(0)).isEmpty()) {
                            fleetList = null;
                        }
                    }

                    String itemListString = row.optString("specialItemRewards");
                    List<String> itemList = null;
                    if (itemListString != null) {
                        itemList = new ArrayList<>(Arrays.asList(itemListString.split("\\s*(,\\s*)+")));
                        if (itemList.isEmpty() || (itemList.get(0)).isEmpty()) {
                            itemList = null;
                        }
                    }

                    String prerequisiteBountiesString = row.optString("neverSpawnUnlessBountiesCompleted");
                    List<String> prerequisiteBountiesList = null;
                    if (prerequisiteBountiesString != null) {
                        prerequisiteBountiesList = new ArrayList<>(Arrays.asList(prerequisiteBountiesString.split("\\s*(,\\s*)+")));
                        if (prerequisiteBountiesList.isEmpty() || (prerequisiteBountiesList.get(0)).isEmpty()) {
                            prerequisiteBountiesList = null;
                        }
                    }
                    
                    //convert poster faction into market list for board intel
                    List<String> postingMarket = new ArrayList<>();
                    postingMarket.add(row.getString("postedByFaction"));
                    
                    
                    //convert bounty faction into market list for board intel
                    List<String> enemyMarket = new ArrayList<>();
                    /*
                    enemyMarket.add(row.getString("faction"));
                    */
                    
                    //convert required bounty list into memkey list
                    Map <String,Boolean> memKeyAll=new HashMap<>();
                    if(prerequisiteBountiesList!=null){
                        for(String req : prerequisiteBountiesList){
                            memKeyAll.put(req, true);
                        }
                    }
                    
                    //convert hostility requirement
                    Map <String,Float> relationshipAtLeast=new HashMap<>();
                    if(row.getBoolean("neverSpawnWhenFactionHostile")){
                        relationshipAtLeast.put(row.getString("faction"), -0.49f);
                    } 
                    Map <String,Float> relationshipAtMost=new HashMap<>();
                    if(row.getBoolean("neverSpawnWhenFactionNonHostile")){
                        relationshipAtMost.put(row.getString("faction"), -0.5f);
                    }
                    
                    //convert item list
                    Map <String,Integer> itemMap=new HashMap<>();
                    if(itemList!=null){
                        for(String item : itemList){
                            if(itemMap.containsKey(item)){
                                itemMap.put(item, itemMap.get(item)+1);
                            } else {
                                itemMap.put(item, 1);
                            }
                        }
                    }
                    
                    //convert fleet list
                    Map <String,Integer> fleetMap=new HashMap<>();
                    if(fleetList!=null){
                        for(String ship:fleetList){
                            if(fleetMap.containsKey(ship)){
                                fleetMap.put(ship, fleetMap.get(ship)+1);
                            } else {
                                fleetMap.put(ship, 1);
                            }
                        }
                    }
                    
                    //convert gender
                    String genderString = row.getString("gender");
                    FullName.Gender gender = null;
                    if(genderString!=null){
                        switch (genderString) {
                            case "MALE":
                                gender = FullName.Gender.MALE;
                                break;
                            case "FEMALE":
                                gender = FullName.Gender.FEMALE;
                                break;
                            default:
                                break;
                        }
                    }
                    
                    String faction = row.getString("faction");
                    if(faction.equals("hvb_hostile")){
                        faction=MagicVariables.BOUNTY_FACTION;
                        LOG.info("Replacing hvb_hostile with ML_bounty");
                    }
                    
                    int level=row.getInt("neverSpawnBeforeLevel");
                    if(level>15){
                        //if min level is superior to 15, it is divided by 2 (old level cap was 30) to avoid outdated bounties from being un-triggerable
                        level*=0.5f;
                    }
                    
                    MagicBountyData.bountyData this_bounty = new MagicBountyData.bountyData(
                            //List <String> trigger_market_id,
                            new ArrayList<String>(),
                            //List <String> trigger_marketFaction_any,
                            postingMarket,
                            //boolean trigger_marketFaction_alliedWith,
                            false,
                            //List <String> trigger_marketFaction_none,
                            enemyMarket,
                            //boolean trigger_marketFaction_enemyWith,
                            false,
                            //int trigger_market_minSize,
                            3,
                            //int trigger_player_minLevel,
                            level,
                            //int trigger_min_days_elapsed,
                            row.getInt("neverSpawnBeforeCycle")*365,
                            //int trigger_min_fleet_size,
                            row.getInt("neverSpawnBeforeFleetPoints"),
                            //float trigger_weight_mult,
                            0.25f,
                            //Map <String,Boolean> trigger_memKeys_all,
                            memKeyAll,
                            //Map <String,Boolean> trigger_memKeys_any,
                            null,
                            //Map <String,Boolean> trigger_memKeys_none,
                            null,
                            //Map <String,Float> trigger_playerRelationship_atLeast,
                            relationshipAtLeast,
                            //Map <String,Float> trigger_playerRelationship_atMost,
                            relationshipAtMost,                            
                            //getFloat(bountyId, "trigger_giverTargetRelationship_atLeast"),
                            -99f,
                            //getFloat(bountyId, "trigger_giverTargetRelationship_atMost"),
                            99f,
                            //String job_name,
                            getString("mb_hvb_title")+row.getString("firstName")+" "+row.getString("lastName"),
                            //String job_description,
                            row.getString("intelText"),
                            //String job_comm_reply,
                            row.getString("greetingText"),
                            //String job_intel_success,
                            null,
                            //String job_intel_failure,
                            null,
                            //String job_intel_expired,
                            null,
                            //String job_forFaction,
                            row.getString("postedByFaction"),
                            //String job_difficultyDescription,
                            "auto",
                            //int job_deadline,
                            360,
                            //int job_credit_reward,
                            row.getInt("creditReward"),
                            //float job_credit_scaling,
                            (float) row.getDouble("playerFPScalingFactor")/2,
                            //float job_reputation_reward,
                            (float) row.getInt("repReward") / 100.0F,
                            //Map <String,Integer> job_item_reward,
                            itemMap,
                            //String job_type,
                            //MagicBountyData.JobType.Assassination,
                            "assassination",
                            //boolean job_show_type,
                            true,
                            //boolean job_show_captain,
                            true,
                            //String job_show_fleet,
                            //MagicBountyData.ShowFleet.Vanilla,
                            "vanilla",
                            //String job_show_distance,
                            //MagicBountyData.ShowDistance.VanillaDistance,
                            "vanillaDistance",
                            //boolean job_show_arrow,
                            false,
                            //String job_pick_option,
                            null,
                            //String job_pick_script,
                            null,
                            //String job_memKey,
                            "$HVB_"+bountyId,
                            //String job_conclusion_script,
                            null,                            
                            //String existing_target_memkey; 
                            null,
                            //String target_first_name,
                            row.getString("firstName"),
                            //String target_last_name,
                            row.getString("lastName"),
                            //String target_portrait,
                            row.getString("portrait"),
                            //FullName.Gender target_gender,
                            gender,
                            //String target_rank,
                            Ranks.SPACE_COMMANDER,
                            //String target_post,
                            Ranks.POST_FLEET_COMMANDER,
                            //String target_personality,
                            row.optString("captainPersonality", "aggressive"),
                            //String target_aiCoreId,
                            null,
                            //int target_level,
                            row.getInt("level"),
                            //int target_elite_skills,
                            -1,
                            //SkillPickPreference target_skill_preference,
                            OfficerManagerEvent.SkillPickPreference.GENERIC,
                            //Map <String,Integer> target_skills,
                            null,
                            //String fleet_name,
                            row.getString("fleetName"),
                            //String fleet_faction,
                            faction,
                            //String fleet_flagship_variant,
                            row.getString("flagshipVariantId"),
                            //String fleet_flagship_name,
                            row.getString("flagshipName"),
                            //boolean fleet_flagship_recoverable,
                            row.optDouble("chanceToAutoRecover", 1.0f)>0f,
                            //boolean fleet_flagship_autofit; 
                            false,
                            //Map <String,Integer> fleet_preset_ships,
                            fleetMap,
                            //public boolean fleet_preset_autofit;   
                            false,
                            //float fleet_scaling_multiplier,
                            (float) row.getDouble("playerFPScalingFactor"),
                            //int fleet_min_FP,
                            row.getInt("minimumFleetFP"),
                            //String fleet_composition_faction,
                            faction,
                            //float fleet_composition_quality,
                            2,
                            //boolean fleet_transponder,
                            false,
                            //FleetAssignment fleet_behavior,
                            FleetAssignment.DEFEND_LOCATION,
                            //List<String> location_marketIDs,
                            null,
                            //List<String> location_marketFactions,
                            null,
                            //String location_distance,
                            "CLOSE",
                            //List<String> location_themes,
                            WHITELIST,
                            //List<String> location_themes_blacklist,
                            BLACKLIST,
                            //List<String> location_entities,
                            null,
                            //boolean location_prioritizeUnexplored,
                            false,
                            //boolean location_defaultToAnyEntity
                            true
                    
                    );
                    
                    //add the bounty if it doesn't exist and hasn't been taken already or if the script is redoing the whole thing
                    if((!appendOnly || (!BOUNTIES.containsKey(bountyId) && !Global.getSector().getMemoryWithoutUpdate().contains(this_bounty.job_memKey)))){
                        BOUNTIES.put(bountyId, this_bounty);
                        if(verbose){
                            LOG.info("SUCCESS");
                            hvb++;
                        }
                    } else if(verbose){
                        LOG.info("SKIPPED");
                    }
                } else {
                    if(verbose){
                        LOG.info("hit empty line, unique bounty loading ended");
                    }
                }                
                if(verbose){
                    LOG.info("Successfully converted "+hvb+" HVBs");
                }                
            }
        } catch (IOException | JSONException exception) {
            LOG.error("MagicLib - Failed to load HighValueBountyData! - ",exception);
        }
    }

    
    public static void generateFancyCommanderDescription( TextPanelAPI text, CampaignFleetAPI fleet, PersonAPI person) {
        if (person==null) return;
        if (person.getStats()==null) return;

        PersonAPI commander = fleet.getCommander();
        
        String who = getString("mb_hvb_they");
        if(person.getGender()== FullName.Gender.MALE) who = getString("mb_hvb_he");
        if(person.getGender()== FullName.Gender.FEMALE) who = getString("mb_hvb_she");
        if(person.isAICore()) who = getString("mb_hvb_it");
        
        String levelDesc;
        String skillDesc;

        int personLevel = person.getStats().getLevel();
        if (personLevel <= 2) levelDesc = getString("mb_hvb_levelLow");
        else if (personLevel <= 4) levelDesc = getString("mb_hvb_levelMid");
        else if (personLevel <= 6) levelDesc = getString("mb_hvb_levelHigh");
        else levelDesc = getString("mb_hvb_levelMax");

        List<MutableCharacterStatsAPI.SkillLevelAPI> knownSkills = commander.getStats().getSkillsCopy();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

        for (MutableCharacterStatsAPI.SkillLevelAPI skill : knownSkills) {
            String skillName = skill.getSkill().getId();
            switch (skillName) {
                case Skills.WEAPON_DRILLS:
                    picker.add(getString("mb_hvb_skillWD"));
                    break;
                case Skills.AUXILIARY_SUPPORT:
                    picker.add(getString("mb_hvb_skillAS"));
                    break;
                case Skills.COORDINATED_MANEUVERS:
                    picker.add(getString("mb_hvb_skillCM"));
                    break;
                case Skills.WOLFPACK_TACTICS:
                    picker.add(getString("mb_hvb_skillWT"));
                    break;
                case Skills.CREW_TRAINING:
                    picker.add(getString("mb_hvb_skillCT"));
                    break;
                case Skills.CARRIER_GROUP:
                    picker.add(getString("mb_hvb_skillCG"));
                    break;
                case Skills.OFFICER_TRAINING:
                    picker.add(getString("mb_hvb_skillOT"));
                    break;
                case Skills.OFFICER_MANAGEMENT:
                    picker.add(getString("mb_hvb_skillOM"));
                    break;
                case Skills.NAVIGATION:
                    picker.add(getString("mb_hvb_skillN"));
                    break;
                case Skills.SENSORS:
                    picker.add(getString("mb_hvb_skillS"));
                    break;
                case Skills.ELECTRONIC_WARFARE:
                    picker.add(getString("mb_hvb_skillEW"));
                    break;
                case Skills.FIGHTER_UPLINK:
                    picker.add(getString("mb_hvb_skillFU"));
                    break;
                case Skills.FLUX_REGULATION:
                    picker.add(getString("mb_hvb_skillFluxR"));
                    break;
                case Skills.PHASE_CORPS:
                    picker.add(getString("mb_hvb_skillPC"));
                    break;
                case Skills.FIELD_REPAIRS:
                    picker.add(getString("mb_hvb_skillFR"));
                    break;
                case Skills.DERELICT_CONTINGENT:
                    picker.add(getString("mb_hvb_skillDC"));
                    break;
            }
        }

        Random random = new Random(person.getId().hashCode() * 1337L);
        picker.setRandom(random);

        skillDesc = picker.isEmpty() ? getString("mb_hvb_skillNone") : picker.pick();
//        if (levelDesc.equals(getString("mb_hvb_levelLow")))
//            levelDesc = getString("mb_hvb_levelNone");

        text.addPara(who + getString("mb_hvb_captain"), commander.getFaction().getBaseUIColor(), levelDesc, skillDesc);
    }

    public static void generateFancyFleetDescription(TextPanelAPI text, CampaignFleetAPI fleet, PersonAPI person) {
        if (person==null)return;

        int fleetSize = fleet.getNumShips();
        FleetMemberAPI flagship = fleet.getFlagship();
        PersonAPI commander = fleet.getCommander();
        String shipType = flagship.getHullSpec().getHullNameWithDashClass() + " " + flagship.getHullSpec().getDesignation().toLowerCase();
        String fleetDesc;

        if (fleetSize <= 10) fleetDesc = getString("mb_hvb_fleetSize0");
        else if (fleetSize <= 20) fleetDesc = getString("mb_hvb_fleetSize1");
        else if (fleetSize <= 30) fleetDesc = getString("mb_hvb_fleetSize2");
        else if (fleetSize <= 40) fleetDesc = getString("mb_hvb_fleetSize3");
        else if (fleetSize <= 50) fleetDesc = getString("mb_hvb_fleetSize4");
        else fleetDesc = getString("mb_hvb_fleetSize5");
        
        String own = getString("mb_hvb_fleetTheir");
        if(person.getGender()== FullName.Gender.MALE) own = getString("mb_hvb_fleetHis");
        if(person.getGender()== FullName.Gender.FEMALE) own = getString("mb_hvb_fleetHer");
        if(person.isAICore()) own = getString("mb_hvb_fleetIts");
        
        text.addPara(
                getString("mb_hvb_fleet0")+own+getString("mb_hvb_fleet1"), 
                commander.getFaction().getBaseUIColor(),                
                commander.getFaction().getRank(commander.getRankId()) + " " + person.getName().getFullName(),
                fleetDesc,
                flagship.getShipName(),
                shipType
        );
    }
}