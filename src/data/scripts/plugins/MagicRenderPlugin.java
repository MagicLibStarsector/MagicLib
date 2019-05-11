/*
 * By Tartiflette
 */
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.util.MagicRender;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagicRenderPlugin extends BaseEveryFrameCombatPlugin {
    
    private static List<renderData> SINGLEFRAME = new ArrayList<>();
    private static List<battlespaceData> BATTLESPACE = new ArrayList<>();
    private static List<objectspaceData> OBJECTSPACE = new ArrayList<>();
    private static List<screenspaceData> SCREENSPACE = new ArrayList<>();
    
    @Override
    public void init(CombatEngineAPI engine) { 
        //reinitialize the lists
        SINGLEFRAME.clear();
        BATTLESPACE.clear();
        OBJECTSPACE.clear();
        SCREENSPACE.clear();
    }
    
    public static void addSingleframe(SpriteAPI sprite, Vector2f loc, CombatEngineLayers layer){
        SINGLEFRAME.add(new renderData(sprite, loc, layer));
    }
    
    public static void addBattlespace(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f growth, float spin, float fadein, float full, float fadeout, CombatEngineLayers layer) {    
        BATTLESPACE.add(new battlespaceData(sprite, loc, vel, growth, spin, fadein, full, fadeout, 0, layer));
    }
    
    public static void addObjectspace(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f loc, Vector2f offset, Vector2f vel, Vector2f growth, float angle, float spin, boolean parent, float fadein, float full, float fadeout, boolean fadeOnDeath, CombatEngineLayers layer) {
        OBJECTSPACE.add(new objectspaceData(sprite, anchor, loc, offset, vel, growth, angle, spin, parent, fadein, full, fadeout, fadeOnDeath, 0, layer));
    }
    
    public static void addScreenspace(SpriteAPI sprite, MagicRender.positioning pos, Vector2f loc, Vector2f vel, Vector2f ratio, Vector2f growth, float spin, float fadein, float full, float fadeout, CombatEngineLayers layer) { 
        SCREENSPACE.add(new screenspaceData(sprite, pos, loc, vel, ratio, growth, spin, fadein, full, fadeout, 0, layer));        
    }  
    
    //////////////////////////////
    //                          //
    //         MAIN LOOP        //
    //                          //
    //////////////////////////////    
    
    
    @Override
    public void renderInWorldCoords(ViewportAPI view){        
        CombatEngineAPI engine = Global.getCombatEngine();        
        if (engine == null){return;}        
        
        float amount=0;
        if(!engine.isPaused()){
            amount=engine.getElapsedInLastFrame();
        }
        
        if(!BATTLESPACE.isEmpty()){
            //iterate throught the BATTLESPACE data first:
            for(Iterator<battlespaceData> iter=BATTLESPACE.iterator(); iter.hasNext(); ){
                battlespaceData entry = iter.next();
                
                //add the time spent, that means sprites will never start at 0 exactly, but it simplifies a lot the logic
                entry.TIME+=amount;
                if(entry.TIME>entry.FADEOUT){
                    //remove expended ones
                    iter.remove();
                    continue;
                }
                
                //grow/shrink the sprite to a new size if needed
                if(entry.GROWTH!= null && entry.GROWTH!=new Vector2f()){
                    entry.SPRITE.setSize(entry.SPRITE.getWidth()+(entry.GROWTH.x*amount), entry.SPRITE.getHeight()+(entry.GROWTH.y*amount));
                    //check if the growth made the sprite too small
                    if(entry.SPRITE.getHeight()<=0 || entry.SPRITE.getWidth()<=0){                        
                        //remove sprites that completely shrunk
                        iter.remove();
                        continue;
                    }
                }
                
                //move the sprite to a new center if needed
                if(entry.VEL!= null && entry.VEL!=new Vector2f()){
                    Vector2f move = new Vector2f(entry.VEL);
                    move.scale(amount);
                    Vector2f.add(entry.LOC, move, entry.LOC);
                }

                //spin the sprite if needed
                if(entry.SPIN!=0){
                    entry.SPRITE.setAngle(entry.SPRITE.getAngle()+entry.SPIN*amount);
                }
                
                //fading stuff
                if(entry.TIME<entry.FADEIN){
                    entry.SPRITE.setAlphaMult(entry.TIME/entry.FADEIN);
                } else if(entry.TIME>entry.FULL){                    
                    entry.SPRITE.setAlphaMult(1-((entry.TIME-entry.FULL)/(entry.FADEOUT-entry.FULL)));
                } else {
                    entry.SPRITE.setAlphaMult(1);
                }
                
                //finally render that stuff
//                render(new renderData(entry.SPRITE, entry.LOC, entry.LAYER));
                MagicRenderer.spriteRender(entry.LAYER, engine.getViewport(), new renderData(entry.SPRITE, entry.LOC, entry.LAYER));
            }
        }
        
        if(!OBJECTSPACE.isEmpty()){
            //then iterate throught the OBJECTSPACE data:
            for(Iterator<objectspaceData> iter=OBJECTSPACE.iterator(); iter.hasNext(); ){
                objectspaceData entry = iter.next();
                
                //check for possible removal when the anchor isn't in game
                if(!entry.DEATHFADE && !engine.isEntityInPlay(entry.ANCHOR)){
                    iter.remove();
                    continue;
                }
                
                //check for projectile attachement fadeout
                if(entry.ANCHOR instanceof DamagingProjectileAPI){
                    //if the proj is fading or removed, offset the fadeout time to the current time
                    if (entry.TIME<entry.FULL && (
                            ((DamagingProjectileAPI)entry.ANCHOR).isFading() 
                            || !engine.isEntityInPlay(entry.ANCHOR))
                            ){
                        entry.FADEOUT=(entry.FADEOUT-entry.FULL)+entry.TIME;
                        entry.FULL=entry.TIME;
                    }
                }
                
                //add the time spent, that means sprites will never start at 0 exactly, but it simplifies a lot the logic
                entry.TIME+=amount;
                if(entry.TIME>entry.FADEOUT){
                    //remove expended ones
                    iter.remove();
                    continue;
                }                
                
                //grow/shrink the sprite to a new size if needed
                if(entry.GROWTH!= null && entry.GROWTH!=new Vector2f()){
                    entry.SPRITE.setSize(entry.SPRITE.getWidth()+(entry.GROWTH.x*amount), entry.SPRITE.getHeight()+(entry.GROWTH.y*amount));
                    //check if the growth made the sprite too small
                    if(entry.SPRITE.getHeight()<=0 || entry.SPRITE.getWidth()<=0){                        
                        //remove sprites that completely shrunk
                        iter.remove();
                        continue;
                    }
                }
                
                //adjust the offset if needed
                if(entry.VEL!= null && entry.VEL!=new Vector2f()){
                    Vector2f move = new Vector2f(entry.VEL);
                    move.scale(amount);
                    Vector2f.add(entry.OFFSET, move, move);
                    entry.OFFSET = move;
                }
                
                //addjust the position and orientation
                Vector2f location = new Vector2f(entry.OFFSET); //base offset
                
                //for parenting, check if the anchor is present
                if(entry.PARENT && engine.isEntityInPlay(entry.ANCHOR)){  
                    //if the sprite is parented, use the ANGLE to store the offset
                    if(entry.SPIN!=0){
                        entry.ANGLE+=entry.SPIN*amount;
                    }
                    entry.SPRITE.setAngle(entry.ANCHOR.getFacing()+90+entry.ANGLE);
                    //orient the offset with the facing
                    VectorUtils.rotate(location, entry.ANCHOR.getFacing(), location);
                } else {
                    //otherwise just orient the sprite
                    if(entry.SPIN!=0){
                        entry.SPRITE.setAngle(entry.SPRITE.getAngle()+entry.SPIN*amount);
                    }
                }
                
                //move the offset on the anchor
                if(engine.isEntityInPlay(entry.ANCHOR)){
                    Vector2f loc = new Vector2f(entry.ANCHOR.getLocation());
                    Vector2f.add(location, loc, location);
                    entry.LOCATION=loc;
                } else {
                    Vector2f.add(location, entry.LOCATION, location);
                }
                
                //fading stuff
                if(entry.TIME<entry.FADEIN){
                    entry.SPRITE.setAlphaMult(entry.TIME/entry.FADEIN);
                } else if(entry.TIME>entry.FULL){                    
                    entry.SPRITE.setAlphaMult(1-((entry.TIME-entry.FULL)/(entry.FADEOUT-entry.FULL)));
                } else {
                    entry.SPRITE.setAlphaMult(1);
                }
                
                //finally render that stuff
                
//                 render(new renderData(entry.SPRITE, location, entry.LAYER));

                MagicRenderer.spriteRender(entry.LAYER, engine.getViewport(), new renderData(entry.SPRITE, location, entry.LAYER));
            }
        }
        
        if(!SCREENSPACE.isEmpty()){
            //iterate throught the BATTLESPACE data first:
            
            Vector2f center;
            ViewportAPI screen = Global.getCombatEngine().getViewport();
            
            for(Iterator<screenspaceData> iter=SCREENSPACE.iterator(); iter.hasNext(); ){
                screenspaceData entry = iter.next();
                
                
                if(entry.FADEOUT<0){
                    // SINGLE FRAME RENDERING
                    if(entry.POS == MagicRender.positioning.FULLSCREEN_MAINTAIN_RATIO){                    
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(entry.SIZE.x*screen.getVisibleWidth(), entry.SIZE.y*screen.getVisibleHeight());
                    } else if(entry.POS == MagicRender.positioning.STRETCH_TO_FULLSCREEN){
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(screen.getVisibleWidth(), screen.getVisibleHeight());
                    } else {
                        Vector2f refPoint=screen.getCenter();
                        switch (entry.POS){

                            case LOW_LEFT:
                                refPoint = new Vector2f(refPoint.x-(screen.getVisibleWidth()/2), refPoint.y-(screen.getVisibleHeight()/2));
                                break;

                            case LOW_RIGHT:
                                refPoint = new Vector2f(refPoint.x-(screen.getVisibleWidth()/2), refPoint.y+(screen.getVisibleHeight()/2));
                                break;

                            case UP_LEFT:
                                refPoint = new Vector2f(refPoint.x+(screen.getVisibleWidth()/2), refPoint.y-(screen.getVisibleHeight()/2));
                                break;

                            case UP_RIGHT:
                                refPoint = new Vector2f(refPoint.x+(screen.getVisibleWidth()/2), refPoint.y+(screen.getVisibleHeight()/2));
                                break;

                            default:
                        }                
                        center = new Vector2f(entry.LOC);
                        center.scale(screen.getViewMult());
                        Vector2f.add(center, refPoint, center);                
                    }

                    //finally render that stuff
//                    render(new renderData(entry.SPRITE, center, entry.LAYER));

                    MagicRenderer.spriteRender(entry.LAYER, engine.getViewport(), new renderData(entry.SPRITE, center, entry.LAYER));
                    //and immediatelly remove
                    iter.remove();
                } else {
                    // TIMED RENDERING                    
                    //add the time spent, that means sprites will never start at 0 exactly, but it simplifies a lot the logic
                    entry.TIME+=amount;
                    if(entry.FADEOUT>0 && entry.TIME>entry.FADEOUT){
                        //remove expended ones
                        iter.remove();
                        continue;
                    }                

                    if(entry.POS == MagicRender.positioning.FULLSCREEN_MAINTAIN_RATIO){                    
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(entry.SIZE.x*screen.getVisibleWidth(), entry.SIZE.y*screen.getVisibleHeight());
                    } else if(entry.POS == MagicRender.positioning.STRETCH_TO_FULLSCREEN){
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(screen.getVisibleWidth(), screen.getVisibleHeight());
                    } else {
                        Vector2f refPoint=screen.getCenter();
                        switch (entry.POS){

                            case LOW_LEFT:
                                refPoint = new Vector2f(refPoint.x-(screen.getVisibleWidth()/2), refPoint.y-(screen.getVisibleHeight()/2));
                                break;

                            case LOW_RIGHT:
                                refPoint = new Vector2f(refPoint.x-(screen.getVisibleWidth()/2), refPoint.y+(screen.getVisibleHeight()/2));
                                break;

                            case UP_LEFT:
                                refPoint = new Vector2f(refPoint.x+(screen.getVisibleWidth()/2), refPoint.y-(screen.getVisibleHeight()/2));
                                break;

                            case UP_RIGHT:
                                refPoint = new Vector2f(refPoint.x+(screen.getVisibleWidth()/2), refPoint.y+(screen.getVisibleHeight()/2));
                                break;

                            default:
                        }                    

                        //move the sprite to a new center if needed
                        if(entry.VEL!= null && entry.VEL!=new Vector2f()){
                            Vector2f move = new Vector2f(entry.VEL);
                            move.scale(amount);
                            Vector2f.add(entry.LOC, move, entry.LOC);
                        }
                        center = new Vector2f(entry.LOC);
                        center.scale(screen.getViewMult());
                        Vector2f.add(center, refPoint, center);

                        //grow/shrink the sprite to a new size if needed
                        if(entry.GROWTH!= null && entry.GROWTH!=new Vector2f()){
                            entry.SIZE = new Vector2f(entry.SIZE.x+(entry.GROWTH.x*amount), entry.SIZE.y+(entry.GROWTH.y*amount));
                            //check if the growth made the sprite too small
                            if(entry.SIZE.x<=0 || entry.SIZE.y<=0){                        
                                //remove sprites that completely shrunk
                                iter.remove();
                                continue;
                            }
                        }
                        entry.SPRITE.setSize(entry.SIZE.x*screen.getViewMult(), entry.SIZE.y*screen.getViewMult());

                        //spin the sprite if needed
                        if(entry.SPIN!=0){
                            entry.SPRITE.setAngle(entry.SPRITE.getAngle()+entry.SPIN*amount);
                        }
                    }

                    //fading stuff
                    if(entry.TIME<entry.FADEIN){
                        entry.SPRITE.setAlphaMult(entry.TIME/entry.FADEIN);
                    } else if(entry.TIME>entry.FULL){                    
                        entry.SPRITE.setAlphaMult(1-((entry.TIME-entry.FULL)/(entry.FADEOUT-entry.FULL)));
                    } else {
                        entry.SPRITE.setAlphaMult(1);
                    }
                    
                    //finally render that stuff
                    
//                    render(new renderData(entry.SPRITE, center, entry.LAYER));

                    MagicRenderer.spriteRender(entry.LAYER, engine.getViewport(), new renderData(entry.SPRITE, center, entry.LAYER));

                    if(entry.FADEOUT<0){
                        iter.remove();
                    }
                }                
            }
        }
        
        //Single frame sprite rendering
        if(!SINGLEFRAME.isEmpty()){
            for(renderData d : SINGLEFRAME){
//                render(d);
                
                MagicRenderer.spriteRender(d.LAYER, engine.getViewport(), d);
            }
            SINGLEFRAME.clear();
        }
    }
    
    //////////////////////////////
    //                          //
    //          RENDER          //
    //                          //
    //////////////////////////////
    
//    private void render (renderData data){
//        //where the magic happen
//        SpriteAPI sprite = data.SPRITE;  
//        sprite.renderAtCenter(data.LOC.x, data.LOC.y);
//    }
    
    
    //////////////////////////////
    //                          //
    //      RENDER CLASSES      //
    //                          //
    //////////////////////////////    
    
    public static class renderData {   
        public final SpriteAPI SPRITE; 
        public final Vector2f LOC; 
        public final CombatEngineLayers LAYER;
        
        public renderData(SpriteAPI sprite, Vector2f loc, CombatEngineLayers layer) {
            this.SPRITE = sprite;
            this.LOC = loc;
            this.LAYER = layer;
        }
    }
    
    private static class battlespaceData {   
        private final SpriteAPI SPRITE; 
        private Vector2f LOC; 
        private final Vector2f VEL;
        private final Vector2f GROWTH;
        private final float SPIN;
        private final float FADEIN;
        private final float FULL; //fade in + full
        private final float FADEOUT; //full duration
        private float TIME;
        private final CombatEngineLayers LAYER;
        
        public battlespaceData(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f growth, float spin, float fadein, float full, float fadeout, float time, CombatEngineLayers layer) {
            this.SPRITE = sprite;
            this.LOC = loc;
            this.VEL = vel;
            this.GROWTH = growth;
            this.SPIN = spin;
            this.FADEIN = fadein;
            this.FULL = full;
            this.FADEOUT = fadeout;
            this.TIME = time;
            this.LAYER = layer;
        }
    }
    
    private static class objectspaceData {   
        private final SpriteAPI SPRITE; 
        private final CombatEntityAPI ANCHOR;
        private Vector2f LOCATION;
        private Vector2f OFFSET; 
        private final Vector2f VEL;
        private final Vector2f GROWTH;
        private float ANGLE;
        private final float SPIN;
        private final boolean PARENT;
        private final float FADEIN;
        private float FULL; //fade in + full
        private float FADEOUT; //full duration
        private final boolean DEATHFADE;
        private float TIME;
        private final CombatEngineLayers LAYER;
        
        public objectspaceData(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f loc, Vector2f offset, Vector2f vel, Vector2f growth, float angle, float spin, boolean parent, float fadein, float full, float fadeout, boolean fade, float time, CombatEngineLayers layer) {
            this.SPRITE = sprite;
            this.ANCHOR = anchor;
            this.LOCATION = loc;
            this.OFFSET = offset;
            this.VEL = vel;
            this.GROWTH = growth;
            this.ANGLE = angle;
            this.SPIN = spin;
            this.PARENT = parent;
            this.FADEIN = fadein;
            this.FULL = full;
            this.FADEOUT = fadeout;
            this.DEATHFADE = fade;
            this.TIME = time;
            this.LAYER = layer;
        }
    }
    
    private static class screenspaceData {   
        private final SpriteAPI SPRITE;
        private final MagicRender.positioning POS;
        private Vector2f LOC; 
        private final Vector2f VEL;
        private Vector2f SIZE;
        private final Vector2f GROWTH;
        private final float SPIN;
        private final float FADEIN;
        private final float FULL; //fade in + full
        private final float FADEOUT; //full duration
        private float TIME;
        private final CombatEngineLayers LAYER;
        
        public screenspaceData(SpriteAPI sprite, MagicRender.positioning position, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float spin, float fadein, float full, float fadeout, float time, CombatEngineLayers layer) {
            this.SPRITE = sprite;
            this.POS = position;
            this.LOC = loc;
            this.VEL = vel;
            this.SIZE = size;
            this.GROWTH = growth;
            this.SPIN = spin;
            this.FADEIN = fadein;
            this.FULL = full;
            this.FADEOUT = fadeout;
            this.TIME = time;
            this.LAYER = layer;
        }
    }    
}

class MagicRenderer extends BaseCombatLayeredRenderingPlugin {
    //@Override
    public static void spriteRender (CombatEngineLayers layer, ViewportAPI view, MagicRenderPlugin.renderData data) {
        //Initial checks to see if required components exist
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null){
            return;
        }
        
        SpriteAPI sprite = data.SPRITE;  
        sprite.renderAtCenter(data.LOC.x, data.LOC.y);
    }
}