package org.magiclib.achievements;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.magiclib.LunaWrapper;
import org.magiclib.LunaWrapperSettingsListener;
import org.magiclib.util.MagicMisc;
import org.magiclib.util.MagicVariables;

import java.util.*;

/**
 * Manages and tracks achievements.
 * <p>
 * An achievement is made from a spec, which is used to create the achievement class with the code to detect the achievement conditions.
 * <p>
 * To add an achievement, add it to the magic_achievements.csv file in your mod's data/config folder.
 * Alternatively, call {@link #addAchievementSpecs(MagicAchievementSpec...)} in your mod plugin's onGameLoad() method.
 * <p>
 * Then, create a class that extends MagicAchievement and implement the detection logic.
 * You will need to call setComplete() and then saveChanges() to complete the achievement.
 *
 * @since 1.3.0
 */
public class MagicAchievementManager {
    private static MagicAchievementManager instance;
    private static final Logger logger = Global.getLogger(MagicAchievementManager.class);
    private static final String specsFilename = "data/config/magic_achievements.csv";
    private static final String commonFilename = "magic_achievements.json";
    private static final String achievementsJsonObjectKey = "achievements";
    private static final String isIntelImportantMemKey = "$magiclib_isAchievementIntelImportant";

    /**
     * Loaded once at launch and not again.
     */
    @NotNull
    private Map<String, MagicAchievementSpec> achievementSpecs = new HashMap<>();
    @NotNull
    private final Map<String, MagicAchievement> achievements = new HashMap<>();
    private final List<String> completedAchievementIdsThatUserHasBeenNotifiedFor = new ArrayList<>();
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
     * Use this to create achievements that are not defined in magic_achievements.csv. You may want to do this to better hide
     * an achievement from players.
     * <p>
     * If the spec is already present, it will be replaced with <code>spec</code>.
     * <p>
     * The <code>script</code> parameter should be the fully qualified class name of the achievement, same as if it were in the csv.
     */
    public void addAchievementSpecs(@NotNull MagicAchievementSpec... specs) {
        for (MagicAchievementSpec spec : specs) {
            achievementSpecs.put(spec.getId(), spec);
        }

        reloadAchievements(Global.getSector() != null);
    }

    public void removeAchievementSpec(@NotNull String specId) {
        achievementSpecs.remove(specId);
    }

    /**
     * Loads achievements (so that any that track stuff outside the campaign are started).
     */
    public void onApplicationLoad() {
        // Set up LunaLib settings.
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            // Add settings listener.
            LunaWrapper.addSettingsListener(new LunaWrapperSettingsListener() {
                @Override
                public void settingsChanged(@NotNull String settings) {
                    Boolean lunaAreAchievementsEnabled = LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enableAchievements");

                    if (lunaAreAchievementsEnabled != null) {
                        areAchievementsEnabled = lunaAreAchievementsEnabled;

                        boolean isGameLoading = Global.getCurrentState() == GameState.CAMPAIGN;
                        setAchievementsEnabled(areAchievementsEnabled, isGameLoading);
                    }
                }
            });

            // Read from settings for initial load.
            Boolean lunaAreAchievementsEnabled = LunaWrapper.getBoolean(MagicVariables.MAGICLIB_ID, "magiclib_enableAchievements");

            if (lunaAreAchievementsEnabled != null) {
                areAchievementsEnabled = lunaAreAchievementsEnabled;
            }
        }

        setAchievementsEnabled(areAchievementsEnabled, false);
    }

    /**
     * (Re)loads achievements (so that any that track stuff outside the campaign are started).
     */
    public void onGameLoad() {
        if (areAchievementsEnabled && Global.getSettings().isDevMode()) {
            instance.achievementSpecs.putAll(getSpecsFromFiles());
        }

        setAchievementsEnabled(areAchievementsEnabled, true);
    }

    /**
     * Sets whether achievements are enabled or not.
     * <p>
     * If setting them to enabled, reloads them, even if they were already enabled.
     *
     * @param isSaveLoaded Whether a save game is loaded. If true, calls {@link MagicAchievement#onSaveGameLoaded(boolean)} on all achievements.
     */
    public void setAchievementsEnabled(boolean areAchievementsEnabled, boolean isSaveLoaded) {
        this.areAchievementsEnabled = areAchievementsEnabled;

        if (areAchievementsEnabled) {
            reloadAchievements(isSaveLoaded);

            if (isSaveLoaded) {
                initIntel();

                if (!Global.getSector().hasTransientScript(MagicAchievementRunner.class)) {
                    Global.getSector().addTransientScript(new MagicAchievementRunner());
                }
            }
        } else {
            logger.info("MagicLib achievements are disabled.");
            removeIntel();
            saveAchievements(true);

            if (isSaveLoaded) {
                Global.getSector().removeTransientScriptsOfClass(MagicAchievementRunner.class);
            }

            for (MagicAchievement magicAchievement : achievements.values()) {
                magicAchievement.onDestroyed();
            }
        }
    }

    public boolean areAchievementsEnabled() {
        return areAchievementsEnabled;
    }

    public void initIntel() {
        if (Global.getSector() == null) return;
        removeIntel();

        // Don't show achievements if there aren't any.
        if (getAchievementSpecs().isEmpty()) return;

        MagicAchievementIntel intel = new MagicAchievementIntel();
        Global.getSector().getIntelManager().addIntel(intel, true);
        intel.setImportant(Global.getSector().getMemoryWithoutUpdate().getBoolean(isIntelImportantMemKey));
        intel.setNew(false);
    }

    public void completeAchievement(@NotNull String specId) {
        MagicAchievement achievement = getAchievement(specId);

        if (achievement != null) {
            achievement.completeAchievement();
            achievement.saveChanges();
        }
    }

    public void completeAchievement(@NotNull Class<? extends MagicAchievement> achievementClass) {
        for (MagicAchievement achievement : achievements.values()) {
            if (achievement.getClass().equals(achievementClass)) {
                completeAchievement(achievement.getSpecId());
            }
        }
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

    /**
     * Gets loaded achievements.
     * <p>
     * Do not add achievements this way. Use {@link #addAchievementSpecs(MagicAchievementSpec...)} instead.
     */
    @NotNull
    public Map<String, MagicAchievement> getAchievements() {
        return achievements;
    }

    private String lastSavedJson = "";

    /**
     * This writes to disk.
     */
    protected void saveAchievements(boolean printUnchangedResultToLog) {
        JSONObject commonJson;
        JSONArray savedAchievements = new JSONArray();

        // Put all achievements into a json array.
        for (MagicAchievement achievement : achievements.values()) {
            try {
                savedAchievements.put(achievement.toJsonObject());
            } catch (Exception e) {
                logger.warn("Unable to save achievement " + achievement.getSpecId(), e);
            }
        }

        // Compare the json for the current achievements to the json for the most recently saved achievements.
        String newAchievementsJsonString = "";
        int indent = 3;
        try {
            newAchievementsJsonString = savedAchievements.toString(indent);

            if (newAchievementsJsonString.equals(lastSavedJson)) {
                if (printUnchangedResultToLog) {
                    logger.info("Not saving achievements because they haven't changed.");
                }
                return;
            }
        } catch (Exception e) {
            logger.warn("Unable to save achievements to " + commonFilename, e);
        }

        // Read the current json from the file.
        try {
            commonJson = new JSONObject(Global.getSettings().readTextFileFromCommon(commonFilename));
            commonJson = (commonJson.length() > 0 ? commonJson : new JSONObject());
        } catch (Exception ex) {
            commonJson = new JSONObject();
        }

        // Replace the achievements loaded from disk with the current achievements.
        try {
            commonJson.put(achievementsJsonObjectKey, savedAchievements);
            String newJsonString = commonJson.toString(indent); // 3 indent

            // Save to disk.
            Global.getSettings().writeTextFileToCommon(commonFilename, newJsonString);
            lastSavedJson = newAchievementsJsonString;
        } catch (Exception e) {
            logger.warn("Unable to save achievements to " + commonFilename, e);
        }

        logger.info("Saved " + achievements.size() + " achievements.");
    }

    /**
     * Unloads all current achievements from the sector and reloads them from files.
     * <p>
     * Achievement specs are loaded from the mod's magic_achievements.csv file, allowing mods to update/change achievements,
     * and player progress is loaded from common.
     * <p>
     * If a mod has removed an achievement (or the player is no longer using the mod),
     * the achievement will be loaded as an "unloaded" achievement and still shown using the spec saved in common.
     * <p>
     * This is fine to call multiple times, but it will reload achievements from files each time.
     *
     * @param isSaveGameLoaded Whether a save game is loaded. If true, calls {@link MagicAchievement#onSaveGameLoaded(boolean)} on all achievements.
     */
    public void reloadAchievements(boolean isSaveGameLoaded) {
        // Generate fresh, unused achievements from the specs.
        Map<String, MagicAchievement> generatedAchievementsById = generateAchievementsFromSpec(instance.achievementSpecs);

        // Load achievements already saved in Common and overwrite the generated ones with their data.
        JSONUtils.CommonDataJSONObject commonJson;
        JSONArray savedAchievementsJson;

        try {
            // Create file if it doesn't exist.
            if (!Global.getSettings().fileExistsInCommon(commonFilename)) {
                saveAchievements(true);
            }

            try {
                commonJson = JSONUtils.loadCommonJSON(commonFilename);
                savedAchievementsJson = commonJson.getJSONArray(achievementsJsonObjectKey);
            } catch (JSONException ex) {
                // If the achievement file is broken, make a backup and then remake it.
                logger.warn("Unable to load achievements from " + commonFilename + ", making a backup and remaking it.", ex);
                Global.getSettings().writeTextFileToCommon(commonFilename + ".backup", Global.getSettings().readTextFileFromCommon(commonFilename));
                Global.getSettings().deleteTextFileFromCommon(commonFilename);
                saveAchievements(true);
                commonJson = JSONUtils.loadCommonJSON(commonFilename);
                savedAchievementsJson = commonJson.getJSONArray(achievementsJsonObjectKey);
            }
        } catch (Exception e) {
            logger.warn("Unable to load achievements from " + commonFilename, e);
            return;
        }


        // Load all achievements that were saved in the file.

        // If the specId doesn't exist in the current mod list, load it as an "unloaded" achievement.
        // This prevents achievements from being lost if a mod is removed or the achievement is removed from a mod.
        for (int i = 0; i < savedAchievementsJson.length(); i++) {
            try {
                JSONObject savedAchievementJson = savedAchievementsJson.getJSONObject(i);
                String specId = savedAchievementJson.optString("id", "");

                if (specId.isEmpty()) {
                    continue;
                }

                // Try to load the achievement from a spec in a loaded mod.
                MagicAchievement loadedAchievement = generatedAchievementsById.get(specId);

                if (loadedAchievement == null) {
                    // If the achievement isn't in a loaded mod, load it as an "unloaded" achievement.
                    logger.warn("Achievement " + specId + " doesn't exist in the current mod list.");
                    loadedAchievement = new MagicUnloadedAchievement();
                }

                // Hydrate the generated achievement from the saved data containing the player's progress.
                loadedAchievement.loadFromJsonObject(savedAchievementJson);

                // If the achievement was loaded from a spec (i.e. mod is loaded), use that spec, rather than the spec saved in common.
                // This will load any changes made to the achievement's spec in the mod without affecting completion or saved data.
                MagicAchievementSpec achievementSpec = instance.achievementSpecs.get(specId);

                if (achievementSpec != null) {
                    loadedAchievement.spec = achievementSpec;
                }

                generatedAchievementsById.put(loadedAchievement.getSpecId(), loadedAchievement);
            } catch (Exception e) {
                logger.warn("Unable to load achievement #" + i, e);
            }
        }

        for (MagicAchievement achievement : achievements.values()) {
            achievement.onDestroyed();
        }

        // Not removing old achievements; we never want to risk deleting any,
        // and putting the new ones in the map will overwrite the old ones.
        achievements.putAll(generatedAchievementsById);

        // Calling onApplicationLoaded and onGameLoaded would seem to make more sense to do
        // in their respective methods, but doing it here ensures that they're called.
        // This method is called from both onApplicationLoad and onGameLoad.
        for (MagicAchievement achievement : achievements.values()) {
            achievement.onApplicationLoaded(achievement.isComplete());
        }

        // Can't just check GameState, it shows a TITLE after pressing Continue on the title page.
        if (isSaveGameLoaded) {
            for (MagicAchievement achievement : achievements.values()) {
                achievement.onSaveGameLoaded(achievement.isComplete());
            }
        }

        // Give scripts that errored out another chance on a reload.
        achievementScriptsWithRunError.clear();

        completedAchievementIdsThatUserHasBeenNotifiedFor.clear();
        for (MagicAchievement prevAchi : achievements.values()) {
            if (prevAchi.isComplete()) {
                completedAchievementIdsThatUserHasBeenNotifiedFor.add(prevAchi.getSpecId());
            }
        }

        logger.info("Loaded " + achievements.size() + " achievements.");
    }

    /**
     * Creates achievements from the given specs, creates the script instances from the class name, and returns them.
     * Note that this is not loading achievements from previous sessions, just creating fresh, unused ones.
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
                if (spec != null)
                    logger.warn(String.format("Unable to load achievement '%s' because class '%s' didn't load!", spec.getId(), spec.getScript()), e);
                else
                    logger.warn("Unable to load achievement because spec was null! What are you doing?!", e);
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
                    String tooltip = item.getString("tooltip").trim();
                    String script = item.getString("script").trim();
                    String image = item.optString("image", null);
                    image = image == null ? null : image.trim();
                    String spoilerLevelStr = item.optString("spoilerLevel", MagicAchievementSpoilerLevel.Visible.name());
                    MagicAchievementSpoilerLevel spoilerLevel = getSpoilerLevel(spoilerLevelStr);
                    String rarityStr = item.optString("rarity", MagicAchievementRarity.Common.name());
                    MagicAchievementRarity rarity = getRarity(rarityStr, id);

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
                                tooltip,
                                script,
                                image,
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
        for (MagicAchievementSpec spec : achievementSpecs.values()) {
            achievement.spec = spec;
            return;
        }
    }

    /**
     * Calls all achievements' beforeGameSave() method.
     */
    public void beforeGameSave() {
        if (getIntel() != null)
            Global.getSector().getMemoryWithoutUpdate().set(isIntelImportantMemKey, getIntel().isImportant());

        // No reason to add intel to the save.
        removeIntel();

        for (MagicAchievement achievement : achievements.values()) {
            if (!achievement.isComplete()) {
                achievement.beforeGameSave();
            }
        }
    }

    /**
     * Calls all achievements' afterGameSave() method.
     */
    public void afterGameSave() {
        initIntel();

        for (MagicAchievement achievement : achievements.values()) {
            if (!achievement.isComplete()) {
                achievement.afterGameSave();
            }
        }
    }

    /**
     * Set of achievement IDs that have thrown an error in their advance method.
     */
    public Set<String> achievementScriptsWithRunError = new HashSet<>();

    /**
     * Called by MagicAchievementRunner.
     * Only called in campaign, not in title, which means the sector is always non-null.
     */
    void advance(float amount) {
        if (!areAchievementsEnabled()) return;

        // Call the advance method on all achievements.
        for (MagicAchievement achievement : achievements.values()) {
            if (achievementScriptsWithRunError.contains(achievement.getSpecId())) {
                continue;
            }

            try {
                if (!achievement.isComplete()) {
                    achievement.advanceInternal(amount);
                }
            } catch (Exception e) {
                String errorStr =
                        String.format("Error running achievement '%s' from mod '%s'!", achievement.getSpecId(), achievement.getModName());
                logger.warn(errorStr, e);
                achievementScriptsWithRunError.add(achievement.getSpecId());
                achievement.errorMessage = errorStr;

                // If dev mode, crash.
                if (Global.getSettings().isDevMode()) {
                    throw new RuntimeException("DevMode is on. Crashing instead of handling error.", e);
                }
            }
        }

        // For all achievements that were just completed, show intel update.
        // Only notify intel if in campaign and not showing a dialog.
        // If in combat, the intel will be shown when the player returns to the campaign.
        if (Global.getCurrentState() == GameState.CAMPAIGN && Global.getSector().getCampaignUI().getCurrentInteractionDialog() == null) {
            for (MagicAchievement achievement : achievements.values()) {
                if (achievement.isComplete() && !completedAchievementIdsThatUserHasBeenNotifiedFor.contains(achievement.getSpecId())) {
                    // Player has unlocked a new achievement! Let's notify them.

                    MagicAchievementIntel intel = getIntel();

                    try {
                        intel.tempAchievement = achievement;
                        intel.sendUpdateIfPlayerHasIntel(null, false, false);
                        intel.tempAchievement = null;
                        playSoundEffect(achievement);
                        completedAchievementIdsThatUserHasBeenNotifiedFor.add(achievement.getSpecId());
                    } catch (Exception e) {
                        logger.warn("Unable to notify intel of achievement " + achievement.getSpecId(), e);
                    }
                }
            }
        }
    }

    /**
     * Set of achievement IDs that have thrown an error in their advanceInCombat method.
     */
    public Set<String> achievementScriptsWithCombatRunError = new HashSet<>();

    /**
     * You shouldn't need to call this. It's called automatically when an achievement is completed.
     */
    public static void playSoundEffect(MagicAchievement achievement) {
        try {
            if (!Global.getSettings().isSoundEnabled()) return;
            Global.getSoundPlayer().playUISound(achievement.getSoundEffectId(), 1, 1);
        } catch (Exception e) {
            logger.warn("Unable to play sound effect for achievement " + achievement.getSpecId() + " in mod " + achievement.getModName(), e);
        }
    }

    /**
     * Called by MagicAchievementCombatScript.
     */
    void advanceInCombat(float amount, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT)
            return;
        if (!areAchievementsEnabled()) return;
        CombatEngineAPI combatEngine = Global.getCombatEngine();
        if (combatEngine == null)
            return;

        // Check for any completed achievements that haven't been notified yet and display them as text above the ship.
        for (MagicAchievement achievement : achievements.values()) {
            if (achievement.isComplete() && !completedAchievementIdsThatUserHasBeenNotifiedFor.contains(achievement.getSpecId())) {
                combatEngine.addFloatingText(combatEngine.getPlayerShip().getLocation(),
                        "Achievement Unlocked: " + achievement.getName(),
                        40,
                        achievement.getVisualEffectData().color,
                        combatEngine.getPlayerShip(),
                        0, 0);
                playSoundEffect(achievement);
                completedAchievementIdsThatUserHasBeenNotifiedFor.add(achievement.getSpecId());
            }
        }

        // Call the advanceInCombat method on all achievements.
        for (MagicAchievement achievement : achievements.values()) {
            if (!achievement.isComplete()) {
                if (achievementScriptsWithCombatRunError.contains(achievement.getSpecId())) {
                    continue;
                }

                try {
                    achievement.advanceInCombat(amount, events, combatEngine.isSimulation());
                } catch (Exception e) {
                    String errorStr =
                            String.format("Error running achievement '%s' from mod '%s' during combat!", achievement.getSpecId(), achievement.getModName());
                    logger.warn(errorStr, e);
                    achievementScriptsWithCombatRunError.add(achievement.getSpecId());
                    achievement.errorMessage = errorStr;

                    // If dev mode, crash.
                    if (Global.getSettings().isDevMode()) {
                        throw e;
                    }
                }
            }
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
    private static MagicAchievementRarity getRarity(String rarityStr, String specId) {
        MagicAchievementRarity rarity = MagicAchievementRarity.Common;

        if (rarityStr != null) {
            try {
                rarity = MagicAchievementRarity.valueOf(Misc.ucFirst(rarityStr.trim().toLowerCase()));
            } catch (Exception e) {
                logger.warn("Invalid rarity '" + rarityStr + " for achievement '" + specId + "', using Common.");
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
}
