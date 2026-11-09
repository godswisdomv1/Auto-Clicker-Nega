package com.zdmgold.autoclickernega.service.capture

import com.zdmgold.autoclickernega.core.error.CaptureCause

/** Turns the error code of a failed screenshot into the cause the rest of the app understands. */
object CaptureErrors {
    /** The system refused because screenshots were requested too quickly. */
    const val INTERVAL_TIME_SHORT = 3

    /** The window is protected, such as banking or protected video. */
    const val SECURE_WINDOW = 6

    fun cause(code: Int): CaptureCause =
        when (code) {
            INTERVAL_TIME_SHORT -> CaptureCause.TooFrequent
            SECURE_WINDOW -> CaptureCause.SecureScreen
            else -> CaptureCause.Internal
        }
}
