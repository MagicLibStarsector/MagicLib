package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicSettings;
import data.scripts.util.MagicTxt;
import data.scripts.util.MagicVariables;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static data.scripts.util.MagicCampaign.createFleet;
import static data.scripts.util.MagicCampaign.findSuitableTarget;
import static data.scripts.util.MagicTxt.nullStringIfEmpty;
import static data.scripts.util.MagicVariables.MAGICLIB_ID;

/**
 * The point of entry into MagicBounty scripting.
 * Contains methods for getting all or specific {@link ActiveBounty}s, as well as general bounty management logic.
 *
 * <pre>
 * Usage: MagicBountyCoordinator.getInstance()
 * </pre>
 *
 * @author Wisp
 */
public final class MagicBountyCoordinator {
    private static MagicBountyCoordinator instance;
    private static final long MILLIS_PER_DAY = 86400000L;
    private static Logger logger = Global.getLogger(MagicBountyCoordinator.class);

    @NotNull
    public static MagicBountyCoordinator getInstance() {
        return instance;
    }

    public static void onGameLoad() {
        instance = new MagicBountyCoordinator();
        for (Map.Entry<String, MagicBountyData.bountyData> dataEntry : MagicBountyData.BOUNTIES.entrySet()) {
            validateAndCorrectIds(dataEntry.getKey(), dataEntry.getValue());
        }
    }

    @Nullable
    private Map<String, ActiveBounty> activeBountiesByKey = null;
    @Nullable
    private List<String> completedBountyKeys;
    private final static String BOUNTIES_MEMORY_KEY = "$MagicBounties_active_bounties";
    private final static String COMPLETED_BOUNTIES_MEMORY_KEY = "$MagicBounties_completed_keys";
    private final static String BOUNTIES_MARKETBOUNTIES_KEY = "$MagicBounties_bountyBar_bountykeys_";
    private final static String BOUNTIES_SEED_KEY = "$MagicBounties_bountyBarGenSeed";
    private final long UNACCEPTED_BOUNTY_LIFETIME_MILLIS =
            MagicSettings.getInteger(MAGICLIB_ID, "bounty_boardRefreshTimePerMarketInDays") * MILLIS_PER_DAY;

    /**
     * Returns a map (bounty key, bounty) of all active bounties. Note that this does not necessarily mean that they have
     * been accepted, just that they've been inflated from json (like being instantiated).
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public Map<String, ActiveBounty> getActiveBounties() {
        if (activeBountiesByKey == null) {
            activeBountiesByKey = (Map<String, ActiveBounty>) Global.getSector().getMemoryWithoutUpdate().get(BOUNTIES_MEMORY_KEY);

            if (activeBountiesByKey == null) {
                Global.getSector().getMemoryWithoutUpdate().set(BOUNTIES_MEMORY_KEY, new HashMap<>());
                activeBountiesByKey = (Map<String, ActiveBounty>) Global.getSector().getMemory().get(BOUNTIES_MEMORY_KEY);
            }
        }

        for (Iterator<Map.Entry<String, ActiveBounty>> iterator = activeBountiesByKey.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, ActiveBounty> entry = iterator.next();
            long timestampSinceBountyCreated = Math.max(0, Global.getSector().getClock().getTimestamp() - entry.getValue().getBountyCreatedTimestamp());

            // Clear out old bounties that were never accepted after UNACCEPTED_BOUNTY_LIFETIME_MILLIS days.
            if (timestampSinceBountyCreated > UNACCEPTED_BOUNTY_LIFETIME_MILLIS && entry.getValue().getStage() == ActiveBounty.Stage.NotAccepted) {
                Global.getLogger(MagicBountyCoordinator.class).info(
                        String.format("Removing expired bounty '%s' (not accepted after %d days), \"%s\"",
                                entry.getKey(),
                                timestampSinceBountyCreated / MILLIS_PER_DAY,
                                entry.getValue().getSpec().job_name));
                entry.getValue().endBounty(new ActiveBounty.BountyResult.ExpiredWithoutAccepting());
                iterator.remove();
            } else if (entry.getValue().getStage().ordinal() >= ActiveBounty.Stage.FailedSalvagedFlagship.ordinal()
                    && entry.getValue().getFleet().isDespawning()
                    && entry.getValue().getIntel() == null) {
                // Remove bounties that have completed and their fleets have/are despawning and the intel has timed out.
                iterator.remove();
                getCompletedBounties().add(entry.getKey());
            }
        }

        return activeBountiesByKey;
    }

    @NotNull
    public List<String> getCompletedBounties() {
        if (completedBountyKeys == null) {
            completedBountyKeys = (List<String>) Global.getSector().getMemoryWithoutUpdate().get(COMPLETED_BOUNTIES_MEMORY_KEY);

            if (completedBountyKeys == null) {
                Global.getSector().getMemoryWithoutUpdate().set(COMPLETED_BOUNTIES_MEMORY_KEY, new ArrayList<>());
                completedBountyKeys = (List<String>) Global.getSector().getMemory().get(COMPLETED_BOUNTIES_MEMORY_KEY);
            }
        }

        return completedBountyKeys;
    }

    /**
     * Gets a single active bounty by key.
     */
    @Nullable
    public ActiveBounty getActiveBounty(@NotNull String bountyKey) {
        return getActiveBounties().get(bountyKey);
    }

    /**
     * Whether or not a market is a viable candidate for showing the bounty board.
     * Takes into account blacklisting of locations.
     */
    public boolean shouldShowBountyBoardAt(@Nullable MarketAPI marketAPI) {
        if (marketAPI == null) return false;

        return !getBountiesWithChanceToSpawnAtMarketById(marketAPI).isEmpty();
    }

    @NotNull
    public Map<String, MagicBountyData.bountyData> getBountiesWithChanceToSpawnAtMarketById(@NotNull MarketAPI market) {
        Map<String, MagicBountyData.bountyData> available = new HashMap<>();

        // Run checks on each bounty to see if it should be displayed.
        for (String bountyKey : MagicBountyData.BOUNTIES.keySet()) {
            MagicBountyData.bountyData bountySpec = MagicBountyData.BOUNTIES.get(bountyKey);

            // If the bounty has been completed, don't offer it.
            if (getCompletedBounties().contains(bountyKey)) {
                continue;
            }

            // If it's not available at this market, skip over it.
            if (!MagicCampaign.isAvailableAtMarket(
                    market,
                    bountySpec.trigger_market_id,
                    bountySpec.trigger_marketFaction_any,
                    bountySpec.trigger_marketFaction_alliedWith,
                    bountySpec.trigger_marketFaction_none,
                    bountySpec.trigger_marketFaction_enemyWith,
                    bountySpec.trigger_market_minSize)) {
                continue;
            }

            //adding testing mode
            if (!MagicVariables.bounty_test_mode) {
                if (!MagicCampaign.isAvailableToPlayer(
                        bountySpec.trigger_player_minLevel,
                        bountySpec.trigger_min_days_elapsed,
                        bountySpec.trigger_min_fleet_size,
                        bountySpec.trigger_memKeys_all,
                        bountySpec.trigger_memKeys_any,
                        bountySpec.trigger_memKeys_none,
                        bountySpec.trigger_playerRelationship_atLeast,
                        bountySpec.trigger_playerRelationship_atMost
                )) {
                    continue;
                }
            }
            
            //CHECK FOR EXISTING FLEET
            if(bountySpec.existing_target_memkey!=null){
                boolean targetFleetGone=true;
                for(StarSystemAPI s : Global.getSector().getStarSystems()){
                    for(CampaignFleetAPI f : s.getFleets()){
                        if(f.getMemoryWithoutUpdate().contains(bountySpec.existing_target_memkey)){
                            targetFleetGone=false;
                            break;
                        }                        
                    }
                    if(!targetFleetGone)break;
                }
                if(targetFleetGone)continue;
            }
            
            ActiveBounty activeBounty = getActiveBounty(bountyKey);

            // If the bounty has already been created and it's not not-accepted, don't offer it (it's been accepted, failed, timed out, etc).
            if (activeBounty != null && activeBounty.getStage() != ActiveBounty.Stage.NotAccepted) {
                continue;
            }

            // Passed all checks, add to list
            available.put(bountyKey, bountySpec);
        }

        return available;
    }

    public int getBountySlotsAtMarket(MarketAPI marketAPI) {
        int offersAtSizeThree = MagicSettings.getInteger(MAGICLIB_ID, "bounty_offersAtSizeThree");
        return Math.max(0, marketAPI.getSize() - 3 + offersAtSizeThree);
    }

    @Nullable
    public List<String> getBountiesAcceptedAtMarket(@NotNull MarketAPI marketAPI) {
        MemoryAPI memoryAPI = marketAPI.getMemory();
        String key = BOUNTIES_MARKETBOUNTIES_KEY;

        if (memoryAPI.contains(key) && memoryAPI.get(key) != null) {
            try {
                return (List<String>) memoryAPI.get(key);
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }

        return null;
    }

    /**
     * Marks the bounty as blocking a slot on the market until expiration (default 30 days).
     * Until expiration, another bounty cannot take that slot. This prevents accepting all bounties from a market at once.
     */
    public void setBlockBountyAtMarket(@NotNull MarketAPI marketAPI, @NotNull String bountyId) {
        MemoryAPI memoryAPI = marketAPI.getMemoryWithoutUpdate();
        List<String> bountiesBlockedAtMarket = memoryAPI.get(BOUNTIES_MARKETBOUNTIES_KEY) != null
                ? (List<String>) memoryAPI.get(BOUNTIES_MARKETBOUNTIES_KEY)
                : new ArrayList<String>();

        if (!bountiesBlockedAtMarket.contains(bountyId)) {
            bountiesBlockedAtMarket.add(bountyId);
            memoryAPI.set(BOUNTIES_MARKETBOUNTIES_KEY, bountiesBlockedAtMarket, 30f);
        }
    }

    /**
     * Gets the seed used to generate bounties at the given market.
     * This seed will change every 30 days.
     * Unless the player times it to save scum just as the seed changes, this should prevent it.
     */
    public int getMarketBountyBoardGenSeed(@NotNull MarketAPI marketAPI) {
        MemoryAPI memoryWithoutUpdate = Global.getSector().getMemoryWithoutUpdate();
        String key = BOUNTIES_SEED_KEY;

        if (!memoryWithoutUpdate.contains(key) && memoryWithoutUpdate.getLong(key) != 0L) {
            memoryWithoutUpdate.set(key, Misc.genRandomSeed(), 30f);
        }

        return Objects.hash(memoryWithoutUpdate.getLong(key) + marketAPI.getId());
    }

    public ActiveBounty createActiveBounty(String bountyKey, MagicBountyData.bountyData spec) {
        
        SectorEntityToken suitableTargetLocation=null;
        CampaignFleetAPI fleet=null;
        ArrayList<String> presetShipIds = new ArrayList<>();
        
        //CHECK IF THE BOUNTY IS ON A NEW FLEET OR EXISTING ONE
        if(spec.existing_target_memkey!=null){
            
            //the bounty is on an existing fleet, so this is pretty quick, the fleet must exist otherwise the bounty cannot be offered
            for(StarSystemAPI s : Global.getSector().getStarSystems()){
                for(CampaignFleetAPI f : s.getFleets()){
                    if(f.getMemoryWithoutUpdate().contains(spec.existing_target_memkey)){
                        fleet=f;
                        break;
                    }                        
                }
                if(fleet!=null)break;
            }
            if(fleet==null){
                Global.getLogger(MagicBountyCoordinator.class).error("Existing fleet couldn't be found for bounty " + bountyKey);
                return null;
            }
            suitableTargetLocation=fleet.getCurrentAssignment().getTarget();
            
        } else {
            
            //the bounty has to create the fleet
            suitableTargetLocation = findSuitableTarget(
                    spec.location_marketIDs,
                    spec.location_marketFactions,
                    spec.location_distance,
                    spec.location_themes,
                    spec.location_themes_blacklist,
                    spec.location_entities,
                    spec.location_defaultToAnyEntity,
                    spec.location_prioritizeUnexplored,
                    Global.getSettings().isDevMode()
            );

            if (suitableTargetLocation == null) {
                Global.getLogger(MagicBountyCoordinator.class).error("No suitable spawn location could be found for bounty " + bountyKey);
                return null;
            }

            PersonAPI captain = MagicCampaign.createCaptain(
                    // because apparently putting null in the json shows up as "null", a string...
                    spec.target_aiCoreId != null && !spec.target_aiCoreId.equals("null"),
                    spec.target_aiCoreId,
                    nullStringIfEmpty(spec.target_first_name),
                    nullStringIfEmpty(spec.target_last_name),
                    nullStringIfEmpty(spec.target_portrait),
                    spec.target_gender,
                    nullStringIfEmpty(spec.fleet_composition_faction),
                    nullStringIfEmpty(spec.target_rank),
                    nullStringIfEmpty(spec.target_post),
                    nullStringIfEmpty(spec.target_personality),
                    spec.target_level,
                    spec.target_elite_skills,
                    spec.target_skill_preference,
                    spec.target_skills
            );

            fleet = createFleet(
                    spec.fleet_name,
                    spec.fleet_faction,
                    FleetTypes.PERSON_BOUNTY_FLEET,
                    spec.fleet_flagship_name,
                    spec.fleet_flagship_variant,
                    false,
                    spec.fleet_flagship_autofit,
                    captain,
                    spec.fleet_preset_ships,
                    spec.fleet_preset_autofit,
                    calculateDesiredFP(spec),
                    spec.fleet_composition_faction,
                    spec.fleet_composition_quality,
                    null,
                    spec.fleet_behavior,
                    null,
                    false,
                    spec.fleet_transponder,
                    MagicVariables.VARIANT_PATH
            );
            presetShipIds = new ArrayList<>(MagicVariables.presetShipIdsOfLastCreatedFleet);

            if (fleet == null) {
                Global.getLogger(MagicBountyCoordinator.class).warn(String.format("Unable to create fleet for bounty %s, look for earlier warnings/errors.", bountyKey));
                return null;
            }

            // Add both a constant tag to the fleet as well as the bounty key that it is for.
            fleet.addTag(MagicBountyData.BOUNTY_FLEET_TAG);
            fleet.addTag(bountyKey);

            // Set fleet to max CR
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            }
        }

        ActiveBounty newBounty = new ActiveBounty(bountyKey, fleet, suitableTargetLocation, presetShipIds, spec);

        getActiveBounties().put(bountyKey, newBounty);
        configureBountyListeners();
        return newBounty;
    }

    /**
     * Idem potently ensures that each `ActiveBounty` has a `MagicBountyBattleListener` running.
     */
    public void configureBountyListeners() {
        Collection<ActiveBounty> bounties = getActiveBounties().values();

        for (ActiveBounty bounty : bounties) {
            boolean doesBountyHaveListener = false;

            for (FleetEventListener eventListener : bounty.getFleet().getEventListeners()) {
                if (eventListener instanceof MagicBountyBattleListener) {
                    doesBountyHaveListener = true;
                    break;
                }
            }

            if (!doesBountyHaveListener) {
                bounty.getFleet().addEventListener(new MagicBountyBattleListener(bounty.getKey()));
            }
        }
    }

    public void resetBounty(@NotNull String bountyKey) {
        ActiveBounty activeBounty = getActiveBounty(bountyKey);

        if (activeBounty == null) return;
        MagicBountyData.bountyData spec = activeBounty.getSpec();

        if (MagicTxt.nullStringIfEmpty(spec.job_memKey) != null) {
            Global.getSector().getMemoryWithoutUpdate().set(spec.job_memKey, null);
        }

        MagicBountyIntel intel = activeBounty.getIntel();

        if (intel != null) {
            intel.endImmediately();
        }

        activeBounty.despawn();
    }

    /**
     * Per Tartiflette:
     * ```
     * For example,
     * the bounty has a min FP of 100
     * a reward of 100K
     * a fleet scaling of 0.75
     * <p>
     * if say the player has a fleet worth 200FP
     * the bounty fleet will be scaled to 175FP (100FP + 0.75 x 100FP of difference)
     * ```
     */
    private static int calculateDesiredFP(MagicBountyData.bountyData spec) {
        int differenceBetweenBountyMinDPAndPlayerFleetDP = Global.getSector().getPlayerFleet().getFleetPoints() - spec.fleet_min_FP;

        // Math.max so that if the player fleet is super weak and the difference is negative, we don't scale to below
        // the minimum DP.
        int desiredFP = Math.round(spec.fleet_min_FP + Math.max(0, spec.fleet_scaling_multiplier * differenceBetweenBountyMinDPAndPlayerFleetDP));

        Global.getLogger(MagicBountyCoordinator.class)
                .info(String.format("After scaling, bounty fleet '%s' should have %s FP (%s min + (%s mult * %s diff between player FP and min FP))",
                        spec.job_name,
                        desiredFP,
                        spec.fleet_min_FP,
                        spec.fleet_scaling_multiplier,
                        differenceBetweenBountyMinDPAndPlayerFleetDP));

        return desiredFP;
    }

    private static boolean validateAndCorrectIds(String bountyId, MagicBountyData.bountyData this_bounty) {
        Logger logger = Global.getLogger(MagicBountyCoordinator.class);

        try {
            // fleet_faction
            String fleetFactionId = nullStringIfEmpty(this_bounty.fleet_faction);
            if(fleetFactionId!=null){
                FactionAPI faction = StringMatcher.findBestFactionMatch(fleetFactionId);

                if (faction == null) {
                    logger.info(String.format("Unable to find faction '%s' from bounty %s.", fleetFactionId, bountyId));
                    return false;
                } else if (!Objects.equals(faction.getId(), fleetFactionId)) {
                    logger.info(String.format("Corrected faction id '%s' to '%s' in bounty %s.", fleetFactionId, faction.getId(), bountyId));
                    this_bounty.fleet_faction = faction.getId();
                }
            }

            // fleet_composition_faction
            String compositionFactionId = nullStringIfEmpty(this_bounty.fleet_composition_faction);

            if (compositionFactionId != null) {
                FactionAPI fleetCompositionFaction = StringMatcher.findBestFactionMatch(compositionFactionId);

                if (fleetCompositionFaction != null && !Objects.equals(fleetCompositionFaction.getId(), compositionFactionId)) {
                    logger.info(String.format("Corrected fleet_composition_faction '%s' to '%s' in bounty %s.", compositionFactionId, fleetCompositionFaction.getId(), bountyId));
                    this_bounty.fleet_composition_faction = fleetCompositionFaction.getId();
                }
            }

            // job_forFaction
            String job_forFactionId = nullStringIfEmpty(this_bounty.job_forFaction);

            if (job_forFactionId != null) {
                FactionAPI job_forFaction = StringMatcher.findBestFactionMatch(job_forFactionId);

                if (job_forFaction != null && !Objects.equals(job_forFaction.getId(), job_forFactionId)) {
                    logger.info(String.format("Corrected job_forFactionId '%s' to '%s' in bounty %s.", job_forFactionId, job_forFaction.getId(), bountyId));
                    this_bounty.job_forFaction = job_forFaction.getId();
                }
            }

            // location_marketFactions
            List<String> location_marketFactions = this_bounty.location_marketFactions;

            for (int i = location_marketFactions.size() - 1; i >= 0; i--) {
                String location_marketFactionId = location_marketFactions.get(i);
                FactionAPI location_marketFaction = StringMatcher.findBestFactionMatch(location_marketFactionId);

                if (location_marketFaction != null && !Objects.equals(location_marketFaction.getId(), location_marketFactionId)) {
                    logger.info(String.format("Corrected location_marketFactionId '%s' to '%s' in bounty %s.", location_marketFactionId, location_marketFaction.getId(), bountyId));
                    this_bounty.location_marketFactions.remove(i);
                    this_bounty.location_marketFactions.add(location_marketFaction.getId());
                }
            }

            // trigger_marketFaction_any
            List<String> trigger_marketFaction_any = this_bounty.trigger_marketFaction_any;

            for (int i = trigger_marketFaction_any.size() - 1; i >= 0; i--) {
                String trigger_marketFaction_anyId = trigger_marketFaction_any.get(i);
                FactionAPI trigger_marketFaction = StringMatcher.findBestFactionMatch(trigger_marketFaction_anyId);

                if (trigger_marketFaction != null && !Objects.equals(trigger_marketFaction.getId(), trigger_marketFaction_anyId)) {
                    logger.info(String.format("Corrected trigger_marketFaction_any '%s' to '%s' in bounty %s.", trigger_marketFaction_anyId, trigger_marketFaction.getId(), bountyId));
                    this_bounty.trigger_marketFaction_any.remove(i);
                    this_bounty.trigger_marketFaction_any.add(trigger_marketFaction.getId());
                }
            }

            // trigger_marketFaction_none
            List<String> trigger_marketFaction_none = this_bounty.trigger_marketFaction_none;

            for (int i = trigger_marketFaction_none.size() - 1; i >= 0; i--) {
                String trigger_marketFaction_noneId = trigger_marketFaction_none.get(i);
                FactionAPI trigger_marketFaction = StringMatcher.findBestFactionMatch(trigger_marketFaction_noneId);

                if (trigger_marketFaction != null && !Objects.equals(trigger_marketFaction.getId(), trigger_marketFaction_noneId)) {
                    logger.info(String.format("Corrected trigger_marketFaction_none '%s' to '%s' in bounty %s.", trigger_marketFaction_noneId, trigger_marketFaction.getId(), bountyId));
                    this_bounty.trigger_marketFaction_none.remove(i);
                    this_bounty.trigger_marketFaction_none.add(trigger_marketFaction.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Something went wrong when validating " + bountyId, e);
        }

        return true;
    }
}
