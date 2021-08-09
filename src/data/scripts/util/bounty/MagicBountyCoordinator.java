package data.scripts.util.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
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
                spec.fleet_min_DP,
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
}
