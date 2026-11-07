package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.port.GesturePort
import com.zdmgold.autoclickernega.core.run.CounterStore
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.vision.GestureOutcome

/**
 * Performs one planned action. Timing between actions belongs to the caller. Ten gestures in a row that the
 * system refuses block the run, since some apps and secure screens do not accept injected gestures.
 */
class ActionExecutor(
    private val gestures: GesturePort,
    private val counters: CounterStore,
    private val runScript: suspend (String) -> ExecSignal,
    private val onRejected: (Int) -> Unit = {},
    private val maxRejectedInRow: Int = MAX_REJECTED_IN_ROW,
) {
    /** How the most recent gesture ended, for the detection log. */
    var lastOutcome: GestureOutcome = GestureOutcome.None
        private set

    private var rejectedInRow = 0

    suspend fun perform(action: Action): ExecSignal =
        when (action) {
            is Action.Gesture -> {
                gesture(action)
            }

            is Action.Wait -> {
                ExecSignal.Continue
            }

            is Action.CounterOp -> {
                counters.apply(action)
                ExecSignal.Continue
            }

            is Action.RunScript -> {
                runScript(action.scriptId)
            }

            Action.Stop -> {
                ExecSignal.Stop
            }
        }

    private suspend fun gesture(g: Action.Gesture): ExecSignal {
        val outcome = gestures.dispatch(g)
        lastOutcome = outcome
        when (outcome) {
            GestureOutcome.Rejected -> {
                rejectedInRow++
                onRejected(rejectedInRow)
            }

            GestureOutcome.Completed -> {
                rejectedInRow = 0
            }

            else -> {
                Unit
            }
        }
        return if (rejectedInRow >= maxRejectedInRow) ExecSignal.Blocked else ExecSignal.Continue
    }

    companion object {
        const val MAX_REJECTED_IN_ROW = 10
    }
}
