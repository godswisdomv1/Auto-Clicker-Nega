package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import kotlin.math.abs

/** Colour conditions: a colour at a point, or anywhere in a region, within a tolerance on every channel. */
object ColorMatcher {
    /** The first matching pixel, or null. A frame without colour data never matches, so no tap is made blind. */
    fun find(
        f: Frame,
        c: Condition.Color,
    ): Point? {
        val rgb = f.rgb ?: return null
        val tolerance = Limits.clampTolerance(c.tolerance)
        val region = c.region
        if (region == null) {
            val pixel = f.colorAt(c.point) ?: return null
            return if (close(pixel, c.rgb, tolerance)) c.point else null
        }
        val area = region.intersect(f.bounds)
        for (y in area.t until area.b) {
            for (x in area.l until area.r) {
                if (close(rgb[y * f.w + x], c.rgb, tolerance)) return Point(x, y)
            }
        }
        return null
    }

    fun matches(
        f: Frame,
        c: Condition.Color,
    ): Boolean = find(f, c) != null

    private fun close(
        a: Int,
        b: Int,
        tolerance: Int,
    ): Boolean =
        abs(((a shr 16) and CHANNEL) - ((b shr 16) and CHANNEL)) <= tolerance &&
            abs(((a shr 8) and CHANNEL) - ((b shr 8) and CHANNEL)) <= tolerance &&
            abs((a and CHANNEL) - (b and CHANNEL)) <= tolerance

    private const val CHANNEL = 0xFF
}
