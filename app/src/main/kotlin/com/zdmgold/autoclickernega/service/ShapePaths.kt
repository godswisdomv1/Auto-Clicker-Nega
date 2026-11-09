package com.zdmgold.autoclickernega.service

import android.graphics.Path
import com.zdmgold.autoclickernega.core.model.Point

/** Paths shared by the gestures that are performed and the markers that preview them, so both follow the same line. */
internal object ShapePaths {
    fun line(
        from: Point,
        to: Point,
    ): Path =
        Path().apply {
            moveTo(from.x.toFloat(), from.y.toFloat())
            lineTo(to.x.toFloat(), to.y.toFloat())
        }

    /** A smooth path through the placed points: each middle point bends the line towards the midpoint after it. */
    fun curve(points: List<Point>): Path {
        val path = Path()
        path.moveTo(points.first().x.toFloat(), points.first().y.toFloat())
        for (i in 1 until points.size - 1) {
            val p = points[i]
            val next = points[i + 1]
            path.quadTo(p.x.toFloat(), p.y.toFloat(), (p.x + next.x) / 2f, (p.y + next.y) / 2f)
        }
        path.lineTo(points.last().x.toFloat(), points.last().y.toFloat())
        return path
    }
}
