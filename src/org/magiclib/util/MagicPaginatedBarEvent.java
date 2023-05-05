package org.magiclib.util;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.DevMenuOptions;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.DumpMemory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapted from {@link com.fs.starfarer.api.impl.campaign.rulecmd.PaginatedOptions}.
 *
 * @author Wisp
 */
public abstract class MagicPaginatedBarEvent extends BaseBarEvent {

    public static final String OPTION_NEXT_PAGE = "core_option_next_page";
    public static final String OPTION_PREV_PAGE = "core_option_prev_page";

    public static class MagicPaginatedOption {
        @NotNull
        public final String text;
        @NotNull
        public final Object id;
        @Nullable
        public final String tooltip;
        @Nullable
        public final Integer hotkey;

        /**
         * @param hotkey org.lwjgl.input.Keyboard
         */
        public MagicPaginatedOption(@NotNull String text, @NotNull Object id, @Nullable String tooltip, @Nullable Integer hotkey) {
            this.text = text;
            this.id = id;
            this.tooltip = tooltip;
            this.hotkey = hotkey;
        }
    }


    protected List<MagicPaginatedOption> options = new ArrayList<>();
    protected List<MagicPaginatedOption> optionsAllPages = new ArrayList<>();
    protected int optionsPerPage = 5;
    protected int currPage = 0;
    protected boolean withSpacers = true;

    public void addOption(String text, Object id, String tooltip, @Nullable Integer hotkey) {
        options.add(new MagicPaginatedOption(text, id, tooltip, hotkey));
    }

    public void addOptionAllPages(String text, Object id, String tooltip, @Nullable Integer hotkey) {
        optionsAllPages.add(new MagicPaginatedOption(text, id, tooltip, hotkey));
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

                if (option.hotkey != null) {
                    dialog.getOptionPanel().setShortcut(option.id, option.hotkey, false, false, false, false);
                }
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

        for (MagicPaginatedOption allPagesOption : optionsAllPages) {
            dialog.getOptionPanel().addOption(allPagesOption.text, allPagesOption.id, allPagesOption.tooltip);

            if (allPagesOption.hotkey != null) {
                dialog.getOptionPanel().setShortcut(allPagesOption.id, allPagesOption.hotkey, false, false, false, false);
            }
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
        } else if (optionData == OPTION_NEXT_PAGE) {
            currPage++;
            showOptions();
            return;
        }

//        if (optionText != null) {
//            dialog.getTextPanel().addParagraph(optionText, Global.getSettings().getColor("buttonText"));
//        }

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
}
