package com.zdmgold.autoclickernega.core.lab

/** Small statistics for the Detection lab. Values are milliseconds or counts. */
object LabStats {
    private const val PERCENT = 100.0
    private const val MEDIAN_PERCENT = 50

    fun median(values: List<Long>): Long = percentile(values, MEDIAN_PERCENT)

    /** The nearest-rank percentile, or 0 for an empty list. */
    fun percentile(
        values: List<Long>,
        percent: Int,
    ): Long {
        if (values.isEmpty()) return 0L
        val sorted = values.sorted()
        val rank = Math.ceil(percent / PERCENT * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    /** Gaps between consecutive times, in the same unit. */
    fun gaps(times: List<Long>): List<Long> = times.zipWithNext { a, b -> b - a }
}
