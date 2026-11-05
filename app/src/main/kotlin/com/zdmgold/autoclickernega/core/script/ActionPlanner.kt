package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.run.Jitter

/**
 * One step ready to run: [action] holds final screen coordinates, and [intervalMs] is the effective
 * start-to-start time to the next step after timing jitter.
 */
data class PlannedStep(
    val index: Int,
    val action: Action,
    val intervalMs: Long,
)

/** Simple mode: turns a script's steps into the list the runner executes, scaled and jittered. */
object ActionPlanner {
    /** One pass of the steps. The runner asks again with the next [iteration] for each loop. */
    fun plan(
        script: Script,
        screen: ScreenSpec,
        seed: Long = 0L,
        iteration: Int = 0,
    ): List<PlannedStep> {
        val body = script.body as? ScriptBody.Simple ?: return emptyList()
        return body.steps.mapIndexed { index, action ->
            planOne(index, action, script.screen, screen, Jitter.mix(seed, iteration, index))
        }
    }

    private fun planOne(
        index: Int,
        action: Action,
        from: ScreenSpec,
        to: ScreenSpec,
        seed: Long,
    ): PlannedStep =
        when (action) {
            is Action.Tap -> {
                val target =
                    when (val t = action.at) {
                        is Target.At -> {
                            val moved = CoordinateScaler.scale(t.p, from, to)
                            val jitter = Jitter.point(seed, action.jitterPx)
                            Target.At(clamp(Point(moved.x + jitter.x, moved.y + jitter.y), to))
                        }

                        is Target.AtMatch -> {
                            Target.AtMatch(CoordinateScaler.scaleOffset(t.offset, from, to))
                        }
                    }
                step(index, action.copy(at = target), action, seed)
            }

            is Action.Swipe -> {
                step(
                    index,
                    action.copy(
                        from = clamp(CoordinateScaler.scale(action.from, from, to), to),
                        to = clamp(CoordinateScaler.scale(action.to, from, to), to),
                    ),
                    action,
                    seed,
                )
            }

            is Action.Curve -> {
                step(
                    index,
                    action.copy(points = action.points.map { clamp(CoordinateScaler.scale(it, from, to), to) }),
                    action,
                    seed,
                )
            }

            is Action.Multi -> {
                step(
                    index,
                    action.copy(
                        strokes =
                            action.strokes.map {
                                Stroke(
                                    clamp(CoordinateScaler.scale(it.from, from, to), to),
                                    clamp(CoordinateScaler.scale(it.to, from, to), to),
                                )
                            },
                    ),
                    action,
                    seed,
                )
            }

            is Action.Wait -> {
                PlannedStep(index, action, action.ms)
            }

            else -> {
                PlannedStep(index, action, 0L)
            }
        }

    private fun step(
        index: Int,
        planned: Action.Gesture,
        original: Action.Gesture,
        seed: Long,
    ): PlannedStep {
        val jitter = Jitter.timing(seed, original.timingJitterMs)
        return PlannedStep(index, planned, Limits.clampInterval(original.intervalMs + jitter))
    }

    private fun clamp(
        p: Point,
        screen: ScreenSpec,
    ): Point = Point(p.x.coerceIn(0, maxOf(screen.w - 1, 0)), p.y.coerceIn(0, maxOf(screen.h - 1, 0)))
}
