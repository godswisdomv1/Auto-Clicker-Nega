package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.error.WaitCancelCause
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.EngineBlockReason
import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.StopReason
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.port.FrameSource
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptResolver
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.script.TestScripts.event
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSessionTest {
    private val target = SyntheticFrames.pattern(48, 32, 7)
    private val scene = SyntheticFrames.embed(SyntheticFrames.background(540, 1200, 1), target, 200, 500)

    private inner class Rig(
        val scripts: Map<String, Script> = emptyMap(),
        latency: Long = 0,
        frames: (Boolean) -> FrameSource = { FakeFrameSource.constant(scene) },
    ) {
        val clock = FakeClock()
        val gestures = FakeGesturePort(clock, latency)
        val sinks =
            com.zdmgold.autoclickernega.service.engine
                .EngineSinks()
        val log = DetectionLog()
        val events = mutableListOf<EngineEvent>()
        val deps =
            RunDeps(
                clock,
                gestures,
                frames,
                null,
                TemplateProvider { _, _ -> target },
                ScriptResolver { scripts[it] },
                log = log,
            )

        fun watch(scope: CoroutineScope): Job = scope.launch { sinks.events.collect { events += it } }

        fun session(
            script: Script,
            scope: CoroutineScope,
            cap: Long? = null,
        ) = RunSession(deps, RunRequest(script, TestScripts.screen, cap, 0), scope, sinks)

        val stopped: EngineEvent.Stopped get() = events.filterIsInstance<EngineEvent.Stopped>().single()
        val state: EngineState get() = sinks.state.value
    }

    private fun tap(
        x: Int,
        interval: Long = 100,
    ) = Action.Tap(Target.At(Point(x, 1)), intervalMs = interval)

    private fun simple(
        vararg steps: Action,
        loops: Int = 1,
        delay: Long = 0,
        countdown: Int = 0,
        timer: Long = 0,
        id: String = "s1",
    ) = TestScripts.simple(*steps, id = id).copy(run = RunConfig(loops, timer, delay, countdown))

    private fun xs(rig: Rig) = rig.gestures.calls.map { ((it.gesture as Action.Tap).at as Target.At).p.x }

    private fun runTest(
        rig: Rig = Rig(),
        block: suspend CoroutineScope.(Rig) -> Unit,
    ) = runBlocking {
        val watcher = rig.watch(this)
        rig.clock.settle()
        block(rig)
        watcher.cancel()
    }

    @Test
    fun aSimpleRunCompletes() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), tap(2), loops = 2), this)
            s.start()
            rig.clock.advanceBy(1_000)
            assertEquals(listOf(0L, 100L, 200L, 300L), rig.gestures.times)
            assertEquals(EngineEvent.Stopped("s1", StopReason.Done), rig.stopped)
            assertEquals(EngineState.Idle, rig.state)
            assertFalse(s.isActive)
        }

    @Test
    fun theStartInXWaitCountsDownAndThenRuns() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), delay = 3_000), this)
            s.start()
            rig.clock.settle()
            assertEquals(EngineState.Waiting("s1", 3_000, 3_000), rig.state)
            rig.clock.advanceTo(1_000)
            assertEquals(EngineState.Waiting("s1", 2_000, 3_000), rig.state)
            rig.clock.advanceTo(2_000)
            assertEquals(EngineState.Waiting("s1", 1_000, 3_000), rig.state)
            assertTrue(rig.gestures.calls.isEmpty())
            rig.clock.advanceTo(3_000)
            assertEquals(listOf(3_000L), rig.gestures.times)
            assertEquals(StopReason.Done, rig.stopped.reason)
        }

    @Test
    fun aTwelveHourWaitEndsOnTimeAndTheRunStarts() =
        runTest { rig ->
            val twelveHours = 43_200_000L
            val s = rig.session(simple(tap(1), delay = twelveHours), this)
            s.start()
            rig.clock.advanceTo(twelveHours - 1)
            assertTrue(rig.state is EngineState.Waiting)
            assertTrue(rig.gestures.calls.isEmpty())
            rig.clock.advanceTo(twelveHours)
            assertEquals(listOf(twelveHours), rig.gestures.times)
            assertEquals(StopReason.Done, rig.stopped.reason)
        }

    @Test
    fun cancellingTheWaitSaysWhyAndNothingRuns() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), delay = 60_000), this)
            s.start()
            rig.clock.advanceTo(10_000)
            s.stop()
            rig.clock.settle()
            assertEquals(EngineEvent.Stopped("s1", StopReason.WaitCancelled, WaitCancelCause.User), rig.stopped)
            assertEquals(EngineState.Idle, rig.state)
            assertTrue(rig.gestures.calls.isEmpty())
            assertEquals(0, rig.clock.pending)
        }

    @Test
    fun theScreenTurningOffCancelsTheWait() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), delay = 60_000), this)
            s.start()
            rig.clock.advanceTo(5_000)
            s.screenOff()
            rig.clock.settle()
            assertEquals(EngineEvent.Stopped("s1", StopReason.WaitCancelled, WaitCancelCause.ScreenOff), rig.stopped)
            assertEquals(EngineState.Idle, rig.state)
        }

    @Test
    fun losingTheServiceCancelsTheWait() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), delay = 60_000), this)
            s.start()
            rig.clock.advanceTo(5_000)
            s.serviceLost()
            rig.clock.settle()
            assertEquals(EngineEvent.Stopped("s1", StopReason.WaitCancelled, WaitCancelCause.ServiceLost), rig.stopped)
            assertEquals(EngineState.ServiceLost, rig.state)
        }

    @Test
    fun theCountdownTicksOncePerSecondBeforeTheRun() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), countdown = 3), this)
            s.start()
            rig.clock.settle()
            assertEquals(EngineState.Countdown("s1", 3), rig.state)
            rig.clock.advanceTo(1_000)
            assertEquals(EngineState.Countdown("s1", 2), rig.state)
            rig.clock.advanceTo(2_000)
            assertEquals(EngineState.Countdown("s1", 1), rig.state)
            rig.clock.advanceTo(3_000)
            assertEquals(listOf(3_000L), rig.gestures.times)
        }

    @Test
    fun theGlobalTimerEndsTheRunMeasuredFromTheRunStart() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), loops = 0, timer = 1_050, delay = 2_000), this)
            s.start()
            rig.clock.advanceBy(10_000)
            assertEquals(StopReason.Timer, rig.stopped.reason)
            assertEquals(11, rig.gestures.calls.size)
            assertEquals(2_000L, rig.gestures.times.first())
            assertEquals(EngineState.Idle, rig.state)
        }

    @Test
    fun theGateCapAndTheTimerEndTheRunAtWhicheverComesFirst() {
        runTest { rig ->
            rig.session(simple(tap(1), loops = 0, timer = 5_000), this, cap = 2_000).start()
            rig.clock.advanceBy(10_000)
            assertEquals(StopReason.Cap, rig.stopped.reason)
            assertEquals("taps at 0 to 1900, none at the cap itself", 20, rig.gestures.calls.size)
        }
        runTest { rig ->
            rig.session(simple(tap(1), loops = 0, timer = 1_000), this, cap = 5_000).start()
            rig.clock.advanceBy(10_000)
            assertEquals(StopReason.Timer, rig.stopped.reason)
        }
    }

    @Test
    fun theCapDoesNotCountTheWait() =
        runTest { rig ->
            rig.session(simple(tap(1), loops = 0, delay = 1_000), this, cap = 500).start()
            rig.clock.advanceBy(10_000)
            assertEquals(StopReason.Cap, rig.stopped.reason)
            assertEquals(1_000L, rig.gestures.times.first())
            assertTrue(rig.gestures.times.all { it <= 1_500 })
        }

    @Test
    fun pausingHoldsTheRunAndResumingContinuesWithoutABurst() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), loops = 0), this)
            s.start()
            rig.clock.advanceBy(350)
            s.pause()
            assertEquals(EngineState.Paused("s1"), rig.state)
            rig.clock.advanceBy(1_000)
            assertEquals(4, rig.gestures.calls.size)
            s.resume()
            assertEquals(EngineState.Running("s1", 0, null), rig.state)
            rig.clock.advanceBy(250)
            val after = rig.gestures.times.drop(4)
            assertTrue(after.isNotEmpty() && after.all { it >= 1_350 })
            assertTrue(after.zipWithNext().all { (a, b) -> b - a == 100L })
            s.stop()
            rig.clock.settle()
        }

    @Test
    fun theUserCanStopARunningScript() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), loops = 0), this)
            s.start()
            rig.clock.advanceBy(250)
            s.stop()
            rig.clock.advanceBy(1_000)
            assertEquals(EngineEvent.Stopped("s1", StopReason.User), rig.stopped)
            assertEquals(EngineState.Idle, rig.state)
            assertEquals(3, rig.gestures.calls.size)
            assertEquals(0, rig.clock.pending)
        }

    @Test
    fun tenRefusedGesturesBlockTheRunWithAVisibleState() =
        runTest { rig ->
            rig.gestures.default = GestureOutcome.Rejected
            rig.session(simple(tap(1), loops = 0), this).start()
            rig.clock.advanceBy(5_000)
            assertEquals(EngineState.Blocked(EngineBlockReason.GesturesRejected), rig.state)
            assertEquals(StopReason.Blocked, rig.stopped.reason)
            val refusals = rig.events.filterIsInstance<EngineEvent.GestureRejected>().map { it.consecutive }
            assertEquals((1..10).toList(), refusals)
        }

    @Test
    fun theStopActionEndsTheRunAsDone() =
        runTest { rig ->
            rig.session(simple(tap(1), Action.Stop, tap(2)), this).start()
            rig.clock.advanceBy(1_000)
            assertEquals(StopReason.Done, rig.stopped.reason)
            assertEquals(listOf(1), xs(rig))
        }

    @Test
    fun theScreenTurningOffStopsARun() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), loops = 0), this)
            s.start()
            rig.clock.advanceBy(250)
            s.screenOff()
            rig.clock.settle()
            assertEquals(EngineEvent.Stopped("s1", StopReason.ScreenOff), rig.stopped)
            assertEquals(EngineState.Idle, rig.state)
        }

    @Test
    fun losingTheServiceStopsARunAndShowsTheServiceState() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), loops = 0), this)
            s.start()
            rig.clock.advanceBy(250)
            s.serviceLost()
            rig.clock.settle()
            assertEquals(StopReason.ServiceLost, rig.stopped.reason)
            assertEquals(EngineState.ServiceLost, rig.state)
        }

    @Test
    fun aChildScriptRunsInsideTheRunAndItsStopOnlyEndsTheChild() {
        val child = simple(tap(3), Action.Stop, tap(4), id = "child")
        runTest(Rig(mapOf("child" to child))) { rig ->
            rig.session(simple(tap(1), Action.RunScript("child"), tap(2)), this).start()
            rig.clock.advanceBy(2_000)
            assertEquals(listOf(1, 3, 2), xs(rig))
            assertEquals(StopReason.Done, rig.stopped.reason)
        }
    }

    @Test
    fun scriptsThatRunEachOtherDoNotLoop() {
        val c = simple(tap(3), Action.RunScript("p"), id = "c")
        runTest(Rig(mapOf("c" to c))) { rig ->
            rig.session(simple(tap(1), Action.RunScript("c"), tap(2), id = "p"), this).start()
            rig.clock.advanceBy(2_000)
            assertEquals(listOf(1, 3, 2), xs(rig))
            assertEquals(StopReason.Done, rig.stopped.reason)
        }
    }

    @Test
    fun nestingStopsAtThreeLevelsBelowTheRoot() {
        val scripts =
            mapOf(
                "a" to simple(Action.RunScript("b"), id = "a"),
                "b" to simple(Action.RunScript("c"), id = "b"),
                "c" to simple(Action.RunScript("d"), tap(30), id = "c"),
                "d" to simple(tap(40), id = "d"),
            )
        runTest(Rig(scripts)) { rig ->
            rig.session(simple(Action.RunScript("a"), id = "root"), this).start()
            rig.clock.advanceBy(2_000)
            assertEquals(listOf(30), xs(rig))
            assertEquals(StopReason.Done, rig.stopped.reason)
        }
    }

    @Test
    fun aMissingChildScriptIsSkipped() =
        runTest { rig ->
            rig.session(simple(Action.RunScript("nope"), tap(2)), this).start()
            rig.clock.advanceBy(1_000)
            assertEquals(listOf(2), xs(rig))
        }

    @Test
    fun aChildThatLoopsForeverRunsOnce() {
        val child = simple(tap(3), loops = 0, id = "child")
        runTest(Rig(mapOf("child" to child))) { rig ->
            rig.session(simple(Action.RunScript("child"), tap(2)), this).start()
            rig.clock.advanceBy(2_000)
            assertEquals(listOf(3, 2), xs(rig))
        }
    }

    private val image = Condition.Image("t1", mode = MatchMode.Fast)

    private fun smartScript(mode: MatchMode = MatchMode.Fast): Script {
        val find =
            event(
                "find",
                1,
                image.copy(mode = mode),
                Condition.Counter("c1", Comparison.Lt, 3),
                actions = listOf(TestScripts.tapOnMatch, Action.CounterOp("c1", CounterOpKind.Add, 1)),
            )
        val done = event("done", 2, Condition.Counter("c1", Comparison.Gte, 3), actions = listOf(Action.Stop))
        return TestScripts.smart(find, done).copy(run = RunConfig(countdownSec = 0))
    }

    private fun finish(
        rig: Rig,
        s: RunSession,
    ) = runBlocking {
        var steps = 0
        while (s.isActive && steps++ < 500) rig.clock.advanceBy(100)
    }

    @Test
    fun aSmartRunFindsTheTargetTapsItAndStopsWhenTheCounterSaysSo() =
        runTest { rig ->
            val s = rig.session(smartScript(), this)
            s.start()
            finish(rig, s)
            assertFalse(s.isActive)
            assertEquals(StopReason.Done, rig.stopped.reason)
            assertEquals(
                listOf(Point(224, 516), Point(224, 516), Point(224, 516)),
                rig.gestures.calls.map {
                    (it.gesture as Action.Tap).at.let { t ->
                        (t as Target.At).p
                    }
                },
            )
            assertEquals(3, rig.sinks.counters.value["c1"])
            assertEquals(
                listOf("find", "find", "find"),
                rig.events.filterIsInstance<EngineEvent.EventFired>().map { it.eventId }.filter {
                    it ==
                        "find"
                },
            )
        }

    @Test
    fun safeModeFiresOnlyAfterTheTargetHeldForTwoFrames() =
        runTest { rig ->
            val s = rig.session(smartScript(MatchMode.Safe), this)
            s.start()
            rig.clock.advanceBy(100)
            assertTrue(rig.gestures.calls.isEmpty())
            rig.clock.advanceBy(200)
            assertEquals(listOf(200L), rig.gestures.times.take(1))
            s.stop()
            rig.clock.settle()
        }

    @Test
    fun aSecureScreenMakesNoTapsAndIsReported() {
        val failure = Outcome.Failure(AppError.CaptureUnavailable(CaptureCause.SecureScreen))
        runTest(Rig(frames = { FakeFrameSource { failure } })) { rig ->
            val s = rig.session(smartScript(), this)
            s.start()
            rig.clock.advanceBy(1_000)
            assertTrue(rig.gestures.calls.isEmpty())
            assertTrue(rig.events.contains(EngineEvent.CaptureUnavailable(CaptureCause.SecureScreen)))
            assertTrue(rig.log.snapshot().all { it.miss == MissReason.CaptureUnavailable })
            assertTrue(s.isActive)
            s.stop()
            rig.clock.settle()
            assertEquals(StopReason.User, rig.stopped.reason)
        }
    }

    @Test
    fun countersArePublishedLive() =
        runTest { rig ->
            val script =
                simple(Action.CounterOp("c1", CounterOpKind.Add, 7), tap(1)).let {
                    it.copy(
                        counters =
                            listOf(
                                com.zdmgold.autoclickernega.core.model
                                    .CounterDef("c1", "C", 1),
                            ),
                    )
                }
            rig.session(script, this).start()
            rig.clock.advanceBy(1_000)
            assertEquals(mapOf("c1" to 8), rig.sinks.counters.value)
        }

    @Test
    fun aSessionRunsOnlyOnce() =
        runTest { rig ->
            val s = rig.session(simple(tap(1)), this)
            s.start()
            assertTrue(runCatching { s.start() }.isFailure)
            rig.clock.advanceBy(1_000)
        }

    @Test
    fun pausingOutsideARunDoesNothing() =
        runTest { rig ->
            val s = rig.session(simple(tap(1), delay = 5_000), this)
            s.start()
            rig.clock.settle()
            s.pause()
            assertTrue(rig.state is EngineState.Waiting)
            s.stop()
            rig.clock.settle()
        }
}
