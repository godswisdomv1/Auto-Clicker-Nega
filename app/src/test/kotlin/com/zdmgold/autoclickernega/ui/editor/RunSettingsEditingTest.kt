package com.zdmgold.autoclickernega.ui.editor

import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.run.StartWaitPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunSettingsEditingTest {
    private val run = RunConfig()

    @Test
    fun loopsAreClampedAndZeroMeansForever() {
        assertEquals(0, RunSettingsEditing.loops(run, -3).loops)
        assertEquals(0, RunSettingsEditing.loops(run, 0).loops)
        assertEquals(12, RunSettingsEditing.loops(run, 12).loops)
        assertEquals(RunSettingsEditing.MAX_LOOPS, RunSettingsEditing.loops(run, 1_000_000).loops)
    }

    @Test
    fun theTimerIsKeptInWholeMinutes() {
        val r = RunSettingsEditing.timerMinutes(run, 90)
        assertEquals(90 * 60_000L, r.globalTimerMs)
        assertEquals(90, RunSettingsEditing.timerMinutes(r))
        assertEquals(0L, RunSettingsEditing.timerMinutes(run, -5).globalTimerMs)
        assertEquals(24 * 60, RunSettingsEditing.timerMinutes(RunSettingsEditing.timerMinutes(run, 99_999)))
    }

    @Test
    fun theCountdownStopsAtThirtySeconds() {
        assertEquals(30, RunSettingsEditing.countdown(run, 99).countdownSec)
        assertEquals(0, RunSettingsEditing.countdown(run, -1).countdownSec)
        assertEquals(7, RunSettingsEditing.countdown(run, 7).countdownSec)
    }

    @Test
    fun startWaitIsZeroOrAtLeastOneMinuteAndAtMostTwelveHours() {
        assertEquals(0L, RunSettingsEditing.startWait(run, 0).startDelayMs)
        assertEquals(60_000L, RunSettingsEditing.startWait(run, 1_000).startDelayMs)
        assertEquals(300_000L, RunSettingsEditing.startWait(run, 300_000).startDelayMs)
        assertEquals(
            StartWaitPolicy.MAX_START_WAIT_MS,
            RunSettingsEditing.startWait(run, Long.MAX_VALUE / 2).startDelayMs,
        )
        assertEquals(0L, RunSettingsEditing.startWait(run, -10).startDelayMs)
    }

    @Test
    fun theCustomEntryCoversTheWholeRange() {
        assertEquals(720, RunSettingsEditing.MAX_CUSTOM_WAIT_MINUTES)
        val r = RunSettingsEditing.startWait(run, 45 * 60_000L)
        assertEquals(45, RunSettingsEditing.startWaitMinutes(r))
    }

    @Test
    fun thePlugInWarningStartsAboveThirtyMinutes() {
        assertFalse(RunSettingsEditing.needsPlugIn(RunSettingsEditing.startWait(run, 30 * 60_000L)))
        assertTrue(RunSettingsEditing.needsPlugIn(RunSettingsEditing.startWait(run, 31 * 60_000L)))
    }

    @Test
    fun presetsAreRecognisedAndCustomValuesAreNot() {
        assertTrue(RunSettingsEditing.isPreset(0))
        assertTrue(RunSettingsEditing.isPreset(2 * 3_600_000L))
        assertFalse(RunSettingsEditing.isPreset(7 * 60_000L))
    }

    @Test
    fun searchRateIsSet() {
        assertEquals(SearchRate.Fast, RunSettingsEditing.searchRate(run, SearchRate.Fast).searchRate)
    }
}
