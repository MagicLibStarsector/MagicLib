/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.util.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicBountyData;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicPaginatedBarEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.scripts.util.MagicTxt.nullStringIfEmpty;

/**
 * TODO: Add a "Dismiss forever" option in addition to "Accept" and "Not now".
 */
public final class MagicBountyBarEvent extends MagicPaginatedBarEvent {
    private List<String> keysOfBountiesToShow;

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        //TODO implement blacklists (both faction and individual markets) via modSettings
        //TODO filter: min market size? stability? unrest?

        Map<String, MagicBountyData.bountyData> qualifyingBounties = getBountiesAtMarketById(market);
        keysOfBountiesToShow = new ArrayList<>(qualifyingBounties.keySet());

        return !keysOfBountiesToShow.isEmpty();
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        // Display the text that will appear when the player first enters the bar and looks around
        dialog.getTextPanel().addPara(
                "A subroutine from your implant informs you that this establishment is broadcasting an informal job board."
        );

        // Display the option that lets the player choose to investigate our bar event
        dialog.getOptionPanel().addOption("Connect to the local unsanctioned bounty board.", this);
    }

    /**
     * Called when the player chooses this event from the list of options shown when they enter the bar.
     *
     * @param dialog
     * @param memoryMap
     */
    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);

        // If player starts our event, then backs out of it, `done` will be set to true.
        // If they then start the event again without leaving the bar, we should reset `done` to false.        
        done = false;
        // Clear on init in case it's being reopened
        options.clear();
        optionsAllPages.clear();

        dialog.getVisualPanel().saveCurrentVisual();

        // The boolean is for whether to show only minimal person information. True == minimal
//        dialog.getVisualPanel().showPersonInfo(person, true);

        // Launch into our event by triggering the "INIT" option, which will call `optionSelected()`
        this.optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        super.optionSelected(optionText, optionData);

        if (optionData instanceof OptionId) {
            TextPanelAPI text = dialog.getTextPanel();

            Map<String, MagicBountyData.bountyData> bountiesToShow = getBountiesToShow();
            OptionId optionId = (OptionId) optionData;
            switch (optionId) {
                case INIT:
                case BACK_TO_BOARD:
                    options.clear();
                    dialog.getVisualPanel().restoreSavedVisual();
                    dialog.getVisualPanel().saveCurrentVisual();

                    // TODO text to display when selected
                    text.addPara("%s " + (keysOfBountiesToShow.size() == 1 ? "bounty is" : "bounties are") + " available on the bounty board.",
                            Misc.getHighlightColor(),
                            Integer.toString(keysOfBountiesToShow.size()));

                    for (String key : keysOfBountiesToShow) {
                        MagicBountyData.bountyData bounty = MagicBountyData.getBountyData(key);
                        if (bounty != null) {
                            String name = bounty.job_name;
                            if (bounty.job_name == null) {
                                name = "Unnamed job"; // TODO default job name
                            }
                            addOption(name, getBountyOptionKey(key), "Key: " + key, null);
                        }
                    }

                    addOptionAllPages("Close", OptionId.CLOSE, "Close the bounty board.", Keyboard.KEY_ESCAPE);

                    break;
                case CLOSE:
                    noContinue = true;
                    done = true;
                    break;
                default:
            }
        } else if (optionData instanceof String) {
            String data = (String) optionData;
            if (data.startsWith("accept-")) {
                try {
                    String bountyKey = data.replaceFirst("accept-", "");
                    MagicBountyData.bountyData bounty = MagicBountyData
                            .getBountyData(bountyKey);
                    text.addPara("%s", Misc.getHighlightColor(), "Accepted job: " + bounty.job_name);
                    ActiveBounty activeBounty = MagicBountyCoordinator.getActiveBounty(bountyKey);
                    activeBounty.acceptBounty(dialog.getInteractionTarget(), activeBounty.calculateCreditReward());
                    // TODO remove bounty, send user up one level
                    // TODO Job accepted!
                    // TODO set job_memKey, but on what and to what?
                } catch (Exception e) {
                    Global.getLogger(this.getClass()).error(e.getMessage(), e);
                }
            } else {
                for (String key : keysOfBountiesToShow) {
                    if (getBountyOptionKey(key).equals(optionData)) {
                        // User has selected to view a bounty
                        final MagicBountyData.bountyData bounty = MagicBountyData.getBountyData(key);

                        if (bounty == null)
                            continue;

                        ActiveBounty activeBounty = MagicBountyCoordinator.getActiveBounty(key);

                        if (activeBounty == null) {
                            activeBounty = MagicBountyCoordinator.createActiveBounty(key, bounty);
                            MagicBountyCoordinator.putActiveBounty(key, activeBounty);

                            if (activeBounty == null) continue;
                        }

                        // TODO VERY IMPORTANT - need to despawn the fleet when this bar event is destroyed.

                        activeBounty.addDescriptionToTextPanel(text);

                        if (nullStringIfEmpty(bounty.job_forFaction) != null) {
                            FactionAPI faction = Global.getSector().getFaction(bounty.job_forFaction);

                            if (faction != null) {
                                text.addPara("Posted by %s.", faction.getBaseUIColor(), faction.getDisplayNameWithArticle());
                            }
                        }

                        if (activeBounty.calculateCreditReward() != null) {
                            text.addPara("Reward: %s", Misc.getHighlightColor(), Misc.getDGSCredits(activeBounty.calculateCreditReward()));
                        }

                        if (bounty.job_deadline > 0) {
                            text.addPara("Time limit: %s days", Misc.getHighlightColor(), Misc.getWithDGS(bounty.job_deadline));
                        }

                        if (bounty.job_show_captain) {
                            dialog.getVisualPanel().showPersonInfo(activeBounty.getFleet().getCommander());
                        } else if (nullStringIfEmpty(bounty.job_forFaction) != null) {
                            // TODO: Show faction flag?
                        }

                        if (bounty.job_show_fleet) {
                            text.addPara("Fleet information is attached to the posting.");
                            int columns = 10;
                            text.beginTooltip()
                                    .addShipList(columns, 2, (dialog.getTextWidth() - 10) / columns,
                                            activeBounty.getFleet().getFaction().getBaseUIColor(),
                                            activeBounty.getFleet().getMembersWithFightersCopy(), 10f);
                            text.addTooltip();
                        }

                        options.clear();
                        optionsAllPages.clear();
                        addOption(bounty.job_pick_option != null && !bounty.job_pick_option.isEmpty()
                                ? bounty.job_pick_option
                                : "Accept", "accept-" + key, null, null);
                        addOption("Back", OptionId.BACK_TO_BOARD, null, Keyboard.KEY_ESCAPE);
                    }
                }
            }
        }

        showOptions();
    }

    enum OptionId {
        INIT,
        CLOSE,
        BACK_TO_BOARD
    }

    @Override
    public boolean isAlwaysShow() {
        return true;
    }

    private String getBountyOptionKey(String key) {
        return "optionkey-" + key;
    }

    private Map<String, MagicBountyData.bountyData> getBountiesAtMarketById(MarketAPI market) {
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
        return available;
    }

    private Map<String, MagicBountyData.bountyData> getBountiesToShow() {
        Map<String, MagicBountyData.bountyData> ret = new HashMap<>(keysOfBountiesToShow.size());

        for (String key : keysOfBountiesToShow) {
            ret.put(key, MagicBountyData.BOUNTIES.get(key));
        }

        return ret;
    }
}
