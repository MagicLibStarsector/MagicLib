package data.scripts.bounty;

/**
 *
 * @author Schaf-Unschaf, Tartiflette (and Vayra)
 */

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
//import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
//import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.bounty.MagicBountyData.BOUNTIES;
import static data.scripts.bounty.MagicBountyData.verbose;
import data.scripts.util.MagicSettings;
import static data.scripts.util.MagicTxt.getString;
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
    
    public static void convertHVBs(boolean appendOnly) {
        
        if(Global.getSettings().isDevMode())MagicBountyData.verbose=true;
        if(MagicBountyData.verbose){
            LOG.info("\n ######################\n\n   HVB CONVERSION  \n\n ######################");
        }
        
        try {
            JSONArray uniqueBountyDataJSON = Global.getSettings().getMergedSpreadsheetDataForMod("bounty_id", VAYRA_UNIQUE_BOUNTIES_FILE, "MagicLib");

            for (int i = 0; i < uniqueBountyDataJSON.length(); ++i) {
                JSONObject row = uniqueBountyDataJSON.getJSONObject(i);
                if (row.has("bounty_id") && row.getString("bounty_id") != null && !row.getString("bounty_id").isEmpty()) {
                    String bountyId = row.getString("bounty_id");
                    LOG.info("loading HVB " + bountyId);
                    
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
                    enemyMarket.add(row.getString("faction"));
                    
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
                    
                    List <String> themes = new ArrayList<>();
                    themes.add("procgen_no_theme_pulsar_blackhole");
                    
                    MagicBountyData.bountyData this_bounty = new MagicBountyData.bountyData(
                            //List <String> trigger_market_id,
                            null,
                            //List <String> trigger_marketFaction_any,
                            postingMarket,
                            //boolean trigger_marketFaction_alliedWith,
                            true,
                            //List <String> trigger_marketFaction_none,
                            enemyMarket,
                            //boolean trigger_marketFaction_enemyWith,
                            true,
                            //int trigger_market_minSize,
                            3,
                            //int trigger_player_minLevel,
                            row.getInt("neverSpawnBeforeLevel"),
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
                            //Map <String,Float> trigger_playerRelationship_atLeast,
                            relationshipAtLeast,
                            //Map <String,Float> trigger_playerRelationship_atMost,
                            relationshipAtMost,
                            //String job_name,
                            getString("mb_hvb_title"),
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
                            row.getString("faction"),
                            //String fleet_flagship_variant,
                            row.getString("flagshipVariantId"),
                            //String fleet_flagship_name,
                            row.getString("flagshipName"),
                            //boolean fleet_flagship_recoverable,
                            row.optDouble("chanceToAutoRecover", 1.0D)>0f,
                            //Map <String,Integer> fleet_preset_ships,
                            fleetMap,
                            //float fleet_scaling_multiplier,
                            (float) row.getDouble("playerFPScalingFactor"),
                            //int fleet_min_DP,
                            row.getInt("minimumFleetFP"),
                            //String fleet_composition_faction,
                            row.getString("faction"),
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
                            themes,
                            //List<String> location_themes_blacklist,
                            null,
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
                        }
                    } else if(verbose){
                        LOG.info("SKIPPED");
                    }
                    
                    /*
                    HighValueBountyData bountyData = new HighValueBountyData(bountyId,
                            row.getInt("level"),
                            row.getString("rank"),
                            row.getString("firstName"),
                            row.getString("lastName"),
                            row.optString("captainPersonality", "aggressive"),
                            row.getString("fleetName"),
                            row.getString("flagshipName"),
                            row.getString("gender"),
                            row.getString("faction"),
                            row.getString("portrait"),
                            row.getString("greetingText"),
                            row.getBoolean("suppressIntel"),
                            row.getString("postedByFaction"),
                            row.getInt("creditReward"),
                            (float) row.getInt("repReward") / 100.0F,
                            row.getString("intelText"),
                            row.getString("flagshipVariantId"),
                            fleetList,
                            row.getInt("minimumFleetFP"),
                            (float) row.getDouble("playerFPScalingFactor"),
                            (float) row.optDouble("chanceToAutoRecover", 1.0D),
                            itemList,
                            prerequisiteBountiesList,
                            row.getBoolean("neverSpawnWhenFactionHostile"),
                            row.getBoolean("neverSpawnWhenFactionNonHostile"),
                            row.getInt("neverSpawnBeforeCycle"),
                            row.getInt("neverSpawnBeforeLevel"),
                            row.getInt("neverSpawnBeforeFleetPoints")
                    );
                    HighValueBountyManager.highValueBountyData.put(bountyId, bountyData);
                    LOG.info("loaded unique bounty id " + bountyId);
                    */
                } else {
                    LOG.info("hit empty line, unique bounty loading ended");
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
        String heOrShe = person.getHeOrShe();
        String levelDesc;
        String skillDesc;

        int personLevel = person.getStats().getLevel();
        if (personLevel <= 4) levelDesc = "an unremarkable officer";
        else if (personLevel <= 7) levelDesc = "a capable officer";
        else if (personLevel <= 11) levelDesc = "a highly capable officer";
        else levelDesc = "an exceptionally capable officer";

        List<MutableCharacterStatsAPI.SkillLevelAPI> knownSkills = commander.getStats().getSkillsCopy();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

        for (MutableCharacterStatsAPI.SkillLevelAPI skill : knownSkills) {
            String skillName = skill.getSkill().getId();
            switch (skillName) {
                case Skills.WEAPON_DRILLS:
                    picker.add("a great number of illegal weapon modifications");
                    break;
                case Skills.AUXILIARY_SUPPORT:
                    picker.add("armed-to-the-teeth support ships");
                    break;
                case Skills.COORDINATED_MANEUVERS:
                    picker.add("a high effectiveness in coordinating the maneuvers of ships during combat");
                    break;
                case Skills.WOLFPACK_TACTICS:
                    picker.add("highly coordinated frigate attacks");
                    break;
                case Skills.CREW_TRAINING:
                    picker.add("a very courageous crew");
                    break;
                case Skills.CARRIER_GROUP:
                    picker.add("a noteworthy level of skill in running carrier operations");
                    break;
                case Skills.OFFICER_TRAINING:
                    picker.add("having extremely skilled subordinates");
                    break;
                case Skills.OFFICER_MANAGEMENT:
                    picker.add("having a high number of skilled subordinates");
                    break;
                case Skills.NAVIGATION:
                    picker.add("having highly skilled navigators");
                    break;
                case Skills.SENSORS:
                    picker.add("having overclocked sensory equipment");
                    break;
                case Skills.ELECTRONIC_WARFARE:
                    picker.add("being proficient in electronic warfare");
                    break;
                case Skills.FIGHTER_UPLINK:
                    picker.add("removing engine-safety-protocols from fighters");
                    break;
                case Skills.FLUX_REGULATION:
                    picker.add("using overclocked flux coils");
                    break;
                case Skills.PHASE_CORPS:
                    picker.add("using experimental phase coils");
                    break;
                case Skills.FIELD_REPAIRS:
                    picker.add("having highly skilled mechanics");
                    break;
                case Skills.DERELICT_CONTINGENT:
                    picker.add("using military-grade duct tape");
                    break;
            }
        }

        Random random = new Random(person.getId().hashCode() * 1337L);
        picker.setRandom(random);

        skillDesc = picker.isEmpty() ? "nothing, really" : picker.pick();
        if (levelDesc.contains("unremarkable"))
            levelDesc = "an otherwise unremarkable officer";

        text.addPara(Misc.ucFirst(heOrShe) + " is %s known for %s.", commander.getFaction().getBaseUIColor(), levelDesc, skillDesc);
    }

    public static void generateFancyFleetDescription(TextPanelAPI text, CampaignFleetAPI fleet, PersonAPI person) {
        if (person==null)return;

        int fleetSize = fleet.getNumShips();
        FleetMemberAPI flagship = fleet.getFlagship();
        PersonAPI commander = fleet.getCommander();
        String shipType = flagship.getHullSpec().getHullNameWithDashClass() + " " + flagship.getHullSpec().getDesignation().toLowerCase();
        String hisOrHer = person.getHisOrHer();
        String fleetDesc;

        if (fleetSize <= 10) fleetDesc = "small fleet";
        else if (fleetSize <= 20) fleetDesc = "medium-sized fleet";
        else if (fleetSize <= 30) fleetDesc = "large fleet";
        else if (fleetSize <= 40) fleetDesc = "very large fleet";
        else if (fleetSize <= 50) fleetDesc = "gigantic fleet";
        else fleetDesc = "freaking armada";

        text.addPara("%s is in command of a %s and personally commands the %s, a %s, as "+hisOrHer+" flagship.", commander.getFaction().getBaseUIColor(),
                commander.getFaction().getRank(commander.getRankId()) + " " + person.getName().getFullName(), fleetDesc, flagship.getShipName(), shipType);
    }
}