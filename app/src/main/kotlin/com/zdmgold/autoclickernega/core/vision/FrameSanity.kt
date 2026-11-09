package com.zdmgold.autoclickernega.core.vision

/**
 * Catches frames that cannot be trusted. A screen showing protected content comes back from the system as one flat
 * colour, and matching against it could make a "not found" condition pass. Such a frame is treated as unavailable.
 */
object FrameSanity {
    fun isBlank(f: Frame): Boolean {
        val first = f.px[0]
        for (b in f.px) if (b != first) return false
        return true
    }
}
