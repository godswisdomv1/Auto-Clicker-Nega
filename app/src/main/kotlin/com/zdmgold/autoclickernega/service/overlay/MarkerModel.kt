package com.zdmgold.autoclickernega.service.overlay

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.Target

/** A numbered marker on the live screen. Circles are taps, arrows are swipes and curves, brackets are image targets. */
sealed interface Marker {
    val number: Int

    /** The step this marker edits, or null for a marker that has not been saved into the script yet. */
    val stepIndex: Int?

    data class Tap(
        override val number: Int,
        override val stepIndex: Int?,
        val at: Point,
    ) : Marker

    data class Swipe(
        override val number: Int,
        override val stepIndex: Int?,
        val from: Point,
        val to: Point,
    ) : Marker

    data class Curve(
        override val number: Int,
        override val stepIndex: Int?,
        val points: List<Point>,
    ) : Marker

    data class Image(
        override val number: Int,
        override val stepIndex: Int?,
        val rect: Rect,
    ) : Marker
}

/** Turns the steps of a Simple script into markers and back. Only the geometry of a step is edited on screen. */
object Markers {
    private const val NEW_SWIPE_MS = 300L
    private const val NEW_CURVE_MS = 400L
    private const val NEW_INTERVAL_MS = 500L

    private fun isManaged(a: Action): Boolean =
        (a is Action.Tap && a.at is Target.At) || a is Action.Swipe || a is Action.Curve

    /** One marker for each tap, swipe and curve step, numbered in order. Other steps have no marker. */
    fun fromSteps(steps: List<Action>): List<Marker> {
        var number = 0
        return steps.mapIndexedNotNull { index, a ->
            if (!isManaged(a)) return@mapIndexedNotNull null
            number++
            when (a) {
                is Action.Tap -> Marker.Tap(number, index, (a.at as Target.At).p)
                is Action.Swipe -> Marker.Swipe(number, index, a.from, a.to)
                is Action.Curve -> Marker.Curve(number, index, a.points)
                else -> null
            }
        }
    }

    /**
     * Writes edited markers back into the steps. A step whose marker was removed is dropped, a moved marker moves its
     * step and keeps its timing, and new markers are added at the end with default timing.
     */
    fun applyTo(
        steps: List<Action>,
        markers: List<Marker>,
    ): List<Action> {
        val byStep = markers.filter { it.stepIndex != null }.associateBy { it.stepIndex }
        val kept =
            steps.mapIndexedNotNull { index, a ->
                when {
                    !isManaged(a) -> a
                    else -> byStep[index]?.let { moved(a, it) }
                }
            }
        return kept + markers.filter { it.stepIndex == null }.mapNotNull(::created)
    }

    private fun moved(
        a: Action,
        m: Marker,
    ): Action =
        when {
            a is Action.Tap && m is Marker.Tap -> a.copy(at = Target.At(m.at))
            a is Action.Swipe && m is Marker.Swipe -> a.copy(from = m.from, to = m.to)
            a is Action.Curve && m is Marker.Curve -> a.copy(points = m.points)
            else -> a
        }

    private fun created(m: Marker): Action? =
        when (m) {
            is Marker.Tap -> Action.Tap(Target.At(m.at))
            is Marker.Swipe -> Action.Swipe(m.from, m.to, NEW_SWIPE_MS, NEW_INTERVAL_MS)
            is Marker.Curve -> Action.Curve(m.points, NEW_CURVE_MS, NEW_INTERVAL_MS)
            is Marker.Image -> null
        }

    /** Renumbers markers 1 to n in their current order. */
    fun renumber(markers: List<Marker>): List<Marker> =
        markers.mapIndexed { i, m ->
            when (m) {
                is Marker.Tap -> m.copy(number = i + 1)
                is Marker.Swipe -> m.copy(number = i + 1)
                is Marker.Curve -> m.copy(number = i + 1)
                is Marker.Image -> m.copy(number = i + 1)
            }
        }

    /** A new tap marker, swipe arrow or curve placed around [center], ready to be dragged into position. */
    fun newTap(
        number: Int,
        center: Point,
    ) = Marker.Tap(number, null, center)

    fun newSwipe(
        number: Int,
        center: Point,
        length: Int,
    ) = Marker.Swipe(number, null, Point(center.x, center.y + length / 2), Point(center.x, center.y - length / 2))

    fun newCurve(
        number: Int,
        center: Point,
        length: Int,
    ): Marker.Curve {
        val half = length / 2
        return Marker.Curve(
            number,
            null,
            listOf(
                Point(center.x - half, center.y + half),
                Point(center.x + half, center.y),
                Point(center.x - half, center.y - half),
            ),
        )
    }

    private const val MIN_IMAGE_SIDE = 24
    private const val CORNERS = 4
    private const val CENTER = 4

    /** Where the draggable handles of a marker are. An image marker has four corners and then its centre. */
    fun handlesOf(m: Marker): List<Point> =
        when (m) {
            is Marker.Tap -> {
                listOf(m.at)
            }

            is Marker.Swipe -> {
                listOf(m.from, m.to)
            }

            is Marker.Curve -> {
                m.points
            }

            is Marker.Image -> {
                listOf(
                    Point(m.rect.l, m.rect.t),
                    Point(m.rect.r, m.rect.t),
                    Point(m.rect.r, m.rect.b),
                    Point(m.rect.l, m.rect.b),
                    m.rect.center,
                )
            }
        }

    /** The marker after its handle [index] was dragged to [to]. A drag that would make an image target too small is ignored. */
    fun moveHandle(
        m: Marker,
        index: Int,
        to: Point,
    ): Marker =
        when (m) {
            is Marker.Tap -> m.copy(at = to)
            is Marker.Swipe -> if (index == 0) m.copy(from = to) else m.copy(to = to)
            is Marker.Curve -> m.copy(points = m.points.mapIndexed { i, p -> if (i == index) to else p })
            is Marker.Image -> m.copy(rect = movedRect(m.rect, index, to))
        }

    private fun movedRect(
        r: Rect,
        index: Int,
        to: Point,
    ): Rect {
        if (index == CENTER) {
            val c = r.center
            val dx = to.x - c.x
            val dy = to.y - c.y
            return Rect(r.l + dx, r.t + dy, r.r + dx, r.b + dy)
        }
        if (index !in 0 until CORNERS) return r
        val fixed = handlesOf(Marker.Image(0, null, r))[(index + 2) % CORNERS]
        val moved = Rect(minOf(to.x, fixed.x), minOf(to.y, fixed.y), maxOf(to.x, fixed.x), maxOf(to.y, fixed.y))
        return if (moved.width < MIN_IMAGE_SIDE || moved.height < MIN_IMAGE_SIDE) r else moved
    }
}
