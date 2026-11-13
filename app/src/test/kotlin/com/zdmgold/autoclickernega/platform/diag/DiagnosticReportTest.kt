package com.zdmgold.autoclickernega.platform.diag

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.vision.DetectionEntry
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.core.vision.MissReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {
    private val info = DeviceInfo("0.1.0", "Pixel 6", "14", 34)

    private fun detection(
        t: Long,
        event: String = "secret-event-id",
    ) = DetectionEntry(
        frameTimeMs = t,
        eventId = event,
        score = 0.8765f,
        box = Rect(10, 20, 30, 40),
        tap = Point(15, 25),
        miss = MissReason.BelowConfidence,
        gesture = GestureOutcome.Completed,
        searchMs = 12,
    )

    private fun crash(i: Int) =
        CrashEntry(
            i.toLong(),
            "main",
            "java.lang.IllegalStateException",
            "boom $i",
            List(30) {
                "Frame$it"
            },
        )

    @Test
    fun theHeaderNamesTheAppTheDeviceAndAndroid() {
        val text = DiagnosticReport.build(info, emptyMap(), emptyList(), emptyList())
        assertTrue(text.contains("App version: 0.1.0"))
        assertTrue(text.contains("Device: Pixel 6"))
        assertTrue(text.contains("Android: 14 (API 34)"))
    }

    @Test
    fun emptySectionsSayNone() {
        val text = DiagnosticReport.build(info, emptyMap(), emptyList(), emptyList())
        assertTrue(text.contains("Errors seen:\n  none"))
        assertTrue(text.contains("Recent detections (0):"))
        assertTrue(text.contains("Crashes (0):"))
    }

    @Test
    fun errorClassesAreCountedAndSorted() {
        val log = ErrorLog()
        listOf(AppError.NothingFound, AppError.ServiceKilled, AppError.NothingFound).forEach(log::record)
        val text = DiagnosticReport.build(info, log.counts(), emptyList(), emptyList())
        assertTrue(text.indexOf("nothing_found x2") in 0 until text.indexOf("service_killed x1"))
    }

    @Test
    fun detectionsShowOnlyScoreReasonAndOutcomeNeverNamesOrPlaces() {
        val text = DiagnosticReport.build(info, emptyMap(), listOf(detection(5)), emptyList())
        assertTrue(text.contains("t=5 score=0.88 miss=BelowConfidence gesture=Completed search=12ms"))
        assertFalse(text.contains("secret-event-id"))
        assertFalse(text.contains("15"))
    }

    @Test
    fun onlyTheLatestThirtyDetectionsAreListed() {
        val text = DiagnosticReport.build(info, emptyMap(), (1L..50L).map { detection(it) }, emptyList())
        assertEquals(30, text.lines().count { it.trimStart().startsWith("t=") && it.contains("score=") })
        assertTrue(text.contains("t=50 "))
        assertFalse(text.contains("t=20 "))
    }

    @Test
    fun crashesShowTheirMessageAndAShortStack() {
        val text = DiagnosticReport.build(info, emptyMap(), emptyList(), listOf(crash(1)))
        assertTrue(text.contains("java.lang.IllegalStateException: boom 1"))
        assertEquals(12, text.lines().count { it.trimStart().startsWith("at Frame") })
    }

    @Test
    fun theErrorLogKeepsOnlyTheMostRecentAndOnlyTheirClass() {
        val log = ErrorLog(capacity = 3)
        repeat(5) { log.record(AppError.Storage("secret detail $it")) }
        log.record(AppError.NothingFound)
        assertEquals(mapOf("storage" to 2, "nothing_found" to 1), log.counts())
        log.clear()
        assertTrue(log.counts().isEmpty())
    }
}
