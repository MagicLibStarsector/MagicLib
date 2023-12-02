package org.magiclib.util.ui;

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * BaseIntelPlugin that allows you to refresh the large description panel.
 * <p>
 * Note that it cannot persist UI state (e.g. scroll position) between refreshes, so you'll have to do that yourself.
 */
public class MagicRefreshableBaseIntelPlugin extends BaseIntelPlugin {
    private transient @Nullable CustomPanelAPI largeDescBasePanel;
    private transient @Nullable CustomPanelAPI largeDescPanel;

    private transient final @NotNull List<MagicFunction> beforeRefreshFunctions = new ArrayList<>();
    private transient final @NotNull List<MagicFunction> afterRefreshFunctions = new ArrayList<>();

    @Override
    public final void createLargeDescription(CustomPanelAPI rootPanel, float width, float height) {
        largeDescBasePanel = rootPanel;
        refreshPanel();
    }

    /**
     * Refreshes the large description panel. Call this if you need to update the UI.
     * <p>
     * UI state will be lost (e.g. scroll position), so save that in beforeLargeDescriptionRefresh() if you need it, then apply it here or in afterLargeDescriptionRefresh().
     */
    public final void refreshPanel() {
        for (MagicFunction beforeRefreshFunction : beforeRefreshFunctions) {
            beforeRefreshFunction.doFunction();
        }

        beforePanelRefresh();

        if (largeDescPanel != null) {
            largeDescBasePanel.removeComponent(largeDescPanel);
        }

        largeDescPanel = largeDescBasePanel.createCustomPanel(largeDescBasePanel.getPosition().getWidth(), largeDescBasePanel.getPosition().getHeight(), null);
        largeDescBasePanel.addComponent(largeDescPanel);
        createLargeDescriptionImpl(largeDescPanel, largeDescBasePanel.getPosition().getWidth(), largeDescBasePanel.getPosition().getHeight());

        for (MagicFunction afterRefreshFunction : afterRefreshFunctions) {
            afterRefreshFunction.doFunction();
        }
        afterRefreshFunctions.clear();
        afterPanelRefresh();
    }

    /**
     * If you need to grab UI state and save it, implement this and do it here.
     */
    protected void beforePanelRefresh() {
    }

    /**
     * Implement this to restore UI state here, or do it in createLargeDescriptionImpl().
     */
    protected void afterPanelRefresh() {
    }

    /**
     * Alternative, Kotlin-friendly way of adding functions to run before.
     * <p>
     * Usage:
     * <pre>
     * val panelTooltip = panel.createUIElement(width, height, true)
     * doBeforeRefresh { scrollPos = panelTooltip?.externalScroller?.yOffset }
     * doAfterRefresh { panelTooltip?.externalScroller?.yOffset = scrollPos ?: 0f }
     * </pre>
     */
    protected final void doBeforeRefresh(@NotNull MagicFunction function) {
        beforeRefreshFunctions.add(function);
    }

    /**
     * Alternative, Kotlin-friendly way of adding functions to run after.
     * <p>
     * Usage:
     * <pre>
     * val panelTooltip = panel.createUIElement(width, height, true)
     * doBeforeRefresh { scrollPos = panelTooltip?.externalScroller?.yOffset }
     * doAfterRefresh { panelTooltip?.externalScroller?.yOffset = scrollPos ?: 0f }
     * </pre>
     */
    protected final void doAfterRefresh(@NotNull MagicFunction function) {
        afterRefreshFunctions.add(function);
    }

    /**
     * Override this to create the large description panel.
     * <p>
     * If you need to restore UI state, do it here or in afterLargeDescriptionRefresh().
     */
    public void createLargeDescriptionImpl(@NotNull CustomPanelAPI panel, float width, float height) {

    }

    @Override
    public boolean hasLargeDescription() {
        // If you aren't using this class to display a large description, what are you doing?
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        // Large and small are mutually exclusive since they occupy the same space.
        return false;
    }
}
