package org.magiclib.achievements;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.magiclib.util.MagicMisc;
import org.magiclib.util.MagicVariables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagicAchievementManager {

    private static MagicAchievementManager instance;
    private static final Logger logger = Global.getLogger(MagicAchievementManager.class);
    private static final String specsFilename = "data/config/magic_achievements.csv";
    private static final String commonFilename = "magic_achievements.json";
    private static final String achievementsJsonObjectKey = "achievements";

    @NotNull
    private Map<String, MagicAchievementSpec> achievementSpecs = new HashMap<>();
    @NotNull
    private final Map<String, MagicAchievement> achievements = new HashMap<>();
    private final List<String> previouslyCompleted = new ArrayList<>();
    private boolean areAchievementsEnabled = true;

    @NotNull
    public static MagicAchievementManager getInstance() {
        if (instance == null) {
            instance = new MagicAchievementManager();
            instance.achievementSpecs = getSpecsFromFiles();
//            instance.reloadAchievements();
//            instance.saveAchievements();
        }

        return instance;
    }

    /**
     * Loads achievements (so that any that track stuff outside of the campaign are started).
     */
    public void onApplicationLoad() {
        // Set up LunaLib settings.
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            // Add settings listener.
            LunaSettings.addSettingsListener(new LunaSettingsListener() {
                @Override
                public void settingsChanged(@NotNull String settings) {
                    Boolean lunaAreAchievementsEnabled = LunaSettings.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enableAchievements");

                    if (lunaAreAchievementsEnabled != null) {
                        areAchievementsEnabled = lunaAreAchievementsEnabled;

                        boolean isGameLoading = Global.getCurrentState() == GameState.CAMPAIGN;
                        setAchievementsEnabled(areAchievementsEnabled, isGameLoading);
                    }
                }
            });

            // Read from settings for initial load.
            Boolean lunaAreAchievementsEnabled = LunaSettings.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enableAchievements");

            if (lunaAreAchievementsEnabled != null) {
                areAchievementsEnabled = lunaAreAchievementsEnabled;
            }
        }

        setAchievementsEnabled(areAchievementsEnabled, false);
    }

    /**
     * (Re)loads achievements (so that any that track stuff outside of the campaign are started).
     */
    public void onGameLoad() {
        setAchievementsEnabled(areAchievementsEnabled, true);
    }

    public void setAchievementsEnabled(boolean areAchievementsEnabled, boolean isSaveLoaded) {
        if (areAchievementsEnabled) {
            if (isSaveLoaded) {
                initIntel();

                if (!Global.getSector().hasTransientScript(MagicAchievementRunner.class)) {
                    Global.getSector().addTransientScript(new MagicAchievementRunner());
                }
            }

//            if (Global.getSettings().isDevMode()) {
            MagicAchievementManager.getInstance().reloadAchievements(isSaveLoaded);
//            }
        } else {
            logger.info("MagicLib achievements are disabled.");
            removeIntel();
            saveAchievements();
            Global.getSector().removeTransientScriptsOfClass(MagicAchievementRunner.class);

            for (MagicAchievement magicAchievement : achievements.values()) {
                magicAchievement.onDestroyed();
            }
        }
    }

    public void initIntel() {
        if (Global.getSector() == null) return;
        removeIntel();

        MagicAchievementIntel intel = new MagicAchievementIntel();
        Global.getSector().getIntelManager().addIntel(intel, true);
    }

    @Nullable
    public MagicAchievementIntel getIntel() {
        try {
            return (MagicAchievementIntel) Global.getSector().getIntelManager().getFirstIntel(MagicAchievementIntel.class);
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

        // for all achievements that were just completed, notify intel
        for (MagicAchievement achievement : achievements.values()) {
            if (achievement.isComplete() && !previouslyCompleted.contains(achievement.getSpecId())) {
                MagicAchievementIntel intel = getIntel();
                try {
                    intel.tempAchievement = achievement;
                    intel.sendUpdateIfPlayerHasIntel(null, false, false);
                    intel.tempAchievement = null;
                } catch (Exception e) {
                    logger.warn("Unable to notify intel of achievement " + achievement.getSpecId(), e);
                }

                previouslyCompleted.add(achievement.getSpecId());
            }
        }

        logger.info("Saved " + achievements.size() + " achievements.");
    }

    /**
     * Unloads all current achievements from the sector and reloads them from files.
     *
     * @param isSaveGameLoaded Whether a save game is being loaded.
     */
    public void reloadAchievements(boolean isSaveGameLoaded) {
        Map<String, MagicAchievement> newAchievementsById = generateAchievementsFromSpec(instance.achievementSpecs);

        // Load achievements already saved in Common and overwrite the generated ones with their data.
        JSONUtils.CommonDataJSONObject commonJson;
        JSONArray savedAchievements;

        try {
            // Create file if it doesn't exist.
            if (!Global.getSettings().fileExistsInCommon(commonFilename)) {
                saveAchievements();
            }

            try {
                //noinspection resource
                commonJson = JSONUtils.loadCommonJSON(commonFilename);
                savedAchievements = commonJson.getJSONArray(achievementsJsonObjectKey);
            } catch (JSONException ex) {
                // If the achievement file is broken, make a backup and then remake it.
                logger.warn("Unable to load achievements from " + commonFilename + ", making a backup and remaking it.", ex);
                Global.getSettings().writeTextFileToCommon(commonFilename + ".backup", Global.getSettings().readTextFileFromCommon(commonFilename));
                Global.getSettings().deleteTextFileFromCommon(commonFilename);
                saveAchievements();
                commonJson = JSONUtils.loadCommonJSON(commonFilename);
                savedAchievements = commonJson.getJSONArray(achievementsJsonObjectKey);
            }
        } catch (Exception e) {
            logger.warn("Unable to load achievements from " + commonFilename, e);
            return;
        }


        // Load all achievements that were saved in the file.

        // If the specId doesn't exist in the current mod list, load it as an "unloaded" achievement.
        // This prevents achievements from being lost if a mod is removed or the achievement is removed from a mod.
        for (int i = 0; i < savedAchievements.length(); i++) {
            try {
                JSONObject savedAchievementJson = savedAchievements.getJSONObject(i);
                String specId = savedAchievementJson.optString("id", "");
                // Try to load the achievement from a spec in a loaded mod.
                MagicAchievement loadedAchievement = newAchievementsById.get(specId);

                if (loadedAchievement == null) {
                    // If the achievement isn't in a loaded mod, load it as an "unloaded" achievement.
                    logger.warn("Achievement " + specId + " doesn't exist in the current mod list.");
                    loadedAchievement = new MagicUnloadedAchievement();
                }

                loadedAchievement.loadFromJsonObject(savedAchievementJson);

                // If the achievement was loaded from a spec (ie mod is loaded), use that spec, rather than the spec saved in common.
                // This will load any changes made to the achievement's spec in the mod without affecting completion or saved data.
                MagicAchievementSpec achievementSpec = instance.achievementSpecs.get(specId);

                if (achievementSpec != null) {
                    loadedAchievement.spec = achievementSpec;
                }

                newAchievementsById.put(loadedAchievement.getSpecId(), loadedAchievement);
            } catch (Exception e) {
                logger.warn("Unable to load achievement #" + i, e);
            }
        }

        for (MagicAchievement achievement : achievements.values()) {
            achievement.onDestroyed();
        }

        // Not removing old achievements; we never want to risk deleting any,
        // and putting the new ones in the map will overwrite the old ones.
        achievements.putAll(newAchievementsById);

        // Calling onApplicationLoaded and onGameLoaded would seem to make more sense to do
        // in their respective methods, but doing it here ensures that they're called.
        for (MagicAchievement achievement : achievements.values()) {
            achievement.onApplicationLoaded();
        }

        // Can't just check GameState, it shows a TITLE after pressing Continue on the title page.
        if (isSaveGameLoaded) {
            for (MagicAchievement achievement : achievements.values()) {
                achievement.onGameLoaded();
            }
        }

        logger.info("Loaded " + achievements.size() + " achievements.");

        for (MagicAchievement prevAchi : achievements.values()) {
            if (prevAchi.isComplete()) {
                previouslyCompleted.add(prevAchi.getSpecId());
            }
        }
    }

    /**
     * Creates achievements from the given specs, creates the script instances from the class name, and returns them.
     */
    public static @NotNull Map<String, MagicAchievement> generateAchievementsFromSpec(@NotNull Map<String, MagicAchievementSpec> specs) {
        Map<String, MagicAchievement> newAchievementsById = new HashMap<>();

        for (MagicAchievementSpec spec : specs.values()) {
            try {
                final Class<?> commandClass = Global.getSettings().getScriptClassLoader().loadClass(spec.getScript());
                if (!MagicAchievement.class.isAssignableFrom(commandClass)) {
                    throw new RuntimeException(String.format("%s does not extend %s", commandClass.getCanonicalName(), MagicAchievement.class.getCanonicalName()));
                }

                MagicAchievement magicAchievement = (MagicAchievement) commandClass.newInstance();
                magicAchievement.spec = spec;
                newAchievementsById.put(spec.getId(), magicAchievement);
                logger.info("Loaded achievement " + spec.getId() + " from " + spec.getModId() + " with script " + spec.getScript() + ".");
            } catch (Exception e) {
                logger.warn(String.format("Unable to load achievement '%s' because class '%s' didn't load!", spec.getId(), spec.getScript()), e);
            }
        }

        return newAchievementsById;
    }

    /**
     * Reads achievement specs from CSV files in mods and returns them.
     */
    public static @NotNull Map<String, MagicAchievementSpec> getSpecsFromFiles() {
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
                    String spoilerLevelStr = item.optString("spoilerLevel", MagicAchievementSpoilerLevel.Visible.name());
                    MagicAchievementSpoilerLevel spoilerLevel = getSpoilerLevel(spoilerLevelStr);
                    String rarityStr = item.optString("rarity", MagicAchievementRarity.Common.name());
                    MagicAchievementRarity rarity = getRarity(rarityStr);

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
                                mod.getName(),
                                id,
                                name,
                                description,
                                script,
                                image,
                                hasProgressBar,
                                spoilerLevel,
                                rarity
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("Unable to load achievement #" + i + " (" + id + ") in " + mod.getId() + " by " + mod.getAuthor().substring(0, 30) + " from file " + specsFilename, e);
                }
            }
        }

        Map<String, MagicAchievementSpec> newAchievementSpecsById = new HashMap<>();

        for (MagicAchievementSpec newAchievementSpec : newAchievementSpecs) {
            newAchievementSpecsById.put(newAchievementSpec.getId(), newAchievementSpec);
        }

        return newAchievementSpecsById;
    }

    public @NotNull Map<String, MagicAchievementSpec> getAchievementSpecs() {
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

    /**
     * Resets the achievement passed in to the spec. This is useful for when a mod changes the achievement.
     * Does not uncomplete it, even if it would no longer be earned.
     */
    public void resetAchievementToSpec(@NotNull MagicAchievement achievement) {
        for (MagicAchievementSpec spec : MagicAchievementManager.getSpecsFromFiles().values()) {
            achievement.spec = spec;
            return;
        }
    }

    /**
     * Calls all achievements' beforeGameSave() method.
     */
    public void beforeGameSave() {
        // No reason to add intel to the save.
        removeIntel();

        for (MagicAchievement achievement : achievements.values()) {
            achievement.beforeGameSave();
        }
    }

    /**
     * Calls all achievements' afterGameSave() method.
     */
    public void afterGameSave() {
        initIntel();

        for (MagicAchievement achievement : achievements.values()) {
            achievement.afterGameSave();
        }
    }

    @NotNull
    private static MagicAchievementSpoilerLevel getSpoilerLevel(String spoilerLevelStr) {
        MagicAchievementSpoilerLevel spoilerLevel = MagicAchievementSpoilerLevel.Visible;

        if (spoilerLevelStr != null) {
            if (spoilerLevelStr.equalsIgnoreCase("spoiler")) {
                spoilerLevel = MagicAchievementSpoilerLevel.Spoiler;
            } else if (spoilerLevelStr.equalsIgnoreCase("hidden")) {
                spoilerLevel = MagicAchievementSpoilerLevel.Hidden;
            }
        }

        return spoilerLevel;
    }

    @NotNull
    private static MagicAchievementRarity getRarity(String rarityStr) {
        MagicAchievementRarity rarity = MagicAchievementRarity.Common;

        if (rarityStr != null) {
            if (rarityStr.equalsIgnoreCase("uncommon")) {
                rarity = MagicAchievementRarity.Uncommon;
            } else if (rarityStr.equalsIgnoreCase("rare")) {
                rarity = MagicAchievementRarity.Rare;
            } else if (rarityStr.equalsIgnoreCase("epic")) {
                rarity = MagicAchievementRarity.Epic;
            }
        }

        return rarity;
    }

    private static void removeIntel() {
        if (Global.getSector() == null) return;

        IntelManagerAPI intelManager = Global.getSector().getIntelManager();

        while (intelManager.hasIntelOfClass(MagicAchievementIntel.class)) {
            intelManager.removeIntel(intelManager.getFirstIntel(MagicAchievementIntel.class));
        }
    }

    /**
     * Called by MagicAchievementCombatScript.
     */
    void advanceInCombat(float amount, List<InputEventAPI> events) {
        for (MagicAchievement achievement : achievements.values()) {
            if (!achievement.isComplete()) {
                achievement.advanceInCombat(amount, events);
            }
        }
    }
}
