
package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.magiclib.plugins.MagicRenderPlugin;

import java.awt.*;

/**
 * Draw arbitrary sprites on screen with constraints to entities/camera when needed. Most drawing functions come with three optional declarations:
 * A simple declaration that should cover most use cases,
 * an advanced declaration that adds jitter/flicker controls plus render layer overrides,
 * and an advanced declaration that also includes OpenGL blending options.
 * Note that every element will be drawn one frame late.
 *
 * @author Tartiflette
 */
public class MagicRender {

    /**
     * Checks if a point is within a certain distance of the screen's edges;
     * Used to avoid spawning particles or other effects that won't be seen by the player while impacting performances.
     * The longer lived the effect is, the farther should be the cut-off distance
     *
     * @param distance Distance away from the edges of the screen, in screen width.
     *                 A good default is 0.5f, or half a screen away from the edge.
     *                 Can be shorter for short lived particles and such.
     * @param point    Location to check.
     * @return
     */
    public static boolean screenCheck(
            float distance,
            Vector2f point
    ) {
        float space = Global.getCombatEngine().getViewport().getVisibleWidth();
        space = (space / 2) * (distance + 1.4f);
        return MathUtils.isWithinRange(point, Global.getCombatEngine().getViewport().getCenter(), space);
    }

    //////////////////////////////////
    //                              //
    //     SINGLE FRAME RENDER      //
    //                              //
    //////////////////////////////////

    /**
     * Single frame render in absolute engine coordinates,                      can be used for animations.
     *
     * @param sprite   SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc      Vector2f, center in world coordinates.
     * @param size     Vector2f(width, height) in pixels.
     * @param angle    float of the sprite's azimuth. 0 is pointing top.
     * @param color    Color() override, also used for fading.
     * @param additive boolean for additive blending.
     */
    public static void singleframe(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f size,
            float angle,
            Color color,
            boolean additive
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }
        MagicRenderPlugin.addSingleframe(sprite, loc, CombatEngineLayers.BELOW_INDICATORS_LAYER);
    }

    /**
     * Single frame render, absolute engine coordinates,                        can be used for animations.
     *
     * @param sprite   SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc      Vector2f, center in world coordinates.
     * @param size     Vector2f(width, height) in pixels.
     * @param angle    float of the sprite's azimuth. 0 is pointing top.
     * @param color    Color() override, also used for fading.
     * @param additive boolean for additive blending.
     * @param layer    layer to render at
     */
    public static void singleframe(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f size,
            float angle,
            Color color,
            boolean additive,
            CombatEngineLayers layer
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }
        MagicRenderPlugin.addSingleframe(sprite, loc, layer);
    }

    /**
     * Single frame render in absolute engine coordinates with OpenGL blending options, can be used for animations.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc           Vector2f, center in world coordinates.
     * @param size          Vector2f(width, height) in pixels.
     * @param angle         float of the sprite's azimuth. 0 is pointing top.
     * @param color         Color() override, also used for fading.
     * @param layer         layer to render at
     * @param srcBlendFunc  openGL source blend function
     * @param destBlendFunc openGL destination blend function
     */
    public static void singleframe(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f size,
            float angle,
            Color color,
            CombatEngineLayers layer,
            int srcBlendFunc,
            int destBlendFunc
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);
        MagicRenderPlugin.addSingleframe(sprite, loc, layer);
    }


    //////////////////////////////////
    //                              //
    //      BATTLESPACE RENDER      //
    //                              //
    //////////////////////////////////

    /**
     * Draws a sprite in absolute engine coordinates for a duration. Simple declaration.
     *
     * @param sprite   SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc      Vector2f, center in world coordinates.
     * @param vel      Vector2f() velocity of the sprite.
     * @param size     Vector2f(width, height) in pixels.
     * @param growth   Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle    float of the sprite's azimuth. 0 is pointing top.
     * @param spin     float of the sprite's rotation, in degree/sec.
     * @param color    Color() override, also used for fading.
     * @param additive boolean for additive blending.
     * @param fadein   time in sec for fading in.
     * @param full     time in sec at maximum opacity (clamped by color)
     * @param fadeout  time in sec for fading out
     */
    public static void battlespace(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            boolean additive,
            float fadein,
            float full,
            float fadeout
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }
        MagicRenderPlugin.addBattlespace(
                sprite,
                new Vector2f(loc), new Vector2f(vel),
                growth, spin,
                0, 0, null,
                0, 0, null,
                fadein, fadein + full, fadein + full + fadeout,
                CombatEngineLayers.BELOW_INDICATORS_LAYER
        );
    }

    /**
     * Draws a sprite in absolute engine coordinates for a duration. Advanced declaration.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc           Vector2f, center in world coordinates.
     * @param vel           Vector2f() velocity of the sprite.
     * @param size          Vector2f(width, height) in pixels.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle         float of the sprite's azimuth. 0 is pointing top.
     * @param spin          float of the sprite's rotation, in degree/sec.
     * @param color         Color() override, also used for fading.
     * @param additive      boolean for additive blending.
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param fadein        time in sec for fading in.
     * @param full          time in sec at maximum opacity (clamped by color)
     * @param fadeout       time in sec for fading out
     * @param layer         : layer to render at
     */
    public static void battlespace(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            boolean additive,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            CombatEngineLayers layer
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }
        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addBattlespace(
                sprite,
                new Vector2f(loc), new Vector2f(vel),
                growth, spin,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout,
                layer);
    }

    /**
     * Draws a sprite in absolute engine coordinates for a duration. Advanced declaration with OpenGL blending options
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc           Vector2f, center in world coordinates.
     * @param vel           Vector2f() velocity of the sprite.
     * @param size          Vector2f(width, height) in pixels.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle         float of the sprite's azimuth. 0 is pointing top.
     * @param spin          float of the sprite's rotation, in degree/sec.
     * @param color         Color() override, also used for fading.
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param fadein        time in sec for fading in.
     * @param full          time in sec at maximum opacity (clamped by color)
     * @param fadeout       time in sec for fading out
     * @param layer         : layer to render at
     * @param srcBlendFunc  : openGL source blend function
     * @param destBlendFunc : openGL destination blend function
     */
    public static void battlespace(
            SpriteAPI sprite,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            CombatEngineLayers layer,
            int srcBlendFunc,
            int destBlendFunc
    ) {
        sprite.setSize(size.x, size.y);
        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);

        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addBattlespace(
                sprite,
                new Vector2f(loc), new Vector2f(vel),
                growth, spin,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout,
                layer
        );
    }

    //////////////////////////////////
    //                              //
    //      OBJECTSPACE RENDER      //
    //                              //
    //////////////////////////////////

    /**
     * Draws a sprite attached to an entity for a duration. Simple declaration.
     *
     * @param sprite      SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param anchor      CombatEntityAPI the sprite will follow.
     * @param offset      Vector2f, offset from the anchor's center in world coordinates. If "parent" is true, it will be kept relative to the anchor's orientation.
     * @param vel         Vector2f() velocity of the sprite relative to the anchor. If "parent" is true, it will be relative to the anchor's orientation.
     * @param size        Vector2f(width, height) in pixels.
     * @param growth      Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle       float of the sprite's azimuth. 0 is pointing front. If "parent" is true, 0 will match the anchor's orientation.
     * @param spin        float of the sprite's rotation, in degree/sec. If "parent" is true, it will be relative to the anchor's orientation.
     * @param parent      boolean, if true the sprite will also follow the anchor's orientation in addition to the position.
     * @param color       Color() override, also used for fading.
     * @param additive    boolean for additive blending.
     * @param fadein      time in sec for fading in.
     * @param full        time in sec at maximum opacity (clamped by color). If attached to a projectile that value can be longer than the maximum flight time, for example 99s.
     * @param fadeout     time in sec for fading out. If attached to a projectile, the sprite will immediately start to fade if the anchor hit or fade.
     * @param fadeOnDeath if true the sprite will fadeout in case the anchor is removed, if false it will be instantly removed. Mostly useful if you want to put effects on missiles or projectiles.
     */
    public static void objectspace(
            SpriteAPI sprite,
            CombatEntityAPI anchor,
            Vector2f offset,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            boolean parent,
            Color color,
            boolean additive,
            float fadein,
            float full,
            float fadeout,
            boolean fadeOnDeath
    ) {
        sprite.setSize(size.x, size.y);
        if (parent) {
            sprite.setAngle(anchor.getFacing() + angle + 90);
        } else {
            sprite.setAngle(angle + 90);
        }
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }

        Vector2f loc = new Vector2f(50000, 50000);
        if (anchor.getLocation() != null) {
            loc = new Vector2f(anchor.getLocation());
        }

        MagicRenderPlugin.addObjectspace(
                sprite,
                anchor,
                loc, offset, new Vector2f(vel),
                growth, angle, spin,
                parent,
                0, 0, null,
                0, 0, null,
                fadein, fadein + full, fadein + full + fadeout, fadeOnDeath,
                CombatEngineLayers.BELOW_INDICATORS_LAYER
        );
    }

    /**
     * Draws a sprite attached to an entity for a duration. Advanced declaration.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param anchor        CombatEntityAPI the sprite will follow.
     * @param offset        Vector2f, offset from the anchor's center in world coordinates. If "parent" is true, it will be kept relative to the anchor's orientation.
     * @param vel           Vector2f() velocity of the sprite relative to the anchor. If "parent" is true, it will be relative to the anchor's orientation.
     * @param size          Vector2f(width, height) in pixels.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle         float of the sprite's azimuth. 0 is pointing front. If "parent" is true, 0 will match the anchor's orientation.
     * @param spin          float of the sprite's rotation, in degree/sec. If "parent" is true, it will be relative to the anchor's orientation.
     * @param parent        boolean, if true the sprite will also follow the anchor's orientation in addition to the position.
     * @param color         Color() override, also used for fading.
     * @param additive      boolean for additive blending.
     * @param fadein        time in sec for fading in.
     * @param full          time in sec at maximum opacity (clamped by color). If attached to a projectile that value can be longer than the maximum flight time, for example 99s.
     * @param fadeout       time in sec for fading out. If attached to a projectile, the sprite will immediately start to fade if the anchor hit or fade.
     * @param fadeOnDeath   if true the sprite will fadeout in case the anchor is removed, if false it will be instantly removed. Mostly useful if you want to put effects on missiles or projectiles.
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param layer         layer to render at
     */
    public static void objectspace(
            SpriteAPI sprite,
            CombatEntityAPI anchor,
            Vector2f offset,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            boolean parent,
            Color color,
            boolean additive,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            boolean fadeOnDeath,
            CombatEngineLayers layer
    ) {
        sprite.setSize(size.x, size.y);
        if (parent) {
            sprite.setAngle(anchor.getFacing() + angle + 90);
        } else {
            sprite.setAngle(angle + 90);
        }
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }

        Vector2f loc = new Vector2f(50000, 50000);
        if (anchor.getLocation() != null) {
            loc = new Vector2f(anchor.getLocation());
        }

        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addObjectspace(
                sprite,
                anchor,
                loc, offset, new Vector2f(vel),
                growth, angle, spin,
                parent,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout, fadeOnDeath,
                layer
        );
    }


    /**
     * Draws a sprite attached to an entity for a duration. Advanced declaration with OpenGl blending options.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param anchor        CombatEntityAPI the sprite will follow.
     * @param offset        Vector2f, offset from the anchor's center in world coordinates. If "parent" is true, it will be kept relative to the anchor's orientation.
     * @param vel           Vector2f() velocity of the sprite relative to the anchor. If "parent" is true, it will be relative to the anchor's orientation.
     * @param size          Vector2f(width, height) in pixels.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle         float of the sprite's azimuth. 0 is pointing front. If "parent" is true, 0 will match the anchor's orientation.
     * @param spin          float of the sprite's rotation, in degree/sec. If "parent" is true, it will be relative to the anchor's orientation.
     * @param parent        boolean, if true the sprite will also follow the anchor's orientation in addition to the position.
     * @param color         Color() override, also used for fading.
     * @param fadein        time in sec for fading in.
     * @param full          time in sec at maximum opacity (clamped by color). If attached to a projectile that value can be longer than the maximum flight time, for example 99s.
     * @param fadeout       time in sec for fading out. If attached to a projectile, the sprite will immediately start to fade if the anchor hit or fade.
     * @param fadeOnDeath   if true the sprite will fadeout in case the anchor is removed, if false it will be instantly removed. Mostly useful if you want to put effects on missiles or projectiles.
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param layer         layer to render at
     * @param srcBlendFunc  openGL source blend function
     * @param destBlendFunc openGL destination blend function
     */
    public static void objectspace(
            SpriteAPI sprite,
            CombatEntityAPI anchor,
            Vector2f offset,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            boolean parent,
            Color color,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            boolean fadeOnDeath,
            CombatEngineLayers layer,
            int srcBlendFunc,
            int destBlendFunc
    ) {
        sprite.setSize(size.x, size.y);
        if (parent) {
            sprite.setAngle(anchor.getFacing() + angle + 90);
        } else {
            sprite.setAngle(angle + 90);
        }
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);

        Vector2f loc = new Vector2f(50000, 50000);
        if (anchor.getLocation() != null) {
            loc = new Vector2f(anchor.getLocation());
        }

        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addObjectspace(
                sprite,
                anchor,
                loc, offset, new Vector2f(vel),
                growth, angle, spin,
                parent,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout, fadeOnDeath,
                layer
        );
    }

    //////////////////////////////////
    //                              //
    //      SCREENSPACE RENDER      //
    //                              //
    //////////////////////////////////

    /**
     * Draws a sprite attached to the camera for a duration. Simple declaration.
     *
     * @param sprite   SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param pos      Positioning mode, set the point of reference, useful for UI elements.
     *                 use MagicRender.positioning
     *                 STRETCH_TO_FULLSCREEN will override the size, FULLSCREEN_MAINTAIN_RATIO will use the size as a ratio reference and then scale the sprite accordingly.
     * @param loc      Vector2f, center in world coordinates. Ignore for fullscreen.
     * @param vel      Vector2f() velocity of the sprite. Ignore for fullscreen.
     * @param size     Vector2f(width, height) in pixels. size reference for FULLSCREEN_MAINTAIN_RATIO, ignore for STRETCH_TO_FULLSCREEN.
     * @param growth   Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed. Ignore for fullscreen.
     * @param angle    float of the sprite's azimuth. 0 is pointing top. Ignore for fullscreen.
     * @param spin     float of the sprite's rotation, in degree/sec. Ignore for fullscreen.
     * @param color    Color() override, also used for fading.
     * @param additive boolean for additive blending.
     * @param fadein   time in sec for fading in. Set to -1 for single frame render.
     * @param full     time in sec at maximum opacity (clamped by color). Set to -1 for single frame render.
     * @param fadeout  time in sec for fading out. Set to -1 for single frame render.
     */
    public static void screenspace(
            SpriteAPI sprite,
            positioning pos,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            boolean additive,
            float fadein,
            float full,
            float fadeout
    ) {
        ViewportAPI screen = Global.getCombatEngine().getViewport();

        Vector2f ratio = size;
        Vector2f screenSize = new Vector2f(screen.getVisibleWidth(), screen.getVisibleHeight());

        switch (pos) {
            case STRETCH_TO_FULLSCREEN:
                sprite.setSize(screenSize.x, screenSize.y);
                break;
            case FULLSCREEN_MAINTAIN_RATIO:
                if (size.x / size.y > screenSize.x / screenSize.y) {
                    ratio = new Vector2f((size.x / size.y) / (screenSize.x / screenSize.y), 1);
                } else {
                    ratio = new Vector2f(1, (size.y / size.x) / (screenSize.y / screenSize.x));
                    sprite.setSize(Global.getCombatEngine().getViewport().getVisibleWidth() * ratio.x, Global.getCombatEngine().getViewport().getVisibleHeight() * ratio.y);
                }
                break;
            default:
                sprite.setSize(size.x * screen.getViewMult(), size.y * screen.getViewMult());
        }

        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }

        Vector2f velocity = new Vector2f(vel);
        MagicRenderPlugin.addScreenspace(
                sprite,
                pos, loc, velocity,
                ratio, growth, spin,
                0, 0, null,
                0, 0, null,
                fadein, fadein + full, fadein + full + fadeout,
                CombatEngineLayers.BELOW_INDICATORS_LAYER
        );
    }

    /**
     * Draws a sprite attached to the camera for a duration. Advanced declaration.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param pos           Positioning mode, set the point of reference, useful for UI elements.
     *                      use MagicRender.positioning
     *                      STRETCH_TO_FULLSCREEN will override the size,                            FULLSCREEN_MAINTAIN_RATIO will use the size as a ratio reference and then scale the sprite accordingly.
     * @param loc           Vector2f, center in world coordinates. Ignore for fullscreen.
     * @param vel           Vector2f() velocity of the sprite. Ignore for fullscreen.
     * @param size          Vector2f(width, height) in pixels. size reference for FULLSCREEN_MAINTAIN_RATIO, ignore for STRETCH_TO_FULLSCREEN.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed. Ignore for fullscreen.
     * @param angle         float of the sprite's azimuth. 0 is pointing top. Ignore for fullscreen.
     * @param spin          float of the sprite's rotation, in degree/sec. Ignore for fullscreen.
     * @param color         Color() override, also used for fading.
     * @param additive      boolean for additive blending.
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param fadein        time in sec for fading in. Set to -1 for single frame render.
     * @param full          time in sec at maximum opacity (clamped by color). Set to -1 for single frame render.
     * @param fadeout       time in sec for fading out. Set to -1 for single frame render.
     * @param layer         layer to render at
     */
    public static void screenspace(
            SpriteAPI sprite,
            positioning pos,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            boolean additive,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            CombatEngineLayers layer
    ) {
        ViewportAPI screen = Global.getCombatEngine().getViewport();

        Vector2f ratio = size;
        Vector2f screenSize = new Vector2f(screen.getVisibleWidth(), screen.getVisibleHeight());
        switch (pos) {
            case STRETCH_TO_FULLSCREEN:
                sprite.setSize(screenSize.x, screenSize.y);
                break;
            case FULLSCREEN_MAINTAIN_RATIO:
                if (size.x / size.y > screenSize.x / screenSize.y) {
                    ratio = new Vector2f((size.x / size.y) / (screenSize.x / screenSize.y), 1);
                } else {
                    ratio = new Vector2f(1, (size.y / size.x) / (screenSize.y / screenSize.x));
                    sprite.setSize(Global.getCombatEngine().getViewport().getVisibleWidth() * ratio.x, Global.getCombatEngine().getViewport().getVisibleHeight() * ratio.y);
                }
                break;
            default:
                sprite.setSize(size.x * screen.getViewMult(), size.y * screen.getViewMult());
        }

        sprite.setAngle(angle);
        sprite.setColor(color);
        if (additive) {
            sprite.setAdditiveBlend();
        }

        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addScreenspace(
                sprite,
                pos, new Vector2f(loc), new Vector2f(vel),
                ratio, growth, spin,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout,
                layer);
    }

    /**
     * Draws a sprite attached to the camera for a duration. Advanced declaration with OpenGl blending options.
     *
     * @param sprite        SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param pos           Positioning mode, set the point of reference, useful for UI elements.
     *                      use MagicRender.positioning
     *                      STRETCH_TO_FULLSCREEN will override the size,                            FULLSCREEN_MAINTAIN_RATIO will use the size as a ratio reference and then scale the sprite accordingly.
     * @param loc           Vector2f, center in world coordinates. Ignore for fullscreen.
     * @param vel           Vector2f() velocity of the sprite. Ignore for fullscreen.
     * @param size          Vector2f(width, height) in pixels. size reference for FULLSCREEN_MAINTAIN_RATIO, ignore for STRETCH_TO_FULLSCREEN.
     * @param growth        Vector2f() change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed. Ignore for fullscreen.
     * @param angle         float of the sprite's azimuth. 0 is pointing top. Ignore for fullscreen.
     * @param spin          float of the sprite's rotation, in degree/sec. Ignore for fullscreen.
     * @param color         Color() override, also used for fading.
     * @param fadein        time in sec for fading in. Set to -1 for single frame render.
     * @param full          time in sec at maximum opacity (clamped by color). Set to -1 for single frame render.
     * @param fadeout       time in sec for fading out. Set to -1 for single frame render.
     * @param layer         layer to render at
     * @param jitterRange   max jitter offset from base position
     * @param jitterTilt    max jitter rotation from base position
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off
     * @param flickerMedian default opacity before flickering, can be > or < 0
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value
     * @param srcBlendFunc  openGL source blend function
     * @param destBlendFunc openGL destination blend function
     */
    public static void screenspace(
            SpriteAPI sprite,
            positioning pos,
            Vector2f loc,
            Vector2f vel,
            Vector2f size,
            Vector2f growth,
            float angle,
            float spin,
            Color color,
            float jitterRange,
            float jitterTilt,
            float flickerRange,
            float flickerMedian,
            float maxDelay,
            float fadein,
            float full,
            float fadeout,
            CombatEngineLayers layer,
            int srcBlendFunc,
            int destBlendFunc
    ) {
        ViewportAPI screen = Global.getCombatEngine().getViewport();

        Vector2f ratio = size;
        Vector2f screenSize = new Vector2f(screen.getVisibleWidth(), screen.getVisibleHeight());
        switch (pos) {
            case STRETCH_TO_FULLSCREEN:
                sprite.setSize(screenSize.x, screenSize.y);
                break;
            case FULLSCREEN_MAINTAIN_RATIO:
                if (size.x / size.y > screenSize.x / screenSize.y) {
                    ratio = new Vector2f((size.x / size.y) / (screenSize.x / screenSize.y), 1);
                } else {
                    ratio = new Vector2f(1, (size.y / size.x) / (screenSize.y / screenSize.x));
                    sprite.setSize(Global.getCombatEngine().getViewport().getVisibleWidth() * ratio.x, Global.getCombatEngine().getViewport().getVisibleHeight() * ratio.y);
                }
                break;
            default:
                sprite.setSize(size.x * screen.getViewMult(), size.y * screen.getViewMult());
        }

        sprite.setAngle(angle);
        sprite.setColor(color);
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc);

        IntervalUtil delay = new IntervalUtil(0.016f, Math.max(0.016f, maxDelay));

        MagicRenderPlugin.addScreenspace(
                sprite,
                pos, new Vector2f(loc), new Vector2f(vel),
                ratio, growth, spin,
                jitterRange, jitterTilt, new Vector3f(),
                flickerRange, flickerMedian, delay,
                fadein, fadein + full, fadein + full + fadeout,
                layer);
    }


    public static enum positioning {
        CENTER,
        LOW_LEFT,
        LOW_RIGHT,
        UP_LEFT,
        UP_RIGHT,
        STRETCH_TO_FULLSCREEN,
        FULLSCREEN_MAINTAIN_RATIO,
    }
}