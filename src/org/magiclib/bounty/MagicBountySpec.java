package org.magiclib.bounty;

import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.magiclib.util.MagicTxt;

import java.util.List;
import java.util.Map;

/**
 * The code representation of a bounty json definition.
 */
public class MagicBountySpec {

    /*
     * trigger parameters
     * ALL OPTIONAL
     */
    //------------
    /**
     * will default to the other preferences if those are defined and the location doesn't exists due to Nexerelin random mode
     */
    @NotNull
    public List<String> trigger_market_id;
    @NotNull
    public List<String> trigger_marketFaction_any;
    /**
     * visited market is at least neutral with one those factions
     */
    public boolean trigger_marketFaction_alliedWith;
    @NotNull
    public List<String> trigger_marketFaction_none;
    /**
     * visited market is at best inhospitable with all those factions
     */
    public boolean trigger_marketFaction_enemyWith;
    public int trigger_market_minSize;
    public int trigger_player_minLevel;
    public int trigger_min_days_elapsed;
    public int trigger_min_fleet_size;
    /**
     * simple frequency multiplier
     */
    public float trigger_weight_mult;
    @NotNull
    public Map<String, Boolean> trigger_memKeys_all;
    @NotNull
    public Map<String, Boolean> trigger_memKeys_any;
    @NotNull
    public Map<String, Boolean> trigger_memKeys_none;
    /**
     * minimal player relationship with those factions
     */
    @NotNull
    public Map<String, Float> trigger_playerRelationship_atLeast;
    /**
     * maximum player relationship with those factions
     */
    @NotNull
    public Map<String, Float> trigger_playerRelationship_atMost;
    /**
     * minimal relationship between the bounty giver and the target
     */
    @NotNull
    public Float trigger_giverTargetRelationship_atLeast;
    /**
     * maximum relationship between the bounty giver and the target
     */
    @NotNull
    public Float trigger_giverTargetRelationship_atMost;

    // section: job description

    /**
     * job name in the dialog pick list
     */
    public String job_name;
    /**
     * Full text of the bounty offer. The description will handle some text variables such as "$he_or_she". See documentation for more details
     */
    public String job_description;
    /**
     * Reply of the enemy to your hail, default to "The other $shipOrFleet does not answer to you hails."
     */
    public String job_comm_reply;
    /**
     * short text added to the Intel object after the job has been successfully completed
     */
    public String job_intel_success;
    /**
     * short text added to the Intel object after the job has been failed
     */
    public String job_intel_failure;
    /**
     * short text added to the Intel object after the job has been left to expire
     */
    public String job_intel_expired;
    /**
     * successfully completing this mission with give a small reputation reward with this faction
     */
    public String job_forFaction;
    /**
     * "none": no description, "auto": bounty board describes how dangerous the bounty is, any other text: bounty board displays the text
     */
    @Nullable
    public String job_difficultyDescription;
    public int job_deadline;
    public int job_credit_reward;
    /**
     * only used with fleet scaling: total reward = job_credits_reward * (job_reward_scaling * (bounty fleet DP / fleet_minimal_DP) )
     */
    public float job_credit_scaling;
    public float job_reputation_reward;
    @NotNull
    public Map<String, Integer> job_item_reward;
    /**
     * assassination, destruction, obliteration, neutralization
     */
    public MagicBountyLoader.JobType job_type;
    /**
     * assassination: requires only to disable the flagship
     * destruction: requires the complete destruction of the flagship without recovery
     * obliteration: requires the complete destruction or disabling of the enemy fleet
     * neutralisation: requires the destruction or disabling of 2/3rd of the enemy fleet
     * default to assassination
     */
    public boolean job_show_type;
    public boolean job_show_captain;
    /**
     * how much of the fleet to show on the bounty board.
     * none: default
     * text: "the target has a massive fleet comprised of around 15 ships."
     * flagship: only shows an image of the flagship
     * flagshipText: shows an image of the flagships and a text with the number of other ships
     * preset: only show an image of the Flagship and the preset fleet
     * presetText: show an image of the Flagship and the preset fleet, plus a text with the number of other ships
     * vanilla: shows the Flagship and the 9 biggest ships of the fleet, plus a text with the number of other ships
     * all: show an image of all the ships in the fleet.
     */
    public MagicBountyLoader.ShowFleet job_show_fleet;
    /**
     * how precisely the distance to the target is shown on the bounty board.
     * none: default
     * vague: "The target is located somewhere in the vicinity of the core worlds."
     * distance: "It is located roughly %s LY away from your current position."
     * system: "The target is located in the <system> system."
     * vanilla: "The target is located near a giant in a system with a yellow primary star, in the Nebulon constellation."
     * vanillaDistance: "The target is located near a giant in a system with a yellow primary star, in the Nebulon constellation. It is located roughly %s LY away from your current position."
     */
    public MagicBountyLoader.ShowDistance job_show_distance;
    public boolean job_show_arrow;
    /**
     * dialog text to pick the job
     */
    public String job_pick_option;
    /**
     * optional, can be used to trigger further scripts when the mission is taken, for example you may want to have competing bounty hunters
     */
    public String job_pick_script;
    /**
     * optional, MemKey set to false is added when accepting the job, set to true if the job is sucessful
     */
    public String job_memKey;
    /**
     * optional, can be used to give additional rewards or add further consequences in case of failure using memkeys to check the outcome
     */
    public String job_conclusion_script;
    /**
     * existing fleet
     * if non empty, the bounty will be placed on the existing fleet with that memkey. OVERRIDES EVERYTHING AFTER!
     */
    public String existing_target_memkey;

    //ALL OPTIONAL BELOW HERE
    /**
     * Enemy captain's first name.
     */
    public String target_first_name;
    /**
     * Enemy captain's last name.
     */
    public String target_last_name;
    /**
     * id of the sprite in settings.json/graphics/characters
     */
    public String target_portrait;
    /**
     * MALE, FEMALE, ANY
     */
    public FullName.Gender target_gender;
    /**
     * rank from campaign.ids.Ranks
     */
    public String target_rank;
    /**
     * post from campaign.ids.Ranks
     */
    public String target_post;
    /**
     * personality from campaign.ids.Personalities
     */
    public String target_personality;
    /**
     * if properly set, turn the target into a AI, makes it drop AI cores
     */
    public String target_aiCoreId;
    public int target_level;
    /**
     * Overrides the regular number of elite skills, set to -1 to ignore.
     */
    public int target_elite_skills;
    /**
     * GENERIC, PHASE, CARRIER, ANY from OfficerManagerEvent.SkillPickPreference
     */
    public OfficerManagerEvent.SkillPickPreference target_skill_preference;
    /**
     * OVERRIDES ALL RANDOM SKILLS!
     */
    public Map<String, Integer> target_skills;
    /**
     * bounty fleet
     */
    public String fleet_name;
    /**
     * faction of the fleet once it is generated, but not necessarily the faction of the ships inside
     */
    public String fleet_faction;
    public String fleet_flagship_variant;
    public String fleet_flagship_name;
    public boolean fleet_flagship_alwaysRecoverable;
    /**
     * if false the weapons won't get changed, but no D-mod will be added at low quality either
     */
    public boolean fleet_flagship_autofit;
    /**
     * optional preset fleet generated with the flagship, [variantId:number_of_ships]
     */
    public Map<String, Integer> fleet_preset_ships;
    /**
     * if false the weapons won't get changed, but no D-mod will be added at low quality either
     */
    public boolean fleet_preset_autofit;
    /**
     * dynamic reinforcements to match that amount of player fleet DP, set to 0 to ignore
     */
    public float fleet_scaling_multiplier;
    public int fleet_min_FP;
    /**
     * Faction of the extra ship, can be different from the bounty faction (in case of pirate deserters for example)
     */
    public String fleet_composition_faction;
    /**
     * default to 2 (no Dmods) if <0
     */
    public float fleet_composition_quality;
    public boolean fleet_transponder;
    /**
     * default to false, prevents the enemy from retreating
     */
    public boolean fleet_no_retreat;
    /**
     * PASSIVE, GUARDED, AGGRESSIVE, ROAMING, default to GUARDED (campaign.FleetAssignment.orbit_aggressive)
     */
    public FleetAssignment fleet_behavior;
    /**
     * The musicSetId to use for the fleet, default to the faction's default music set.
     * IMPORTANT: This must be added to the mod's `sounds.json` file. See "music_soe_fight" in the vanilla `sounds.json` for an example.
     */
    public String fleet_musicSetId;

    // Section: location

    /**
     * Preset locations (SectorEntityTokens, not markets, variable is poorly named) to spawn the bounty fleet, will fall back to other choices (if they are defined) and if this preset location doesn't exist (eg due to Nexerelin's random mode).
     */
    public List<String> location_marketIDs;
    /**
     * takes precedence over all other parameters but market ids
     */
    public List<String> location_marketFactions;
    /**
     * prefered distance, "CORE", "CLOSE" or "FAR". Can be left empty to ignore.
     */
    public String location_distance;
    /**
     * campaign.ids.Tags + "procgen_no_theme" + "procgen_no_theme_pulsar_blackhole"
     */
    public List<String> location_themes;
    public List<String> location_themes_blacklist;
    /**
     * PLANET, GATE, STATION, STABLE_LOCATION, DEBRIS, WRECK, PROBE.
     */
    public List<String> location_entities;
    /**
     * will pick in priority systems that have not been visited by the player yet, but won't override the distance requirements
     */
    public boolean location_prioritizeUnexplored;

    /**
     * if true and no system with the suitable theme is found, the script will pick any system that DOES NOT have a blacklisted theme.
     */
    // public boolean location_defaultToAnySystem;

    /**
     * if true and no suitable entity is found in systems with required themes and distance, a random entity will be picked instead.
     * if false, the script will ignore the distance requirement to attempt to find a suitable system
     */
    public boolean location_defaultToAnyEntity;

    public MagicBountySpec(
            List<String> trigger_market_id,
            List<String> trigger_marketFaction_any,
            boolean trigger_marketFaction_alliedWith,
            List<String> trigger_marketFaction_none,
            boolean trigger_marketFaction_enemyWith,
            int trigger_market_minSize,
            int trigger_player_minLevel,
            int trigger_min_days_elapsed,
            int trigger_min_fleet_size,
            float trigger_weight_mult,
            Map<String, Boolean> trigger_memKeys_all,
            Map<String, Boolean> trigger_memKeys_any,
            Map<String, Boolean> trigger_memKeys_none,
            Map<String, Float> trigger_playerRelationship_atLeast,
            Map<String, Float> trigger_playerRelationship_atMost,
            Float trigger_giverTargetRelationship_atLeast,
            Float trigger_giverTargetRelationship_atMost,
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
            Map<String, Integer> job_item_reward,
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
            String existing_target_memkey,
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
            OfficerManagerEvent.SkillPickPreference target_skill_preference,
            Map<String, Integer> target_skills,
            String fleet_name,
            String fleet_faction,
            String fleet_flagship_variant,
            String fleet_flagship_name,
            boolean fleet_flagship_alwaysRecoverable,
            boolean fleet_flagship_autofit,
            Map<String, Integer> fleet_preset_ships,
            boolean fleet_preset_autofit,
            float fleet_scaling_multiplier,
            int fleet_min_FP,
            String fleet_composition_faction,
            float fleet_composition_quality,
            boolean fleet_transponder,
            boolean fleet_no_retreat,
            FleetAssignment fleet_behavior,
            String fleet_musicSetId,
            List<String> location_marketIDs,
            List<String> location_marketFactions,
            String location_distance,
            List<String> location_themes,
            List<String> location_themes_blacklist,
            List<String> location_entities,
            boolean location_prioritizeUnexplored,
            //boolean location_defaultToAnySystem,
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
        this.trigger_memKeys_none = trigger_memKeys_none;
        this.trigger_playerRelationship_atLeast = trigger_playerRelationship_atLeast;
        this.trigger_playerRelationship_atMost = trigger_playerRelationship_atMost;
        this.trigger_giverTargetRelationship_atLeast = trigger_giverTargetRelationship_atLeast;
        this.trigger_giverTargetRelationship_atMost = trigger_giverTargetRelationship_atMost;
        this.job_name = job_name;
        this.job_description = job_description;
        this.job_comm_reply = job_comm_reply;
        this.job_intel_success = job_intel_success;
        this.job_intel_failure = job_intel_failure;
        this.job_intel_expired = job_intel_expired;
        this.job_forFaction = MagicTxt.nullStringIfEmpty(job_forFaction);
        this.job_difficultyDescription = job_difficultyDescription;
        this.job_deadline = job_deadline;
        this.job_credit_reward = job_credit_reward;
        this.job_credit_scaling = job_credit_scaling;
        this.job_reputation_reward = job_reputation_reward;
        this.job_item_reward = job_item_reward;
        if (job_type != null) {
            if (job_type.equalsIgnoreCase("assassination")) {
                this.job_type = MagicBountyLoader.JobType.Assassination;
            } else if (job_type.equalsIgnoreCase("destruction")) {
                this.job_type = MagicBountyLoader.JobType.Destruction;
            } else if (job_type.equalsIgnoreCase("obliteration")) {
                this.job_type = MagicBountyLoader.JobType.Obliteration;
            } else if (job_type.equalsIgnoreCase("neutralisation")
                    || job_type.equalsIgnoreCase("neutralization")) {
                this.job_type = MagicBountyLoader.JobType.Neutralization;
            } else {
                this.job_type = MagicBountyLoader.JobType.Assassination;
            }
        } else {
            this.job_type = MagicBountyLoader.JobType.Assassination;
        }
        this.job_show_type = job_show_type;
        this.job_show_captain = job_show_captain;

        if (job_show_fleet != null) {
            if (job_show_fleet.equalsIgnoreCase("all")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.All;
            } else if (job_show_fleet.equalsIgnoreCase("preset")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.Preset;
            } else if (job_show_fleet.equalsIgnoreCase("presetText")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.PresetText;
            } else if (job_show_fleet.equalsIgnoreCase("flagship")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.Flagship;
            } else if (job_show_fleet.equalsIgnoreCase("flagshipText")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.FlagshipText;
            } else if (job_show_fleet.equalsIgnoreCase("vanilla")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.Vanilla;
            } else if (job_show_fleet.equalsIgnoreCase("none")) {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.None;
            } else {
                this.job_show_fleet = MagicBountyLoader.ShowFleet.Vanilla;
            }
        } else {
            this.job_show_fleet = MagicBountyLoader.ShowFleet.Vanilla;
        }

        if (job_show_distance != null) {
            if (job_show_distance.equalsIgnoreCase("vague")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.Vague;
            } else if (job_show_distance.equalsIgnoreCase("distance")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.Distance;
            } else if (job_show_distance.equalsIgnoreCase("vanilla")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.Vanilla;
            } else if (job_show_distance.equalsIgnoreCase("vanillaDistance")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.VanillaDistance;
            } else if (job_show_distance.equalsIgnoreCase("exact")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.Exact;
            } else if (job_show_distance.equalsIgnoreCase("system")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.System;
            } else if (job_show_distance.equalsIgnoreCase("none")) {
                this.job_show_distance = MagicBountyLoader.ShowDistance.None;
            } else {
                this.job_show_distance = MagicBountyLoader.ShowDistance.VanillaDistance;
            }
        } else {
            this.job_show_distance = MagicBountyLoader.ShowDistance.VanillaDistance;
        }

        this.job_show_arrow = job_show_arrow;

        if (job_pick_option != null && !job_pick_option.equals("")) {
            this.job_pick_option = job_pick_option;
        } else {
            this.job_pick_option = MagicTxt.getString("mb_accept");
        }

        this.job_pick_script = job_pick_script;
        this.job_memKey = job_memKey;
        this.job_conclusion_script = job_conclusion_script;
        this.existing_target_memkey = existing_target_memkey;
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
        this.fleet_flagship_alwaysRecoverable = fleet_flagship_alwaysRecoverable;
        this.fleet_flagship_autofit = fleet_flagship_autofit;
        this.fleet_preset_ships = fleet_preset_ships;
        this.fleet_preset_autofit = fleet_preset_autofit;
        this.fleet_scaling_multiplier = fleet_scaling_multiplier;
        this.fleet_min_FP = fleet_min_FP;
        this.fleet_composition_faction = fleet_composition_faction;
        this.fleet_composition_quality = fleet_composition_quality;
        this.fleet_transponder = fleet_transponder;
        this.fleet_no_retreat = fleet_no_retreat;
        this.fleet_behavior = fleet_behavior;
        this.fleet_musicSetId = fleet_musicSetId;
        this.location_marketIDs = location_marketIDs;
        this.location_marketFactions = location_marketFactions;
        this.location_distance = location_distance;
        this.location_themes = location_themes;
        this.location_themes_blacklist = location_themes_blacklist;
        this.location_entities = location_entities;
        this.location_prioritizeUnexplored = location_prioritizeUnexplored;
        //this.location_defaultToAnySystem = location_defaultToAnySystem;
        this.location_defaultToAnyEntity = location_defaultToAnyEntity;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MagicBountySpec{");
        sb.append("\ntrigger_market_id=").append(trigger_market_id);
        sb.append(", \ntrigger_marketFaction_any=").append(trigger_marketFaction_any);
        sb.append(", \ntrigger_marketFaction_alliedWith=").append(trigger_marketFaction_alliedWith);
        sb.append(", \ntrigger_marketFaction_none=").append(trigger_marketFaction_none);
        sb.append(", \ntrigger_marketFaction_enemyWith=").append(trigger_marketFaction_enemyWith);
        sb.append(", \ntrigger_market_minSize=").append(trigger_market_minSize);
        sb.append(", \ntrigger_player_minLevel=").append(trigger_player_minLevel);
        sb.append(", \ntrigger_min_days_elapsed=").append(trigger_min_days_elapsed);
        sb.append(", \ntrigger_min_fleet_size=").append(trigger_min_fleet_size);
        sb.append(", \ntrigger_weight_mult=").append(trigger_weight_mult);
        sb.append(", \ntrigger_memKeys_all=").append(trigger_memKeys_all);
        sb.append(", \ntrigger_memKeys_any=").append(trigger_memKeys_any);
        sb.append(", \ntrigger_memKeys_none=").append(trigger_memKeys_none);
        sb.append(", \ntrigger_playerRelationship_atLeast=").append(trigger_playerRelationship_atLeast);
        sb.append(", \ntrigger_playerRelationship_atMost=").append(trigger_playerRelationship_atMost);
        sb.append(", \ntrigger_giverTargetRelationship_atLeast=").append(trigger_giverTargetRelationship_atLeast);
        sb.append(", \ntrigger_giverTargetRelationship_atMost=").append(trigger_giverTargetRelationship_atMost);
        sb.append(", \njob_name='").append(job_name).append('\'');
        sb.append(", \njob_description='").append(job_description).append('\'');
        sb.append(", \njob_comm_reply='").append(job_comm_reply).append('\'');
        sb.append(", \njob_intel_success='").append(job_intel_success).append('\'');
        sb.append(", \njob_intel_failure='").append(job_intel_failure).append('\'');
        sb.append(", \njob_intel_expired='").append(job_intel_expired).append('\'');
        sb.append(", \njob_forFaction='").append(job_forFaction).append('\'');
        sb.append(", \njob_difficultyDescription='").append(job_difficultyDescription).append('\'');
        sb.append(", \njob_deadline=").append(job_deadline);
        sb.append(", \njob_credit_reward=").append(job_credit_reward);
        sb.append(", \njob_credit_scaling=").append(job_credit_scaling);
        sb.append(", \njob_reputation_reward=").append(job_reputation_reward);
        sb.append(", \njob_item_reward=").append(job_item_reward);
        sb.append(", \njob_type=").append(job_type);
        sb.append(", \njob_show_type=").append(job_show_type);
        sb.append(", \njob_show_captain=").append(job_show_captain);
        sb.append(", \njob_show_fleet=").append(job_show_fleet);
        sb.append(", \njob_show_distance=").append(job_show_distance);
        sb.append(", \njob_show_arrow=").append(job_show_arrow);
        sb.append(", \njob_pick_option='").append(job_pick_option).append('\'');
        sb.append(", \njob_pick_script='").append(job_pick_script).append('\'');
        sb.append(", \njob_memKey='").append(job_memKey).append('\'');
        sb.append(", \njob_conclusion_script='").append(job_conclusion_script).append('\'');
        sb.append(", \nexisting_target_memkey='").append(existing_target_memkey).append('\'');
        sb.append(", \ntarget_first_name='").append(target_first_name).append('\'');
        sb.append(", \ntarget_last_name='").append(target_last_name).append('\'');
        sb.append(", \ntarget_portrait='").append(target_portrait).append('\'');
        sb.append(", \ntarget_gender=").append(target_gender);
        sb.append(", \ntarget_rank='").append(target_rank).append('\'');
        sb.append(", \ntarget_post='").append(target_post).append('\'');
        sb.append(", \ntarget_personality='").append(target_personality).append('\'');
        sb.append(", \ntarget_aiCoreId='").append(target_aiCoreId).append('\'');
        sb.append(", \ntarget_level=").append(target_level);
        sb.append(", \ntarget_elite_skills=").append(target_elite_skills);
        sb.append(", \ntarget_skill_preference=").append(target_skill_preference);
        sb.append(", \ntarget_skills=").append(target_skills);
        sb.append(", \nfleet_name='").append(fleet_name).append('\'');
        sb.append(", \nfleet_faction='").append(fleet_faction).append('\'');
        sb.append(", \nfleet_flagship_variant='").append(fleet_flagship_variant).append('\'');
        sb.append(", \nfleet_flagship_name='").append(fleet_flagship_name).append('\'');
        sb.append(", \nfleet_flagship_alwaysRecoverable=").append(fleet_flagship_alwaysRecoverable);
        sb.append(", \nfleet_flagship_autofit=").append(fleet_flagship_autofit);
        sb.append(", \nfleet_preset_ships=").append(fleet_preset_ships);
        sb.append(", \nfleet_preset_autofit=").append(fleet_preset_autofit);
        sb.append(", \nfleet_scaling_multiplier=").append(fleet_scaling_multiplier);
        sb.append(", \nfleet_min_FP=").append(fleet_min_FP);
        sb.append(", \nfleet_composition_faction='").append(fleet_composition_faction).append('\'');
        sb.append(", \nfleet_composition_quality=").append(fleet_composition_quality);
        sb.append(", \nfleet_transponder=").append(fleet_transponder);
        sb.append(", \nfleet_no_retreat=").append(fleet_no_retreat);
        sb.append(", \nfleet_behavior=").append(fleet_behavior);
        sb.append(", \nfleet_musicSetId=").append(fleet_musicSetId);
        sb.append(", \nlocation_marketIDs=").append(location_marketIDs);
        sb.append(", \nlocation_marketFactions=").append(location_marketFactions);
        sb.append(", \nlocation_distance='").append(location_distance).append('\'');
        sb.append(", \nlocation_themes=").append(location_themes);
        sb.append(", \nlocation_themes_blacklist=").append(location_themes_blacklist);
        sb.append(", \nlocation_entities=").append(location_entities);
        sb.append(", \nlocation_prioritizeUnexplored=").append(location_prioritizeUnexplored);
        sb.append(", \nlocation_defaultToAnyEntity=").append(location_defaultToAnyEntity);
        sb.append('}');
        return sb.toString();
    }
}
