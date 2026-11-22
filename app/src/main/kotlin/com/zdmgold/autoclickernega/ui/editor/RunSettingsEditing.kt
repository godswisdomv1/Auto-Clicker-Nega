package com.zdmgold.autoclickernega.ui.editor

import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.run.StartWaitPolicy

/** The run settings of a script as plain functions. Every setter clamps to what the engine accepts. */
object RunSettingsEditing {
    private const val MS_PER_MINUTE = 60_000L
    const val MAX_LOOPS = 9999
    const val MAX_TIMER_MINUTES = 24 * 60
    const val MAX_CUSTOM_WAIT_MINUTES = (StartWaitPolicy.MAX_START_WAIT_MS / MS_PER_MINUTE).toInt()

    /** Loops, where 0 means forever. */
    fun loops(
        run: RunConfig,
        value: Int,
    ) = run.copy(loops = value.coerceIn(0, MAX_LOOPS))

    /** The global timer in whole minutes, where 0 means no limit. */
    fun timerMinutes(
        run: RunConfig,
        minutes: Int,
    ) = run.copy(globalTimerMs = minutes.coerceIn(0, MAX_TIMER_MINUTES) * MS_PER_MINUTE)

    fun timerMinutes(run: RunConfig): Int = (run.globalTimerMs / MS_PER_MINUTE).toInt()

    fun countdown(
        run: RunConfig,
        seconds: Int,
    ) = run.copy(countdownSec = Limits.clampCountdown(seconds))

    /** Start in X, in milliseconds. 0 starts at once. Values between 0 and one minute are lifted to one minute. */
    fun startWait(
        run: RunConfig,
        ms: Long,
    ): RunConfig {
        val clamped = StartWaitPolicy.clamp(ms)
        val fixed =
            if (clamped in
                1 until StartWaitPolicy.MIN_PRESET_WAIT_MS
            ) {
                StartWaitPolicy.MIN_PRESET_WAIT_MS
            } else {
                clamped
            }
        return run.copy(startDelayMs = fixed)
    }

    /** The start wait in whole minutes, for the custom entry. */
    fun startWaitMinutes(run: RunConfig): Int = (run.startDelayMs / MS_PER_MINUTE).toInt()

    fun searchRate(
        run: RunConfig,
        rate: SearchRate,
    ) = run.copy(searchRate = rate)

    /** True when the wait is long enough that the phone should be plugged in. */
    fun needsPlugIn(run: RunConfig): Boolean = StartWaitPolicy.needsPlugInWarning(run.startDelayMs)

    /** True when the wait is one of the presets, so the sheet can mark it. */
    fun isPreset(ms: Long): Boolean = ms == 0L || ms in StartWaitPolicy.presetsMs
}
