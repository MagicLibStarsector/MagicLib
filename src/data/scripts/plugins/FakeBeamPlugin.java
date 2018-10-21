//By Tartiflette with DeathFly's help
//draw arbitrary beam sprites wherever you need them and fade them out
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.FastTrig;

public class FakeBeamPlugin extends BaseEveryFrameCombatPlugin {
  
    //FAKEBEAMS is the core of the script, storing both the weapons that have a flash rendered and the index of the current sprite used    
    public static List<Map<String,Float>> FAKEBEAMS = new ArrayList();
    
    //set the function to access FAKEBEAMS from the weapons scripts    
    public static void addMember(Map<String,Float> data) {
        FAKEBEAMS.add(data);
    }
            
    private List<Map<String,Float>> toRemove = new ArrayList<>();
    
    private SpriteAPI beam = Global.getSettings().getSprite("beams", "fakeBeamFX");
    
    @Override
    public void init(CombatEngineAPI engine) { 
        //reinitialize the map 
        FAKEBEAMS.clear();
    }
    
    @Override
    public void renderInWorldCoords(ViewportAPI view)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){return;}
        
        if (!FAKEBEAMS.isEmpty()){
            float amount = (engine.isPaused() ? 0f : engine.getElapsedInLastFrame());
            
            //dig through the FAKEBEAMS
            for (Map< String,Float > entry : FAKEBEAMS) {
                  
                //Time calculation
                float time = entry.get("t");
                time -= amount;
                
                if (time <= 0){         
                    //faded out, remove the beam and skip                    
                    toRemove.add(entry);
                } else {
                    //draw the beam otherwise                                        
                    float opacity = Math.max(0,(Math.min(1,(float) FastTrig.sin(time * Math.PI))));
                    
                    render(
                            beam, //Sprite to draw
                            entry.get("w") * opacity, //Width entry srinking with the opacity
                            2*entry.get("h"), //Height entry, multiplied by two because centered
                            entry.get("a"), //Angle entry
                            opacity, //opacity duh!
                            entry.get("x"), //X position entry
                            entry.get("y") //Y position entry
                    );
                    
                    //and store the new time value
                    entry.put("t", time);
                }
            }
            //remove the beams that faded out
            //can't be done from within the iterator or it will fail when members will be missing
            if (!toRemove.isEmpty()){
                for(Map< String,Float > w : toRemove ){
                    FAKEBEAMS.remove(w);
                }
                toRemove.clear();
            }            
        }
    }
    
    private void render ( SpriteAPI sprite, float width, float height, float angle, float opacity, float posX, float posY){
        //where the magic happen
        sprite.setAlphaMult(opacity); 
        sprite.setSize(width, height);
        sprite.setAdditiveBlend();
        sprite.setAngle(angle-90);
        sprite.renderAtCenter(posX, posY);     
    }
}
