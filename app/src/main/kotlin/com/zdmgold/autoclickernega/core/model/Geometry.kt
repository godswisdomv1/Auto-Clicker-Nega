package com.zdmgold.autoclickernega.core.model

data class Point(
    val x: Int,
    val y: Int,
)

data class Rect(
    val l: Int,
    val t: Int,
    val r: Int,
    val b: Int,
) {
    val width: Int get() = r - l
    val height: Int get() = b - t
    val isEmpty: Boolean get() = width <= 0 || height <= 0
    val center: Point get() = Point((l + r) / 2, (t + b) / 2)

    fun contains(p: Point): Boolean = p.x in l until r && p.y in t until b

    /** The part of this rectangle that lies inside [bounds], or an empty rectangle when they do not meet. */
    fun intersect(bounds: Rect): Rect {
        val out = Rect(maxOf(l, bounds.l), maxOf(t, bounds.t), minOf(r, bounds.r), minOf(b, bounds.b))
        return if (out.isEmpty) Rect(0, 0, 0, 0) else out
    }
}

/** One finger of a multi-finger gesture. */
data class Stroke(
    val from: Point,
    val to: Point,
)
