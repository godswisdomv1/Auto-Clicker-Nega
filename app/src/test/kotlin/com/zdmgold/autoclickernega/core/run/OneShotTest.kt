package com.zdmgold.autoclickernega.core.run

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.ref.WeakReference

class OneShotTest {
    @Test
    fun theFirstResultWinsAndTheRestAreIgnored() =
        runBlocking {
            var second = true
            val value =
                suspendCancellableCoroutine<Int> { cont ->
                    val shot = OneShot(cont)
                    assertTrue(shot.resume(1))
                    second = shot.resume(2)
                }
            assertEquals(1, value)
            assertFalse(second)
        }

    @Test
    fun aCancelledWaiterIsNotResumed() =
        runBlocking {
            var kept: OneShot<Int>? = null
            val result =
                withTimeoutOrNull(50) {
                    suspendCancellableCoroutine<Int> { cont -> kept = OneShot(cont) }
                }
            assertNull(result)
            assertFalse(kept!!.resume(3))
        }

    @Test
    fun afterTheResultTheHolderNoLongerReachesTheWaiter() =
        runBlocking {
            var kept: OneShot<ByteArray>? = null
            var weak: WeakReference<ByteArray>? = null
            suspendCancellableCoroutine<ByteArray> { cont ->
                val shot = OneShot(cont)
                kept = shot
                val payload = ByteArray(64)
                weak = WeakReference(payload)
                shot.resume(payload)
            }
            // The holder is still referenced here, as a system callback would be, but it must not keep the result.
            @Suppress("ExplicitGarbageCollectionCall")
            System.gc()
            assertTrue(kept != null)
            assertNull(weak!!.get())
        }

    @Test
    fun cancellationIsReportedAsCancellation() {
        val failed =
            runCatching {
                runBlocking {
                    withTimeoutOrNull(10) { suspendCancellableCoroutine<Int> { } }
                    throw CancellationException("done")
                }
            }.exceptionOrNull()
        assertTrue(failed is CancellationException)
    }
}
