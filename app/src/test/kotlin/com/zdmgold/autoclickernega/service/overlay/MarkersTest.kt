package com.zdmgold.autoclickernega.service.overlay

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkersTest {
    private val tap = Action.Tap(Target.At(Point(10, 20)), holdMs = 60, intervalMs = 250)
    private val swipe = Action.Swipe(Point(0, 0), Point(5, 5), 300, 400)
    private val curve = Action.Curve(listOf(Point(1, 1), Point(2, 2), Point(3, 3)), 500, 600)
    private val steps = listOf(tap, Action.Wait(100), swipe, Action.Stop, curve)

    @Test
    fun everyTapSwipeAndCurveGetsANumberedMarker() {
        val markers = Markers.fromSteps(steps)
        assertEquals(
            listOf(
                Marker.Tap(1, 0, Point(10, 20)),
                Marker.Swipe(2, 2, Point(0, 0), Point(5, 5)),
                Marker.Curve(3, 4, listOf(Point(1, 1), Point(2, 2), Point(3, 3))),
            ),
            markers,
        )
    }

    @Test
    fun aTapOnAMatchHasNoMarkerBecauseItHasNoFixedPlace() {
        assertTrue(Markers.fromSteps(listOf(Action.Tap(Target.AtMatch(Point(0, 0))))).isEmpty())
    }

    @Test
    fun unchangedMarkersLeaveTheStepsAsTheyWere() {
        assertEquals(steps, Markers.applyTo(steps, Markers.fromSteps(steps)))
    }

    @Test
    fun movingAMarkerMovesItsStepAndKeepsItsTiming() {
        val markers = Markers.fromSteps(steps).map { if (it is Marker.Tap) it.copy(at = Point(99, 88)) else it }
        val out = Markers.applyTo(steps, markers)
        assertEquals(tap.copy(at = Target.At(Point(99, 88))), out[0])
        assertEquals(60L, (out[0] as Action.Tap).holdMs)
        assertEquals(250L, (out[0] as Action.Tap).intervalMs)
    }

    @Test
    fun removingAMarkerRemovesOnlyItsStep() {
        val markers = Markers.fromSteps(steps).filterNot { it is Marker.Swipe }
        assertEquals(listOf(tap, Action.Wait(100), Action.Stop, curve), Markers.applyTo(steps, markers))
    }

    @Test
    fun newMarkersAreAppendedWithDefaultTiming() {
        val added =
            Markers.fromSteps(steps) + Markers.newTap(4, Point(7, 8)) + Markers.newSwipe(5, Point(100, 100), 200)
        val out = Markers.applyTo(steps, added)
        assertEquals(steps.size + 2, out.size)
        assertEquals(Action.Tap(Target.At(Point(7, 8))), out[steps.size])
        val s = out.last() as Action.Swipe
        assertEquals(Point(100, 200), s.from)
        assertEquals(Point(100, 0), s.to)
        assertEquals(500L, s.intervalMs)
    }

    @Test
    fun aNewCurveHasThreePointsAroundTheCentre() {
        val c = Markers.newCurve(1, Point(100, 100), 80)
        assertEquals(3, c.points.size)
        assertTrue(Markers.applyTo(emptyList(), listOf(c)).single() is Action.Curve)
    }

    @Test
    fun imageMarkersAreNotWrittenIntoSteps() {
        val image =
            Marker.Image(
                1,
                null,
                com.zdmgold.autoclickernega.core.model
                    .Rect(0, 0, 10, 10),
            )
        assertTrue(Markers.applyTo(emptyList(), listOf(image)).isEmpty())
    }

    @Test
    fun renumberingFollowsTheCurrentOrder() {
        val markers = listOf(Markers.newTap(9, Point(1, 1)), Markers.newSwipe(4, Point(2, 2), 10))
        assertEquals(listOf(1, 2), Markers.renumber(markers).map { it.number })
    }

    @Test
    fun everyKindOfMarkerExposesItsHandles() {
        assertEquals(listOf(Point(1, 2)), Markers.handlesOf(Marker.Tap(1, null, Point(1, 2))))
        assertEquals(
            listOf(Point(0, 0), Point(5, 5)),
            Markers.handlesOf(Marker.Swipe(1, null, Point(0, 0), Point(5, 5))),
        )
        assertEquals(3, Markers.handlesOf(Marker.Curve(1, null, listOf(Point(1, 1), Point(2, 2), Point(3, 3)))).size)
        val image =
            Marker.Image(
                1,
                null,
                com.zdmgold.autoclickernega.core.model
                    .Rect(10, 20, 110, 220),
            )
        assertEquals(
            listOf(Point(10, 20), Point(110, 20), Point(110, 220), Point(10, 220), Point(60, 120)),
            Markers.handlesOf(image),
        )
    }

    @Test
    fun draggingAHandleMovesThatPartOfTheMarker() {
        assertEquals(Marker.Tap(1, 3, Point(9, 9)), Markers.moveHandle(Marker.Tap(1, 3, Point(1, 1)), 0, Point(9, 9)))
        val swipe = Marker.Swipe(1, null, Point(0, 0), Point(5, 5))
        assertEquals(Point(7, 7), (Markers.moveHandle(swipe, 1, Point(7, 7)) as Marker.Swipe).to)
        assertEquals(Point(0, 0), (Markers.moveHandle(swipe, 1, Point(7, 7)) as Marker.Swipe).from)
        val curve = Marker.Curve(1, null, listOf(Point(1, 1), Point(2, 2), Point(3, 3)))
        assertEquals(
            listOf(Point(1, 1), Point(8, 8), Point(3, 3)),
            (Markers.moveHandle(curve, 1, Point(8, 8)) as Marker.Curve).points,
        )
    }

    @Test
    fun draggingACornerResizesAnImageTargetAroundTheOppositeCorner() {
        val rect =
            com.zdmgold.autoclickernega.core.model
                .Rect(100, 100, 200, 200)
        val image = Marker.Image(1, null, rect)
        assertEquals(
            com.zdmgold.autoclickernega.core.model
                .Rect(50, 60, 200, 200),
            (Markers.moveHandle(image, 0, Point(50, 60)) as Marker.Image).rect,
        )
        assertEquals(
            com.zdmgold.autoclickernega.core.model
                .Rect(100, 100, 300, 260),
            (Markers.moveHandle(image, 2, Point(300, 260)) as Marker.Image).rect,
        )
    }

    @Test
    fun draggingACornerPastTheOppositeOneFlipsTheRectangle() {
        val image =
            Marker.Image(
                1,
                null,
                com.zdmgold.autoclickernega.core.model
                    .Rect(100, 100, 200, 200),
            )
        val flipped = (Markers.moveHandle(image, 0, Point(300, 300)) as Marker.Image).rect
        assertEquals(
            com.zdmgold.autoclickernega.core.model
                .Rect(200, 200, 300, 300),
            flipped,
        )
    }

    @Test
    fun anImageTargetCannotBeDraggedTooSmall() {
        val image =
            Marker.Image(
                1,
                null,
                com.zdmgold.autoclickernega.core.model
                    .Rect(100, 100, 200, 200),
            )
        assertEquals(image, Markers.moveHandle(image, 0, Point(190, 190)))
    }

    @Test
    fun draggingTheCentreMovesTheWholeImageTarget() {
        val image =
            Marker.Image(
                1,
                null,
                com.zdmgold.autoclickernega.core.model
                    .Rect(100, 100, 200, 200),
            )
        val moved = Markers.moveHandle(image, 4, Point(250, 300)) as Marker.Image
        assertEquals(
            com.zdmgold.autoclickernega.core.model
                .Rect(200, 250, 300, 350),
            moved.rect,
        )
    }
}
