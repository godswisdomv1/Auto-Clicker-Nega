package com.zdmgold.autoclickernega.core.lab

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabReportTest {
    private val match =
        MatchRunResult(
            0.9312f,
            "(10, 20, 106, 84)",
            24,
            12,
            listOf(ConfidenceRow(0.85f, true), ConfidenceRow(0.95f, false)),
        )
    private val repeat = RepeatResult(100, 94, 6, 0, 93, 0.85f, listOf(20, 30, 40), listOf(250, 300, 500), 47_000, 300)
    private val rate = RateResult(100, 12, 88, listOf(1000, 1002, 1100), 100)

    @Test
    fun theReportShowsEverySection() {
        val text = LabReport.build("Pixel", "Android 14", "0.1.0", "96x64", match, repeat, rate)
        assertTrue(text.contains("Device: Pixel"))
        assertTrue(text.contains("best score 0.93"))
        assertTrue(text.contains("Safe 24 ms, Fast 12 ms"))
        assertTrue(text.contains("confidence 0.95: not found"))
        assertTrue(text.contains("detections 93 of 94"))
        assertTrue(text.contains("searches per second 2.0"))
        assertTrue(text.contains("learned minimum gap 300 ms"))
        assertTrue(text.contains("accepted 12, refused 88"))
        assertTrue(text.contains("min 1000"))
    }

    @Test
    fun missingSectionsAreLeftOut() {
        val text = LabReport.build("Pixel", "Android 14", "0.1.0", null, null, null, null)
        assertTrue(text.contains("none captured"))
        assertFalse(text.contains("Repeat test"))
        assertFalse(text.contains("rate probe"))
    }
}
