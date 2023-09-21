package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * The base class for all achievements. Extend this class to create your own.
 * <p>
 * First, create an entry in your <code>data/config/magic_achievements.csv</code> file. Set the <code>script</code> column to this class's package and name.
 * <p>
 * This class is not serialized to the save file.
 *
 * @author Wisp
 * @since 1.3.0
 */
public class MagicAchievement {
    // Note to self: don't use directly, call getLogger().
    private Logger logger;
    @NotNull MagicAchievementSpec spec;

    @Nullable
    private Float progress = null;
    @Nullable
    private Float maxProgress = null;
    @Nullable
    private Date dateCompleted;
    @Nullable
    private String completedByUserId;
    @Nullable
    private String completedByUserName;
    @NotNull
    private final Map<String, Object> memory = new HashMap<>();

    /**
     * Shown if set. Only persisted in memory, not save file.
     */
    @Nullable
    public String errorMessage;

    /**
     * Do not use this constructor. It is called automatically using reflection to instantiate this class.
     * Put your initialization code in {@link #onApplicationLoaded(boolean)} or {@link #onSaveGameLoaded(boolean)} instead.
     */
    public MagicAchievement() {
    }

    /**
     * By default, only run the achievement advance check every 1-2 seconds to improve performance.
     */
    private IntervalUtil advanceInterval = new IntervalUtil(1f, 2f);

    /**
     * Called when Starsector is loaded.
     * Called even if the achievement is complete.
     * <p>
     * Also called during `onSaveGameLoaded`, so ensure that logic here is idempotent (can be called multiple times without problems).
     */
    public void onApplicationLoaded(boolean isComplete) {
    }

    /**
     * Called each time the achievement is loaded, for example when the game is loaded.
     * Place any code here that registers itself with the game, such as adding a listener.
     * Called even if the achievement is completed.
     */
    public void onSaveGameLoaded(boolean isComplete) {
    }

    /**
     * Called each time the game is saved. If you want to avoid any data going into the save file,
     * you can clear it here.
     * Not called if the achievement is complete.
     */
    public void beforeGameSave() {
    }

    /**
     * Called each time the game is loaded.
     * Not called if the achievement is complete.
     */
    public void afterGameSave() {
    }

    /**
     * Do any cleanup logic here, e.g. removing listeners.
     * Called when the achievement is being unloaded from the sector, e.g. they're loading a new save.
     * Do NOT reset progress here, we're not deleting it, just taking it out of memory.
     */
    public void onDestroyed() {
    }

    /**
     * Call when the achievement is completed.
     * Sets the date completed and the current player as the completer.
     */
    public void completeAchievement() {
        completeAchievement(Global.getSector().getPlayerPerson());
    }

    /**
     * Call when the achievement is completed.
     * Sets the date completed and the player who completed it.
     * Does nothing if already completed; uncomplete first, if you want to re-complete it for some reason.
     *
     * @param completedByPlayer The player's character who completed the achievement, if applicable.
     */
    public void completeAchievement(@Nullable PersonAPI completedByPlayer) {
        if (isComplete()) return;

        this.dateCompleted = new Date();

        if (completedByPlayer != null) {
            this.completedByUserId = completedByPlayer.getId();
            this.completedByUserName = completedByPlayer.getName().getFullName();
        }

        getLogger().info("Achievement completed! " + spec.getId());
    }

    /**
     * Don't use this except for very good reasons, e.g. debugging!
     * Imagine if you had a Steam achievement go and take itself away.
     */
    public void uncompleteAchievement() {
        setDateCompleted(null);
        setCompletedByUserId(null);
        setCompletedByUserName(null);

        getLogger().info("Achievement uncompleted! " + spec.getId());
    }

    /**
     * Not meant to be overriden. Use {@link #advanceAfterInterval(float)} instead.
     * Not called if the achievement is complete.
     */
    protected void advanceInternal(float amount) {
        advanceInterval.advance(amount);

        if (advanceInterval.intervalElapsed()) {
            advanceAfterInterval(advanceInterval.getElapsed());
        }
    }

    /**
     * Like regular advance, but NOT CALLED EVERY FRAME.
     * Called every 1-2 seconds by default. Change timing with {@link #setAdvanceIntervalUtil(IntervalUtil)}.
     * Not called if the achievement is complete.
     */
    public void advanceAfterInterval(float amount) {
    }

    /**
     * Called every frame during combat.
     * Not called if the achievement is complete.
     */
    public void advanceInCombat(float amount, List<InputEventAPI> events, boolean isSimulation) {

    }

    /**
     * Call this to save any changes to the achievement (and all others as well).
     */
    public void saveChanges() {
        getLogger().info("Saving achievements triggered by '" + spec.getId() + "' from mod '" + spec.getModName() + "'.");
        MagicAchievementManager.getInstance().saveAchievements();
    }

    /**
     * Serializes this achievement to a JSON object.
     */
    @Nullable
    public JSONObject toJsonObject() {
        try {
            JSONObject jsonObject = spec.toJsonObject();
            jsonObject.put("progress", progress);
            jsonObject.put("maxProgress", maxProgress);
            jsonObject.put("dateCompleted", dateCompleted == null ? null : dateCompleted.getTime());
            jsonObject.put("completedByUserId", completedByUserId);
            jsonObject.put("completedByUserName", completedByUserName);
            jsonObject.put("memory", memory);
            return jsonObject;
        } catch (Exception e) {
            getLogger().warn("Unable to convert achievement to JSON.", e);
            return null;
        }
    }

    /**
     * Sets this achievement's data from the given JSON object.
     */
    public boolean loadFromJsonObject(@NotNull JSONObject jsonObject) {
        try {
            spec = MagicAchievementSpec.fromJsonObject(jsonObject);
            progress = (float) jsonObject.optDouble("progress", 0);
            maxProgress = (float) jsonObject.optDouble("maxProgress", 0);
            long dateCompletedTimestamp = jsonObject.optLong("dateCompleted", 0);
            dateCompleted = dateCompletedTimestamp == 0 ? null : new Date(dateCompletedTimestamp);
            completedByUserId = jsonObject.optString("completedByUserId", null);
            completedByUserName = jsonObject.optString("completedByUserName", null);
            JSONObject memJson = jsonObject.optJSONObject("memory");

            if (memJson != null) {
                for (Iterator<String> it = memJson.keys(); it.hasNext(); ) {
                    String key = it.next();
                    memory.put(key, memJson.get(key));
                }
            }
            return true;
        } catch (Exception e) {
            Global.getLogger(MagicAchievement.class).warn("Unable to convert achievement from JSON.", e);
            return false;
        }
    }

    /**
     * Returns an instance of the logger for this achievement.
     */
    protected Logger getLogger() {
        if (logger == null) {
            logger = Global.getLogger(this.getClass());
        }

        return logger;
    }

    /**
     * Whether or not to show this achievement in the Intel screen.
     * By default, we only show Hidden achievements once they're completed.
     */
    public boolean shouldShowInIntel() {
        return getSpoilerLevel() != MagicAchievementSpoilerLevel.Hidden
                || isComplete();
    }

    /**
     * Returns the time interval in seconds between each call to {@link #advanceAfterInterval(float)}.
     */
    public IntervalUtil getAdvanceIntervalUtil() {
        return advanceInterval;
    }

    /**
     * Sets the time interval between each call to {@link #advanceAfterInterval(float)}.
     */
    public void setAdvanceIntervalUtil(IntervalUtil interval) {
        advanceInterval = interval;
    }

    public boolean getHasProgressBar() {
        return spec.getHasProgressBar();
    }

    public void setHasProgressBar(boolean hasProgressBar) {
        spec.setHasProgressBar(hasProgressBar);
    }

    /**
     * Returns the progress of the achievement, or null if it's not a progress bar achievement.
     * Progress is relative to the max progress.
     * Either override this method or call {@link #setProgress(Float)} to set the progress.
     */
    public @Nullable Float getProgress() {
        if (isComplete())
            return getMaxProgress();

        return progress;
    }

    /**
     * Sets the progress. Only used for progress bar achievements.
     * Note that if you override {@link #getProgress()}, this may not be used, depending on your implementation.
     */
    public void setProgress(@Nullable Float progress) {
        this.progress = progress;
    }

    /**
     * Returns the max progress of the achievement, or null if it's not a progress bar achievement.
     * Either override this method or call {@link #setMaxProgress(Float)} to set the max progress.
     */
    public @Nullable Float getMaxProgress() {
        return maxProgress;
    }

    /**
     * Sets the max progress. Only used for progress bar achievements.
     * Note that if you override {@link #getMaxProgress()}, this may not be used, depending on your implementation.
     */
    public void setMaxProgress(@Nullable Float maxProgress) {
        this.maxProgress = maxProgress;
    }


    public @NotNull String getModId() {
        return spec.getModId();
    }

    public @NotNull String getModName() {
        return spec.getModName();
    }

    public @NotNull String getSpecId() {
        return spec.getId();
    }

    public @NotNull String getName() {
        return spec.getName();
    }

    public void setName(@NotNull String name) {
        spec.setName(name);
    }

    public @NotNull String getDescription() {
        return spec.getDescription();
    }

    public void setDescription(@NotNull String description) {
        spec.setDescription(description);
    }

    public boolean hasTooltip() {
        return getTooltip() != null && !getTooltip().isEmpty();
    }

    public @Nullable String getTooltip() {
        return spec.getTooltip();
    }

    public void setTooltip(@Nullable String tooltip) {
        spec.setTooltip(tooltip);
    }

    public void createTooltipHeader(@NotNull TooltipMakerAPI tooltipMakerAPI) {
        tooltipMakerAPI.addTitle(getName());
        tooltipMakerAPI.addPara(getDescription(), 10f);
        tooltipMakerAPI.addSpacer(10f);
    }

    /**
     * Allows full control of the tooltip.
     * Called when the achievement is hovered over in the Intel screen.
     * By default, shows the name and tooltip.
     * <p>
     * Make sure {@link #hasTooltip()} is true.
     */
    public void createTooltip(@NotNull TooltipMakerAPI tooltipMakerAPI, boolean isExpanded, float width) {
        if (hasTooltip()) {
            createTooltipHeader(tooltipMakerAPI);

            if (getTooltip() != null && !getTooltip().isEmpty()) {
                tooltipMakerAPI.addPara(getTooltip(), 0f);
            }
        }
    }

    public @NotNull String getScript() {
        return spec.getScript();
    }

    public void setScript(@NotNull String script) {
        spec.setScript(script);
    }

    public @Nullable String getImage() {
        if (spec.getImage() != null && !spec.getImage().isEmpty())
            return spec.getImage();

        switch (getRarity()) {
            case Common:
                return Global.getSettings().getSpriteName("intel", "achievement_bronze");
            case Uncommon:
                return Global.getSettings().getSpriteName("intel", "achievement_silver");
            case Rare:
                return Global.getSettings().getSpriteName("intel", "achievement_gold");
            case Epic:
            case Legendary:
                return Global.getSettings().getSpriteName("intel", "achievement_purple");
            default:
                return null;
        }
    }

    public void setImage(@Nullable String image) {
        spec.setImage(image);
    }

    public @NotNull MagicAchievementSpoilerLevel getSpoilerLevel() {
        return spec.getSpoilerLevel();
    }

    public void setSpoilerLevel(@NotNull MagicAchievementSpoilerLevel spoilerLevel) {
        spec.setSpoilerLevel(spoilerLevel);
    }

    public @NotNull MagicAchievementRarity getRarity() {
        return spec.getRarity();
    }

    /**
     * Returns the color of the particle effect for this achievement's rarity.
     * Common rarity doesn't have a particle effect.
     */
    public @NotNull Color getRarityColor() {
        switch (getRarity()) {
            case Common:
                // Not shown as a particle effect, but used for the text color during combat.
                return Misc.getTextColor();
            case Uncommon:
                // Bronze
//                return new Color(0xCD7F32);
                // Silver
                return new Color(0xE0DFDF);
            case Rare:
                // Gold
                return Color.YELLOW;
            case Epic:
                // Purple
                return new Color(0x9876EC);
            case Legendary:
                // Purple
                return new Color(0x7754C9);
        }

        return Color.red;
    }

    public void setRarity(@NotNull MagicAchievementRarity rarity) {
        spec.setRarity(rarity);
    }

    public boolean isComplete() {
        return dateCompleted != null;
    }

    public @Nullable Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(@Nullable Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public @Nullable String getCompletedByUserId() {
        return completedByUserId;
    }

    public void setCompletedByUserId(@Nullable String completedByUserId) {
        this.completedByUserId = completedByUserId;
    }

    public @Nullable String getCompletedByUserName() {
        return completedByUserName;
    }

    public void setCompletedByUserName(@Nullable String completedByUserName) {
        this.completedByUserName = completedByUserName;
    }

    public @NotNull Map<String, Object> getMemory() {
        return memory;
    }

    public String getSoundEffectId() {
        return "magiclib_achievementunlocked";
    }
}
