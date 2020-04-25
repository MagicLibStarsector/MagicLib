/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MagicInterference {    
    
    //////////////////////////////
    //                          //
    //   INTERFERENCE EFFECT    //
    //                          //
    //////////////////////////////  

    /**
     * Reduces a ship's passive dissipation if more than one weapon causing interferences is installed. The strength of the effect has a quadratic growth with the number of such weapons installed.
     * 
     * @param shipVariant
     * Variant of the ship affected by the interference debuff
     */
    
    private static final String INTERFERENCE_HULLMOD = "ML_interferenceWarning";
    
    public static void ApplyInterference (ShipVariantAPI shipVariant){
        //get interference data
        if(RATES.isEmpty() || Global.getSettings().isDevMode()){
            mergeInterference();
        }
        
        if(getTotalInterference(shipVariant)>0){
            if(!shipVariant.getHullMods().contains(INTERFERENCE_HULLMOD)){
                shipVariant.getHullMods().add(INTERFERENCE_HULLMOD);
            }
        } else if(shipVariant.getHullMods().contains(INTERFERENCE_HULLMOD)){
            shipVariant.getHullMods().remove(INTERFERENCE_HULLMOD);
        }
    }
    

    
    //////////////////////////////
    //                          //
    //    INTERFERENCE DATA     //
    //                          //
    //////////////////////////////            
        
    private static final Logger LOG=  Global.getLogger(MagicInterference.class);
    private static final String INTERFERENCE = "data/config/magicLib/magicInterference.csv";
    private static final Map<String, Float> WEAPONS =new HashMap<>();
    private static final String SETTINGS = "data/config/magicLib/magicInterference_settings.csv";
    private static final Map<String, Integer> RATES = new HashMap<>();
    private static float RFC=0;
    
    public static void mergeInterference(){
        
        WEAPONS.clear();
        RATES.clear(); 
        
        //read interference_settings.csv
        try {
            LOG.info("loading interference_settings.csv");
            JSONArray settings = Global.getSettings().getMergedSpreadsheetDataForMod("small", SETTINGS, "MagicLib");            
            JSONObject row = settings.getJSONObject(0);
            RATES.put("WEAK", row.getInt("weak"));
            LOG.info("small weapon interference rate: "+row.getInt("weak"));
            RATES.put("MILD", row.getInt("mild"));
            LOG.info("medium weapon interference rate: "+row.getInt("mild"));
            RATES.put("STRONG", row.getInt("strong"));
            LOG.info("large weapon interference rate: "+row.getInt("strong"));
            RFC= (float)row.getDouble("RFCmult");
            LOG.info("RFC hullmod effect reduction : "+RFC);
        } catch (IOException | JSONException ex) {
            LOG.info("unable to read interference_settings.csv");
        } 
        
        //read interference.csv
        try {
            LOG.info("loading interference.csv");
            JSONArray weapons = Global.getSettings().getMergedSpreadsheetDataForMod("id", INTERFERENCE, "MagicLib");
            for(int i=0; i<weapons.length();i++){
                JSONObject row = weapons.getJSONObject(i);
                if(row.getString("id")!=null && row.getString("intensity")!=null){
                    WEAPONS.put(row.getString("id"),(float)RATES.get(row.getString("intensity")));
                    LOG.info("interference source: "+row.getString("id") + " with a "+row.getString("intensity")+" intensity");
                }
            }
        } catch (IOException | JSONException ex) {
            LOG.info("unable to read interference.csv");
        } 
    }
    
    
    //////////////////////////////
    //                          //
    // INTERFERENCE COMPUTATION //
    //                          //
    //////////////////////////////  

    /**
     *
     * @param shipVariant
     * Variant of the ship affected by the interferences
     * 
     * @return
     * Uncapped reduction of the flux dissipation caused by the interfering weapons
     */
    
    
    public static Float getTotalInterference (ShipVariantAPI shipVariant){
        
        float total=0;        
        Map<String,Float> theDebuffs = getDebuffs(shipVariant);
        
        //compute total of the interferences
        if(theDebuffs.size()>1){
            for(String w : theDebuffs.keySet()){
                total+=theDebuffs.get(w);
            }
        }
        
        return total;
    }
    
    public static Map<String, Integer> getRates(){
         if(RATES.isEmpty()){
            mergeInterference();
         }        
        return RATES;
    }
    
    public static float getRFC(){
         if(RFC==0){
            mergeInterference();
         }        
        return RFC;
    }
    
    /**
     *
     * @param shipVariant
     * Variant of the ship affected by the interferences
     * 
     * @return
     * Map of the mounts fitted with a weapon with interference, with their current individual effects
     */
    public static Map<String, Float> getDebuffs (ShipVariantAPI shipVariant){
        
        //double check if all the values were loaded
        if(RATES.isEmpty()){
            mergeInterference();
        }
        
        Map<String,Float> theDebuffs = new HashMap<>();
        
        LOG.info("computing interference debuff");
        
        //scan all weapons for interference sources
        for(String w : shipVariant.getNonBuiltInWeaponSlots()){
            if(shipVariant.getFittedWeaponSlots().contains(w) && WEAPONS.containsKey(shipVariant.getWeaponId(w))){
                theDebuffs.put(w,0f);
                
                LOG.info("added interference source: "+shipVariant.getWeaponId(w));
            }
        }
        
        float hullmod=1;
        //scan for interference-reducing hullmod
        if(shipVariant.getHullMods().contains("fluxbreakers")){
            hullmod*=RFC;
            
            LOG.info("Resistant Flux Conduits installed, debuff reduced.");
        }
        
        //compute all the debuff
        LOG.info("found "+theDebuffs.size()+" interference sources");
        
        if(theDebuffs.size()>1){
            for (String w : theDebuffs.keySet()){
                
                theDebuffs.put(
                        w,
                        (float) (theDebuffs.size()-1) //interference sources
                                *WEAPONS.get(shipVariant.getWeaponId(w)) //
                                *hullmod
                );
                
                LOG.info(shipVariant.getWeaponSpec(w).getWeaponName() + " debuff: "+WEAPONS.get(shipVariant.getWeaponId(w)));
            }
        }
        
        return theDebuffs;
    }
}