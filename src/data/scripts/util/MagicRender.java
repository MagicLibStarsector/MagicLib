/*
 * By Tartiflette
 * Direct sprite rendering script to create new visual effect or add new UI elements with only one line of code. 
 * Note that every element will be drawn one frame late.
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.plugins.MagicRenderPlugin;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class MagicRender {
    
    /**
     * Checks if a point is within a certain distance of the screen's edges;
     * Used to avoid spawning particles or other effects that won't be seen by the player while impacting performances.
     * The longer lived the effect is, the farther should be the cut-off distance
     * 
     * @param distance
     * Distance away from the edges of the screen, in screen width.
     * A good default is 0.5f, or half a screen away from the edge.
     * Can be shorter for short lived particles and such.
     * 
     * @param point
     * Location to check.
     * 
     * @return 
     */
    public static boolean screenCheck (float distance, Vector2f point){
        float space = Global.getCombatEngine().getViewport().getVisibleWidth();
        space = (space/2)*(distance+1.4f);
        return MathUtils.isWithinRange(point, Global.getCombatEngine().getViewport().getCenter(), space);
    }
    
    /**
     * Single frame render,
     * absolute engine coordinates, 
     * can be used for animations.
     * 
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
    public static void singleframe(SpriteAPI sprite, Vector2f loc, Vector2f size, float angle, Color color, boolean additive) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }
        MagicRenderPlugin.addSingleframe(sprite, loc, CombatEngineLayers.BELOW_INDICATORS_LAYER);
    }

    // layer : layer to render at
    public static void singleframe(SpriteAPI sprite, Vector2f loc, Vector2f size, float angle, Color color, boolean additive, CombatEngineLayers layer) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }
        MagicRenderPlugin.addSingleframe(sprite, loc, layer);
    }

    // srcBlendFunc : openGL source blend function
    // destBlendFunc : openGL destination blend function
    public static void singleframe(SpriteAPI sprite, Vector2f loc, Vector2f size, float angle, Color color, CombatEngineLayers layer, int srcBlendFunc, int destBlendFunc) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);
        MagicRenderPlugin.addSingleframe(sprite, loc, layer);
    }


    /**
     * Draws a sprite in absolute engine coordinates for a duration.
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
    public static void battlespace(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }        
        Vector2f velocity=new Vector2f(vel);        
        MagicRenderPlugin.addBattlespace(sprite, loc, velocity, growth, spin, fadein, fadein+full, fadein+full+fadeout, CombatEngineLayers.BELOW_INDICATORS_LAYER);
    }

    // layer : layer to render at
    public static void battlespace(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout, CombatEngineLayers layer) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if(additive){
            sprite.setAdditiveBlend();
        }        
        Vector2f velocity=new Vector2f(vel);        
        MagicRenderPlugin.addBattlespace(sprite, loc, velocity, growth, spin, fadein, fadein+full, fadein+full+fadeout, layer);
    }

    // srcBlendFunc : openGL source blend function
    // destBlendFunc : openGL destination blend function
    public static void battlespace(SpriteAPI sprite, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, float fadein, float full, float fadeout, CombatEngineLayers layer, int srcBlendFunc, int destBlendFunc) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);
        Vector2f velocity=new Vector2f(vel);
        MagicRenderPlugin.addBattlespace(sprite, loc, velocity, growth, spin, fadein, fadein+full, fadein+full+fadeout, layer);
    }


    /**
     * Draws a sprite attached to an entity for a duration.
     * 
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     *
     * @param anchor
     * CombatEntityAPI the sprite will follow.
     *
     * @param offset
     * Vector2f, offset from the anchor's center in world coordinates. If "parent" is true, it will be kept relative to the anchor's orientation.
     * 
     * @param vel
     * Vector2f() velocity of the sprite relative to the anchor. If "parent" is true, it will be relative to the anchor's orientation.
     * 
     * @param size
     * Vector2f(width, height) in pixels.
     * 
     * @param growth
     * Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * 
     * @param angle
     * float of the sprite's azimuth. 0 is pointing front. If "parent" is true, 0 will match the anchor's orientation.
     * 
     * @param spin
     * float of the sprite's rotation, in degree/sec. If "parent" is true, it will be relative to the anchor's orientation.
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
    public static void objectspace(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f offset, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, boolean parent, Color color, boolean additive, float fadein, float full, float fadeout, boolean fadeOnDeath) {
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
        
        MagicRenderPlugin.addObjectspace(sprite, anchor, loc, offset, velocity, growth, angle, spin, parent, fadein, fadein+full, fadein+full+fadeout, fadeOnDeath, CombatEngineLayers.BELOW_INDICATORS_LAYER);
    }

    // layer : layer to render at
    public static void objectspace(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f offset, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, boolean parent, Color color, boolean additive, float fadein, float full, float fadeout, boolean fadeOnDeath, CombatEngineLayers layer) {
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
        
        MagicRenderPlugin.addObjectspace(sprite, anchor, loc, offset, velocity, growth, angle, spin, parent, fadein, fadein+full, fadein+full+fadeout, fadeOnDeath, layer);
    }

    // srcBlendFunc : openGL source blend function
    // destBlendFunc : openGL destination blend function
    public static void objectspace(SpriteAPI sprite, CombatEntityAPI anchor, Vector2f offset, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, boolean parent, Color color, float fadein, float full, float fadeout, boolean fadeOnDeath, CombatEngineLayers layer, int srcBlendFunc, int destBlendFunc) {
        sprite.setSize(size.x, size.y);
        if(parent){
            sprite.setAngle(anchor.getFacing()+angle+90);
        } else {
            sprite.setAngle(angle+90);
        }
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);

        Vector2f loc=new Vector2f(50000,50000);
        if(anchor.getLocation()!=null){
            loc=new Vector2f(anchor.getLocation());
        }
        Vector2f velocity=new Vector2f(vel);

        MagicRenderPlugin.addObjectspace(sprite, anchor, loc, offset, velocity, growth, angle, spin, parent, fadein, fadein+full, fadein+full+fadeout, fadeOnDeath, layer);
    }

    
    /**
     * Draws a sprite attached to the camera for a duration.
     *
     * @param sprite
     * SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * 
     * @param pos
     * Positioning mode, set the point of reference, useful for UI elements.
     * use MagicRender.positioning
     * STRETCH_TO_FULLSCREEN will override the size, FULLSCREEN_MAINTAIN_RATIO will use the size as a ratio reference and then scale the sprite accordingly.
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
    public static void screenspace(SpriteAPI sprite, positioning pos, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, boolean additive, float fadein, float full, float fadeout) {
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
        MagicRenderPlugin.addScreenspace(sprite, pos, loc, velocity, ratio, growth, spin, fadein, fadein+full, fadein+full+fadeout, CombatEngineLayers.BELOW_INDICATORS_LAYER);        
    }

    // srcBlendFunc : openGL source blend function
    // destBlendFunc : openGL destination blend function
    public static void screenspace(SpriteAPI sprite, positioning pos, Vector2f loc, Vector2f vel, Vector2f size, Vector2f growth, float angle, float spin, Color color, float fadein, float full, float fadeout, int srcBlendFunc, int destBlendFunc) {
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
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);

        Vector2f velocity=new Vector2f(vel);
        MagicRenderPlugin.addScreenspace(sprite, pos, loc, velocity, ratio, growth, spin, fadein, fadein+full, fadein+full+fadeout, CombatEngineLayers.BELOW_INDICATORS_LAYER);
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
}