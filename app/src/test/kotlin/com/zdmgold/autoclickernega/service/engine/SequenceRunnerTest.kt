package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.run.CounterStore
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceRunnerTest {
    private val clock = FakeClock()
    private val screen = TestScripts.screen

    private fun tap(
        x: Int = 1,
        interval: Long = 100,
    ) = Action.Tap(Target.At(Point(x, 1)), intervalMs = interval)

    private fun script(
        loops: Int,
        vararg steps: Action,
    ) = TestScripts.simple(*steps).copy(run = RunConfig(loops = loops, countdownSec = 0))

    private class Rig(
        clock: FakeClock,
        latency: Long,
        counters: CounterStore = CounterStore(),
    ) {
        val gestures = FakeGesturePort(clock, latency)
        val executor = ActionExecutor(gestures, counters, { ExecSignal.Continue })
    }

    private fun runFor(
        script: com.zdmgold.autoclickernega.core.model.Script,
        latency: Long = 0,
        advance: Long = 10_000,
        loopsOverride: Int? = null,
        counters: CounterStore = CounterStore(),
        resumed: suspend () -> Unit = {},
    ): Pair<Rig, ExecSignal> =
        runBlocking {
            val rig = Rig(clock, latency, counters)
            val job = async { SequenceRunner(clock, rig.executor, resumed).run(script, screen, 0, loopsOverride) }
            clock.advanceBy(advance)
            rig to job.await()
        }

    @Test
    fun stepsStartOnAFixedScheduleEvenWhenGesturesTakeTime() {
        val (rig, signal) = runFor(script(3, tap()), latency = 30)
        assertEquals(listOf(0L, 100L, 200L), rig.gestures.times)
        assertEquals(ExecSignal.Continue, signal)
    }

    @Test
    fun aRunThatFallsBehindDoesNotBurstToCatchUp() {
        val (rig, _) = runFor(script(3, tap()), latency = 350)
        assertEquals(listOf(0L, 350L, 700L), rig.gestures.times)
    }

    @Test
    fun aWaitStepDelaysTheNextStep() {
        val (rig, _) = runFor(script(1, tap(1), Action.Wait(500), tap(2)))
        assertEquals(listOf(0L, 600L), rig.gestures.times)
    }

    @Test
    fun loopsRepeatTheWholeList() {
        val (rig, _) = runFor(script(2, tap(1), tap(2)))
        assertEquals(listOf(0L, 100L, 200L, 300L), rig.gestures.times)
        assertEquals(listOf(1, 2, 1, 2), rig.gestures.calls.map { ((it.gesture as Action.Tap).at as Target.At).p.x })
    }

    @Test
    fun zeroLoopsRunsUntilCancelled() {
        runBlocking {
            val rig = Rig(clock, 0)
            val job = async { SequenceRunner(clock, rig.executor).run(script(0, tap()), screen, 0) }
            clock.advanceBy(1_000)
            assertEquals(11, rig.gestures.calls.size)
            job.cancel()
            clock.settle()
            assertEquals(0, clock.pending)
        }
    }

    @Test
    fun aLoopsOverrideReplacesTheScriptsLoops() {
        val (rig, _) = runFor(script(0, tap(), tap()), loopsOverride = 1)
        assertEquals(2, rig.gestures.calls.size)
    }

    @Test
    fun counterStepsRunWithoutAGesture() {
        val counters = CounterStore(listOf(CounterDef("c1", "C", 0)))
        val steps = arrayOf(Action.CounterOp("c1", CounterOpKind.Add, 5), tap())
        val (rig, _) = runFor(script(1, *steps), counters = counters)
        assertEquals(5, counters.get("c1"))
        assertEquals(1, rig.gestures.calls.size)
    }

    @Test
    fun theStopActionEndsTheRunImmediately() {
        val (rig, signal) = runFor(script(5, tap(1), Action.Stop, tap(2)))
        assertEquals(ExecSignal.Stop, signal)
        assertEquals(1, rig.gestures.calls.size)
    }

    @Test
    fun tenRefusedGesturesInARowBlockTheRun() {
        runBlocking {
            val rig = Rig(clock, 0)
            rig.gestures.default = GestureOutcome.Rejected
            val job = async { SequenceRunner(clock, rig.executor).run(script(0, tap()), screen, 0) }
            clock.advanceBy(5_000)
            assertEquals(ExecSignal.Blocked, job.await())
            assertEquals(10, rig.gestures.calls.size)
        }
    }

    @Test
    fun aCompletedGestureResetsTheRefusalCount() {
        runBlocking {
            val rig = Rig(clock, 0)
            repeat(9) { rig.gestures.queued.addLast(GestureOutcome.Rejected) }
            rig.gestures.queued.addLast(GestureOutcome.Completed)
            repeat(9) { rig.gestures.queued.addLast(GestureOutcome.Rejected) }
            val job = async { SequenceRunner(clock, rig.executor).run(script(19, tap()), screen, 0) }
            clock.advanceBy(10_000)
            assertEquals(ExecSignal.Continue, job.await())
            assertEquals(19, rig.gestures.calls.size)
        }
    }

    @Test
    fun theRunWaitsWhilePausedAndDoesNotBurstAfterwards() {
        runBlocking {
            val rig = Rig(clock, 0)
            var gate = CompletableDeferred<Unit>().also { it.complete(Unit) }
            val job = async { SequenceRunner(clock, rig.executor) { gate.await() }.run(script(0, tap()), screen, 0) }
            clock.advanceBy(350)
            assertEquals(listOf(0L, 100L, 200L, 300L), rig.gestures.times)
            gate = CompletableDeferred()
            clock.advanceBy(1_000)
            assertEquals(4, rig.gestures.calls.size)
            gate.complete(Unit)
            clock.advanceBy(250)
            val after = rig.gestures.times.drop(4)
            assertTrue(after.all { it >= 1_350 })
            assertTrue(after.zipWithNext().all { (a, b) -> b - a == 100L })
            job.cancel()
        }
    }

    @Test
    fun anEmptyScriptEndsAtOnce() {
        val (rig, signal) = runFor(script(0))
        assertEquals(ExecSignal.Continue, signal)
        assertTrue(rig.gestures.calls.isEmpty())
    }
}
