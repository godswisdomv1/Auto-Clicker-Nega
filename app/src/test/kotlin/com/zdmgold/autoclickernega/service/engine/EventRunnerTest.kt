package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.run.ConditionEvaluator
import com.zdmgold.autoclickernega.core.run.CounterStore
import com.zdmgold.autoclickernega.core.run.EventScheduler
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.run.RunContext
import com.zdmgold.autoclickernega.core.script.EventCompiler
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.script.TestScripts.event
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRunnerTest {
    private val target = SyntheticFrames.pattern(48, 32, 7)
    private val scene = SyntheticFrames.embed(SyntheticFrames.background(540, 1200, 1), target, 200, 500)
    private val empty = SyntheticFrames.background(540, 1200, 2)
    private val image = Condition.Image("t1", mode = MatchMode.Fast)
    private val noMatch = Condition.Counter("c1", Comparison.Eq, 9)

    private class Rig(
        script: Script,
        target: com.zdmgold.autoclickernega.core.vision.Frame,
    ) {
        val clock = FakeClock()
        val gestures = FakeGesturePort(clock)
        val counters = CounterStore(script.counters)
        val plan = (EventCompiler.compile(script) as Outcome.Success).value
        val scheduler = EventScheduler(plan)
        val ctx = RunContext(clock, counters, 0, scheduler::lastFiredAt)
        val log = DetectionLog()
        val executor = ActionExecutor(gestures, counters, { ExecSignal.Continue })
        val fired = mutableListOf<String>()
        var actions = 0
        val runner =
            EventRunner(
                scheduler,
                ConditionEvaluator(mapOf("t1" to target), null),
                ctx,
                executor,
                log,
                clock,
                script.screen,
                script.screen,
                0,
                { fired += it },
                { actions++ },
            )
    }

    private fun rig(vararg events: com.zdmgold.autoclickernega.core.model.Event) =
        Rig(TestScripts.smart(*events), target)

    private val tapOnMatch = TestScripts.tapOnMatch

    private fun center(call: FakeGesturePort.Call) = ((call.gesture as Action.Tap).at as Target.At).p

    @Test
    fun theFirstPassingEventByPriorityRuns() {
        val r =
            rig(
                event("low", 2, image, actions = listOf(Action.Tap(Target.At(Point(9, 9))))),
                event("high", 1, image, actions = listOf(tapOnMatch)),
            )
        runBlocking { r.runner.onFrame(scene, 0) }
        assertEquals(listOf("high"), r.fired)
        assertEquals(listOf(Point(224, 516)), r.gestures.calls.map(::center))
    }

    @Test
    fun aLowerPriorityEventRunsWhenTheHigherOneDoesNotPass() {
        val r =
            rig(
                event("high", 1, noMatch, image, actions = listOf(tapOnMatch)),
                event("low", 2, image, actions = listOf(Action.Tap(Target.At(Point(9, 9))))),
            )
        runBlocking { r.runner.onFrame(scene, 0) }
        assertEquals(listOf("low"), r.fired)
        assertEquals(listOf(Point(9, 9)), r.gestures.calls.map(::center))
    }

    @Test
    fun onlyOneEventRunsPerFrame() {
        val r =
            rig(
                event("a", 1, image, actions = listOf(tapOnMatch)),
                event("b", 2, image, actions = listOf(tapOnMatch)),
            )
        runBlocking { r.runner.onFrame(scene, 0) }
        assertEquals(1, r.gestures.calls.size)
    }

    @Test
    fun theTapLandsOnTheMatchPlusItsOffset() {
        val r = rig(event("a", 1, image, actions = listOf(Action.Tap(Target.AtMatch(Point(5, -3))))))
        runBlocking { r.runner.onFrame(scene, 0) }
        assertEquals(Point(229, 513), center(r.gestures.calls.single()))
    }

    @Test
    fun aCooldownKeepsAnEventFromFiringAgainOnTheSameScreen() {
        val r = rig(event("a", 1, image, actions = listOf(tapOnMatch)).copy(cooldownMs = 500))
        runBlocking {
            r.runner.onFrame(scene, 0)
            r.runner.onFrame(scene, 100)
            r.runner.onFrame(scene, 499)
            assertEquals(1, r.gestures.calls.size)
            r.runner.onFrame(scene, 500)
        }
        assertEquals(2, r.gestures.calls.size)
        assertEquals(listOf("a", "a"), r.fired)
    }

    @Test
    fun aFiredEventIsLoggedWithoutPixelsAndResetsTheCadence() {
        val r = rig(event("a", 1, image, actions = listOf(tapOnMatch)))
        runBlocking { r.runner.onFrame(scene, 42) }
        val entry = r.log.last()!!
        assertEquals("a", entry.eventId)
        assertEquals(42L, entry.frameTimeMs)
        assertEquals(Point(224, 516), entry.tap)
        assertEquals(GestureOutcome.Completed, entry.gesture)
        assertEquals(MissReason.None, entry.miss)
        assertTrue(entry.score!! >= 0.99f)
        assertEquals(1, r.actions)
    }

    @Test
    fun aMissIsLoggedWithItsReasonForEventsThatLookForAnImage() {
        val r = rig(event("a", 1, image, actions = listOf(tapOnMatch)))
        val signal = runBlocking { r.runner.onFrame(empty, 7) }
        assertEquals(ExecSignal.Continue, signal)
        assertTrue(r.gestures.calls.isEmpty())
        val entry = r.log.last()!!
        assertEquals(MissReason.BelowConfidence, entry.miss)
        assertEquals(null, entry.tap)
        assertTrue(r.fired.isEmpty())
    }

    @Test
    fun theActionsOfAnEventRunInOrderOnTheirOwnSchedule() {
        val steps =
            listOf(
                Action.Tap(Target.At(Point(1, 1)), intervalMs = 200),
                Action.CounterOp("c1", CounterOpKind.Add, 1),
                Action.Tap(Target.At(Point(2, 2)), intervalMs = 100),
            )
        val r = rig(event("a", 1, image, actions = steps))
        runBlocking {
            val job = async { r.runner.onFrame(scene, 0) }
            r.clock.advanceBy(1_000)
            job.await()
        }
        assertEquals(listOf(0L, 200L), r.gestures.times)
        assertEquals(1, r.counters.get("c1"))
    }

    @Test
    fun aStopActionEndsTheRun() {
        val r = rig(event("a", 1, image, actions = listOf(Action.Stop, Action.Tap(Target.At(Point(1, 1))))))
        assertEquals(ExecSignal.Stop, runBlocking { r.runner.onFrame(scene, 0) })
        assertTrue(r.gestures.calls.isEmpty())
    }

    @Test
    fun aTapOnAMatchIsDroppedWhenTheEventPassedWithoutOne() {
        val either =
            event("a", 1, image, noMatch, actions = listOf(tapOnMatch, Action.CounterOp("c1", CounterOpKind.Add, 1)))
                .copy(whenExpr = Expr(BoolOp.Or, listOf(image, Condition.Counter("c1", Comparison.Eq, 0))))
        val r = rig(either)
        runBlocking { r.runner.onFrame(empty, 0) }
        assertEquals(listOf("a"), r.fired)
        assertTrue("never a blind tap", r.gestures.calls.isEmpty())
        assertEquals(1, r.counters.get("c1"))
    }

    @Test
    fun refusedGesturesAreRecordedInTheLog() {
        val r = rig(event("a", 1, image, actions = listOf(tapOnMatch)))
        r.gestures.default = GestureOutcome.Rejected
        runBlocking { r.runner.onFrame(scene, 0) }
        assertEquals(GestureOutcome.Rejected, r.log.last()!!.gesture)
    }
}
