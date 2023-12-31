package org.magiclib.achievements

import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getDistance
import org.magiclib.kotlin.random
import java.awt.Rectangle
import kotlin.random.Random

internal class MagicAchievementParticleScript {
    private val customRenderer: CustomPanelCustomRenderer = CustomPanelCustomRenderer()
    private val baseMinInterval = 0.03f
    private val baseMaxInterval = 0.04f

    private var objDensityInternal: IntervalUtil =
        IntervalUtil(baseMinInterval, baseMaxInterval)
            .apply { forceIntervalElapsed() }

    fun advance(amount: Float) {
        customRenderer.advance(amount)
        objDensityInternal.advance(amount)
    }

    /**
     * If [initialSeed] is true, the particle will appear already in-flight, rather than always at the start and fading in.
     * Useful for when you want to have a bunch of particles look like they're already in the middle of their animation.
     */
    @JvmOverloads
    fun render(
        rect: Rectangle,
        anchor: Vector2f,
        achievement: MagicAchievement,
        initialSeed: Boolean = false
    ) {
        customRenderer.render()

        val objDensityInternalRef = objDensityInternal

        if (!initialSeed && !objDensityInternalRef.intervalElapsed()) return
//        if (achievement.rarity == MagicAchievementRarity.Common) return

        val lines = listOf(
            Vector2f(rect.x.toFloat(), rect.y.toFloat()) to Vector2f(
                rect.x.toFloat(),
                rect.y.toFloat() + rect.height.toFloat()
            ),
            Vector2f(
                rect.x.toFloat(),
                rect.y.toFloat() + rect.height.toFloat()
            ) to Vector2f(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat()),
            Vector2f(
                rect.x.toFloat() + rect.width.toFloat(),
                rect.y.toFloat() + rect.height.toFloat()
            ) to Vector2f(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat()),
            Vector2f(rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat()) to Vector2f(
                rect.x.toFloat(),
                rect.y.toFloat()
            )
        )

        val visualEffectData = achievement.visualEffectData
        val velocityScale = .015f
        val sizeScale = 0.75f
        val durationScale = 4f
        val rampUpScale = 2.0f
        val rampDownScale = 1.0f
        val endSizeScale = 1.55f
        val densityScale = 8f * (1f / visualEffectData.particleDensity) // Lower is more dense
        val vel = Vector2f(100f * velocityScale, 100f * velocityScale)
            .rotate(Random.nextFloat() * 360f)

        // If density changed, reset interval to new density.
        if (objDensityInternal.minInterval != (baseMinInterval * densityScale)
            || objDensityInternal.maxInterval != (baseMaxInterval * densityScale)
        ) {
            objDensityInternal = IntervalUtil(baseMinInterval * densityScale, baseMaxInterval * densityScale)
        }

//        lines.forEach { line ->
        customRenderer.addNebula(
            location = getRandomPointInRectangleAvoidingCenter(
                rect,
                avoidCenterRadius = 15f
            ), // MathUtils.getRandomPointOnLine(line.first, line.second),
            anchorLocation = anchor,
            velocity = vel,
            size = 5f * sizeScale,
            endSizeMult = endSizeScale,
            duration = (1.2f..1.5f).random() * durationScale,
            inFraction = 0.1f * rampUpScale,
            outFraction = 0.5f * rampDownScale,
            color = visualEffectData.color,
            type = CustomRenderer.NebulaType.NORMAL,
            negative = false
        )
            .apply {
                if (initialSeed)
                    lifetime = Random.nextDouble(0f.toDouble(), duration.toDouble()).toFloat()
            }
//        }
    }
}

// method that gets a random point in a rectange but avoids the center
fun getRandomPointInRectangleAvoidingCenter(rect: Rectangle, avoidCenterRadius: Float): Vector2f {
    val x = Random.nextInt(rect.x, rect.x + rect.width)
    val y = Random.nextInt(rect.y, rect.y + rect.height)
    val point = Vector2f(x.toFloat(), y.toFloat())
    if (avoidCenterRadius > 0f) {
        val center =
            Vector2f(rect.x.toFloat() + rect.width.toFloat() / 2f, rect.y.toFloat() + rect.height.toFloat() / 2f)
        if (point.getDistance(center) < avoidCenterRadius) {
            return getRandomPointInRectangleAvoidingCenter(rect, avoidCenterRadius)
        }
    }

    return point
}

fun getRandomPointInRectangle(rect: Rectangle): Vector2f {
    val x = Random.nextInt(rect.x, rect.x + rect.width)
    val y = Random.nextInt(rect.y, rect.y + rect.height)
    return Vector2f(x.toFloat(), y.toFloat())
}
