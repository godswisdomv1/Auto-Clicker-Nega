package com.zdmgold.autoclickernega.core.vision

import kotlin.math.abs

/**
 * Safe mode: a match counts only when the same target is found within [tolerancePx] on two consecutive frames.
 * This filters one-frame flashes such as transitions. Use one stabiliser per image condition.
 */
class MatchStabilizer(
    private val tolerancePx: Int = DEFAULT_TOLERANCE_PX,
) {
    private var previous: Match? = null

    /** Feed the latest result. Returns the match once it has held for two frames, otherwise null. */
    fun accept(m: Match?): Match? {
        val before = previous
        previous = m
        if (m == null || before == null) return null
        val a = before.center
        val b = m.center
        return if (abs(a.x - b.x) <= tolerancePx && abs(a.y - b.y) <= tolerancePx) m else null
    }

    fun reset() {
        previous = null
    }

    companion object {
        const val DEFAULT_TOLERANCE_PX = 12
    }
}
