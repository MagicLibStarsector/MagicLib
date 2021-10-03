package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;

/**
 *
 * @author Tartiflette
 */
public class MagicVariables {
    
    static float SECTOR_SIZE=0;
    static float SECTOR_HEIGT=0;
    static float SECTOR_WIDTH=0;
    
    private static void calculateSectorDimentions(){        
        float dist=0;        
        for ( StarSystemAPI s : Global.getSector().getStarSystems()){
            SECTOR_WIDTH=Math.max(SECTOR_WIDTH,Math.abs(s.getLocation().x));
            SECTOR_HEIGT=Math.max(SECTOR_HEIGT,Math.abs(s.getLocation().y));
            dist=Math.max(dist,s.getLocation().lengthSquared());
        }
        SECTOR_SIZE=(float)Math.sqrt(dist);        
    }
    
    public static float getSectorSize(){
        if(SECTOR_SIZE==0){
            calculateSectorDimentions();
        }
        return SECTOR_SIZE;
    }
    public static float getSectorSizeLY(){
        if(SECTOR_SIZE==0){
            calculateSectorDimentions();
        }
        return SECTOR_SIZE/Misc.getUnitsPerLightYear();
    }
    
    public static float getSectorHeight(){
        if(SECTOR_HEIGT==0){
            calculateSectorDimentions();
        }
        return SECTOR_HEIGT;
    }    
    public static float getSectorHeightLY(){
        if(SECTOR_HEIGT==0){
            calculateSectorDimentions();
        }
        return SECTOR_HEIGT/Misc.getUnitsPerLightYear();
    }
    
    public static float getSectorWidth(){
        if(SECTOR_WIDTH==0){
            calculateSectorDimentions();
        }
        return SECTOR_WIDTH;
    }
    
    public static float getSectorWidthLY(){
        if(SECTOR_WIDTH==0){
            calculateSectorDimentions();
        }
        return SECTOR_WIDTH/Misc.getUnitsPerLightYear();
    }
}
