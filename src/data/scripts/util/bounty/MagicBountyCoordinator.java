package data.scripts.util.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicBountyData;
import data.scripts.util.MagicCampaign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static data.scripts.util.MagicCampaign.createFleet;
import static data.scripts.util.MagicCampaign.findSuitableTarget;
import static data.scripts.util.MagicTxt.nullStringIfEmpty;

public final class MagicBountyCoordinator {
    private static MagicBountyCoordinator instance;
    private static final long MILLIS_PER_DAY = 86400000L;

    @NotNull
    public static MagicBountyCoordinator getInstance() {
        return instance;
    }

    public static void onGameLoad() {
        instance = new MagicBountyCoordinator();
    }

    @Nullable
    private Map<String, ActiveBounty> activeBountiesByKey = null;
    private final static String BOUNTIES_MEMORY_KEY = "$MagicBounties";
    private final static long UNACCEPTED_BOUNTY_LIFETIME_MILLIS = 90L * MILLIS_PER_DAY;

    @SuppressWarnings("unchecked")
    @NotNull
    public Map<String, ActiveBounty> getActiveBounties() {
        if (activeBountiesByKey == null) {
            activeBountiesByKey = (Map<String, ActiveBounty>) Global.getSector().getMemoryWithoutUpdate().get(BOUNTIES_MEMORY_KEY);

            if (activeBountiesByKey == null) {
                Global.getSector().getMemoryWithoutUpdate().set(BOUNTIES_MEMORY_KEY, new HashMap<>());
                activeBountiesByKey = (Map<String, ActiveBounty>) Global.getSector().getMemoryWithoutUpdate().get(BOUNTIES_MEMORY_KEY);
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
            }
        }

        return activeBountiesByKey;
    }

    @Nullable
    public ActiveBounty getActiveBounty(@NotNull String bountyKey) {
        return getActiveBounties().get(bountyKey);
    }

    public boolean shouldShowBountyBoardAt(@Nullable MarketAPI marketAPI) {
        //TODO implement blacklists (both faction and individual markets) via modSettings
        //TODO filter: min market size? stability? unrest?
        if (marketAPI == null) return false;

        return !getBountiesAtMarketById(marketAPI).isEmpty();
    }

    @NotNull
    public Map<String, MagicBountyData.bountyData> getBountiesAtMarketById(MarketAPI market) {
        Map<String, MagicBountyData.bountyData> available = new HashMap<>();

        for (String key : MagicBountyData.BOUNTIES.keySet()) {
            MagicBountyData.bountyData bounty = MagicBountyData.BOUNTIES.get(key);

            if (MagicCampaign.isAvailableAtMarket(
                    market,
                    bounty.trigger_market_id,
                    bounty.trigger_marketFaction_any,
                    bounty.trigger_marketFaction_alliedWith,
                    bounty.trigger_marketFaction_none,
                    bounty.trigger_marketFaction_enemyWith,
                    bounty.trigger_market_minSize)) {
                available.put(key, bounty);
            }
        }

        // Use iterator so we can remove items while looping
        for (Iterator<String> iterator = available.keySet().iterator(); iterator.hasNext(); ) {
            String bountyKey = iterator.next();

            MagicBountyData.bountyData bountySpec = available.get(bountyKey);

            // If a memKey is defined and doesn't exist, don't offer the bounty.
            // TODO might be using this wrong
//            if (MagicTxt.nullStringIfEmpty(bountySpec.job_memKey) != null
//                    && Global.getSector().getMemoryWithoutUpdate().get(bountySpec.job_memKey) == null) {
//                iterator.remove();
//                continue;
//            }

            ActiveBounty activeBounty = getActiveBounty(bountyKey);

            // If the bounty has already been created and they've accepted it, don't offer it again
            if (activeBounty != null && activeBounty.getStage() != ActiveBounty.Stage.NotAccepted) {
                iterator.remove();
            }
        }

        return available;
    }

    public ActiveBounty createActiveBounty(String bountyKey, MagicBountyData.bountyData spec) {
        SectorEntityToken suitableTargetLocation = findSuitableTarget(
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
                spec.target_isAI,
                null, // TODO
                nullStringIfEmpty(spec.target_first_name),
                nullStringIfEmpty(spec.target_last_name),
                nullStringIfEmpty(spec.target_portrait),
                spec.target_gender,
                nullStringIfEmpty(spec.fleet_faction),
                nullStringIfEmpty(spec.target_rank),
                nullStringIfEmpty(spec.target_post),
                nullStringIfEmpty(spec.target_personality),
                spec.target_level,
                spec.target_elite_skills,
                spec.target_skill_preference,
                spec.target_skills
        );

        CampaignFleetAPI fleet = createFleet(
                spec.fleet_name,
                spec.fleet_faction,
                FleetTypes.PERSON_BOUNTY_FLEET,
                spec.fleet_flagship_name,
                spec.fleet_flagship_variant,
                captain,
                spec.fleet_preset_ships,
                calculateDesiredFP(spec),
                spec.fleet_composition_faction,
                spec.fleet_composition_quality,
                null,
                spec.fleet_behavior,
                null,
                false,
                spec.fleet_transponder
        );

        if (fleet == null) {
            return null;
        }

        // Set fleet to max CR
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }

        if (spec.fleet_flagship_recoverable) {
            float weaponProb = Global.getSettings().getFloat("salvageWeaponProb");
            float wingProb = Global.getSettings().getFloat("salvageWingProb");
            FleetEncounterContext.prepareShipForRecovery(
                    fleet.getFlagship(), true, true, false, weaponProb, wingProb, Misc.random
            );
        } else {
            fleet.getFlagship().getVariant().removeTag(Tags.SHIP_RECOVERABLE);
        }

        ActiveBounty newBounty = new ActiveBounty(bountyKey, fleet, suitableTargetLocation, spec);
        getActiveBounties().put(bountyKey, newBounty);
        configureBountyListeners();
        return newBounty;
    }

    /**
     * Idempotently ensures that `MagicBountyScript` exists and is running.
     */
    public void configureBountyScript() {
        if (!Global.getSector().hasScript(MagicBountyScript.class)) {
            Global.getSector().addScript(new MagicBountyScript());
        }
    }

    /**
     * Idempotently ensures that each `ActiveBounty` has a `MagicBountyBattleListener` running.
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
        int differenceBetweenBountyMinDPAndPlayerFleetDP = Global.getSector().getPlayerFleet().getFleetPoints() - spec.fleet_min_DP;

        // Math.max so that if the player fleet is super weak and the difference is negative, we don't scale to below
        // the minimum DP.
        int desiredFP = Math.round(spec.fleet_min_DP + Math.max(0, spec.fleet_scaling_multiplier * differenceBetweenBountyMinDPAndPlayerFleetDP));

        Global.getLogger(MagicBountyCoordinator.class)
                .info(String.format("Bounty '%s' should have %s FP (%s min + (%s mult * %s diff between player FP and min FP))",
                        spec.job_name,
                        desiredFP,
                        spec.fleet_min_DP,
                        spec.fleet_scaling_multiplier,
                        differenceBetweenBountyMinDPAndPlayerFleetDP));

        return desiredFP;
    }
}
