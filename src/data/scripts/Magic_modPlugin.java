package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.thoughtworks.xstream.XStream;
import data.scripts.plugins.MagicAutoTrails;
import data.scripts.plugins.MagicCampaignTrailPlugin;
import data.scripts.terrain.MagicAsteroidBeltTerrainPlugin;
import data.scripts.terrain.MagicAsteroidFieldTerrainPlugin;
import data.scripts.util.*;

@Deprecated
public class Magic_modPlugin {

    public static void onApplicationLoad() {
        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();

        //gather mod's system themes
        MagicVariables.loadThemesBlacklist();
        MagicVariables.verbose = Global.getSettings().isDevMode();
    }


    public static void onDevModeF8Reload() {
        MagicSettings.loadModSettings();
        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();
    }

    ////////////////////////////////////////
    //                                    //
    //            ON GAME LOAD            //
    //                                    //
    ////////////////////////////////////////


    public static void onGameLoad(boolean newGame) {
        MagicIncompatibleHullmods.clearData();

        //Add industry item wrangler
        SectorAPI sector = Global.getSector();
        if (sector != null) {
            sector.addTransientListener(new MagicIndustryItemWrangler());
            sector.addTransientScript(new MagicCampaignTrailPlugin());
        }
    }

    /**
     * Define how classes are named in the save xml, allowing class renaming without
     * breaking saves.
     *
     * @param x
     */

    public static void configureXStream(XStream x) {
        // Keep the Magic replacements out of the save file.
        // The game will automatically swap to the Magic replacements on load because `terrain.json` replaces the vanilla ones.
        x.alias("AsteroidBeltTerrainPlugin", MagicAsteroidBeltTerrainPlugin.class);
        x.alias("AsteroidFieldTerrainPlugin", MagicAsteroidFieldTerrainPlugin.class);
    }
}
