package org.magiclib.util

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import org.magiclib.plugins.MagicRenderPlugin
import java.awt.Color
import kotlin.math.max

/**
 * Draw arbitrary sprites on screen with constraints to entities/camera when needed. Most drawing functions come with three optional declarations:
 * A simple declaration that should cover most use cases,
 * an advanced declaration that adds jitter/flicker controls plus render layer overrides,
 * and an advanced declaration that also includes OpenGL blending options.
 * Note that every element will be drawn one frame late.
 *
 * @author Tartiflette
 */
object MagicRender {
    /**
     * Checks if a point is within a certain distance of the screen's edges;
     * Used to avoid spawning particles or other effects that won't be seen by the player while impacting performances.
     * The longer lived the effect is, the farther should be the cut-off distance
     *
     * @param distance Distance away from the edges of the screen, in screen width.
     * A good default is 0.5f, or half a screen away from the edge.
     * Can be shorter for short lived particles and such.
     * @param point    Location to check.
     */
    @JvmStatic
    fun screenCheck(
        distance: Float,
        point: Vector2f?
    ): Boolean {
        var space = Global.getCombatEngine().viewport.visibleWidth
        space = space / 2 * (distance + 1.4f)
        return MathUtils.isWithinRange(point, Global.getCombatEngine().viewport.center, space)
    }

    /**
     * Single frame render, absolute engine coordinates, can be used for animations.
     *
     * @param sprite   SpriteAPI to render. Use Global.getSettings().getSprite(settings category, settings id)
     * @param loc      Vector2f, center in world coordinates.
     * @param size     Vector2f(width, height) in pixels.
     * @param angle    float of the sprite's azimuth. 0 is pointing top.
     * @param color    Color() override, also used for fading.
     * @param additive boolean for additive blending.
     * @param layer    layer to render at
     */
    @JvmStatic
    @JvmOverloads
    fun singleframe(
        sprite: SpriteAPI,
        loc: Vector2f?,
        size: Vector2f,
        angle: Float,
        color: Color?,
        additive: Boolean,
        layer: CombatEngineLayers = CombatEngineLayers.BELOW_INDICATORS_LAYER
    ) {
        sprite.setSize(size.x, size.y)
        sprite.angle = angle
        sprite.color = color
        if (additive) {
            sprite.setAdditiveBlend()
        }
        MagicRenderPlugin.addSingleframe(sprite, loc, layer)
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
    @JvmStatic
    fun singleframe(
        sprite: SpriteAPI,
        loc: Vector2f?,
        size: Vector2f,
        angle: Float,
        color: Color?,
        layer: CombatEngineLayers?,
        srcBlendFunc: Int,
        destBlendFunc: Int
    ) {
        sprite.setSize(size.x, size.y)
        sprite.angle = angle
        sprite.color = color
        sprite.setBlendFunc(srcBlendFunc, destBlendFunc)
        MagicRenderPlugin.addSingleframe(sprite, loc, layer)
    }


    /**
     * Draws a sprite in absolute engine coordinates for a duration.
     *
     * @param sprite        [SpriteAPI] to render. Use `Global.getSettings().getSprite(settings category, settings id)`.
     * @param loc           [Vector2f], center in world coordinates.
     * @param vel           [Vector2f] velocity of the sprite.
     * @param size          `Vector2f(width, height)` in pixels.
     * @param growth        [Vector2f] change of size over time in pixels/sec. Can be negative, a sprite that completely shrunk will be removed.
     * @param angle         float of the sprite's azimuth. 0 is pointing top.
     * @param spin          float of the sprite's rotation, in degree/sec.
     * @param color         [Color] override, also used for fading.
     * @param additive      boolean for additive blending.
     * @param jitterRange   max jitter offset from base position.
     * @param jitterTilt    max jitter rotation from base position.
     * @param flickerRange  max flickering range, can be >1 to maintain the sprite on or off.
     * @param flickerMedian default opacity before flickering, can be > or < 0.
     * @param maxDelay      base frequency is 60 update per second, delay can be randomly increased to this value.
     * @param fadein        time in sec for fading in.
     * @param full          time in sec at maximum opacity (clamped by color).
     * @param fadeout       time in sec for fading out.
     * @param layer         [CombatEngineLayers] layer to render at.
     */
    @JvmStatic
    @JvmOverloads
    fun battlespace(
        sprite: SpriteAPI,
        loc: Vector2f?,
        vel: Vector2f?,
        size: Vector2f,
        growth: Vector2f?,
        angle: Float,
        spin: Float,
        color: Color?,
        additive: Boolean,
        jitterRange: Float = 0f,
        jitterTilt: Float = 0f,
        flickerRange: Float = 0f,
        flickerMedian: Float = 0f,
        maxDelay: Float? = null,
        fadein: Float,
        full: Float,
        fadeout: Float,
        layer: CombatEngineLayers?
    ) {
        sprite.setSize(size.x, size.y)
        sprite.angle = angle
        sprite.color = color
        if (additive) {
            sprite.setAdditiveBlend()
        }
        val delay: IntervalUtil? = maxDelay?.let { IntervalUtil(0.016f, max(0.016f, maxDelay)) }

        MagicRenderPlugin.addBattlespace(
            /* sprite = */ sprite,
            /* loc = */ Vector2f(loc),
            /* vel = */ Vector2f(vel),
            /* growth = */ growth,
            /* spin = */ spin,
            /* jitterRange = */ jitterRange,
            /* jitterTilt = */ jitterTilt,
            /* jitter = */ if (maxDelay == null) null else Vector3f(),
            /* flickerRange = */ flickerRange,
            /* flickerMedian = */ flickerMedian,
            /* delay = */ delay,
            /* fadein = */ fadein,
            /* full = */ fadein + full,
            /* fadeout = */ fadein + full + fadeout,
            /* layer = */ layer
        )
    }

    /**
     * Draws a sprite attached to an entity for a duration.
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
    @JvmStatic
    @JvmOverloads
    fun objectspace(
        sprite: SpriteAPI,
        anchor: CombatEntityAPI,
        offset: Vector2f?,
        vel: Vector2f?,
        size: Vector2f,
        growth: Vector2f?,
        angle: Float,
        spin: Float,
        parent: Boolean,
        color: Color?,
        additive: Boolean,
        jitterRange: Float = 0f,
        jitterTilt: Float = 0f,
        flickerRange: Float = 0f,
        flickerMedian: Float = 0f,
        maxDelay: Float? = null,
        fadein: Float,
        full: Float,
        fadeout: Float,
        fadeOnDeath: Boolean,
        layer: CombatEngineLayers? = CombatEngineLayers.BELOW_INDICATORS_LAYER
    ) = objectspaceInternal(
        sprite = sprite,
        anchor = anchor,
        offset = offset,
        vel = vel,
        size = size,
        growth = growth,
        angle = angle,
        spin = spin,
        parent = parent,
        color = color,
        additive = additive,
        srcBlendFunc = 0,
        destBlendFunc = 0,
        jitterRange = jitterRange,
        jitterTilt = jitterTilt,
        flickerRange = flickerRange,
        flickerMedian = flickerMedian,
        maxDelay = maxDelay,
        fadein = fadein,
        full = full,
        fadeout = fadeout,
        fadeOnDeath = fadeOnDeath,
        layer = layer
    )

    /**
     * Draws a sprite attached to an entity for a duration.
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
    @JvmStatic
    @JvmOverloads
    fun objectspace(
        sprite: SpriteAPI,
        anchor: CombatEntityAPI,
        offset: Vector2f?,
        vel: Vector2f?,
        size: Vector2f,
        growth: Vector2f?,
        angle: Float,
        spin: Float,
        parent: Boolean,
        color: Color?,
        jitterRange: Float = 0f,
        jitterTilt: Float = 0f,
        flickerRange: Float = 0f,
        flickerMedian: Float = 0f,
        maxDelay: Float? = null,
        fadein: Float,
        full: Float,
        fadeout: Float,
        fadeOnDeath: Boolean,
        layer: CombatEngineLayers? = CombatEngineLayers.BELOW_INDICATORS_LAYER,
        srcBlendFunc: Int,
        destBlendFunc: Int
    ) = objectspaceInternal(
        sprite = sprite,
        anchor = anchor,
        offset = offset,
        vel = vel,
        size = size,
        growth = growth,
        angle = angle,
        spin = spin,
        parent = parent,
        color = color,
        additive = false,
        srcBlendFunc = srcBlendFunc,
        destBlendFunc = destBlendFunc,
        jitterRange = jitterRange,
        jitterTilt = jitterTilt,
        flickerRange = flickerRange,
        flickerMedian = flickerMedian,
        maxDelay = maxDelay,
        fadein = fadein,
        full = full,
        fadeout = fadeout,
        fadeOnDeath = fadeOnDeath,
        layer = layer
    )

    private fun objectspaceInternal(
        sprite: SpriteAPI,
        anchor: CombatEntityAPI,
        offset: Vector2f?,
        vel: Vector2f?,
        size: Vector2f,
        growth: Vector2f?,
        angle: Float,
        spin: Float,
        parent: Boolean,
        color: Color?,
        additive: Boolean,
        srcBlendFunc: Int,
        destBlendFunc: Int,
        jitterRange: Float,
        jitterTilt: Float,
        flickerRange: Float,
        flickerMedian: Float,
        maxDelay: Float?,
        fadein: Float,
        full: Float,
        fadeout: Float,
        fadeOnDeath: Boolean,
        layer: CombatEngineLayers?
    ) {
        sprite.setSize(size.x, size.y)
        if (parent) {
            sprite.angle = anchor.facing + angle + 90
        } else {
            sprite.angle = angle + 90
        }
        sprite.color = color

        if (additive) {
            sprite.setAdditiveBlend()
        } else {
            sprite.setBlendFunc(srcBlendFunc, destBlendFunc)
        }

        var loc = Vector2f(50000f, 50000f)
        if (anchor.location != null) {
            loc = Vector2f(anchor.location)
        }
        val delay = maxDelay?.let { IntervalUtil(0.016f, max(0.016f, maxDelay)) }
        MagicRenderPlugin.addObjectspace(
            /* sprite = */ sprite,
            /* anchor = */ anchor,
            /* loc = */ loc,
            /* offset = */ offset,
            /* vel = */ Vector2f(vel),
            /* growth = */ growth,
            /* angle = */ angle,
            /* spin = */ spin,
            /* parent = */ parent,
            /* jitterRange = */ jitterRange,
            /* jitterTilt = */ jitterTilt,
            /* jitter = */ if (maxDelay == null) null else Vector3f(),
            /* flickerRange = */ flickerRange,
            /* flickerMedian = */ flickerMedian,
            /* delay = */ delay,
            /* fadein = */ fadein,
            /* full = */ fadein + full,
            /* fadeout = */ fadein + full + fadeout,
            /* fadeOnDeath = */ fadeOnDeath,
            /* layer = */ layer
        )
    }

    private fun screenspaceInternal(
        sprite: SpriteAPI,
        pos: positioning,
        loc: Vector2f,
        vel: Vector2f,
        size: Vector2f,
        growth: Vector2f,
        angle: Float,
        spin: Float,
        color: Color,
        jitterRange: Float,
        jitterTilt: Float,
        flickerRange: Float,
        flickerMedian: Float,
        maxDelay: Float,
        fadein: Float,
        full: Float,
        fadeout: Float,
        layer: CombatEngineLayers,
        additive: Boolean,
        srcBlendFunc: Int,
        destBlendFunc: Int
    ) {
        val screen = Global.getCombatEngine().viewport

        var ratio = size
        val screenSize = Vector2f(screen.visibleWidth, screen.visibleHeight)
        when (pos) {
            positioning.STRETCH_TO_FULLSCREEN -> sprite.setSize(screenSize.x, screenSize.y)
            positioning.FULLSCREEN_MAINTAIN_RATIO -> if (size.x / size.y > screenSize.x / screenSize.y) {
                ratio = Vector2f(size.x / size.y / (screenSize.x / screenSize.y), 1f)
            } else {
                ratio = Vector2f(1f, size.y / size.x / (screenSize.y / screenSize.x))
                sprite.setSize(
                    Global.getCombatEngine().viewport.visibleWidth * ratio.x,
                    Global.getCombatEngine().viewport.visibleHeight * ratio.y
                )
            }

            else -> sprite.setSize(size.x * screen.viewMult, size.y * screen.viewMult)
        }

        sprite.angle = angle
        sprite.color = color

        if (additive) {
            sprite.setAdditiveBlend()
        } else {
            sprite.setBlendFunc(srcBlendFunc, destBlendFunc)
        }

        val delay = IntervalUtil(0.016f, Math.max(0.016f, maxDelay))

        MagicRenderPlugin.addScreenspace(
            sprite,
            pos,
            Vector2f(loc),
            Vector2f(vel),
            ratio,
            growth,
            spin,
            jitterRange,
            jitterTilt,
            Vector3f(),
            flickerRange,
            flickerMedian,
            delay,
            fadein,
            fadein + full,
            fadein + full + fadeout,
            layer
        )
    }


    enum class positioning {
        CENTER,
        LOW_LEFT,
        LOW_RIGHT,
        UP_LEFT,
        UP_RIGHT,
        STRETCH_TO_FULLSCREEN,
        FULLSCREEN_MAINTAIN_RATIO
    }

}