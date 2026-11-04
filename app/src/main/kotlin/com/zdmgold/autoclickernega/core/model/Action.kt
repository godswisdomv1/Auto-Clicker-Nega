package com.zdmgold.autoclickernega.core.model

import com.zdmgold.autoclickernega.core.model.Limits.DEFAULT_HOLD_MS
import com.zdmgold.autoclickernega.core.model.Limits.DEFAULT_INTERVAL_MS

sealed interface Action {
    /** An action that touches the screen and waits [intervalMs] start to start before the next one. */
    sealed interface Gesture : Action {
        val intervalMs: Long
        val timingJitterMs: Int
    }

    data class Tap(
        val at: Target,
        val holdMs: Long = DEFAULT_HOLD_MS,
        val jitterPx: Int = 0,
        override val intervalMs: Long = DEFAULT_INTERVAL_MS,
        override val timingJitterMs: Int = 0,
    ) : Gesture

    data class Swipe(
        val from: Point,
        val to: Point,
        val durationMs: Long,
        override val intervalMs: Long,
        override val timingJitterMs: Int = 0,
    ) : Gesture

    /** A path through 3 to 5 placed points. */
    data class Curve(
        val points: List<Point>,
        val durationMs: Long,
        override val intervalMs: Long,
        override val timingJitterMs: Int = 0,
    ) : Gesture

    /** 2 to 3 simultaneous strokes. */
    data class Multi(
        val strokes: List<Stroke>,
        val durationMs: Long,
        override val intervalMs: Long,
        override val timingJitterMs: Int = 0,
    ) : Gesture

    data class Wait(
        val ms: Long,
    ) : Action

    data class CounterOp(
        val id: String,
        val op: CounterOpKind,
        val value: Int,
    ) : Action

    /** Runs another script under the same run, with no second gate. */
    data class RunScript(
        val scriptId: String,
    ) : Action

    data object Stop : Action
}

enum class CounterOpKind { Set, Add, Subtract }
