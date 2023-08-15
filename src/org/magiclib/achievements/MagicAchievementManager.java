package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.magiclib.util.MagicMisc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagicAchievementManager {

    private static MagicAchievementManager instance;
    private static final Logger logger = Global.getLogger(MagicAchievementManager.class);
    private static final String specsFilename = "data/config/magic_achievements.csv";
    private final String commonFilename = "magic_achievements.json";
    @NotNull
    private List<MagicAchievementSpec> achievementSpecs = new ArrayList<>();
    @NotNull
    private final Map<String, MagicAchievement> achievements = new HashMap<>();
    private static final String achievementsJsonObjectKey = "achievements";

    @NotNull
    public static MagicAchievementManager getInstance() {
        if (instance == null) {
            instance = new MagicAchievementManager();
            instance.loadSpecs();
            instance.loadAchievements();
        }

        return instance;
    }

    public void onGameLoad() {
        initIntel();
        Global.getSector().addTransientScript(new MagicAchievementRunner());

        if (Global.getSettings().isDevMode()) {
            MagicAchievementManager.getInstance().loadAchievements();
        }
    }

    public void initIntel() {
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();

        while (intelManager.hasIntelOfClass(MagicAchievementIntel.class)) {
            intelManager.removeIntel(intelManager.getFirstIntel(MagicAchievementIntel.class));
        }

        MagicAchievementIntel intel = new MagicAchievementIntel();
        intelManager.addIntel(intel, true);
    }

    @Nullable
    public MagicAchievementIntel getIntel() {
        try {
            return (MagicAchievementIntel) Global.getSector().getIntelManager().getIntel(MagicAchievementIntel.class);
        } catch (Exception ex) {
            logger.warn("Unable to get MagicAchievementIntel.", ex);
            return null;
        }
    }

    @NotNull
    public Map<String, MagicAchievement> getAchievements() {
        return achievements;
    }

    public void saveAchievements() {
        JSONUtils.CommonDataJSONObject commonJson;
        JSONArray savedAchievements = new JSONArray();

        try {
            commonJson = JSONUtils.loadCommonJSON(commonFilename);
        } catch (Exception e) {
            logger.warn("Unable to load achievements from " + commonFilename, e);
            return;
        }

        for (MagicAchievement achievement : achievements.values()) {
            try {
                savedAchievements.put(achievement.toJsonObject());
            } catch (Exception e) {
                logger.warn("Unable to save achievement " + achievement.getSpecId(), e);
            }
        }

        try {
            commonJson.put(achievementsJsonObjectKey, savedAchievements);
            commonJson.save();
        } catch (Exception e) {
            logger.warn("Unable to save achievements to " + commonFilename, e);
        }

        logger.info("Saved " + achievements.size() + " achievements.");
    }

    public void loadAchievements() {
        Map<String, MagicAchievement> newAchievementsById = new HashMap<>();

        // Create all achievement objects from specs.
        for (MagicAchievementSpec spec : achievementSpecs) {
            try {
                final Class<?> commandClass = Global.getSettings().getScriptClassLoader().loadClass(spec.getScript());
                if (!MagicAchievement.class.isAssignableFrom(commandClass)) {
                    throw new RuntimeException(String.format("%s does not extend %s", commandClass.getCanonicalName(), MagicAchievement.class.getCanonicalName()));
                }

                MagicAchievement magicAchievement = (MagicAchievement) commandClass.newInstance();
                magicAchievement.spec = spec;
                newAchievementsById.put(spec.getId(), magicAchievement);
            } catch (Exception e) {
                logger.warn(String.format("Unable to load achievement '%s' because class '%s' didn't load!", spec.getId(), spec.getScript()), e);
            }
        }

        // Load achievements already saved in Common and overwrite the generated ones with their data.
        JSONUtils.CommonDataJSONObject commonJson;
        JSONArray savedAchievements;

        try {
            // Create file if it doesn't exist.
            if (!Global.getSettings().fileExistsInCommon(commonFilename)) {
                saveAchievements();
            }

            //noinspection resource
            commonJson = JSONUtils.loadCommonJSON(commonFilename);
            savedAchievements = commonJson.getJSONArray(achievementsJsonObjectKey);
        } catch (Exception e) {
            logger.warn("Unable to load achievements from " + commonFilename, e);
            return;
        }

        for (int i = 0; i < savedAchievements.length(); i++) {
            try {
                JSONObject savedAchievementJson = savedAchievements.getJSONObject(i);
                String specId = savedAchievementJson.optString("sssid", "");
                // Try to load the achievement from a spec in a loaded mod.
                MagicAchievement blankAchievement = newAchievementsById.get(specId);

                if (blankAchievement == null) {
                    // If the achievement isn't in a loaded mod, load it as an "unloaded" achievement.
                    logger.warn("Achievement " + specId + " doesn't exist in the current mod list.");
                    blankAchievement = new MagicUnloadedAchievement();
                    blankAchievement.spec = MagicAchievementSpec.fromJsonObject(savedAchievementJson);
                }

                blankAchievement.loadFromJsonObject(savedAchievementJson);
                newAchievementsById.put(blankAchievement.getSpecId(), blankAchievement);
            } catch (Exception e) {
                logger.warn("Unable to load achievement #" + i, e);
            }
        }


        for (MagicAchievement achievement : achievements.values()) {
            achievement.onDestroyed();
        }

        achievements.putAll(newAchievementsById);

        for (MagicAchievement achievement : achievements.values()) {
            achievement.onCreated();
        }

        logger.info("Loaded " + achievements.size() + " achievements.");

        saveAchievements();
    }

    public void loadSpecs() {
        List<MagicAchievementSpec> newAchievementSpecs = new ArrayList<>();

        for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            JSONArray modCsv = null;
            try {
                modCsv = Global.getSettings().loadCSV(specsFilename, mod.getId());
            } catch (Exception e) {
                if (e instanceof RuntimeException && e.getMessage().contains("not found in")) {
                    // Swallow exceptions caused by the mod not having achievements.
                } else {
                    logger.warn("Unable to load achievements in " + mod.getId() + " by " + MagicMisc.takeFirst(mod.getAuthor(), 50) + " from file " + specsFilename, e);
                }
            }

            if (modCsv == null) continue;
            logger.info(modCsv);
            for (int i = 0; i < modCsv.length(); i++) {
                String id = null;
                try {
                    JSONObject item = modCsv.getJSONObject(i);
                    id = item.getString("id").trim();
                    String name = item.getString("name").trim();
                    String description = item.getString("description").trim();
                    String script = item.getString("script").trim();
                    String image = item.optString("image", null);
                    image = image == null ? null : image.trim();
                    boolean hasProgressBar = item.optBoolean("hasProgressBar", false);
                    String spoilerLevelStr = item.optString("spoilerLevel", SpoilerLevel.Visible.name());
                    SpoilerLevel spoilerLevel = getSpoilerLevel(spoilerLevelStr);

                    boolean skip = false;

                    for (MagicAchievementSpec achievement : newAchievementSpecs) {
                        if (achievement.getId().equals(id)) {
                            skip = true;
                            logger.warn(String.format("Achievement with id %s in mod %s already exists in mod %s, skipping.",
                                    id, mod.getId(), achievement.getModId()));
                            break;
                        }
                    }

                    if (!skip) {
                        newAchievementSpecs.add(new MagicAchievementSpec(
                                mod.getId(),
                                id,
                                name,
                                description,
                                script,
                                image,
                                hasProgressBar,
                                spoilerLevel
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("Unable to load achievement #" + i + " (" + id + ") in " + mod.getId() + " by " + mod.getAuthor().substring(0, 30) + " from file " + specsFilename, e);
                }
            }
        }

        this.achievementSpecs = newAchievementSpecs;
    }

    public void beforeGameSave() {
        for (MagicAchievement achievement : achievements.values()) {
            achievement.beforeGameSave();
        }
    }

    public void afterGameSave() {
        for (MagicAchievement achievement : achievements.values()) {
            achievement.afterGameSave();
        }
    }

    @NotNull
    private static SpoilerLevel getSpoilerLevel(String spoilerLevelStr) {
        SpoilerLevel spoilerLevel = SpoilerLevel.Visible;

        if (spoilerLevelStr != null) {
            if (spoilerLevelStr.equalsIgnoreCase("spoiler")) {
                spoilerLevel = SpoilerLevel.Spoiler;
            } else if (spoilerLevelStr.equalsIgnoreCase("hidden")) {
                spoilerLevel = SpoilerLevel.Hidden;
            }
        }
        return spoilerLevel;
    }

    public @NotNull List<MagicAchievementSpec> getAchievementSpecs() {
        return achievementSpecs;
    }

    @Nullable
    public MagicAchievement getAchievement(String specId) {
        for (MagicAchievement achievement : achievements.values()) {
            if (achievement.getSpecId().equals(specId)) {
                return achievement;
            }
        }

        return null;
    }
}
