package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import kotlin.math.roundToInt

/** Rescales points, regions and templates when the screen size or density differs from the authoring screen. */
object CoordinateScaler {
    fun scale(
        p: Point,
        from: ScreenSpec,
        to: ScreenSpec,
    ): Point =
        Point(
            scaleAxis(p.x, from.w, to.w),
            scaleAxis(p.y, from.h, to.h),
        )

    fun scale(
        r: Rect,
        from: ScreenSpec,
        to: ScreenSpec,
    ): Rect =
        Rect(
            scaleAxis(r.l, from.w, to.w),
            scaleAxis(r.t, from.h, to.h),
            scaleAxis(r.r, from.w, to.w),
            scaleAxis(r.b, from.h, to.h),
        )

    /** An offset such as a match anchor follows the density ratio, since it is a distance on a target. */
    fun scaleOffset(
        p: Point,
        from: ScreenSpec,
        to: ScreenSpec,
    ): Point {
        val ratio = densityRatio(from, to)
        return Point((p.x * ratio).roundToInt(), (p.y * ratio).roundToInt())
    }

    /** How much a reference image must be resized: the density of the running screen over the authoring one. */
    fun densityRatio(
        from: ScreenSpec,
        to: ScreenSpec,
    ): Float = if (from.density <= 0 || to.density <= 0) 1f else to.density.toFloat() / from.density

    /** Rotating the phone changes coordinates, so a script made in the other orientation needs a warning. */
    fun orientationMismatch(
        from: ScreenSpec,
        to: ScreenSpec,
    ): Boolean = from.isLandscape != to.isLandscape

    private fun scaleAxis(
        value: Int,
        from: Int,
        to: Int,
    ): Int = if (from <= 0 || from == to) value else (value.toDouble() * to / from).roundToInt()
}
