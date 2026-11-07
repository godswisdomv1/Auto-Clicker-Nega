package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.SearchRate

/**
 * Sets how long to wait between screen searches. The base interval is the larger of the OS capture limit and
 * 200 ms. When the screen has not changed for five frames the interval backs off to 500 ms, then 1 s, and any
 * change or action starts over. Fast never backs off, and Battery saver searches half as often.
 */
class CadenceController(
    private val rate: SearchRate = SearchRate.Auto,
    private val osMinIntervalMs: Long = DEFAULT_OS_MIN_INTERVAL_MS,
) {
    private var unchanged = 0

    /** Call once per captured frame with whether the screen changed since the previous one. Returns the wait before the next search. */
    fun nextIntervalMs(changed: Boolean): Long {
        unchanged = if (changed) 0 else unchanged + 1
        return intervalFor(unchanged)
    }

    /** An action just happened, so the screen is about to change. */
    fun onAction() {
        unchanged = 0
    }

    fun reset() = onAction()

    /** The wait after a failed capture. Failures never back off, so a screen that becomes visible is noticed quickly. */
    fun retryIntervalMs(): Long = base()

    private fun base(): Long =
        when (rate) {
            SearchRate.Fast -> maxOf(osMinIntervalMs, FAST_FLOOR_MS)
            SearchRate.Auto -> maxOf(osMinIntervalMs, BASE_MS)
            SearchRate.BatterySaver -> maxOf(osMinIntervalMs, BASE_MS) * 2
        }

    private fun intervalFor(unchangedFrames: Int): Long {
        val base = base()
        if (rate == SearchRate.Fast) return base
        val factor = if (rate == SearchRate.BatterySaver) 2 else 1
        return when {
            unchangedFrames >= SECOND_STEP_AFTER -> maxOf(base, SECOND_STEP_MS * factor)
            unchangedFrames >= FIRST_STEP_AFTER -> maxOf(base, FIRST_STEP_MS * factor)
            else -> base
        }
    }

    companion object {
        const val DEFAULT_OS_MIN_INTERVAL_MS = 200L
        private const val BASE_MS = 200L
        private const val FAST_FLOOR_MS = 100L
        private const val FIRST_STEP_MS = 500L
        private const val SECOND_STEP_MS = 1000L
        private const val FIRST_STEP_AFTER = 5
        private const val SECOND_STEP_AFTER = 10
    }
}
