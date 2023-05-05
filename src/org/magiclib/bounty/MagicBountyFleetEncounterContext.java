package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class MagicBountyFleetEncounterContext extends FleetEncounterContext {

    /**
     * Ensures that recoverable ships are recoverable.
     * Mostly copied from SWP (0.95a Tournament edition), which in turn is mostly copied from vanilla FleetEncounterContext.
     *
     * @author Wisp
     */
    @Override
    public List<FleetMemberAPI> getRecoverableShips(BattleAPI battle, CampaignFleetAPI winningFleet, CampaignFleetAPI otherFleet) {
        List<FleetMemberAPI> recoverableShips = super.getRecoverableShips(battle, winningFleet, otherFleet);

        if (Misc.isPlayerOrCombinedContainingPlayer(otherFleet)) {
            return recoverableShips;
        }

        Collection<ActiveBounty> bounties = MagicBountyCoordinator.getInstance().getActiveBounties().values();

        float playerContribFraction = computePlayerContribFraction();
        DataForEncounterSide winnerData = getDataFor(winningFleet);
        List<FleetMemberData> enemyCasualties = winnerData.getEnemyCasualties();

        for (FleetMemberData data : enemyCasualties) {
            if (Misc.isUnboardable(data.getMember())) {
                continue;
            }

            if ((data.getStatus() != Status.DISABLED) && (data.getStatus() != Status.DESTROYED)) {
                continue;
            }

            /* Don't double-add */
            if (recoverableShips.contains(data.getMember())) {
                continue;
            }
            if (getStoryRecoverableShips().contains(data.getMember())) {
                continue;
            }

            boolean isFlagshipAlwaysRecoverable = false;

            for (ActiveBounty bounty : bounties) {
                if (data.getMember().getId().equals(bounty.getFlagshipId())
                        && bounty.getSpec().fleet_flagship_alwaysRecoverable) {
                    isFlagshipAlwaysRecoverable = true;
                    break;
                }
            }

            if (!isFlagshipAlwaysRecoverable) continue;

            if (playerContribFraction > 0f) {
                // Create a new captain
                data.getMember().setCaptain(Global.getFactory().createPerson());

                //
                ShipVariantAPI variant = data.getMember().getVariant();
                variant = variant.clone();
                variant.setSource(VariantSource.REFIT);
                variant.setOriginalVariant(null);
                data.getMember().setVariant(variant, false, true);

                // Add some D-mods to the damaged ship
                Random dModRandom = new Random(1000000L * data.getMember().getId().hashCode() + Global.getSector().getPlayerBattleSeed());
                dModRandom = Misc.getRandom(dModRandom.nextLong(), 5);
                DModManager.addDMods(data, false, Global.getSector().getPlayerFleet(), dModRandom);
                if (DModManager.getNumDMods(variant) > 0) {
                    DModManager.setDHull(variant);
                }

                float weaponProb = Global.getSettings().getFloat("salvageWeaponProb");
                float wingProb = Global.getSettings().getFloat("salvageWingProb");

                prepareShipForRecovery(data.getMember(), false, true, false, weaponProb, wingProb, getSalvageRandom());

                getStoryRecoverableShips().add(data.getMember());

                Global.getLogger(MagicBountyFleetEncounterContext.class).info(String.format("Added SP recoverable ship: %s", data.getMember().getShipName()));
            }
        }

        return recoverableShips;
    }

    /**
     * Adds specified loot to the drop when player defeats the flagship.
     */
    @Override
    protected void generatePlayerLoot(List<FleetMemberAPI> recoveredShips, boolean withCredits) {
        super.generatePlayerLoot(recoveredShips, withCredits);
        ActiveBounty bounty = null;
        MagicBountyCoordinator magicBountyCoordinator = MagicBountyCoordinator.getInstance();

        if (getLoser() == null) return;

        for (String key : magicBountyCoordinator.getActiveBounties().keySet()) {
            for (CampaignFleetAPI losingFleet : getBattle().getSnapshotSideFor(getLoser())) {
                if (losingFleet.hasTag(key)) {
                    bounty = magicBountyCoordinator.getActiveBounty(key);
                }
            }
        }

        if (bounty == null) {
            Global.getLogger(MagicBountyFleetEncounterContext.class).debug("MagicBounty battle happened but couldn't find bounty key in loser's tags (did you lose?! noob)");
            return;
        }

        FleetMemberAPI bountyFlagship = null;

        for (FleetMemberAPI member : getLoser().getFleetData().getMembersListCopy()) {
            if (Objects.equals(member.getId(), bounty.getFlagshipId())) {
                bountyFlagship = member;
            }
        }

        if (bountyFlagship != null) {
            Global.getLogger(MagicBountyFleetEncounterContext.class).debug("MagicBounty battle happened but flagship wasn't disabled, not giving loot.");
            return;
        }

        // Add AI core if commander was an AI
        if (bounty.getSpec().target_aiCoreId != null) {
            CommoditySpecAPI core = Global.getSector().getEconomy().getCommoditySpec(bounty.getSpec().target_aiCoreId);

            if (core != null) {
                loot.addCommodity(core.getId(), 1);
            }
        }

        //no extra reward if the bounty has been failed
        if (bounty.getStage() == ActiveBounty.Stage.FailedSalvagedFlagship) {
            Global.getLogger(MagicBountyFleetEncounterContext.class).debug("MagicBounty has been failed through flagship recovery, no extra items.");
            return;
        }

        // Add special items
        for (Map.Entry<String, Integer> entry : bounty.getSpec().job_item_reward.entrySet()) {
            String itemId = entry.getKey();
            Integer count = entry.getValue();

            try {
                CommoditySpecAPI commoditySpec = Global.getSector().getEconomy().getCommoditySpec(itemId);
                if (commoditySpec != null) {
                    loot.addItems(CargoAPI.CargoItemType.RESOURCES, itemId, count);
                    continue;
                }
            } catch (Exception ex) {
                Global.getLogger(MagicBountyFleetEncounterContext.class).warn(itemId + "loot is not a commodity", ex);
            }

            try {
                WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(itemId);
                if (weaponSpec != null) {
                    loot.addWeapons(itemId, count);
                    continue;
                }
            } catch (Exception ex) {
                Global.getLogger(MagicBountyFleetEncounterContext.class).warn(itemId + "loot is not a weapon", ex);
            }

            try {
                FighterWingSpecAPI fighterWingSpecSpec = Global.getSettings().getFighterWingSpec(itemId);
                if (fighterWingSpecSpec != null) {
                    loot.addFighters(itemId, count);
                    continue;
                }
            } catch (Exception ex) {
                Global.getLogger(MagicBountyFleetEncounterContext.class).warn(itemId + "loot is not a fighter LCP", ex);
            }

            try {
                HullModSpecAPI hullmodSpec = Global.getSettings().getHullModSpec(itemId);
                if (hullmodSpec != null) {
                    loot.addHullmods(itemId, count);
                    continue;
                }
            } catch (Exception ex) {
                Global.getLogger(MagicBountyFleetEncounterContext.class).warn(itemId + "loot is not a hullmod", ex);
            }

            try {
                String[] split = itemId.split(" ");
                String specialItemId = split[0];
                String data = null;

                if (split.length > 1) {
                    data = split[1];
                }

                SpecialItemSpecAPI specialItemSpec = Global.getSettings().getSpecialItemSpec(specialItemId);
                if (specialItemSpec != null) {
                    loot.addSpecial(new SpecialItemData(specialItemId, data), count);
                } else {
                    Global.getLogger(MagicBountyFleetEncounterContext.class).warn(itemId + "loot is not a special item", new NullPointerException());
                }
            } catch (Exception ex) {
                Global.getLogger(MagicBountyFleetEncounterContext.class).warn("Unable to add loot: " + itemId, ex);
            }
        }
    }
}
