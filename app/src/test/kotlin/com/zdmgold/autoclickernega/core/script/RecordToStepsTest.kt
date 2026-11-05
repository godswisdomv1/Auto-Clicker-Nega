package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordToStepsTest {
    private fun g(
        kind: RecordedKind,
        from: Point,
        start: Long,
        duration: Long,
        to: Point = from,
    ) = RecordedGesture(kind, from, to, start, duration)

    @Test
    fun turnsGapsIntoStartToStartIntervals() {
        val steps =
            RecordToSteps.toSteps(
                listOf(
                    g(RecordedKind.Tap, Point(10, 20), 0, 60),
                    g(RecordedKind.LongPress, Point(30, 40), 1000, 600),
                    g(RecordedKind.Swipe, Point(1, 1), 1300, 250, Point(9, 9)),
                ),
            )
        assertEquals(
            listOf(
                Action.Tap(Target.At(Point(10, 20)), holdMs = 60, intervalMs = 1000),
                Action.Tap(Target.At(Point(30, 40)), holdMs = 600, intervalMs = 300),
                Action.Swipe(Point(1, 1), Point(9, 9), 250, Limits.DEFAULT_INTERVAL_MS),
            ),
            steps,
        )
    }

    @Test
    fun sortsByStartTime() {
        val steps =
            RecordToSteps.toSteps(
                listOf(g(RecordedKind.Tap, Point(2, 2), 500, 50), g(RecordedKind.Tap, Point(1, 1), 0, 50)),
            )
        assertEquals(Target.At(Point(1, 1)), (steps[0] as Action.Tap).at)
        assertEquals(500L, (steps[0] as Action.Tap).intervalMs)
    }

    @Test
    fun shortTapsAndTinyGapsStayAboveTheFloors() {
        val steps =
            RecordToSteps.toSteps(
                listOf(g(RecordedKind.Tap, Point(1, 1), 0, 5), g(RecordedKind.Tap, Point(2, 2), 10, 5)),
            )
        val first = steps[0] as Action.Tap
        assertEquals(Limits.DEFAULT_HOLD_MS, first.holdMs)
        assertEquals(Limits.MIN_INTERVAL_MS, first.intervalMs)
    }

    @Test
    fun anEmptyRecordingGivesNoSteps() {
        assertTrue(RecordToSteps.toSteps(emptyList()).isEmpty())
    }
}
