package org.magiclib.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.magiclib.util.MagicMisc;

import java.util.ArrayList;
import java.util.List;

public class MagicAchievementManager {

    private static MagicAchievementManager instance;
    private static final Logger logger = Global.getLogger(MagicAchievementManager.class);
    private final String filename = "data/config/magic_achievements.csv";
    @NotNull
    private List<MagicAchievementSpec> achievementSpecs = new ArrayList<>();

    @NotNull
    public static MagicAchievementManager getInstance() {
        if (instance == null) {
            instance = new MagicAchievementManager();
            instance.loadData();
        }

        return instance;
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
        List<MagicAchievement> achievements = new ArrayList<>();

        for (MagicAchievementSpec spec : achievementSpecs) {
            achievements.add(new MagicAchievement(spec));
        }

        return achievements;
    }

    public void loadData() {
        List<MagicAchievementSpec> achievementList = new ArrayList<>();

        for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            JSONArray modCsv = null;
            try {
                modCsv = Global.getSettings().loadCSV(filename, mod.getId());
            } catch (Exception e) {
                if (e instanceof RuntimeException && e.getMessage().contains("not found in")) {
                    // Swallow exceptions caused by the mod not having achievements.
                } else {
                    logger.warn("Unable to load achievements in " + mod.getId() + " by " + MagicMisc.takeFirst(mod.getAuthor(), 50) + " from file " + filename, e);
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

                    for (MagicAchievementSpec achievement : achievementList) {
                        if (achievement.getId().equals(id)) {
                            skip = true;
                            logger.warn(String.format("Achievement with id %s in mod %s already exists in mod %s, skipping.",
                                    id, mod.getId(), achievement.getModId()));
                            break;
                        }
                    }

                    if (!skip) {
                        achievementList.add(new MagicAchievementSpec(
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
                    logger.warn("Unable to load achievement #" + i + " (" + id + ") in " + mod.getId() + " by " + mod.getAuthor().substring(0, 30) + " from file " + filename, e);
                }
            }
        }

        achievementSpecs = achievementList;
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
}
