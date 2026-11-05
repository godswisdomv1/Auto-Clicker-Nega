package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Target

enum class RecordedKind { Tap, LongPress, Swipe }

/** One gesture seen by the record layer. [startMs] is when the finger went down, relative to the recording start. */
data class RecordedGesture(
    val kind: RecordedKind,
    val from: Point,
    val to: Point,
    val startMs: Long,
    val durationMs: Long,
)

/** Turns a recording into Simple-mode steps. The gap between two gestures becomes the first one's interval. */
object RecordToSteps {
    fun toSteps(recording: List<RecordedGesture>): List<Action> {
        val sorted = recording.sortedBy { it.startMs }
        return sorted.mapIndexed { index, g ->
            val interval =
                sorted
                    .getOrNull(index + 1)
                    ?.let { Limits.clampInterval(it.startMs - g.startMs) }
                    ?: Limits.DEFAULT_INTERVAL_MS
            when (g.kind) {
                RecordedKind.Tap, RecordedKind.LongPress -> {
                    Action.Tap(
                        at = Target.At(g.from),
                        holdMs = maxOf(g.durationMs, Limits.DEFAULT_HOLD_MS),
                        intervalMs = interval,
                    )
                }

                RecordedKind.Swipe -> {
                    Action.Swipe(
                        from = g.from,
                        to = g.to,
                        durationMs = maxOf(g.durationMs, 1L),
                        intervalMs = interval,
                    )
                }
            }
        }
    }
}
