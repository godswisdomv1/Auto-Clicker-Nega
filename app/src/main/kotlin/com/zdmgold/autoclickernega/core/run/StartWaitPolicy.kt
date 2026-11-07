package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Limits

/** The Start in X wait: its range, its presets and when to warn about the battery. */
object StartWaitPolicy {
    /** 12 hours. Lowered only if the 12-hour acceptance gate fails on a real phone. */
    const val MAX_START_WAIT_MS = Limits.MAX_START_WAIT_MS
    const val MIN_PRESET_WAIT_MS = 60_000L

    /** Above this the start sheet says to plug in the phone, because the screen stays on while waiting. */
    const val PLUG_IN_WARNING_MS = 30 * 60_000L

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    val presetsMs: List<Long> =
        listOf(
            1 * MINUTE,
            5 * MINUTE,
            15 * MINUTE,
            30 * MINUTE,
            1 * HOUR,
            2 * HOUR,
            4 * HOUR,
            8 * HOUR,
            12 * HOUR,
        )

    fun clamp(ms: Long): Long = ms.coerceIn(0L, MAX_START_WAIT_MS)

    /** A custom entry is valid from one minute up to the cap, or zero for no wait. */
    fun isValidCustom(ms: Long): Boolean = ms == 0L || ms in MIN_PRESET_WAIT_MS..MAX_START_WAIT_MS

    fun needsPlugInWarning(ms: Long): Boolean = ms > PLUG_IN_WARNING_MS
}
