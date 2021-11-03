package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.bounty.*;
import data.scripts.plugins.MagicAutoTrails;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicInterference;
import data.scripts.util.MagicSettings;

public class Magic_modPlugin extends BaseModPlugin {

    ////////////////////////////////////////
    //                                    //
    //       ON APPLICATION LOAD          //
    //                                    //
    ////////////////////////////////////////

//    public static List<String> TRAIL_DATA = new ArrayList<>();

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
    }

    @Override
    public void onDevModeF8Reload() {
        MagicSettings.loadModSettings();
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

        if (MagicSettings.getBoolean("MagicLib", "bounty_board_enabled")) {
            if (newGame) {  
                //add all bounties on a new game
                MagicBountyData.loadBountiesFromJSON(false);              
            } else {
                //only add new bounties if there are any on a save load
                MagicBountyData.loadBountiesFromJSON(!Global.getSettings().isDevMode());  
            }

            //check for IBBs presence
            if (Global.getSettings().getModManager().isModEnabled("swp") && SWPModPlugin.Module_FamousBounties == true) {
                Global.getSector().getMemoryWithoutUpdate().set("$IBB_ACTIVE", true);
            } else {
                Global.getSector().getMemoryWithoutUpdate().set("$IBB_ACTIVE", false);
            }
            //check for HVBs presence
            if (Global.getSettings().getModManager().isModEnabled("vayrasector") && VayraModPlugin.UNIQUE_BOUNTIES == true) {
                Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", true);
            } else {
                Global.getSector().getMemoryWithoutUpdate().set("$HVB_ACTIVE", false);
                MagicBountyHVB.convertHVBs(!Global.getSettings().isDevMode());
            }
            
            MagicBountyCoordinator.onGameLoad();
            MagicBountyCoordinator.getInstance().configureBountyListeners();

            Global.getSector().registerPlugin(new MagicBountyCampaignPlugin());
        }
    }

    /*
    @Override
    public void onNewGame() {
        if (MagicSettings.getBoolean("MagicLib", "bounty_board_enabled")) {
            //setup the bounties
            MagicBountyData.loadBountiesFromJSON(false);
        }
    }
    */

    /**
     * Define how classes are named in the save xml, allowing class renaming without
     * breaking saves.
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
