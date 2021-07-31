package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.plugins.MagicAutoTrails;
import data.scripts.plugins.MagicBountyData;
import data.scripts.util.bounty.MagicBountyBarEventCreator;
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
        
        if(MagicSettings.modSettings==null){
            String message = System.lineSeparator()
                    + System.lineSeparator() + "Malformed modSettings.json detected"
                    + System.lineSeparator() + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }
        
        //pre-loading the bounties to throw a crash if the JSON is messed up on merge
        MagicBountyData.loadBountiesFromJSON(false);
        
        if(MagicBountyData.JSONfailed){
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
    public void onGameLoad(boolean newGame){
//        MagicAutoTrails.getTrailData();
        MagicIncompatibleHullmods.clearData();
        if(!newGame){
            //add new bounties if there are any
            MagicBountyData.loadBountiesFromJSON(true);
        }

        if (!BarEventManager.getInstance().hasEventCreator(MagicBountyBarEventCreator.class)) {
            BarEventManager.getInstance().addEventCreator(new MagicBountyBarEventCreator());
        }
    }
    
    @Override
    public void onNewGame(){
        //setup the bounties
        MagicBountyData.loadBountiesFromJSON(false);
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
