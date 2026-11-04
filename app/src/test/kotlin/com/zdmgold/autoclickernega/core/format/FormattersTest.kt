package com.zdmgold.autoclickernega.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {
    @Test
    fun shortDurationsShowMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0, Locale.US))
        assertEquals("0:00", formatDuration(999, Locale.US))
        assertEquals("1:05", formatDuration(65_000, Locale.US))
        assertEquals("59:59", formatDuration(3_599_000, Locale.US))
    }

    @Test
    fun hourLongWaitsShowHours() {
        assertEquals("1:00:00", formatDuration(3_600_000, Locale.US))
        assertEquals("4:30:05", formatDuration(16_205_000, Locale.US))
        assertEquals("12:00:00", formatDuration(43_200_000, Locale.US))
    }

    @Test
    fun negativeDurationsClampToZero() {
        assertEquals("0:00", formatDuration(-5_000, Locale.US))
    }

    @Test
    fun countsFollowTheLocaleGrouping() {
        assertEquals("1,234,567", formatCount(1_234_567, Locale.US))
        assertEquals("1.234.567", formatCount(1_234_567, Locale.GERMANY))
    }
}
