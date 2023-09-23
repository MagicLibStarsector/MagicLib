package org.magiclib.achievements;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * An achievement base class that tracks a list of targets.
 * <p>
 * For an example, see {@link org.magiclib.achievements.examples.ExampleTargetListAchievement}.
 */
public class MagicTargetListAchievement extends MagicAchievement {
    protected String KEY = "magictargetlist_targets";

    public Map<String, Data> getTargets() {
        Object obj = getMemory().get(KEY);

        if (obj == null) {
            return new HashMap<>();
        } else if (obj instanceof Map) {
            return (Map<String, Data>) obj;
        } else {
            try {
                JSONObject jsonTargets = (JSONObject) obj;
                Map<String, Data> result = new HashMap<>(jsonTargets.length());

                for (Iterator it = jsonTargets.keys(); it.hasNext(); ) {
                    String key = (String) it.next();
                    JSONObject jsonData = jsonTargets.getJSONObject(key);
                    result.put(key, new Data(jsonData.getString(Data.DISPLAY_NAME), jsonData.getBoolean(Data.IS_COMPLETE)));
                }

                return result;
            } catch (JSONException e) {
                return new HashMap<>();
            }
        }
    }

    /**
     * Replaces the current target list with the given one, WITHOUT affecting the completion status of the targets.
     * <p>
     * Pass in a map of keys and display names.
     * <p>
     * To set a target to complete, use {@link #setTargetComplete(String, boolean)}.
     * <p>
     * Make sure to call {@link #saveChanges()} after this.
     *
     * <pre>
     * Map<String, String> targets = new HashMap<>();
     * targets.put("target1", "Target 1");
     * targets.put("target2", "Target 2");
     * setTargets(targets);
     * saveChanges();
     * </pre>
     */
    public void setTargets(Map<String, String> targets) {
        Map<String, Data> savedTargets = getTargets();

        for (Map.Entry<String, String> entry : targets.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (savedTargets.containsKey(key)) {
                savedTargets.get(key).setDisplayName(value);
            } else {
                savedTargets.put(key, new Data(value));
            }
        }

        for (String key : savedTargets.keySet()) {
            if (!targets.containsKey(key)) {
                savedTargets.remove(key);
            }
        }

        getMemory().put(KEY, savedTargets);
    }

    /**
     * Adds a target with the given key and display name.
     * <p>
     * If a target with the given key already exists, nothing happens.
     * <p>
     * Make sure to call {@link #saveChanges()} after this.
     */
    public void addTarget(String targetKey, String displayName) {
        Map<String, Data> savedTargets = getTargets();
        if (savedTargets.containsKey(targetKey)) return;

        savedTargets.put(targetKey, new Data(displayName));
        getMemory().put(KEY, savedTargets);
    }

    /**
     * Sets the target with the given key to complete.
     * <p>
     * Make sure to call {@link #saveChanges()} after this.
     */
    public void setTargetComplete(String targetKey) {
        setTargetComplete(targetKey, true);
    }

    /**
     * Sets the completion status of the target with the given key.
     * <p>
     * Make sure to call {@link #saveChanges()} after this.
     */
    public void setTargetComplete(String targetKey, boolean isComplete) {
        Map<String, Data> savedTargets = getTargets();

        if (savedTargets.containsKey(targetKey)) {
            savedTargets.get(targetKey).setComplete(isComplete);
        } else {
            savedTargets.put(targetKey, new Data(targetKey, isComplete));
        }

        getMemory().put(KEY, savedTargets);
    }

    @Override
    public void advanceAfterInterval(float amount) {
        super.advanceAfterInterval(amount);
        Map<String, Data> targets = getTargets();

        if (targets.isEmpty())
            return;

        // If all targets are complete, complete the achievement.
        if (shouldComplete(targets)) return;

        completeAchievement();
        saveChanges();
        onDestroyed();
    }

    @Override
    public void advanceInCombat(float amount, List<InputEventAPI> events, boolean isSimulation) {
        super.advanceInCombat(amount, events, isSimulation);

        if (isSimulation) return;

        Map<String, Data> targets = getTargets();

        if (targets.isEmpty())
            return;

        if (shouldComplete(targets)) return;

        completeAchievement();
        saveChanges();
        onDestroyed();
    }

    /**
     * Returns true if progress and max progress are the same, or, if either of them are null, if all targets are complete.
     * <p>
     * Override this method to change the completion logic.
     */
    public boolean shouldComplete(Map<String, Data> targets) {
        Float progress = getProgress();
        Float maxProgress = getMaxProgress();

        if (progress != null
                && maxProgress != null
                && maxProgress > 0
                && progress >= maxProgress) {
            return true;
        }

        for (Data data : targets.values()) {
            if (!data.isComplete) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable Float getMaxProgress() {
        return getTargets().size() * 1f;
    }

    @Override
    public @Nullable Float getProgress() {
        float i = 0;

        for (Data data : getTargets().values()) {
            if (data.isComplete) i++;
        }

        return i;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void createTooltip(@NotNull TooltipMakerAPI tooltipMakerAPI, boolean isExpanded, float width) {
        super.createTooltip(tooltipMakerAPI, isExpanded, width);

        tooltipMakerAPI.setBulletedListMode("  -  ");
        List<Data> values = new ArrayList<>(getTargets().values());
        Collections.sort(values, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return o1.displayName.compareTo(o2.displayName);
            }
        });

        for (Data data : values) {
            tooltipMakerAPI.addPara(data.displayName, data.isComplete ? Misc.getTextColor() : Misc.getNegativeHighlightColor(), 0f);
        }
        tooltipMakerAPI.setBulletedListMode(null);
    }

    public static class Data extends JSONObject {
        public static final String DISPLAY_NAME = "displayName";
        public static final String IS_COMPLETE = "isComplete";

        @NotNull
        private String displayName;
        private boolean isComplete;

        public Data(@NotNull String displayName, boolean isComplete) {
            this.displayName = displayName;
            this.isComplete = isComplete;
            saveYourself();
        }

        public Data(@NotNull String displayName) {
            this(displayName, false);
        }

        public @NotNull String getDisplayName() {
            return displayName;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public void setComplete(boolean isComplete) {
            this.isComplete = isComplete;
            saveYourself();
        }

        public void setDisplayName(@NotNull String displayName) {
            this.displayName = displayName;
            saveYourself();
        }

        private void saveYourself() {
            try {
                this.put(DISPLAY_NAME, displayName);
                this.put(IS_COMPLETE, isComplete);
            } catch (JSONException e) {
                // eat the bugs
            }
        }
    }
}
