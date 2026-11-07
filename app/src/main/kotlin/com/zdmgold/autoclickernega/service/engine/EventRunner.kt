package com.zdmgold.autoclickernega.service.engine

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.port.Clock
import com.zdmgold.autoclickernega.core.run.ConditionEvaluator
import com.zdmgold.autoclickernega.core.run.EventScheduler
import com.zdmgold.autoclickernega.core.run.ExecSignal
import com.zdmgold.autoclickernega.core.run.Jitter
import com.zdmgold.autoclickernega.core.run.RunContext
import com.zdmgold.autoclickernega.core.script.ActionPlanner
import com.zdmgold.autoclickernega.core.script.CompiledEvent
import com.zdmgold.autoclickernega.core.vision.DetectionEntry
import com.zdmgold.autoclickernega.core.vision.DetectionLog
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.core.vision.MissReason

/**
 * Smart mode. On each captured frame it tries the eligible events in priority order and runs the first one whose
 * conditions pass. The others wait for the next frame, and a fired event's cooldown keeps it from firing again
 * on the same screen.
 */
class EventRunner(
    private val scheduler: EventScheduler,
    private val evaluator: ConditionEvaluator,
    private val ctx: RunContext,
    private val executor: ActionExecutor,
    private val log: DetectionLog,
    private val clock: Clock,
    private val scriptScreen: ScreenSpec,
    private val runningScreen: ScreenSpec,
    private val seed: Long,
    private val onFired: (String) -> Unit = {},
    private val onAction: () -> Unit = {},
) {
    private var fired = 0

    suspend fun onFrame(
        frame: Frame,
        now: Long,
    ): ExecSignal {
        for (compiled in scheduler.eligible(now)) {
            val before = clock.nowMs()
            val eval = evaluator.evaluate(compiled.event.whenExpr, frame, ctx, compiled.event.id)
            val searchMs = clock.nowMs() - before
            if (eval.passed) return fire(compiled, eval.anchor?.box, eval.score, now, searchMs)
            if (compiled.anchor != null) {
                log.add(
                    DetectionEntry(
                        now,
                        compiled.event.id,
                        eval.score,
                        null,
                        null,
                        eval.miss,
                        GestureOutcome.None,
                        searchMs,
                    ),
                )
            }
        }
        return ExecSignal.Continue
    }

    private suspend fun fire(
        compiled: CompiledEvent,
        anchor: com.zdmgold.autoclickernega.core.model.Rect?,
        score: Float?,
        now: Long,
        searchMs: Long,
    ): ExecSignal {
        val event = compiled.event
        scheduler.markFired(event, now)
        onFired(event.id)
        val steps =
            ActionPlanner.planActions(
                event.actions,
                scriptScreen,
                runningScreen,
                Jitter.mix(seed, fired++),
                anchor,
            )
        var signal = ExecSignal.Continue
        var tap: Point? = null
        var outcome = GestureOutcome.None
        for ((i, step) in steps.withIndex()) {
            val startedAt = clock.nowMs()
            val target = (step.action as? Action.Tap)?.at as? Target.At
            if (tap == null && target != null) tap = target.p
            signal = executor.perform(step.action)
            if (step.action is Action.Gesture) outcome = executor.lastOutcome
            if (signal != ExecSignal.Continue) break
            if (i < steps.lastIndex) clock.delayUntil(startedAt + step.intervalMs)
        }
        onAction()
        log.add(DetectionEntry(now, event.id, score, anchor, tap, MissReason.None, outcome, searchMs))
        return signal
    }
}
