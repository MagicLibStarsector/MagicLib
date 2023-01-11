package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.SWPModPlugin;
import data.scripts.VayraModPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tartiflette
 */
public class MagicVariables {

    static float SECTOR_SIZE = 0;
    static float SECTOR_HEIGT = 0;
    static float SECTOR_WIDTH = 0;

    private static void calculateSectorDimentions() {
        float dist = 0;
        for (StarSystemAPI s : Global.getSector().getStarSystems()) {
            SECTOR_WIDTH = Math.max(SECTOR_WIDTH, Math.abs(s.getLocation().x));
            SECTOR_HEIGT = Math.max(SECTOR_HEIGT, Math.abs(s.getLocation().y));
            dist = Math.max(dist, s.getLocation().lengthSquared());
        }
        SECTOR_SIZE = (float) Math.sqrt(dist);
    }

    public static float getSectorSize() {
        if (SECTOR_SIZE == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_SIZE;
    }

    public static float getSectorSizeLY() {
        if (SECTOR_SIZE == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_SIZE / Misc.getUnitsPerLightYear();
    }

    public static float getSectorHeight() {
        if (SECTOR_HEIGT == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_HEIGT;
    }

    public static float getSectorHeightLY() {
        if (SECTOR_HEIGT == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_HEIGT / Misc.getUnitsPerLightYear();
    }

    public static float getSectorWidth() {
        if (SECTOR_WIDTH == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_WIDTH;
    }

    public static float getSectorWidthLY() {
        if (SECTOR_WIDTH == 0) {
            calculateSectorDimentions();
        }
        return SECTOR_WIDTH / Misc.getUnitsPerLightYear();
    }

    public static final String VARIANT_PATH = "data/config/modFiles/magicBounty_variants/";
    public static final String AVOID_COLONIZED_SYSTEM = "theme_already_colonized";
    public static final String AVOID_OCCUPIED_SYSTEM = "theme_already_occupied";
    public static final String AVOID_BLACKHOLE_PULSAR = "no_pulsar_blackhole";
    public static final String SEEK_EMPTY_SYSTEM = "procgen_no_theme";
    public static final String SEEK_EMPTY_SAFE_SYSTEM = "procgen_no_theme_pulsar_blackhole";

    public static final String MAGICLIB_ID = "MagicLib";
    public static final String BOUNTY_FACTION = "ML_bounty";

    public static boolean verbose = false;
    public static boolean bounty_test_mode = false;

    public static List<String> mergedThemesBlacklist = new ArrayList<>();

    public static void loadThemesBlacklist() {
        mergedThemesBlacklist.clear();
        //load list from settings
        List<String> themes = MagicSettings.getList(MAGICLIB_ID, "merged_themes_blacklist");
        for (String s : themes) {
            if (!mergedThemesBlacklist.contains(s)) mergedThemesBlacklist.add(s);
        }
        //default vanilla themes to load
        mergedThemesBlacklist.add("theme_unsafe");
        mergedThemesBlacklist.add("theme_remnant");
        mergedThemesBlacklist.add("theme_remnant_main");
        mergedThemesBlacklist.add("theme_remnant_secondary");
        mergedThemesBlacklist.add("theme_remnant_no_fleets");
        mergedThemesBlacklist.add("theme_remnant_destroyed");
        mergedThemesBlacklist.add("theme_remnant_suppressed");
        mergedThemesBlacklist.add("theme_remnant_resurgent");
    }

    public static List<String> presetShipIdsOfLastCreatedFleet = new ArrayList<>();

    private static boolean checkedBounties = false;
    public static boolean ibb = false;
    public static boolean hvb = false;
    public static boolean MagicBountiesEnabled = false;

    public static boolean getIBB() {
        if (!checkedBounties) checkBountySystems();
        return ibb;
    }

    public static boolean getHVB() {
        if (!checkedBounties) checkBountySystems();
        return hvb;
    }

    public static boolean getMagicBounty() {
        if (!checkedBounties) checkBountySystems();
        return MagicBountiesEnabled;
    }

    public static void checkBountySystems() {

        //Check MagicBounties
        MagicBountiesEnabled = MagicSettings.getBoolean(MAGICLIB_ID, "bounty_board_enabled");

        //check for IBBs presence
        if (Global.getSettings().getModManager().isModEnabled("swp") && SWPModPlugin.Module_FamousBounties == true) {
            Global.getSector().getMemoryWithoutUpdate().set("$IBB_ACTIVE", true);
            ibb = true;
        } else {
            Global.getSector().getMemoryWithoutUpdate().set("$IBB_ACTIVE", false);
        }

        //check for HVBs presence
        if (Global.getSettings().getModManager().isModEnabled("vayrasector") && VayraModPlugin.UNIQUE_BOUNTIES == true) {
            Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", true);
            hvb = true;
        } else {
            Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", false);
        }
        /*
        //check for Bounties Expanded HVBs presence
        if (Global.getSettings().getModManager().isModEnabled("bountiesexpanded") && Settings.HIGH_VALUE_BOUNTY_ACTIVE == true) {
            Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", true);
            hvb=true;
        } else if(!hvb){
            Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", false);
        }
        */
        checkedBounties = true;
    }
}
