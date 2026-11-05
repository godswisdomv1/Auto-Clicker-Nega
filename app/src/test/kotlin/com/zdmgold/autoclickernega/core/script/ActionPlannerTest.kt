package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.script.TestScripts.simple
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ActionPlannerTest {
    private val same = TestScripts.screen
    private val small = ScreenSpec(720, 1600, 280)

    private fun tapAt(step: PlannedStep) = ((step.action as Action.Tap).at as Target.At).p

    @Test
    fun keepsTheOrderOfTheSteps() {
        val plan =
            ActionPlanner.plan(
                simple(Action.Tap(Target.At(Point(1, 1))), Action.Wait(500), Action.Stop),
                same,
            )
        assertEquals(listOf(0, 1, 2), plan.map { it.index })
        assertEquals(listOf(100L, 500L, 0L), plan.map { it.intervalMs })
    }

    @Test
    fun scalesCoordinatesToTheRunningScreen() {
        val script =
            simple(
                Action.Tap(Target.At(Point(540, 1200))),
                Action.Swipe(Point(540, 1800), Point(540, 600), 300, 200),
                Action.Curve(listOf(Point(0, 0), Point(540, 1200), Point(1080, 2400)), 300, 200),
                Action.Multi(
                    listOf(Stroke(Point(540, 1200), Point(0, 0)), Stroke(Point(0, 0), Point(1080, 2400))),
                    300,
                    200,
                ),
            )
        val plan = ActionPlanner.plan(script, small)
        assertEquals(Point(360, 800), tapAt(plan[0]))
        assertEquals(Point(360, 1200), (plan[1].action as Action.Swipe).from)
        assertEquals(Point(719, 1599), (plan[2].action as Action.Curve).points.last())
        assertEquals(Point(360, 800), (plan[3].action as Action.Multi).strokes.first().from)
    }

    @Test
    fun keepsPointsInsideTheScreen() {
        val plan = ActionPlanner.plan(simple(Action.Tap(Target.At(Point(5000, -40)))), same)
        assertEquals(Point(1079, 0), tapAt(plan.single()))
    }

    @Test
    fun jitterIsBoundedAndReproducible() {
        val script = simple(Action.Tap(Target.At(Point(540, 1200)), jitterPx = 5))
        val seen = (0L..40L).map { tapAt(ActionPlanner.plan(script, same, seed = it).single()) }
        assertTrue(seen.all { abs(it.x - 540) <= 5 && abs(it.y - 1200) <= 5 })
        assertTrue(seen.toSet().size > 1)
        assertEquals(seen, (0L..40L).map { tapAt(ActionPlanner.plan(script, same, seed = it).single()) })
    }

    @Test
    fun timingJitterStaysInRangeAndNeverGoesBelowTheFloor() {
        val script = simple(Action.Tap(Target.At(Point(1, 1)), intervalMs = 100, timingJitterMs = 30))
        val intervals = (0L..40L).map { ActionPlanner.plan(script, same, seed = it).single().intervalMs }
        assertTrue(intervals.all { it in 70L..130L })
        assertTrue(intervals.toSet().size > 1)
        val fast = simple(Action.Tap(Target.At(Point(1, 1)), intervalMs = 10))
        assertEquals(Limits.MIN_INTERVAL_MS, ActionPlanner.plan(fast, same).single().intervalMs)
    }

    @Test
    fun eachLoopGetsItsOwnJitter() {
        val script = simple(Action.Tap(Target.At(Point(540, 1200)), jitterPx = 50))
        val loops = (0..20).map { tapAt(ActionPlanner.plan(script, same, seed = 7, iteration = it).single()) }
        assertTrue(loops.toSet().size > 1)
    }

    @Test
    fun matchOffsetsFollowTheDensityRatio() {
        val plan = ActionPlanner.plan(simple(Action.Tap(Target.AtMatch(Point(6, -6)))), small)
        assertEquals(Target.AtMatch(Point(4, -4)), (plan.single().action as Action.Tap).at)
    }

    @Test
    fun aSmartScriptHasNoSimplePlan() {
        assertTrue(ActionPlanner.plan(TestScripts.smart(TestScripts.event("e", 1, TestScripts.image)), same).isEmpty())
    }
}
