package com.zdmgold.autoclickernega.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class SpanPartsTest {
    private fun parts(ms: Long) = SpanParts.of(ms).map { "${it.value} ${it.unit}" }

    @Test
    fun underASecondIsMilliseconds() {
        assertEquals(listOf("0 Millis"), parts(0))
        assertEquals(listOf("40 Millis"), parts(40))
        assertEquals(listOf("999 Millis"), parts(999))
    }

    @Test
    fun wholeSecondsAreSeconds() {
        assertEquals(listOf("1 Seconds"), parts(1000))
        assertEquals(listOf("45 Seconds"), parts(45_000))
    }

    @Test
    fun aPartialSecondUnderAMinuteKeepsItsMilliseconds() {
        assertEquals(listOf("1500 Millis"), parts(1500))
        assertEquals(listOf("59001 Millis"), parts(59_001))
    }

    @Test
    fun minutesAndHoursCombine() {
        assertEquals(listOf("1 Minutes"), parts(60_000))
        assertEquals(listOf("5 Minutes"), parts(300_000))
        assertEquals(listOf("1 Hours"), parts(3_600_000))
        assertEquals(listOf("1 Hours", "30 Minutes"), parts(5_400_000))
        assertEquals(listOf("12 Hours"), parts(43_200_000))
        assertEquals(listOf("1 Minutes", "1 Seconds"), parts(61_000))
    }

    @Test
    fun aNegativeDurationIsZero() {
        assertEquals(listOf("0 Millis"), parts(-5))
    }
}
