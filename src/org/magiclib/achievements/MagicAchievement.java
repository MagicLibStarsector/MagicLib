package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.*;

/**
 * This class is not serialized to the save file.
 *
 * @author Wisp
 * @since 1.3.0
 */
public class MagicAchievement {
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
     * Shown if set. Only persisted in memory.
     */
    @Nullable
    public String errorMessage;

    /**
     * Do not use this constructor.
     * Put your initialization code in {@link #onApplicationLoaded()} instead.
     */
    public MagicAchievement() {

    }

    /**
     * By default, only run the achievement advance check every 1-2 seconds for performance reasons.
     */
    private IntervalUtil advanceInterval = new IntervalUtil(1f, 2f);

    public void onApplicationLoaded() {
        logger = Global.getLogger(this.getClass());
    }

    /**
     * Called each time the achievement is loaded, for example when the game is loaded.
     * Place any code here that registers itself with the game, such as adding a listener.
     */
    public void onGameLoaded() {

    }

    /**
     * Called each time the game is saved. If you want to avoid any data going into the save file,
     * you can clear it here.
     */
    public void beforeGameSave() {
    }

    /**
     * Called each time the game is loaded.
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

        logger.info("Achievement completed! " + spec.getId());
    }

    /**
     * Don't use this except for very good reasons, e.g. debugging!
     * Imagine if you had a Steam achievement go and take itself away.
     */
    public void uncompleteAchievement() {
        setDateCompleted(null);
        setCompletedByUserId(null);
        setCompletedByUserName(null);

        logger.info("Achievement uncompleted! " + spec.getId());
    }

    /**
     * Not meant to be overriden. Use {@link #advanceAfterInterval(float)} instead.
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
     */
    public void advanceAfterInterval(float amount) {
    }

    /**
     * Called every frame during combat unless the achievement is complete.
     */
    public void advanceInCombat(float amount, List<InputEventAPI> events) {

    }

    /**
     * Call this to save any changes to the achievement (and all others as well).
     */
    public void saveChanges() {
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
            logger.warn("Unable to convert achievement to JSON.", e);
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
     * By default, only show Hidden achievements once they're completed.
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

    public @Nullable Float getProgress() {
        if (isComplete())
            return getMaxProgress();

        return progress;
    }

    public void setProgress(@Nullable Float progress) {
        this.progress = progress;
    }

    public @Nullable Float getMaxProgress() {
        return maxProgress;
    }

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

    public @NotNull String getScript() {
        return spec.getScript();
    }

    public void setScript(@NotNull String script) {
        spec.setScript(script);
    }

    public @Nullable String getImage() {
        return spec.getImage();
    }

    public void setImage(@Nullable String image) {
        spec.setImage(image);
    }

    public boolean getHasProgressBar() {
        return spec.getHasProgressBar();
    }

    public void setHasProgressBar(boolean hasProgressBar) {
        spec.setHasProgressBar(hasProgressBar);
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
}
