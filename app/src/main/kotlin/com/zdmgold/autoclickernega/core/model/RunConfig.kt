package com.zdmgold.autoclickernega.core.model

/**
 * Run options of one script.
 *
 * [loops] of 0 means forever and applies to Simple mode only. A [globalTimerMs] of 0 means no limit.
 * [startDelayMs] is the Start in X wait, from 0 up to [Limits.MAX_START_WAIT_MS].
 */
data class RunConfig(
    val loops: Int = 1,
    val globalTimerMs: Long = 0,
    val startDelayMs: Long = 0,
    val countdownSec: Int = Limits.DEFAULT_COUNTDOWN_SEC,
    val searchRate: SearchRate = SearchRate.Auto,
)
