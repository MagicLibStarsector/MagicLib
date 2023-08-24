package org.magiclib.achievements

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.opengl.GL14
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.interpolateColor
import org.magiclib.kotlin.modify
import org.magiclib.kotlin.random
import java.awt.Color
import java.util.*
import kotlin.math.floor
import kotlin.math.sqrt


/**
 * Makes fancy nebula effects.
 * Not intended to be part of the public API.
 *
 * @author Nia Tahl, modified by Wisp
 */
internal interface CustomRenderer {
    val nebulaData: MutableMap<Long, Nebula>

    enum class NebulaType {
        NORMAL, SWIRLY, SPLINTER, DUST
    }

    fun init() {
        nebulaData.clear()
    }

    // Ticking our lifetimes and removing expired
    fun advance(amount: Float) {
        // clean up nebula list
        val nebulaToRemove: MutableList<Long> = ArrayList()
        nebulaData.forEach { (id, nebula) ->
            val timePassed =
                if (Global.getCurrentState() == GameState.COMBAT && Global.getCombatEngine() != null)
                    Global.getCombatEngine()!!.elapsedInLastFrame
                else amount
            val timeToAddToLifetime = timePassed / (Global.getCombatEngine()?.timeMult?.modified ?: 1f)
            nebula.lifetime += timeToAddToLifetime
            if (nebula.lifetime > nebula.duration)
                nebulaToRemove.add(id)
        }

        nebulaToRemove.forEach { nebulaData.remove(it) }
    }

    fun renderNebula(particle: Nebula) {
        // Removed because it causes the fx to disappear when you zoom in.
        // Rude for performance but it's not like there will be a ton of ships using this offscreen.
//        if (!view.isNearViewport(nebula.location, 500f)) return

        val cloudSprite = when (particle.type) {
            NebulaType.NORMAL -> Global.getSettings().getSprite("misc", "nebula_particles")
            NebulaType.SWIRLY -> Global.getSettings().getSprite("misc", "fx_particles2")
            NebulaType.SPLINTER -> Global.getSettings().getSprite("misc", "fx_particles1")
            NebulaType.DUST -> Global.getSettings().getSprite("misc", "dust_particles")
        } ?: return

        var alpha = particle.color.alpha
        if (particle.lifetime < particle.duration * particle.inFraction) {
            alpha = (alpha * (particle.lifetime / (particle.duration * particle.inFraction))).toInt().coerceIn(0, 255)
        } else if (particle.lifetime > particle.duration - particle.duration * particle.outFraction) {
            alpha =
                (alpha - alpha * ((particle.lifetime - particle.duration * (1f - particle.outFraction)) / (particle.duration * particle.outFraction))).toInt()
                    .coerceIn(0, 255)
        }

        val actualSize =
            if (particle.sqrt) {
                particle.size + (particle.endSize - particle.size) * sqrt(particle.lifetime / particle.duration)
            } else {
                particle.size + (particle.endSize - particle.size) * particle.lifetime / particle.duration
            }

        val lifeFraction = particle.lifetime / particle.duration
        cloudSprite.apply {
            color = particle.color

            if (particle.color != particle.outColor) {
                color = color.interpolateColor(particle.outColor, lifeFraction)
            }

            color = color.modify(alpha = alpha)
            setAdditiveBlend()
            angle = particle.angle
            setSize(actualSize * 4f, actualSize * 4f)
        }

        val xIndex: Int = particle.index % 4
        val yIndex = floor(particle.index / 4f).toInt()
        var offsetPos = Vector2f(actualSize * (1.5f - xIndex), actualSize * (1.5f - yIndex))
        offsetPos = VectorUtils.rotate(offsetPos, particle.angle)
        val delta = Vector2f(particle.velocity)
        val actualLocation =
            particle.location + (delta.scale(particle.lifetime) as Vector2f) + (particle.anchorLocation)

        renderSprite(
            particle = particle,
            cloudSprite = cloudSprite,
            actualLocation = actualLocation,
            offsetPos = offsetPos,
            xIndex = xIndex,
            yIndex = yIndex
        )
    }

    fun renderSprite(
        particle: Nebula,
        cloudSprite: SpriteAPI,
        actualLocation: Vector2f,
        offsetPos: Vector2f,
        xIndex: Int,
        yIndex: Int
    ) {
        // OpenGL witchcraft that I don't actually understand
        if (particle.negative)
            GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT)

        cloudSprite//.renderAtCenter(actualLocation.x, actualLocation.y)
            .renderRegionAtCenter(
                actualLocation.x + offsetPos.x,
                actualLocation.y + offsetPos.y,
                0.25f * xIndex,
                0.25f * yIndex,
                0.25f,
                0.25f
            )

        // DO NOT FORGET TO TURN OFF FUNKY MODE
        if (particle.negative)
            GL14.glBlendEquation(GL14.GL_FUNC_ADD)
    }

    data class Nebula(
        val id: Long,
        val location: Vector2f,
        val anchorLocation: Vector2f,
        val velocity: Vector2f,
        val size: Float,
        val endSize: Float,
        val duration: Float,
        val inFraction: Float,
        val outFraction: Float,
        val color: Color,
        val layer: CombatEngineLayers,
        val type: NebulaType,
        val negative: Boolean,
        val sqrt: Boolean,
        val outColor: Color
    ) {
        var lifetime = 0f
        val index = (0..11).random()
        val angle = (0f..359f).random()
    }

    fun addNebula(
        location: Vector2f,
        anchorLocation: Vector2f,
        velocity: Vector2f,
        size: Float,
        endSizeMult: Float,
        duration: Float,
        inFraction: Float,
        outFraction: Float,
        color: Color,
        layer: CombatEngineLayers = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER,
        type: NebulaType = NebulaType.NORMAL,
        negative: Boolean = false,
        expandAsSqrt: Boolean = false,
        outColor: Color = color
    ) = Nebula(
        id = Misc.random.nextLong(),
        location = Vector2f(location),
        anchorLocation = anchorLocation,
        velocity = Vector2f(velocity),
        size = size,
        endSize = endSizeMult * size,
        duration = duration,
        inFraction = inFraction,
        outFraction = outFraction,
        color = color,
        layer = layer,
        type = type,
        negative = negative,
        sqrt = expandAsSqrt,
        outColor = outColor
    )
        .also { newNebula -> nebulaData[newNebula.id] = newNebula }
}

internal class CombatCustomRenderer : CustomRenderer, BaseEveryFrameCombatPlugin() {
    companion object {
        var instance: CombatCustomRenderer? = null
    }

    override val nebulaData = mutableMapOf<Long, CustomRenderer.Nebula>()
    val effectProjectiles: MutableList<DamagingProjectileAPI> = mutableListOf()

    fun addProjectile(projectile: DamagingProjectileAPI) {
        effectProjectiles.add(projectile)
    }

    internal class CombatLayeredCustomRenderingPlugin
        (private val parentPlugin: CombatCustomRenderer) : BaseCombatLayeredRenderingPlugin() {
        override fun render(layer: CombatEngineLayers, view: ViewportAPI) {
            Global.getCombatEngine() ?: return
            parentPlugin.render(layer)
        }

        override fun getRenderRadius(): Float = 9.9999999E14f
        override fun getActiveLayers(): EnumSet<CombatEngineLayers> = EnumSet.allOf(CombatEngineLayers::class.java)
    }

    override fun init(engine: CombatEngineAPI?) {
        engine ?: return
        instance = this
        init()
        val layerRenderer: CombatLayeredRenderingPlugin = CombatLayeredCustomRenderingPlugin(this)
        engine.addLayeredRenderingPlugin(layerRenderer)
    }

    // Ticking our lifetimes and removing expired
    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine() ?: return
        if (engine.isPaused) return
        advance(amount)

        // Can't do projectile in campaign layer
        if (events != null) {
            // clean up spear list
            val projToRemove: MutableList<DamagingProjectileAPI> = ArrayList()
            effectProjectiles.forEach { if (!engine.isEntityInPlay(it)) projToRemove.add(it) }
            effectProjectiles.removeAll(projToRemove)
        }
    }

    fun render(layer: CombatEngineLayers) {
        nebulaData
            .values
            .filter { it.layer == layer }
            .forEach { renderNebula(it) }
    }
}

internal class CustomPanelCustomRenderer : CustomRenderer {
    override val nebulaData = mutableMapOf<Long, CustomRenderer.Nebula>()

    fun render() {
        nebulaData
            .values
            .forEach { renderNebula(it) }
    }
}

internal class CampaignCustomRenderer : CustomRenderer, BaseCustomEntityPlugin() {
    override val nebulaData = mutableMapOf<Long, CustomRenderer.Nebula>()

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        init()
    }

    override fun advance(amount: Float) {
//        if (game.sector.isPaused) return
        super<CustomRenderer>.advance(amount)
//        game.sector.viewport.isExternalControl = false
    }

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI) {
        nebulaData
            .values
            .filter { mapLayer(it.layer) == layer }
            .forEach {
//                if (!viewport.isNearViewport(it.location, 500f))
//                    return@forEach
                renderNebula(it)
            }
    }

    private fun mapLayer(layer: CombatEngineLayers): CampaignEngineLayers =
        CampaignEngineLayers.ABOVE

    override fun getRenderRange(): Float = 1.0E25f
}