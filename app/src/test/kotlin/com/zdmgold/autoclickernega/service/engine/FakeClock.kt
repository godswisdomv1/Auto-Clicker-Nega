package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.port.Clock
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import java.util.PriorityQueue
import kotlin.coroutines.resume

/**
 * A clock that only moves when a test says so. Anything waiting on [delayUntil] is resumed in order of its
 * target time as the clock advances, so a 12 hour wait takes no real time.
 */
class FakeClock(
    start: Long = 0L,
) : Clock {
    private class Waiter(
        val target: Long,
        val seq: Long,
        val cont: CancellableContinuation<Unit>,
    )

    private var now = start
    private var seq = 0L
    private val waiters = PriorityQueue(compareBy<Waiter>({ it.target }, { it.seq }))

    /** How many coroutines are currently waiting on this clock. */
    val pending: Int get() = waiters.size

    override fun nowMs(): Long = now

    override suspend fun delayUntil(targetMs: Long) {
        if (targetMs <= now) return
        suspendCancellableCoroutine { cont ->
            val waiter = Waiter(targetMs, seq++, cont)
            waiters.add(waiter)
            cont.invokeOnCancellation { waiters.remove(waiter) }
        }
    }

    suspend fun advanceBy(ms: Long) = advanceTo(now + ms)

    suspend fun advanceTo(target: Long) {
        settle()
        var next = waiters.peek()
        while (next != null && next.target <= target) {
            waiters.poll()
            now = maxOf(now, next.target)
            next.cont.resume(Unit)
            settle()
            next = waiters.peek()
        }
        now = maxOf(now, target)
        settle()
    }

    /** Lets every ready coroutine run until it suspends again. */
    suspend fun settle() {
        repeat(SETTLE_YIELDS) { yield() }
    }

    private companion object {
        const val SETTLE_YIELDS = 40
    }
}
