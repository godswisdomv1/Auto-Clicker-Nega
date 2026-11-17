package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame

/**
 * One screen capture on request, outside a run. Only the Detection lab in debug builds calls it, to measure what a
 * real phone can do. The frame stays in memory and is never stored or sent.
 */
interface CaptureProbe {
    suspend fun captureFrame(): Outcome<Frame>
}
