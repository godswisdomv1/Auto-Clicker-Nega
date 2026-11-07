package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.StopReason
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptResolver
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineControllerTest {
    private val frame = SyntheticFrames.flat(10, 10, 5)

    private fun deps(
        clock: FakeClock,
        gestures: FakeGesturePort,
    ) = RunDeps(
        clock,
        gestures,
        { FakeFrameSource.constant(frame) },
        null,
        TemplateProvider { _, _ -> null },
        ScriptResolver { null },
    )

    private fun request(
        vararg steps: Action,
        loops: Int = 0,
    ) = RunRequest(
        TestScripts.simple(*steps).copy(run = RunConfig(loops = loops, countdownSec = 0)),
        TestScripts.screen,
    )

    private val tap = Action.Tap(Target.At(Point(1, 1)), intervalMs = 100)

    @Test
    fun onlyOneRunAtATime() =
        runBlocking {
            val clock = FakeClock()
            val controller = EngineController(deps(clock, FakeGesturePort(clock)), this)
            assertTrue(controller.start(request(tap)) is Outcome.Success)
            clock.settle()
            val second = controller.start(request(tap))
            assertTrue((second as Outcome.Failure).error is AppError.Unexpected)
            controller.stop()
            clock.settle()
            assertEquals(EngineState.Idle, controller.state.value)
            assertTrue(controller.start(request(tap, loops = 1)) is Outcome.Success)
            clock.advanceBy(500)
        }

    @Test
    fun commandsWithoutARunAreHarmless() =
        runBlocking {
            val clock = FakeClock()
            val controller = EngineController(deps(clock, FakeGesturePort(clock)), this)
            assertTrue(controller.pause() is Outcome.Success)
            assertTrue(controller.resume() is Outcome.Success)
            assertTrue(controller.stop() is Outcome.Success)
            assertEquals(EngineState.Idle, controller.state.value)
        }

    @Test
    fun theScreenTurningOffAndLosingTheServiceReachTheRun() =
        runBlocking {
            val clock = FakeClock()
            val controller = EngineController(deps(clock, FakeGesturePort(clock)), this)
            val seen = mutableListOf<EngineEvent>()
            val watcher = launch { controller.events.collect { seen += it } }
            clock.settle()
            controller.start(request(tap))
            clock.advanceBy(250)
            controller.onScreenOff()
            clock.settle()
            assertEquals(EngineEvent.Stopped("s1", StopReason.ScreenOff), seen.last())
            controller.start(request(tap))
            clock.advanceBy(250)
            controller.onServiceLost()
            clock.settle()
            assertEquals(EngineState.ServiceLost, controller.state.value)
            watcher.cancel()
        }

    @Test
    fun countersStartFreshForEachRun() =
        runBlocking {
            val clock = FakeClock()
            val controller = EngineController(deps(clock, FakeGesturePort(clock)), this)
            val withCounter =
                TestScripts.simple(Action.CounterOp("c1", CounterOpKind.Add, 4)).copy(
                    counters = listOf(CounterDef("c1", "C", 0)),
                    run = RunConfig(loops = 1, countdownSec = 0),
                )
            val run = RunRequest(withCounter, TestScripts.screen)
            controller.start(run)
            clock.advanceBy(1_000)
            assertEquals(mapOf("c1" to 4), controller.counters.value)
            controller.start(run)
            clock.advanceBy(1_000)
            assertEquals(mapOf("c1" to 4), controller.counters.value)
        }
}
