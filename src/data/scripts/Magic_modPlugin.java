package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.plugins.MagicAutoTrails;
import data.scripts.plugins.MagicCampaignPlugin;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicInterference;
import data.scripts.util.MagicSettings;
import data.scripts.bounty.*;

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

        //pre-loading the bounties to throw a crash if the JSON is messed up on merge
        MagicBountyData.loadBountiesFromJSON(false);

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
        if (!newGame) {
            //add new bounties if there are any
            MagicBountyData.loadBountiesFromJSON(true);
        }

        MagicBountyCoordinator.onGameLoad();
        MagicBountyCoordinator.getInstance().configureBountyScript();
        MagicBountyCoordinator.getInstance().configureBountyListeners();

        Global.getSector().registerPlugin(new MagicCampaignPlugin());
    }

    @Override
    public void onNewGame() {
        //setup the bounties
        MagicBountyData.loadBountiesFromJSON(false);
    }

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
        x.alias("MagicBountyScript", MagicBountyScript.class);
        x.alias("MagicBountyFleetEncounterContext", MagicBountyFleetEncounterContext.class);
        x.alias("MagicBountyFleetInteractionDialogPlugin", MagicBountyFleetInteractionDialogPlugin.class);
        x.alias("MagicCampaignPlugin", MagicCampaignPlugin.class);
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
