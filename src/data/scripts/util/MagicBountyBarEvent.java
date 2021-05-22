/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.plugins.MagicBountyData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Add a "Dismiss forever" option in addition to "Accept" and "Not now".
 */
public class MagicBountyBarEvent extends MagicPaginatedBarEvent {
    private List<String> keysOfBountiesToShow;

    private Map<String, MagicBountyData.bountyData> getBountiesAtMarket(MarketAPI market) {
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
                    bounty.trigger_market_minSize)){
                available.put(key, bounty);
            }
        }
        return available;
    }

    private List<MagicBountyData.bountyData> getBountiesToShow() {
        List<MagicBountyData.bountyData> ret = new ArrayList<>(keysOfBountiesToShow.size());

        for (String key : keysOfBountiesToShow) {
            ret.add(MagicBountyData.BOUNTIES.get(key));
        }

        return ret;
    }

    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        //TODO implement blacklists (both faction and individual markets) via modSettings
        //TODO filter: min market size? stability? unrest?

        Map<String, MagicBountyData.bountyData> qualifyingBounties = getBountiesAtMarket(market);
        keysOfBountiesToShow = new ArrayList<>(qualifyingBounties.keySet());

        if (keysOfBountiesToShow.isEmpty()) return false;

        return true;
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
     * @param dialog
     * @param memoryMap
     */
    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);

        // If player starts our event, then backs out of it, `done` will be set to true.
        // If they then start the event again without leaving the bar, we should reset `done` to false.        
        done = false;

        // The boolean is for whether to show only minimal person information. True == minimal
//        dialog.getVisualPanel().showPersonInfo(person, true);

        // Launch into our event by triggering the "INIT" option, which will call `optionSelected()`
        this.optionSelected(null, OptionId.INIT);

        // TODO text to display when selected
        text.addPara("%s bounties are available on the bounty board.",
                Misc.getHighlightColor(),
                Integer.toString(keysOfBountiesToShow.size()));


        for (String key : keysOfBountiesToShow) {
            MagicBountyData.bountyData bounty = MagicBountyData.getBountyData(key);
            if (bounty != null) {
                String name = bounty.job_name;
                if (bounty.job_name == null) {
                    name = "Unnamed job"; // TODO
                }
                addOption(bounty.job_name, key, null);
            }
        }

        addOptionAllPages("Close", OptionId.CLOSE.name(), "Close the bounty board.");
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        super.optionSelected(optionText, optionData);

        if (optionData instanceof OptionId) {
            TextPanelAPI text = dialog.getTextPanel();
            OptionPanelAPI optionPanel = dialog.getOptionPanel();

            OptionId optionId = (OptionId) optionData;
            switch (optionId) {
                case INIT:
                    break;
                case CLOSE:
                    noContinue = true;
                    done = true;
                    break;
                default:
//                    throw new IllegalStateException("Unexpected value: " + optionId);
            }
        }
    }

    enum OptionId {
        INIT,
        CLOSE
    }

    @Override
    public boolean isAlwaysShow() {
        return Global.getSettings().isDevMode();
    }
}
