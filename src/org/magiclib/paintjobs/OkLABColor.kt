package org.magiclib.paintjobs

import com.fs.starfarer.api.util.Misc
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow


class LinearSRBG internal constructor(val R: Float, val G: Float, val B: Float)

class OKLab internal constructor(val L: Float, val a: Float, val b: Float)


fun OKLabInterpolateColor(from: Color, to: Color, progress: Float): Color {
    var progress = progress
    progress = min(max(0.0, progress.toDouble()), 1.0).toFloat()
    val OKLabFrom = sRBGLinearToOKLab(sRBGLinear(from))
    val OKLabTo = sRBGLinearToOKLab(sRBGLinear(to))
    val out = OKLab(
        Misc.interpolate(OKLabFrom.L, OKLabTo.L, progress),
        Misc.interpolate(OKLabFrom.a, OKLabTo.a, progress),
        Misc.interpolate(OKLabFrom.b, OKLabTo.b, progress)
    )
    return sRBG(OKLabToLinearSRBG(out), Misc.interpolate(from.alpha / 255f, to.alpha / 255f, progress))
}

fun sRBGLinearToOKLab(c: LinearSRBG): OKLab {
    val l = 0.4122214708f * c.R + 0.5363325363f * c.G + 0.0514459929f * c.B
    val m = 0.2119034982f * c.R + 0.6806995451f * c.G + 0.1073969566f * c.B
    val s = 0.0883024619f * c.R + 0.2817188376f * c.G + 0.6299787005f * c.B
    val l_ = Math.cbrt(l.toDouble()).toFloat()
    val m_ = Math.cbrt(m.toDouble()).toFloat()
    val s_ = Math.cbrt(s.toDouble()).toFloat()
    return OKLab(
        0.2104542553f * l_ + 0.7936177850f * m_ - 0.0040720468f * s_,
        1.9779984951f * l_ - 2.4285922050f * m_ + 0.4505937099f * s_,
        0.0259040371f * l_ + 0.7827717662f * m_ - 0.8086757660f * s_
    )
}

fun OKLabToLinearSRBG(c: OKLab): LinearSRBG {
    val l_ = c.L + 0.3963377774f * c.a + 0.2158037573f * c.b
    val m_ = c.L - 0.1055613458f * c.a - 0.0638541728f * c.b
    val s_ = c.L - 0.0894841775f * c.a - 1.2914855480f * c.b
    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_
    return LinearSRBG(
        4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
        -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
        -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    )
}

fun sRBGLinear(`in`: Color): LinearSRBG {
    return LinearSRBG(
        sRBGLinearSingle(`in`.red / 255f),
        sRBGLinearSingle(`in`.green / 255f),
        sRBGLinearSingle(`in`.blue / 255f)
    )
}

fun sRBG(`in`: LinearSRBG, alpha: Float): Color {
    return Color(
        min(max(0.0, sRBGSingle(`in`.R).toDouble()), 1.0).toFloat(),
        min(max(0.0, sRBGSingle(`in`.G).toDouble()), 1.0).toFloat(),
        min(max(0.0, sRBGSingle(`in`.B).toDouble()), 1.0).toFloat(),
        alpha
    )
}

private fun sRBGLinearSingle(x: Float): Float {
    return if (x >= 0.0031308) (1.055 * x.toDouble().pow(1.0 / 2.4) - 0.055).toFloat() else 12.92f * x
}

private fun sRBGSingle(x: Float): Float {
    return if (x >= 0.04045) ((x + 0.055) / 1.055).pow(2.4).toFloat() else x / 12.92f
}