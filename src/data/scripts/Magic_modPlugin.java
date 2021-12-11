package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.bounty.*;
import data.scripts.plugins.MagicAutoTrails;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicInterference;
import data.scripts.util.MagicSettings;
import data.scripts.util.MagicVariables;
import static data.scripts.util.MagicVariables.MAGICLIB_ID;

public class Magic_modPlugin extends BaseModPlugin {

    ////////////////////////////////////////
    //                                    //
    //       ON APPLICATION LOAD          //
    //                                    //
    ////////////////////////////////////////


    @Override
    public void onApplicationLoad() throws ClassNotFoundException {

        MagicSettings.loadModSettings();

        if (MagicSettings.modSettings == null) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "Malformed modSettings.json detected"
                    + System.lineSeparator() + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        //dev-mode pre-loading the bounties to throw a crash if the JSON is messed up on merge
        if(Global.getSettings().isDevMode()){
            MagicBountyData.loadBountiesFromJSON(false);
            if (!Global.getSettings().getModManager().isModEnabled("vayrasector") || VayraModPlugin.UNIQUE_BOUNTIES == false) {
                MagicBountyHVB.convertHVBs(false);
            }
        }

        if (MagicBountyData.JSONfailed) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "Malformed MagicBounty_data.json detected"
                    + System.lineSeparator() + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();
        
//        MagicVariables.checkBountySystems();
        
        //gather mod's system themes
        MagicVariables.loadThemesBlacklist();
        MagicVariables.verbose=Global.getSettings().isDevMode();
        MagicVariables.bounty_test_mode = MagicSettings.getBoolean(MAGICLIB_ID, "bounty_board_test_mode");
        
    }

    @Override
    public void onDevModeF8Reload() {
        MagicSettings.loadModSettings();
        //gather interference data
        MagicInterference.loadInterference();

        //gather trail data
        MagicAutoTrails.getTrailData();
        
        //Check for other bounty systems
        MagicVariables.checkBountySystems();
        
        //gather mod's system themes
        MagicVariables.loadThemesBlacklist();
        MagicVariables.verbose=Global.getSettings().isDevMode();
        MagicVariables.bounty_test_mode = MagicSettings.getBoolean(MAGICLIB_ID, "bounty_board_test_mode");
    }

    ////////////////////////////////////////
    //                                    //
    //            ON GAME LOAD            //
    //                                    //
    ////////////////////////////////////////

    @Override
    public void onGameLoad(boolean newGame) {
//        MagicAutoTrails.getTrailData();
        MagicIncompatibleHullmods.clearData();
        
        MagicVariables.checkBountySystems();
        
        if (MagicVariables.MagicBountiesEnabled) {
            if (newGame) {  
                //add all bounties on a new game
                MagicBountyData.loadBountiesFromJSON(false);
                //convert the HVBs if necessary
                if(!MagicVariables.hvb)MagicBountyHVB.convertHVBs(false);
            } else {
                //only add new bounties if there are any on a save load
                MagicBountyData.loadBountiesFromJSON(!Global.getSettings().isDevMode()); 
                if(!MagicVariables.hvb)MagicBountyHVB.convertHVBs(!Global.getSettings().isDevMode()); 
            }

            MagicBountyCoordinator.onGameLoad();
            MagicBountyCoordinator.getInstance().configureBountyListeners();

            Global.getSector().registerPlugin(new MagicBountyCampaignPlugin());
        }
    }

    /**
     * Define how classes are named in the save xml, allowing class renaming without
     * breaking saves.
     * @param x
     */
    @Override
    public void configureXStream(XStream x) {
        super.configureXStream(x);
        x.alias("MagicBountyBarEvent", MagicBountyBarEvent.class);
        x.alias("MagicBountyActiveBounty", ActiveBounty.class);
        x.alias("MagicBountyBattleListener", MagicBountyBattleListener.class);
        x.alias("MagicBountyIntel", MagicBountyIntel.class);
        x.alias("MagicBountyFleetEncounterContext", MagicBountyFleetEncounterContext.class);
        x.alias("MagicBountyFleetInteractionDialogPlugin", MagicBountyFleetInteractionDialogPlugin.class);
        x.alias("MagicCampaignPlugin", MagicBountyCampaignPlugin.class);
    }

    //    //debugging magic bounties
//    
//    private static final Logger LOG = Global.getLogger(Magic_modPlugin.class);
//    @Override
//    public void onNewGameAfterEconomyLoad() {
//        for(String b : MagicBountyData.BOUNTIES.keySet()){
//            LOG.error(" ");
//            LOG.error("Testing the "+b+" bounty");
//            LOG.error(" ");
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
//                    LOG.error(location.getName()+ " is suitable in "+ location.getStarSystem().getName() +" at a distance of "+(int)location.getStarSystem().getLocation().length());
//                } else {
//                    LOG.error("CANNOT FIND SUITABLE LOCATION");
//                }
//            }
//        }
//        LOG.debug("end of bounty list");
//    }
}
