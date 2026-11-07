package com.zdmgold.autoclickernega.core.port

/**
 * Monotonic milliseconds, and waiting until a point in that time. Every interval and timeout goes through
 * this, so a changed system clock cannot disturb a run and tests can drive time by hand.
 */
interface Clock {
    fun nowMs(): Long

    /** Suspends until [nowMs] reaches [targetMs]. Returns at once when that time has already passed. */
    suspend fun delayUntil(targetMs: Long)
}

/** The wall clock, used only for dates such as the daily run counters. */
interface WallClock {
    fun epochMs(): Long

    /** Today's local date as yyyy-MM-dd. */
    fun localDate(): String
}
