package data.scripts.util;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.DevMenuOptions;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.DumpMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MagicPaginatedBarEvent extends BaseBarEvent {

    public static final String OPTION_NEXT_PAGE = "core_option_next_page";
    public static final String OPTION_PREV_PAGE = "core_option_prev_page";

    public static class MagicPaginatedOption {
        public String text;
        public String id;
        public String tooltip;

        public MagicPaginatedOption(String text, String id, String tooltip) {
            this.text = text;
            this.id = id;
            this.tooltip = tooltip;
        }
    }

    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    protected List<MagicPaginatedOption> options = new ArrayList<>();
    protected List<MagicPaginatedOption> optionsAllPages = new ArrayList<>();
    protected int optionsPerPage = 5;
    protected int currPage = 0;
    protected boolean withSpacers = true;

    public void addOption(String text, String id, String tooltip) {
        options.add(new MagicPaginatedOption(text, id, tooltip));
    }

    public void addOptionAllPages(String text, String id, String tooltip) {
        optionsAllPages.add(new MagicPaginatedOption(text, id, tooltip));
    }

    public void showOptions() {
        dialog.getOptionPanel().clearOptions();

        int maxPages = (int) Math.ceil((float) options.size() / (float) optionsPerPage);
        if (currPage > maxPages - 1) currPage = maxPages - 1;
        if (currPage < 0) currPage = 0;

        int start = currPage * optionsPerPage;
        for (int i = start; i < start + optionsPerPage; i++) {
            if (i >= options.size()) {
                if (maxPages > 1 && withSpacers) {
                    dialog.getOptionPanel().addOption("", "spacer" + i);
                    dialog.getOptionPanel().setEnabled("spacer" + i, false);
                }
            } else {
                MagicPaginatedOption option = options.get(i);
                dialog.getOptionPanel().addOption(option.text, option.id, option.tooltip);
            }
        }

        if (maxPages > 1) {
            dialog.getOptionPanel().addOption(getPreviousPageText(), OPTION_PREV_PAGE);
            dialog.getOptionPanel().addOption(getNextPageText(), OPTION_NEXT_PAGE);

            if (currPage >= maxPages - 1) {
                dialog.getOptionPanel().setEnabled(OPTION_NEXT_PAGE, false);
            }
            if (currPage <= 0) {
                dialog.getOptionPanel().setEnabled(OPTION_PREV_PAGE, false);
            }
        }

        for (MagicPaginatedOption option : optionsAllPages) {
            dialog.getOptionPanel().addOption(option.text, option.id, option.tooltip);
        }

        if (Global.getSettings().isDevMode()) {
            DevMenuOptions.addOptions(dialog);
        }
    }

    public String getPreviousPageText() {
        return "Previous page";
    }

    public String getNextPageText() {
        return "Next page";
    }

    public boolean isNewGameDialog() {
        //return false;
        return Global.getCurrentState() == GameState.TITLE;
    }

    public void optionSelected(String optionText, Object optionData) {
        if (optionData == OPTION_PREV_PAGE) {
            currPage--;
            showOptions();
            return;
        }
        if (optionData == OPTION_NEXT_PAGE) {
            currPage++;
            showOptions();
            return;
        }

        if (optionText != null) {
            dialog.getTextPanel().addParagraph(optionText, Global.getSettings().getColor("buttonText"));
        }

        if (optionData == DumpMemory.OPTION_ID) {
            new DumpMemory().execute(null, dialog, null, getMemoryMap());
            return;
        } else if (DevMenuOptions.isDevOption(optionData)) {
            DevMenuOptions.execute(dialog, (String) optionData);
            return;
        }

        MemoryAPI memory = dialog.getInteractionTarget().getMemory();
        memory.set("$option", optionData);
        memory.expire("$option", 0);
        memoryMap.get(MemKeys.LOCAL).set("$option", optionData, 0); // needed to make it work in conversations
    }


    public void advance(float amount) {
    }

    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    public Object getContext() {
        return null;
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void init(InteractionDialogAPI dialog) {
    }
}
