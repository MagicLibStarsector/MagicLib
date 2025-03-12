package org.magiclib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.thoughtworks.xstream.XStream;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.achievements.MagicAchievementManager;
import org.magiclib.achievements.TestingAchievementSpec;
import org.magiclib.bounty.*;
import org.magiclib.kotlin.MagicKotlinModPlugin;
import org.magiclib.paintjobs.MagicPaintjobCampaignRefitAdder;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.plugins.MagicAutoTrails;
import org.magiclib.plugins.MagicCampaignTrailPlugin;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.terrain.MagicAsteroidBeltTerrainPlugin;
import org.magiclib.terrain.MagicAsteroidFieldTerrainPlugin;
import org.magiclib.util.*;

import java.awt.*;

/**
 * Master ModPlugin for MagicLib. Handles all the loading of data and scripts.
 */
public class Magic_modPlugin extends BaseModPlugin {

    ////////////////////////////////////////
    //                                    //
    //       ON APPLICATION LOAD          //
    //                                    //
    ////////////////////////////////////////


    @Override
    public void onApplicationLoad() throws ClassNotFoundException {
        data.scripts.Magic_modPlugin.onApplicationLoad();

        MagicSettings.loadModSettings();

        if (MagicSettings.modSettings == null) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "Malformed modSettings.json detected"
                    + System.lineSeparator() + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }


        //dev-mode pre-loading the bounties to throw a crash if the JSON is messed up on merge
        if (Global.getSettings().isDevMode()) {
            MagicBountyLoader.loadBountiesFromJSON(false);
//            if (!Global.getSettings().getModManager().isModEnabled("vayrasector") || VayraModPlugin.UNIQUE_BOUNTIES == false) {
//                MagicBountyHVB.convertHVBs(false);
//            }
            if (MagicBountyLoader.JSONfailedFlagForDevMode) {
                String message = System.lineSeparator()
                        + System.lineSeparator() + "Malformed MagicBounty_data.json detected"
                        + System.lineSeparator() + System.lineSeparator();
                throw new ClassNotFoundException(message);
            }
        }

        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();

        //gather mod's system themes
        MagicVariables.loadThemesBlacklist();
        MagicVariables.verbose = Global.getSettings().isDevMode();
        MagicVariables.bounty_test_mode = MagicSettings.getBoolean(MagicVariables.MAGICLIB_ID, "bounty_board_test_mode");

        MagicAchievementManager.getInstance();
        MagicAchievementManager.getInstance().onApplicationLoad();

        MagicPaintjobManager.onApplicationLoad();

        MagicSubsystemsManager.initialize();
    }

    @Override
    public void onDevModeF8Reload() {
        data.scripts.Magic_modPlugin.onDevModeF8Reload();

        MagicSettings.loadModSettings();
        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();

        //Check for other bounty systems
        MagicVariables.checkBountySystems();

        //gather mod's system themes
        MagicVariables.loadThemesBlacklist();
        MagicVariables.verbose = Global.getSettings().isDevMode();
        MagicVariables.bounty_test_mode = MagicSettings.getBoolean(MagicVariables.MAGICLIB_ID, "bounty_board_test_mode");
    }

    ////////////////////////////////////////
    //                                    //
    //            ON GAME LOAD            //
    //                                    //
    ////////////////////////////////////////

    @Override
    public void onGameLoad(boolean newGame) {
        data.scripts.Magic_modPlugin.onGameLoad(newGame);

//        MagicAutoTrails.getTrailData();
        MagicIncompatibleHullmods.clearData();

        //Add industry item wrangler
        SectorAPI sector = Global.getSector();
        if (sector != null) {
            sector.addTransientScript(new MagicPaintjobCampaignRefitAdder());
            sector.addTransientListener(new MagicIndustryItemWrangler());
            sector.addTransientScript(new MagicCampaignTrailPlugin());
        }

        MagicVariables.checkBountySystems();

        if (MagicVariables.getMagicBounty()) {
            if (newGame) {
                //add all bounties on a new game
                MagicBountyLoader.loadBountiesFromJSON(false);
                //convert the HVBs if necessary
                if (!MagicVariables.getHVB()) MagicBountyHVB.convertHVBs(false);
            } else {
                if (MagicSettings.getBoolean(MagicVariables.MAGICLIB_ID, "bounty_board_reloadAll")) {
                    //force cleanup of all the bounties that have not been taken
                    MagicBountyLoader.clearBountyData();
                }
                //only add new bounties if there are any on a save load
                MagicBountyLoader.loadBountiesFromJSON(!Global.getSettings().isDevMode());
                if (!MagicVariables.getHVB()) MagicBountyHVB.convertHVBs(!Global.getSettings().isDevMode());
            }

            MagicBountyCoordinator.onGameLoad();
            MagicBountyCoordinator.getInstance().configureBountyListeners();

            Global.getSector().registerPlugin(new MagicBountyCampaignPlugin());
        }

        MagicKotlinModPlugin.INSTANCE.onGameLoad(newGame);

        if (isMagicLibTestMode()) {
            MagicAchievementManager.getInstance().addAchievementSpecs(new TestingAchievementSpec());
//            testMagicCampaignTrailPlugin();
        }

        MagicAchievementManager.getInstance().onGameLoad();


//        MagicPaintjobManager.getInstance().diable$MagicLib();

        MagicPaintjobManager.onGameLoad();
    }

    @Override
    public void beforeGameSave() {
        super.beforeGameSave();
        if (MagicVariables.getMagicBounty()) {
            MagicBountyCoordinator.beforeGameSave();
        }

        MagicAchievementManager.getInstance().beforeGameSave();
        MagicPaintjobManager.beforeGameSave();
    }

    @Override
    public void afterGameSave() {
        super.afterGameSave();
        if (MagicVariables.getMagicBounty()) {
            MagicBountyCoordinator.afterGameSave();
        }

        MagicAchievementManager.getInstance().afterGameSave();
        MagicPaintjobManager.afterGameSave();
    }

    /**
     * Define how classes are named in the save xml, allowing class renaming without
     * breaking saves.
     *
     * @param x
     */
    @Override
    public void configureXStream(XStream x) {
        super.configureXStream(x);
        data.scripts.Magic_modPlugin.configureXStream(x);

        x.alias("MagicBountyBarEvent", MagicBountyBarEvent.class);
        x.alias("MagicBountyActiveBounty", ActiveBounty.class);
        x.alias("MagicBountyBattleListener", MagicBountyBattleListener.class);
        x.alias("MagicBountyIntel", MagicBountyIntel.class);
        x.alias("MagicBountyFleetEncounterContext", MagicBountyFleetEncounterContext.class);
        x.alias("MagicBountyFleetInteractionDialogPlugin", MagicBountyFleetInteractionDialogPlugin.class);
        x.alias("MagicCampaignPlugin", MagicBountyCampaignPlugin.class);

        // Keep the Magic replacements out of the save file.
        // The game will automatically swap to the Magic replacements on load because `terrain.json` replaces the vanilla ones.
        x.alias("AsteroidBeltTerrainPlugin", MagicAsteroidBeltTerrainPlugin.class);
        x.alias("AsteroidFieldTerrainPlugin", MagicAsteroidFieldTerrainPlugin.class);
    }

    public static boolean isMagicLibTestMode() {
        return Global.getSector() != null
                && Global.getSector().getPlayerPerson().getNameString()
                .equalsIgnoreCase("ML_Test");
    }

    private static void testMagicCampaignTrailPlugin() {
        final float trailId = MagicCampaignTrailPlugin.getUniqueID();
        Global.getSector().addTransientScript(new EveryFrameScript() {
            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean runWhilePaused() {
                return false;
            }

            @Override
            public void advance(float amount) {
                MagicCampaignTrailPlugin.addTrailMemberSimple(Global.getSector().getPlayerFleet(), trailId, Global.getSettings().getSprite("graphics/portraits/godiva.jpg"),
                        Global.getSector().getPlayerFleet().getLocation(),
                        1f, 0f, 500f, 550f, Color.ORANGE,
                        1f, 6f, true, new Vector2f());
            }
        });
    }

    //    //debugging magic bounties
//    
//    private static final Logger LOG = Global.getLogger(Magic_modPlugin.class);
//    @Override
//    public void onNewGameAfterEconomyLoad() {
//        for(String b : MagicBountyData.BOUNTIES.keySet()){
//            LOG.warn(" ");
//            LOG.warn("Testing the "+b+" bounty");
//            LOG.warn(" ");
//            
//            bountyData data = MagicBountyData.getBountyData(b);
//            for(int i=0; i<10; i++){
//                SectorEntityToken location = MagicCampaign.findSuitableTarget(
//                        data.location_marketIDs,
//                        data.location_marketFactions,
//                        data.location_distance,
//                        data.location_themes,
//                        data.location_entities,
//                        data.location_defaultToAnyEntity,
//                        data.location_prioritizeUnexplored,
//                        true);
//                if(location!=null){
//                    LOG.warn(location.getName()+ " is suitable in "+ location.getStarSystem().getName() +" at a distance of "+(int)location.getStarSystem().getLocation().length());
//                } else {
//                    LOG.warn("CANNOT FIND SUITABLE LOCATION");
//                }
//            }
//        }
//        LOG.debug("end of bounty list");
//    }
}
