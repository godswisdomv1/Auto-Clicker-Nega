package com.zdmgold.autoclickernega.core.run

import kotlinx.coroutines.CancellableContinuation
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Hands one result to a coroutine that is waiting for a system callback, and then lets go of that coroutine.
 *
 * Android keeps a callback object after it has been called. A callback that holds the waiting continuation directly
 * keeps the whole finished call chain alive with it, including a frame it produced, so every capture stayed in memory
 * until the system finally dropped the callback and the app ran out of memory after about seventy of them. This
 * holder is cleared on first use, so a callback the system keeps holds nothing.
 */
class OneShot<T>(
    continuation: CancellableContinuation<T>,
) {
    private val waiting = AtomicReference<CancellableContinuation<T>?>(continuation)

    init {
        continuation.invokeOnCancellation { waiting.set(null) }
    }

    /** Resumes the waiting coroutine with [value]. False when it was already resumed or has been cancelled. */
    fun resume(value: T): Boolean {
        val continuation = waiting.getAndSet(null) ?: return false
        if (!continuation.isActive) return false
        continuation.resume(value)
        return true
    }
}
