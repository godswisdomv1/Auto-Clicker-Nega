package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.run.CadenceController
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionPipelineTest {
    private val clock = FakeClock()
    private val a = SyntheticFrames.background(200, 400, 1)
    private val b = SyntheticFrames.background(200, 400, 2)

    private fun times(events: List<FrameEvent>) = events.filterIsInstance<FrameEvent.Captured>().map { it.timeMs }

    @Test
    fun capturesStartToStartAndBacksOffOnAStillScreen() =
        runBlocking {
            val pipeline = DetectionPipeline(FakeFrameSource.constant(a), clock, CadenceController())
            val job = async { pipeline.frames().take(8).toList() }
            clock.advanceBy(10_000)
            assertEquals(listOf(0L, 200L, 400L, 600L, 800L, 1000L, 1500L, 2000L), times(job.await()))
        }

    @Test
    fun anActionBringsTheRateBackToItsBase() =
        runBlocking {
            val pipeline = DetectionPipeline(FakeFrameSource.constant(a), clock, CadenceController())
            val job =
                async {
                    pipeline
                        .frames()
                        .take(8)
                        .onEach {
                            if ((it as FrameEvent.Captured).index ==
                                7L
                            ) {
                                pipeline.onAction()
                            }
                        }.toList()
                }
            clock.advanceBy(10_000)
            val expected = listOf(0L, 200L, 400L, 600L, 800L, 1000L, 1500L, 1700L)
            assertEquals(expected, times(job.await()))
        }

    @Test
    fun aChangingScreenKeepsTheBaseRate() =
        runBlocking {
            val pipeline =
                DetectionPipeline(
                    FakeFrameSource.of {
                        if (it % 2 ==
                            0
                        ) {
                            a
                        } else {
                            b
                        }
                    },
                    clock,
                    CadenceController(),
                )
            val job = async { pipeline.frames().take(6).toList() }
            clock.advanceBy(10_000)
            assertEquals(listOf(0L, 200L, 400L, 600L, 800L, 1000L), times(job.await()))
        }

    @Test
    fun theOwnPanelIsHiddenInEveryFrame() =
        runBlocking {
            val pipeline =
                DetectionPipeline(FakeFrameSource.constant(a), clock, CadenceController(), { Rect(0, 0, 60, 60) })
            val job = async { pipeline.frames().take(2).toList() }
            clock.advanceBy(1_000)
            val frames = job.await().filterIsInstance<FrameEvent.Captured>().map { it.frame }
            assertTrue(frames.all { f -> (0 until 60).all { y -> (0 until 60).all { x -> f.gray(x, y) == 0 } } })
            assertTrue(a.gray(30, 30) != 0)
            assertTrue(frames.all { it.gray(100, 100) == a.gray(100, 100) })
        }

    @Test
    fun aFailedCaptureIsReportedAndRetriedAtTheBaseRate() =
        runBlocking {
            val failure = Outcome.Failure(AppError.CaptureUnavailable(CaptureCause.SecureScreen))
            val source = FakeFrameSource { if (it == 1) failure else Outcome.Success(a) }
            val pipeline = DetectionPipeline(source, clock, CadenceController())
            val job = async { pipeline.frames().take(3).toList() }
            clock.advanceBy(1_000)
            val events = job.await()
            assertTrue(events[0] is FrameEvent.Captured)
            assertEquals(
                AppError.CaptureUnavailable(CaptureCause.SecureScreen),
                (events[1] as FrameEvent.Unavailable).error,
            )
            assertEquals(listOf(0L, 400L), times(events))
            assertEquals(1L, (events[0] as FrameEvent.Captured).index)
            assertEquals(2L, (events[2] as FrameEvent.Captured).index)
        }

    @Test
    fun cancellingTheCollectorStopsCapturing() =
        runBlocking {
            val source = FakeFrameSource.constant(a)
            val pipeline = DetectionPipeline(source, clock, CadenceController())
            val job = async { pipeline.frames().toList() }
            clock.advanceBy(450)
            job.cancel()
            clock.advanceBy(5_000)
            assertEquals(3, source.calls)
            assertEquals(0, clock.pending)
        }
}
