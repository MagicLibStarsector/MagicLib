/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MagicTxt {    
    
    private static final String FILE = "data/strings/magicStrings.csv";
    private static final Logger LOG = Global.getLogger(MagicTxt.class); 
    private static final Map<String, String> MAGIC_STRINGS=new HashMap<>();
    
    public static void readStringsFile(){
        MAGIC_STRINGS.clear();
        try {
            JSONArray StringData = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE, "MagicLib");
            for(int i = 0; i < StringData.length(); i++) {
                JSONObject row = StringData.getJSONObject(i);
                if(row.getString("id")!=null){
                    MAGIC_STRINGS.put(row.getString("id"), row.getString("text"));
                }
            }
        } catch (IOException | JSONException ex) {
            LOG.error("unable to read "+FILE);
        }
    }
    
    public static String getString(String id){
        
        if(MAGIC_STRINGS.isEmpty()){
            readStringsFile();
        } 
        
        if(MAGIC_STRINGS.get(id)!=null){
            return MAGIC_STRINGS.get(id);
        } else {
            LOG.error("WARNING, no text entry found for "+id);
            return "#####";
        }
    }    
}