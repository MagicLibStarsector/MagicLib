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
import java.util.List;

public class MagicAchievementManager {

    private static MagicAchievementManager instance;
    private static final Logger logger = Global.getLogger(MagicAchievementManager.class);
    private static final String specsFilename = "data/config/magic_achievements.csv";
    private final String commonFilename = "magic_achievements.json";
    @NotNull
    private List<MagicAchievementSpec> achievementSpecs = new ArrayList<>();
    @NotNull
    private final List<MagicAchievement> achievements = new ArrayList<>();
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
    public List<MagicAchievement> getAchievements() {
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

        for (MagicAchievement achievement : achievements) {
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
        // Load achievements already saved in Common.
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

        for (MagicAchievement achievement : achievements) {
            achievement.onDestroyed();
        }

        achievements.clear();

        for (int i = 0; i < savedAchievements.length(); i++) {
            try {
                JSONObject savedAchievement = savedAchievements.getJSONObject(i);
                MagicAchievement achievement = MagicAchievement.fromJsonObject(savedAchievement);
                String foundModId = null;

                for (MagicAchievement a : achievements) {
                    if (a.getSpecId().equals(achievement.getSpecId())) {
                        foundModId = a.getModId();
                        break;
                    }
                }

                if (foundModId != null) {
                    logger.warn(String.format("Achievement in mod %s with id %s already exists in %s, skipping.", achievement.getModId(), achievement.getSpecId(), foundModId));
                    continue;
                }

                achievements.add(achievement);
            } catch (Exception e) {
                logger.warn("Unable to load achievement #" + i, e);
            }
        }

        // Load achievements from specs that aren't already saved in Common.
        for (MagicAchievementSpec spec : achievementSpecs) {
            boolean found = false;

            for (MagicAchievement achievement : achievements) {
                if (achievement.getSpecId().equals(spec.getId())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                achievements.add(new MagicAchievement(spec));
            }
        }

        for (MagicAchievement achievement : achievements) {
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
        for (MagicAchievement achievement : achievements) {
            achievement.beforeGameSave();
        }
    }

    public void afterGameSave() {
        for (MagicAchievement achievement : achievements) {
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
        for (MagicAchievement achievement : achievements) {
            if (achievement.getSpecId().equals(specId)) {
                return achievement;
            }
        }

        return null;
    }
}
