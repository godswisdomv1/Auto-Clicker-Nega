package com.zdmgold.autoclickernega.core.port

/** Monotonic milliseconds. Used for every interval and timeout, so a changed system clock cannot disturb a run. */
interface Clock {
    fun nowMs(): Long
}

/** The wall clock, used only for dates such as the daily run counters. */
interface WallClock {
    fun epochMs(): Long

    /** Today's local date as yyyy-MM-dd. */
    fun localDate(): String
}
