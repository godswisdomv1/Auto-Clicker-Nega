package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionLogTest {
    private fun entry(t: Long) =
        DetectionEntry(
            frameTimeMs = t,
            eventId = "e1",
            score = 0.9f,
            box = Rect(1, 2, 3, 4),
            tap = Point(2, 3),
            miss = MissReason.None,
            gesture = GestureOutcome.Completed,
            searchMs = 12,
        )

    @Test
    fun keepsEntriesInOrder() {
        val log = DetectionLog()
        log.add(entry(1))
        log.add(entry(2))
        assertEquals(listOf(1L, 2L), log.snapshot().map { it.frameTimeMs })
        assertEquals(2L, log.last()!!.frameTimeMs)
    }

    @Test
    fun dropsTheOldestWhenFull() {
        val log = DetectionLog(capacity = 3)
        (1L..5L).forEach { log.add(entry(it)) }
        assertEquals(listOf(3L, 4L, 5L), log.snapshot().map { it.frameTimeMs })
    }

    @Test
    fun clearEmptiesTheLog() {
        val log = DetectionLog()
        log.add(entry(1))
        log.clear()
        assertTrue(log.snapshot().isEmpty())
        assertNull(log.last())
    }

    @Test
    fun theSnapshotIsACopy() {
        val log = DetectionLog()
        log.add(entry(1))
        val snap = log.snapshot()
        log.add(entry(2))
        assertEquals(1, snap.size)
    }

    @Test
    fun entriesHoldNoPixelData() {
        val fieldTypes = DetectionEntry::class.java.declaredFields.map { it.type }
        assertTrue(
            fieldTypes.none {
                it == ByteArray::class.java || it == IntArray::class.java ||
                    it == Frame::class.java
            },
        )
    }

    @Test
    fun aMatchResultAcceptsOnlyAtOrAboveTheThreshold() {
        val m = Match(0.84f, Rect(0, 0, 10, 10))
        assertNull(MatchResult(m, 0.85f).accepted)
        assertEquals(m, MatchResult(m, 0.84f).accepted)
        assertEquals(Point(5, 5), m.center)
        assertNull(MatchResult(null, 0.85f).accepted)
    }
}
