package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame

/** Where frames come from: the next screen capture, or a typed failure such as a secure screen. */
interface FrameSource {
    suspend fun next(): Outcome<Frame>
}
