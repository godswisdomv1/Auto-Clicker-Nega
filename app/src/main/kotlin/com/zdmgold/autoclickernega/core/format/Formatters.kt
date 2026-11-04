package com.zdmgold.autoclickernega.core.format

import java.text.NumberFormat
import java.util.Locale

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

/**
 * A clock style duration: m:ss below one hour and h:mm:ss from one hour up, so a 12 hour wait reads
 * 12:00:00. Digits follow the locale.
 */
fun formatDuration(
    ms: Long,
    locale: Locale,
): String {
    val total = maxOf(ms, 0L) / MS_PER_SECOND
    val hours = total / SECONDS_PER_HOUR
    val minutes = (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = total % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%d:%02d", minutes, seconds)
    }
}

/** A whole number with the locale's digits and grouping. */
fun formatCount(
    value: Long,
    locale: Locale,
): String = NumberFormat.getIntegerInstance(locale).format(value)
