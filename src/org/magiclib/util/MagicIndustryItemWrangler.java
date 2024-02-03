package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.GenericInstallableItemPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MagicIndustryItemWrangler extends BaseCampaignEventListener {

    private static final Map ITEMS_PRIORITIES = new HashMap(); // should really be typed <String,ItemPriority>

    public MagicIndustryItemWrangler() {
        super(false);
        Map<String, Float> items = MagicSettings.getFloatMap(MagicVariables.MAGICLIB_ID, "itemPriorities");

        if (items == null || items.isEmpty()) {
            Global.getLogger(MagicIndustryItemWrangler.class).warn("Error loading items priorities. ItemInstallationWrangler will not function.");
            return;
        }

        for (String i : items.keySet()) {
            ITEMS_PRIORITIES.put(i, new FixedPriority(items.get(i)));
        }
        addItemPriority("cryoarithmetic_engine", new ItemPriority() {
            @Override
            public float getPriority(Industry industry) {
                if (industry != null) {
                    MarketAPI market = industry.getMarket();
                    if (market != null && market.hasCondition("very_hot")) {
                        return 100.0f;
                    } else if (market != null && market.hasCondition("hot")) {
                        return 25.0f;
                    }
                }
                return 0.0f;
            }
        });
    }

    public static void addItemPriority(String itemId, ItemPriority priority) {
        ITEMS_PRIORITIES.put(itemId, priority);
    }

    // vanilla implementation is in com.fs.starfarer.api.impl.campaign.CoreScript
    // The vanilla implementation will not install better items over worse,
    //		unless the better item is a pristine nanoforge and the worse item is a corrupted nanoforge.
    // The vanilla implementation will also not split stacks - it will install one item in one industry,
    //		even if the market has multiple industries that a stack of items could apply to.
    // The vanilla implementation will run first, resulting in us getting data that's already been modified -
    //		i.e., if someone sells one thingamajiggit & vanilla code installs that into an industry,
    //		then we'll get data telling us "the player sold a thingamajiggit" which will have already
    //		been removed from the market's cargoAPI. So we've got to watch for that.
    // If you are implementing installable items for your own custom industry, you can implement upgrades via
    // 		that industry's wantsToUseSpecialItem method.
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        super.reportPlayerMarketTransaction(transaction);
        if (ITEMS_PRIORITIES == null || ITEMS_PRIORITIES.isEmpty()) {
            // nothing we can do.
            return;
        }
        MarketAPI market = transaction.getMarket();
        SubmarketAPI submarket = transaction.getSubmarket();
        CargoAPI cargo = transaction.getSubmarket().getCargo();
        // first layer of checks: is this a market that should install items that were sold?
        if (market != null && submarket != null
                && submarket.getPlugin().isParticipatesInEconomy()
                && !market.isPlayerOwned()
                && !submarket.getPlugin().isBlackMarket()
                && submarket.getFaction() == market.getFaction()) {
            // second layer of checks: did the player sell any installable items that we might need to wrangle?
            Set<SpecialItemData> items = new HashSet<>();
            for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
                if (stack.getPlugin() == null) {
                    // it feels like this check shouldn't be needed?
                    // but the CoreScript implementation has it, so we'll do it.
                    continue;
                }
                SpecialItemData item = stack.getSpecialDataIfSpecial();
                if (item == null) {
                    // should be unreachable code. Should.
                    continue;
                }
                Object priority = ITEMS_PRIORITIES.get(item.getId());
                if (priority == null || !(priority instanceof ItemPriority)) {
                    // we're not managing this item; move on.
                    continue;
                }
                float count = cargo.getQuantity(CargoItemType.SPECIAL, item);
                if (count <= 0) {
                    // the player did sell one of these, but the vanilla
                    // item installation code already installed it somewhere.
                    continue;
                }
                items.add(item);
            }
            if (items.isEmpty()) {
                // nothing for us to manage; we're done here.
                return;
            }
            boolean installedAnything = false;
            // Step One: if we can install anything in an industry that has no item, do that.
            // Note that 'has no item' includes industries with non-functional items (though that shouldn't happen).
            for (Industry ind : market.getIndustries()) {
                SpecialItemData installed = ind.getSpecialItem();
                if (installed == null || !canUseItem(ind, installed)) {
                    SpecialItemData best = null;
                    float bestPriority = 0.0f;
                    for (SpecialItemData item : items) {
                        if (canUseItem(ind, item)) {
                            ItemPriority calc = (ItemPriority) ITEMS_PRIORITIES.get(item.getId());
                            if (calc == null) {
                                // there shouldn't be any way to end up here...
                                continue;
                            }
                            try {
                                float priority = calc.getPriority(ind);
                                if (best == null || bestPriority < priority) {
                                    best = item;
                                    bestPriority = priority;

                                }
                            } catch (Throwable th) {
                                Global.getLogger(MagicIndustryItemWrangler.class
                                ).error("Exception thrown from getPriority() for " + item.getId());
                            }
                        }
                    }
                    if (best != null) {
                        installedAnything = true;
                        if (installed != null) {
                            cargo.addItems(CargoItemType.SPECIAL, installed, 1);
                            if (ITEMS_PRIORITIES.containsKey(installed.getId())) {
                                items.add(installed);
                            }
                        }
                        cargo.removeItems(CargoItemType.SPECIAL, best, 1);
                        if (cargo.getQuantity(CargoItemType.SPECIAL, best) <= 0) {
                            items.remove(best);
                        }
                        ind.setSpecialItem(best);
                    }
                }
            }
            // Step Two: run through industries again; this time, allow installations or upgrades.
            // Repeat this up to once per industry - then stop; theoretically could infinite-loop
            // otherwise if someone made a malicious (or just buggy) variable-priority item.
            for (int i = 0; i < market.getIndustries().size(); i++) {
                boolean somethingChangedThisLoop = false;
                for (Industry ind : market.getIndustries()) {
                    SpecialItemData installed = ind.getSpecialItem();
                    if (installed != null && null != ITEMS_PRIORITIES.get(installed.getId())) {
                        SpecialItemData best = installed;
                        float bestPriority;
                        try {
                            bestPriority = ((ItemPriority) ITEMS_PRIORITIES.get(installed.getId())).getPriority(ind);

                        } catch (Throwable th) {
                            Global.getLogger(MagicIndustryItemWrangler.class
                            ).error("Exception thrown from getPriority() for " + installed.getId());
                            continue;
                        }
                        for (SpecialItemData item : items) {
                            if (canUseItem(ind, item)) {
                                ItemPriority calc = (ItemPriority) ITEMS_PRIORITIES.get(item.getId());
                                if (calc == null) {
                                    continue;
                                }
                                try {
                                    float priority = calc.getPriority(ind);
                                    if (best == null || bestPriority < priority) {
                                        best = item;
                                        bestPriority = priority;

                                    }
                                } catch (Throwable th) {
                                    Global.getLogger(MagicIndustryItemWrangler.class
                                    ).error("Exception thrown from getPriority() for " + item.getId());
                                }
                            }
                        }
                        if (!best.equals(installed)) {
                            installedAnything = true;
                            somethingChangedThisLoop = true;
                            cargo.addItems(CargoItemType.SPECIAL, installed, 1);
                            cargo.removeItems(CargoItemType.SPECIAL, best, 1);
                            if (cargo.getQuantity(CargoItemType.SPECIAL, best) <= 0) {
                                items.remove(best);
                            }
                            items.add(installed);
                            ind.setSpecialItem(best);
                        }
                    } else if (installed == null || !canUseItem(ind, installed)) {
                        SpecialItemData best = null;
                        float bestPriority = 0.0f;
                        for (SpecialItemData item : items) {
                            if (canUseItem(ind, item)) {
                                ItemPriority calc = (ItemPriority) ITEMS_PRIORITIES.get(item.getId());
                                if (calc == null) {
                                    // there shouldn't be any way to end up here...
                                    continue;
                                }
                                try {
                                    float priority = calc.getPriority(ind);
                                    if (best == null || bestPriority < priority) {
                                        best = item;
                                        bestPriority = priority;

                                    }
                                } catch (Throwable th) {
                                    Global.getLogger(MagicIndustryItemWrangler.class
                                    ).error("Exception thrown from getPriority() for " + item.getId());
                                }
                            }
                        }
                        if (best != null) {
                            installedAnything = true;
                            somethingChangedThisLoop = true;
                            if (installed != null) {
                                cargo.addItems(CargoItemType.SPECIAL, installed, 1);
                                if (ITEMS_PRIORITIES.containsKey(installed.getId())) {
                                    items.add(installed);
                                }
                            }
                            cargo.removeItems(CargoItemType.SPECIAL, best, 1);
                            if (cargo.getQuantity(CargoItemType.SPECIAL, best) <= 0) {
                                items.remove(best);
                            }
                            ind.setSpecialItem(best);
                        }
                    }
                }
                if (!somethingChangedThisLoop) {
                    break;
                }
            }
            if (installedAnything) {
                cargo.sort();
            }
        }
    }

    private boolean canUseItem(Industry ind, SpecialItemData item) {
        try {
            SpecialItemSpecAPI spec = Global.getSettings().getSpecialItemSpec(item.getId());
            String[] industries = spec.getParams().split(",");
            Set<String> all = new HashSet<>();
            for (String industry : industries) {
                all.add(industry.trim());
            }
            // first part checks 'does this apply to this industry?', second part checks if the item's installation requirements are met.
            return all.contains(ind.getId()) && new GenericInstallableItemPlugin(ind).canBeInstalled(item);
        } catch (Exception e) {
            throw new RuntimeException("Error checking if " + item.getId() + " can be installed in " + ind.getId(), e);
        }
    }

    public static interface ItemPriority {
        // Note that any exception from this will be caught & logged - but without the stack trace.
        // If you do anything fancy in here, deal with your own error logging.
        // Also, while this -should- never be called with a null industry, it's still a good idea to double-check that before using it.

        public float getPriority(Industry industry);
    }

    public static class FixedPriority implements ItemPriority {

        private float priority;

        public FixedPriority(float priority) {
            this.priority = priority;
        }

        @Override
        public float getPriority(Industry industry) {
            return priority;
        }
    }
}
