package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.port.Clock

/** What conditions need to know about the run in progress. */
class RunContext(
    val clock: Clock,
    val counters: CounterStore,
    val runStartMs: Long,
    val lastFiredAt: (String) -> Long?,
) {
    /** Counts captured frames, so safe-mode matching can tell consecutive frames from skipped ones. */
    var frameIndex: Long = 0
}
