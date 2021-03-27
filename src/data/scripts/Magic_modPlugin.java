package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
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

        try {
            Global.getSettings().getScriptClassLoader().loadClass("org.lazywizard.lazylib.ModUtils");
        } catch (ClassNotFoundException ex) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "LazyLib is required to run at least one of the mods you have installed."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download LazyLib at http://fractalsoftworks.com/forum/index.php?topic=5444"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }
        
        MagicSettings.loadModSettings();
        
        if(MagicSettings.modSettings==null){
            String message = System.lineSeparator()
                    + System.lineSeparator() + "Malformed modSettings.json detected"
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
    }
}
