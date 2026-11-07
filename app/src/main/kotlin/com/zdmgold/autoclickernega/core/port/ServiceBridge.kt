package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.EngineEvent
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything needed to start a run. [capMs] is the limit set by the run gate for capped runs, or null.
 * [screen] is the screen the run happens on, so scripts made on another screen are rescaled.
 */
data class RunRequest(
    val script: Script,
    val screen: ScreenSpec,
    val capMs: Long? = null,
    val seed: Long = 0L,
)

/** The UI-facing engine contract: commands in, state out. The UI never drives individual taps. */
interface ServiceBridge {
    val state: StateFlow<EngineState>

    val events: SharedFlow<EngineEvent>

    /** Live counter values of the current run, by counter id. */
    val counters: StateFlow<Map<String, Int>>

    fun start(request: RunRequest): Outcome<Unit>

    fun pause(): Outcome<Unit>

    fun resume(): Outcome<Unit>

    fun stop(): Outcome<Unit>
}
