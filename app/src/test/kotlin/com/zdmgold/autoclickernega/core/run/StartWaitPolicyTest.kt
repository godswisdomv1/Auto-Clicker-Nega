package com.zdmgold.autoclickernega.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartWaitPolicyTest {
    @Test
    fun theCapIsTwelveHours() {
        assertEquals(43_200_000L, StartWaitPolicy.MAX_START_WAIT_MS)
    }

    @Test
    fun presetsRunFromOneMinuteToTwelveHours() {
        assertEquals(
            listOf(1L, 5L, 15L, 30L, 60L, 120L, 240L, 480L, 720L).map { it * 60_000 },
            StartWaitPolicy.presetsMs,
        )
        assertTrue(StartWaitPolicy.presetsMs.all { StartWaitPolicy.isValidCustom(it) })
    }

    @Test
    fun clampKeepsValuesInRange() {
        assertEquals(0L, StartWaitPolicy.clamp(-1))
        assertEquals(5_000L, StartWaitPolicy.clamp(5_000))
        assertEquals(43_200_000L, StartWaitPolicy.clamp(Long.MAX_VALUE))
    }

    @Test
    fun customEntriesAreZeroOrBetweenOneMinuteAndTheCap() {
        assertTrue(StartWaitPolicy.isValidCustom(0))
        assertFalse(StartWaitPolicy.isValidCustom(59_999))
        assertTrue(StartWaitPolicy.isValidCustom(60_000))
        assertTrue(StartWaitPolicy.isValidCustom(43_200_000))
        assertFalse(StartWaitPolicy.isValidCustom(43_200_001))
    }

    @Test
    fun theBatteryWarningStartsAboveThirtyMinutes() {
        assertFalse(StartWaitPolicy.needsPlugInWarning(30 * 60_000L))
        assertTrue(StartWaitPolicy.needsPlugInWarning(30 * 60_000L + 1))
    }
}
