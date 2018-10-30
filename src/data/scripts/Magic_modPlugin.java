package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class Magic_modPlugin extends BaseModPlugin {
    
    ////////////////////////////////////////
    //                                    //
    //       ON APPLICATION LOAD          //
    //                                    //
    ////////////////////////////////////////
    
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
    }    
    
//    ////////////////////////////////////////
//    //                                    //
//    //        ON NEW GAME CREATION        //
//    //                                    //
//    ////////////////////////////////////////
//    
//    @Override
//    public void onNewGame() {
//    }
//    
//    ////////////////////////////////////////
//    //                                    //
//    //            ON GAME LOAD            //
//    //                                    //
//    ////////////////////////////////////////
//    
//    @Override
//    public void onGameLoad(boolean newGame){
//    }    
}
