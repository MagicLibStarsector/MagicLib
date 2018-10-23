/*
 * By Tartiflette
 * Plugin managing direct sprite rendering to create new visual effect or add new UI elements with only one line of code. 
 * Note that every sprite will be drawn one frame late.

sample use:

TRT_spriteRenderManager.screenspaceRender(
        Global.getSettings().getSprite("misc", "graphics/fx/wormhole_ring_bright3.png"),
        SpriteRenderManager.positioning.FULLSCREEN_MAINTAIN_RATIO,
        new Vector2f(),
        null,
        new Vector2f(50,50),
        null,
        0,
        360,
        Color.blue,
        false,
        1,
        3,
        1
);
 */
package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
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
    
    //////////////////////////////
    //                          //
    //     PUBLIC METHODS       //
    //                          //
    //////////////////////////////
    
    /**
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * 
     * @param loc
     * Vector2f, center in world coordinates.
     * 
     * @param size
     * Vector2f(width, height) in pixels.
     * 
     * @param angle
     * float of the sprite's azimuth. 0 is pointing top.
     * 
     * @param color
     * Color() override, also used for fading.
     * 
     * @param additive
     * boolean for additive blending.
     */
    
    public static void singleFrameRender(SpriteAPI sprite, Vector2f loc, Vector2f size, float angle, Color color, boolean additive) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }
        SINGLEFRAME.add(new renderData(sprite, loc));
    }     
    
    /**
     *
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * 
     * @param loc
     * Vector2f, center in world coordinates.
     * 
     * @param vel
     * Vector2f() velocity of the sprite.
     * 
     * @param size
     * Vector2f(width, height) in pixels.
     * 
     * @param growth
     * Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * 
     * @param angle
     * float of the sprite's azimuth. 0 is pointing top.
     * 
     * @param spin
     * float of the sprite's rotation, in degree/sec.
     * 
     * @param color
     * Color() override, also used for fading.
     * 
     * @param additive
     * boolean for additive blending.
     * 
     * @param fadein
     * time in sec for fading in.
     * 
     * @param full
     * time in sec at maximum opacity (clamped by color)
     * 
     * @param fadeout
     * time in sec for fading out
     */
    
    public static void battlespaceRender(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }        
        Vector2f velocity=new Vector2f(vel);        
        BATTLESPACE.add(new battlespaceData(sprite, loc, velocity, growth, spin, fadein, fadein+full, fadein+full+fadeout, 0));
    }       
    
    /**
     *
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     *
     * @param anchor
     * CombatEntityAPI the sprite will follow.
     *
     * @param offset
     * Vector2f, offset from the anchor's center in world coordinates. If parent is true, it will be relative to the anchor's orientation.
     * 
     * @param vel
     * Vector2f() velocity of the sprite relative to the anchor. If parent is true, it will be relative to the anchor's orientation.
     * 
     * @param size
     * Vector2f(width, height) in pixels.
     * 
     * @param growth
     * Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * 
     * @param angle
     * float of the sprite's azimuth. 0 is pointing front. If parent is true, 0 will match the anchor's orientation.
     * 
     * @param spin
     * float of the sprite's rotation, in degree/sec. If parent is true, it will be relative to the anchor's orientation.
     * 
     * @param parent
     * boolean, if true the sprite will also follow the anchor's orientation in addition to the position.
     * 
     * @param color
     * Color() override, also used for fading.
     * 
     * @param additive
     * boolean for additive blending.
     * 
     * @param fadein
     * time in sec for fading in.
     * 
     * @param full
     * time in sec at maximum opacity (clamped by color). If attached to a projectile that value can be longer than the maximum flight time, for example 99s.
     * 
     * @param fadeout
     * time in sec for fading out. If attached to a projectile, the sprite will immediately start to fade if the anchor hit or fade. 
     * 
     * @param fadeOnDeath 
     * if true the sprite will fadeout in case the anchor is removed, if false it will be instantly removed. Mostly useful if you want to put effects on missiles or projectiles.
     */
    
    public static void objectspaceRender(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f offset, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, boolean parent, Color color, boolean additive, float fadein, float full, float fadeout, boolean fadeOnDeath) {
        sprite.setSize(size.x, size.y);
        if(parent){            
            sprite.setAngle(anchor.getFacing()+angle+90);
        } else {
            sprite.setAngle(angle+90);
        }
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }
        
        Vector2f loc=new Vector2f(50000,50000);
        if(anchor.getLocation()!=null){
            loc=new Vector2f(anchor.getLocation());
        }
        Vector2f velocity=new Vector2f(vel);
        
        OBJECTSPACE.add(new objectspaceData(sprite, anchor, loc, offset, velocity, growth, angle, spin, parent, fadein, fadein+full, fadein+full+fadeout, fadeOnDeath, 0));
    }
    
    /**
     *
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * 
     * @param pos
     * Positioning mode, set the point of reference, useful for UI elements.
     * use SpriteRenderManager.positioning
     * STRETCH_TO_FULLSCREEN will override the size, FULLSCREEN_MAINTAIN_RATIO will use the size as a reference and scale the sprite accordingly.
     * 
     * @param loc
     * Vector2f, center in world coordinates. Ignore for fullscreen.
     * 
     * @param vel
     * Vector2f() velocity of the sprite. Ignore for fullscreen.
     * 
     * @param size
     * Vector2f(width, height) in pixels. size reference for FULLSCREEN_MAINTAIN_RATIO, ignore for STRETCH_TO_FULLSCREEN.
     * 
     * @param growth
     * Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed. Ignore for fullscreen.
     * 
     * @param angle
     * float of the sprite's azimuth. 0 is pointing top. Ignore for fullscreen.
     * 
     * @param spin
     * float of the sprite's rotation, in degree/sec. Ignore for fullscreen.
     * 
     * @param color
     * Color() override, also used for fading.
     * 
     * @param additive
     * boolean for additive blending.
     * 
     * @param fadein
     * time in sec for fading in. Set to -1 for single frame render.
     * 
     * @param full
     * time in sec at maximum opacity (clamped by color). Set to -1 for single frame render.
     * 
     * @param fadeout
     * time in sec for fading out. Set to -1 for single frame render.
     */
    
    public static void screenspaceRender(SpriteAPI sprite, positioning pos, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout) {
        ViewportAPI screen = Global.getCombatEngine().getViewport();
        
        Vector2f ratio=size;
        Vector2f screenSize= new Vector2f(screen.getVisibleWidth(),screen.getVisibleHeight());
        if(pos == positioning.STRETCH_TO_FULLSCREEN){
            sprite.setSize(screenSize.x, screenSize.y);
        } else if(pos == positioning.FULLSCREEN_MAINTAIN_RATIO) {
            if(size.x/size.y > screenSize.x/screenSize.y){
                ratio = new Vector2f((size.x/size.y)/(screenSize.x/screenSize.y),1);
            } else {
                ratio = new Vector2f(1, (size.y/size.x)/(screenSize.y/screenSize.x));
                sprite.setSize(Global.getCombatEngine().getViewport().getVisibleWidth()*ratio.x,Global.getCombatEngine().getViewport().getVisibleHeight()*ratio.y);
            }
        } else {
            sprite.setSize(size.x*screen.getViewMult(), size.y*screen.getViewMult());
        }
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }                 
        
        Vector2f velocity=new Vector2f(vel);  
        SCREENSPACE.add(new screenspaceData(sprite, pos, loc, velocity, ratio, growth, spin, fadein, fadein+full, fadein+full+fadeout, 0));        
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
                render(new renderData(entry.SPRITE, entry.LOC));
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
                
                render(new renderData(entry.SPRITE, location));
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
                    if(entry.POS == positioning.FULLSCREEN_MAINTAIN_RATIO){                    
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(entry.SIZE.x*screen.getVisibleWidth(), entry.SIZE.y*screen.getVisibleHeight());
                    } else if(entry.POS == positioning.STRETCH_TO_FULLSCREEN){
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
                    render(new renderData(entry.SPRITE, center));
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

                    if(entry.POS == positioning.FULLSCREEN_MAINTAIN_RATIO){                    
                        center = new Vector2f(screen.getCenter());
                        entry.SPRITE.setSize(entry.SIZE.x*screen.getVisibleWidth(), entry.SIZE.y*screen.getVisibleHeight());
                    } else if(entry.POS == positioning.STRETCH_TO_FULLSCREEN){
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
                    render(new renderData(entry.SPRITE, center));
                    if(entry.FADEOUT<0){
                        iter.remove();
                    }
                }                
            }
        }
        
        //Single frame sprite rendering
        if(!SINGLEFRAME.isEmpty()){
            for(renderData d : SINGLEFRAME){
                render(d);
            }
            SINGLEFRAME.clear();
        }
    }
    
    //////////////////////////////
    //                          //
    //          RENDER          //
    //                          //
    //////////////////////////////
    
    private void render (renderData data){
        //where the magic happen
        SpriteAPI sprite = data.SPRITE;  
        sprite.renderAtCenter(data.LOC.x, data.LOC.y);
    }
    
    //////////////////////////////
    //                          //
    //      RENDER CLASSES      //
    //                          //
    //////////////////////////////    
    
    private static class renderData {   
        private final SpriteAPI SPRITE; 
        private final Vector2f LOC; 
        
        public renderData(SpriteAPI sprite, Vector2f loc) {
            this.SPRITE = sprite;
            this.LOC = loc;
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
        
        public battlespaceData(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f growth, float spin, float fadein, float full, float fadeout, float time) {
            this.SPRITE = sprite;
            this.LOC = loc;
            this.VEL = vel;
            this.GROWTH = growth;
            this.SPIN = spin;
            this.FADEIN = fadein;
            this.FULL = full;
            this.FADEOUT = fadeout;
            this.TIME = time;
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
        
        public objectspaceData(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f loc, Vector2f offset, Vector2f vel, Vector2f growth, float angle, float spin, boolean parent, float fadein, float full, float fadeout, boolean fade, float time) {
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
        }
    }
        
    public static enum positioning{
        CENTER,
        LOW_LEFT,
        LOW_RIGHT,
        UP_LEFT,
        UP_RIGHT,
        STRETCH_TO_FULLSCREEN,
        FULLSCREEN_MAINTAIN_RATIO,
    }
    
    private static class screenspaceData {   
        private final SpriteAPI SPRITE;
        private final positioning POS;
        private Vector2f LOC; 
        private final Vector2f VEL;
        private Vector2f SIZE;
        private final Vector2f GROWTH;
        private final float SPIN;
        private final float FADEIN;
        private final float FULL; //fade in + full
        private final float FADEOUT; //full duration
        private float TIME;
        
        public screenspaceData(SpriteAPI sprite, positioning position, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float spin, float fadein, float full, float fadeout, float time) {
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
        }
    }    
}