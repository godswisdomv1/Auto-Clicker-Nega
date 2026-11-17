package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.port.FrameSource
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.run.CadenceController
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.FrameDiff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** What the pipeline hands to the runner: a captured frame, or the reason there is none. */
sealed interface FrameEvent {
    data class Captured(
        val frame: Frame,
        val index: Long,
        val timeMs: Long,
    ) : FrameEvent

    /** A capture failed, for example on a secure screen. The runner makes no tap for this frame. */
    data class Unavailable(
        val error: AppError,
    ) : FrameEvent
}

private fun isTooFrequent(error: AppError): Boolean =
    (error as? AppError.CaptureUnavailable)?.cause == CaptureCause.TooFrequent

/**
 * Pulls frames from a source at the pace the cadence controller sets, hides the app's own panel in each frame
 * before anything can match it, and tells the cadence whether the screen changed. The flow is cold and stops
 * when its collector stops. A capture refused for being too frequent is not reported: the pipeline learns the
 * system's limit, widens its own gap and retries.
 */
class DetectionPipeline(
    private val source: FrameSource,
    private val clock: Clock,
    private val cadence: CadenceController,
    private val panelMask: () -> Rect? = { null },
) {
    private var previous: Frame? = null
    private var index = 0L

    /** A gesture just ran, so the screen is about to change and the search rate returns to its base. */
    fun onAction() = cadence.onAction()

    fun frames(): Flow<FrameEvent> =
        flow {
            while (true) {
                val startedAt = clock.nowMs()
                val interval =
                    when (val captured = source.next()) {
                        is Outcome.Success -> {
                            val frame = captured.value
                            panelMask()?.let { frame.fill(it) }
                            val changed = previous?.let { FrameDiff.changed(it, frame) } ?: true
                            previous = frame
                            emit(FrameEvent.Captured(frame, ++index, startedAt))
                            cadence.nextIntervalMs(changed)
                        }

                        is Outcome.Failure -> {
                            if (isTooFrequent(captured.error)) {
                                // A rate limit is not a screen we cannot see. Slow down and try again, quietly.
                                cadence.onCaptureTooFrequent()
                            } else {
                                previous = null
                                emit(FrameEvent.Unavailable(captured.error))
                            }
                            cadence.retryIntervalMs()
                        }
                    }
                clock.delayUntil(startedAt + interval)
            }
        }
}
