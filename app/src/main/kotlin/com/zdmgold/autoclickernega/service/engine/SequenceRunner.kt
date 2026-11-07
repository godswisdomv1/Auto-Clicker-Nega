package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.script.ActionPlanner
import kotlinx.coroutines.yield

/**
 * Simple mode. Steps run on a start-to-start schedule from the monotonic clock, so the time a gesture takes does
 * not stretch the interval. A run that falls more than one interval behind restarts its schedule instead of
 * firing a burst to catch up.
 */
class SequenceRunner(
    private val clock: Clock,
    private val executor: ActionExecutor,
    private val awaitResumed: suspend () -> Unit = {},
) {
    /** [loopsOverride] replaces the script's loop count, so a child script that loops forever runs once. */
    suspend fun run(
        script: Script,
        screen: ScreenSpec,
        seed: Long,
        loopsOverride: Int? = null,
    ): ExecSignal {
        val loops = loopsOverride ?: script.run.loops
        var nextAt = clock.nowMs()
        var iteration = 0
        while (loops == 0 || iteration < loops) {
            val plan = ActionPlanner.plan(script, screen, seed, iteration)
            if (plan.isEmpty()) return ExecSignal.Continue
            for (step in plan) {
                clock.delayUntil(nextAt)
                val beforePause = clock.nowMs()
                awaitResumed()
                if (clock.nowMs() > beforePause) nextAt = clock.nowMs()
                val signal = executor.perform(step.action)
                if (signal != ExecSignal.Continue) return signal
                nextAt += step.intervalMs
                if (clock.nowMs() > nextAt + step.intervalMs) nextAt = clock.nowMs()
                yield()
            }
            iteration++
        }
        return ExecSignal.Continue
    }
}
