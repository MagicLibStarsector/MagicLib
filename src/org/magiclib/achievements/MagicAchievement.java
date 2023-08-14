package org.magiclib.achievements;

import com.fs.starfarer.api.characters.PersonAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.magiclib.util.MagicTxt;

import java.util.Date;

/**
 * Note: this class is serialized to the save file.
 *
 * @author Wisp
 * @since 1.3.0
 */
public class MagicAchievement {
    @Nullable
    private Float progress = null;
    @Nullable
    private Float maxProgress = null;
    @NotNull
    private final String modId;
    @NotNull
    private final String id;
    @NotNull
    private String name;
    @NotNull
    private String description;
    @NotNull
    private String script;
    @Nullable
    private String image;
    private boolean hasProgressBar;
    @NotNull
    private SpoilerLevel spoilerLevel;
    @Nullable
    private Date dateCompleted;
    @Nullable
    private String completedByUserId;
    @Nullable
    private String completedByUserName;

    public MagicAchievement(@NotNull MagicAchievementSpec spec) {
        this.modId = spec.getModId();
        this.id = spec.getId();
        this.name = spec.getName();
        this.description = spec.getDescription();
        this.script = spec.getScript();
        this.image = spec.getImage();
        this.hasProgressBar = spec.getHasProgressBar();
        this.spoilerLevel = spec.getSpoilerLevel();
    }

    /**
     * Call when the achievement is completed.
     * Sets the date completed and the player who completed it.
     *
     * @param completedByPlayer The player's character who completed the achievement, if applicable.
     */
    public void completeAchievement(@Nullable PersonAPI completedByPlayer) {
        this.dateCompleted = new Date();

        if (completedByPlayer != null) {
            this.completedByUserId = completedByPlayer.getId();
            this.completedByUserName = completedByPlayer.getName().getFullName();
        }
    }

    public @Nullable Float getProgress() {
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
        return modId;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    public @NotNull String getScript() {
        return script;
    }

    public void setScript(@NotNull String script) {
        this.script = script;
    }

    public @Nullable String getImage() {
        return image;
    }

    public void setImage(@Nullable String image) {
        this.image = image;
    }

    public boolean getHasProgressBar() {
        return hasProgressBar;
    }

    public void setHasProgressBar(boolean hasProgressBar) {
        this.hasProgressBar = hasProgressBar;
    }

    public @NotNull SpoilerLevel getSpoilerLevel() {
        return spoilerLevel;
    }

    public void setSpoilerLevel(@NotNull SpoilerLevel spoilerLevel) {
        this.spoilerLevel = spoilerLevel;
    }

    public boolean isHasProgressBar() {
        return hasProgressBar;
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
}
