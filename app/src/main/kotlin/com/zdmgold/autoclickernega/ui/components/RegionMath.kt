package com.zdmgold.autoclickernega.ui.components

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import kotlin.math.hypot

/** The geometry behind the region picker, kept free of drawing so it can be tested on its own. */
object RegionMath {
    const val MIN_SIDE = 24
    const val NONE = -1
    const val MOVE = -2
    private const val CORNER_COUNT = 4
    private const val OPPOSITE = 2

    fun normalized(
        ax: Int,
        ay: Int,
        bx: Int,
        by: Int,
    ) = Rect(minOf(ax, bx), minOf(ay, by), maxOf(ax, bx), maxOf(ay, by))

    fun isUsable(r: Rect): Boolean = r.width >= MIN_SIDE && r.height >= MIN_SIDE

    fun corners(r: Rect): List<Point> = listOf(Point(r.l, r.t), Point(r.r, r.t), Point(r.r, r.b), Point(r.l, r.b))

    /** The corner within [radius] of the point, 0 to 3, or [NONE]. */
    fun hitCorner(
        r: Rect,
        x: Float,
        y: Float,
        radius: Float,
    ): Int = corners(r).indexOfFirst { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= radius }

    fun contains(
        r: Rect,
        x: Float,
        y: Float,
    ): Boolean = x >= r.l && x <= r.r && y >= r.t && y <= r.b

    fun shifted(
        r: Rect,
        dx: Int,
        dy: Int,
        screen: ScreenSpec,
    ): Rect {
        val nx = dx.coerceIn(-r.l, screen.w - r.r)
        val ny = dy.coerceIn(-r.t, screen.h - r.b)
        return Rect(r.l + nx, r.t + ny, r.r + nx, r.b + ny)
    }

    /** Resizes by dragging [corner] to the point, keeping the opposite corner fixed and staying on screen. */
    fun resized(
        r: Rect,
        corner: Int,
        x: Int,
        y: Int,
        screen: ScreenSpec,
    ): Rect {
        val fixed = corners(r)[(corner + OPPOSITE) % CORNER_COUNT]
        return normalized(fixed.x, fixed.y, x.coerceIn(0, screen.w), y.coerceIn(0, screen.h))
    }

    /** A region drawn from the point where the drag began to the current point, kept on screen. */
    fun drawn(
        startX: Int,
        startY: Int,
        x: Int,
        y: Int,
        screen: ScreenSpec,
    ): Rect =
        normalized(
            startX.coerceIn(0, screen.w),
            startY.coerceIn(0, screen.h),
            x.coerceIn(0, screen.w),
            y.coerceIn(0, screen.h),
        )
}
