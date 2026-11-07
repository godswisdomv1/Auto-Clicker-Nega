package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.port.GesturePort
import com.zdmgold.autoclickernega.core.vision.GestureOutcome

/** Records every gesture with the time it started, and can take time to finish or refuse them. */
class FakeGesturePort(
    private val clock: Clock,
    private val latencyMs: Long = 0L,
    var default: GestureOutcome = GestureOutcome.Completed,
) : GesturePort {
    class Call(
        val atMs: Long,
        val gesture: Action.Gesture,
    )

    val calls = mutableListOf<Call>()
    val queued = ArrayDeque<GestureOutcome>()

    val times: List<Long> get() = calls.map { it.atMs }

    override suspend fun dispatch(g: Action.Gesture): GestureOutcome {
        calls += Call(clock.nowMs(), g)
        if (latencyMs > 0) clock.delayUntil(clock.nowMs() + latencyMs)
        return queued.removeFirstOrNull() ?: default
    }
}
