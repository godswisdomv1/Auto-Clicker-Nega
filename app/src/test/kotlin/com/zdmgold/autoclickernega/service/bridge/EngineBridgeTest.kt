package com.zdmgold.autoclickernega.service.bridge

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptResolver
import com.zdmgold.autoclickernega.core.port.TemplateProvider
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import com.zdmgold.autoclickernega.service.engine.EngineController
import com.zdmgold.autoclickernega.service.engine.FakeClock
import com.zdmgold.autoclickernega.service.engine.FakeFrameSource
import com.zdmgold.autoclickernega.service.engine.FakeGesturePort
import com.zdmgold.autoclickernega.service.engine.RunDeps
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineBridgeTest {
    private val request =
        RunRequest(
            TestScripts
                .simple(
                    Action.Tap(Target.At(Point(1, 1)), intervalMs = 100),
                ).copy(run = RunConfig(loops = 0, countdownSec = 0)),
            TestScripts.screen,
        )

    private fun controller(
        clock: FakeClock,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = EngineController(
        RunDeps(
            clock,
            FakeGesturePort(clock),
            { FakeFrameSource.constant(SyntheticFrames.flat(10, 10, 5)) },
            null,
            TemplateProvider { _, _ -> null },
            ScriptResolver { null },
        ),
        scope,
    )

    private fun notEnabled(o: Outcome<Unit>) = (o as? Outcome.Failure)?.error === AppError.ServiceNotEnabled

    @Test
    fun withNoServiceTheStateIsServiceLostAndCommandsFailWithATypedError() {
        EngineBridge.unregister()
        assertEquals(EngineState.ServiceLost, EngineBridge.state.value)
        assertTrue(notEnabled(EngineBridge.start(request)))
        assertTrue(notEnabled(EngineBridge.pause()))
        assertTrue(notEnabled(EngineBridge.resume()))
        assertTrue(notEnabled(EngineBridge.stop()))
    }

    @Test
    fun aRegisteredServiceSeesCommandsAndShowsItsState() =
        runBlocking {
            val clock = FakeClock()
            val engine = controller(clock, this)
            EngineBridge.register(engine)
            try {
                assertEquals(EngineState.Idle, EngineBridge.state.value)
                assertTrue(EngineBridge.start(request) is Outcome.Success)
                clock.advanceBy(250)
                assertTrue(EngineBridge.state.value is EngineState.Running)
                assertTrue(EngineBridge.pause() is Outcome.Success)
                assertEquals(EngineState.Paused("s1"), EngineBridge.state.value)
                assertTrue(EngineBridge.resume() is Outcome.Success)
                assertTrue(EngineBridge.stop() is Outcome.Success)
                clock.settle()
                assertEquals(EngineState.Idle, EngineBridge.state.value)
            } finally {
                EngineBridge.unregister()
            }
        }

    @Test
    fun unregisteringReturnsToServiceLostAndStopsForwarding() =
        runBlocking {
            val clock = FakeClock()
            val engine = controller(clock, this)
            EngineBridge.register(engine)
            EngineBridge.start(request)
            clock.advanceBy(250)
            EngineBridge.unregister()
            assertEquals(EngineState.ServiceLost, EngineBridge.state.value)
            assertTrue(EngineBridge.counters.value.isEmpty())
            assertTrue(notEnabled(EngineBridge.stop()))
            engine.stop()
            clock.settle()
            assertEquals(EngineState.ServiceLost, EngineBridge.state.value)
        }

    @Test
    fun aNewServiceReplacesTheOldOne() =
        runBlocking {
            val clock = FakeClock()
            val first = controller(clock, this)
            val second = controller(clock, this)
            EngineBridge.register(first)
            EngineBridge.register(second)
            try {
                assertTrue(EngineBridge.start(request) is Outcome.Success)
                clock.advanceBy(250)
                assertTrue(second.state.value is EngineState.Running)
                assertEquals(EngineState.Idle, first.state.value)
                EngineBridge.stop()
                clock.settle()
            } finally {
                EngineBridge.unregister()
            }
        }

    private class FakeOverlay : com.zdmgold.autoclickernega.core.port.OverlayPort {
        val visible = kotlinx.coroutines.flow.MutableStateFlow(false)
        val crops =
            kotlinx.coroutines.flow.MutableSharedFlow<com.zdmgold.autoclickernega.core.port.CropResult>(
                extraBufferCapacity = 4,
            )
        override val panelVisible = visible
        override val panelCrops = crops

        override fun showPanel(): Outcome<Unit> {
            visible.value = true
            return Outcome.Success(Unit)
        }

        override fun hidePanel(): Outcome<Unit> {
            visible.value = false
            return Outcome.Success(Unit)
        }

        override suspend fun captureCrop(): Outcome<com.zdmgold.autoclickernega.core.port.CropResult?> =
            Outcome.Success(
                com.zdmgold.autoclickernega.core.port.CropResult(
                    com.zdmgold.autoclickernega.core.model
                        .Rect(1, 2, 3, 4),
                    byteArrayOf(9),
                ),
            )
    }

    @Test
    fun withNoOverlayThePanelCommandsFailWithATypedError() =
        runBlocking {
            EngineBridge.unregisterOverlay()
            assertTrue(notEnabled(EngineBridge.showPanel()))
            assertTrue(notEnabled(EngineBridge.hidePanel()))
            assertTrue((EngineBridge.captureCrop() as Outcome.Failure).error === AppError.ServiceNotEnabled)
            assertEquals(false, EngineBridge.panelVisible.value)
        }

    @Test
    fun aRegisteredOverlayIsDrivenThroughTheBridge() =
        runBlocking {
            val overlay = FakeOverlay()
            EngineBridge.registerOverlay(overlay)
            try {
                assertTrue(EngineBridge.showPanel() is Outcome.Success)
                assertEquals(true, EngineBridge.panelVisible.value)
                assertTrue(EngineBridge.hidePanel() is Outcome.Success)
                assertEquals(false, EngineBridge.panelVisible.value)
                val crop = (EngineBridge.captureCrop() as Outcome.Success).value!!
                assertEquals(
                    com.zdmgold.autoclickernega.core.model
                        .Rect(1, 2, 3, 4),
                    crop.box,
                )
            } finally {
                EngineBridge.unregisterOverlay()
            }
            assertEquals(false, EngineBridge.panelVisible.value)
            assertTrue(notEnabled(EngineBridge.showPanel()))
        }
}
