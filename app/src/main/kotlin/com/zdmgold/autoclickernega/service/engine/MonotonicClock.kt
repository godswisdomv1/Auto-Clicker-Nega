package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.port.Clock
import kotlinx.coroutines.delay

/** The real clock: monotonic, so it never jumps when the system time changes. */
class MonotonicClock : Clock {
    override fun nowMs(): Long = System.nanoTime() / NANOS_PER_MS

    override suspend fun delayUntil(targetMs: Long) {
        val wait = targetMs - nowMs()
        if (wait > 0) delay(wait)
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
    }
}
