package com.zdmgold.autoclickernega.ui.editor

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target

/** The kinds of step a Simple script can hold. */
enum class StepKind { Tap, Swipe, Curve, Multi, Wait }

fun Action.stepKind(): StepKind? =
    when (this) {
        is Action.Tap -> StepKind.Tap
        is Action.Swipe -> StepKind.Swipe
        is Action.Curve -> StepKind.Curve
        is Action.Multi -> StepKind.Multi
        is Action.Wait -> StepKind.Wait
        is Action.CounterOp, is Action.RunScript, Action.Stop -> null
    }

/** Editing a Simple script as plain functions on immutable data, so every rule is tested without a screen. */
object SimpleEditing {
    private const val QUARTER = 0.25
    private const val HALF = 0.5
    private const val THREE_QUARTERS = 0.75
    private const val SWIPE_FROM = 0.7
    private const val SWIPE_TO = 0.3
    private const val CURVE_START = 0.75
    private const val CURVE_MID = 0.55
    private const val CURVE_END = 0.3
    private const val CURVE_BEND = 0.6
    private const val PINCH_INNER_LEFT = 0.38
    private const val PINCH_INNER_RIGHT = 0.62
    private const val DEFAULT_SWIPE_MS = 300L
    private const val DEFAULT_CURVE_MS = 400L
    private const val DEFAULT_GESTURE_INTERVAL_MS = 500L
    private const val DEFAULT_WAIT_MS = 1000L

    fun steps(script: Script): List<Action> = (script.body as? ScriptBody.Simple)?.steps.orEmpty()

    private fun withSteps(
        script: Script,
        steps: List<Action>,
    ) = script.copy(body = ScriptBody.Simple(steps))

    fun canAdd(script: Script): Boolean = steps(script).size < Limits.MAX_ITEMS

    /** Appends [step]. A script that is already full is returned unchanged. */
    fun add(
        script: Script,
        step: Action,
    ): Script = if (canAdd(script)) withSteps(script, steps(script) + step) else script

    fun replace(
        script: Script,
        index: Int,
        step: Action,
    ): Script {
        val list = steps(script)
        if (index !in list.indices) return script
        return withSteps(script, list.toMutableList().also { it[index] = step })
    }

    fun remove(
        script: Script,
        index: Int,
    ): Script {
        val list = steps(script)
        if (index !in list.indices) return script
        return withSteps(script, list.toMutableList().also { it.removeAt(index) })
    }

    /** Moves the step at [index] by [delta] places, up (negative) or down (positive). Edges do nothing. */
    fun move(
        script: Script,
        index: Int,
        delta: Int,
    ): Script {
        val list = steps(script)
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return script
        val moved = list.toMutableList()
        moved.add(target, moved.removeAt(index))
        return withSteps(script, moved)
    }

    /** Inserts a copy of the step right after the original. A full script is returned unchanged. */
    fun duplicate(
        script: Script,
        index: Int,
    ): Script {
        val list = steps(script)
        if (index !in list.indices || !canAdd(script)) return script
        return withSteps(script, list.toMutableList().also { it.add(index + 1, list[index]) })
    }

    /** A new step of [kind] with sensible values for this screen and the user's defaults. */
    fun blankStep(
        kind: StepKind,
        screen: ScreenSpec,
        defaults: AppSettings,
    ): Action {
        fun x(f: Double) = (screen.w * f).toInt()

        fun y(f: Double) = (screen.h * f).toInt()
        return when (kind) {
            StepKind.Tap -> {
                Action.Tap(
                    at = Target.At(Point(x(HALF), y(HALF))),
                    jitterPx = defaults.defaultJitterPx,
                    intervalMs = Limits.clampInterval(defaults.defaultIntervalMs),
                )
            }

            StepKind.Swipe -> {
                Action.Swipe(
                    Point(x(HALF), y(SWIPE_FROM)),
                    Point(x(HALF), y(SWIPE_TO)),
                    DEFAULT_SWIPE_MS,
                    DEFAULT_GESTURE_INTERVAL_MS,
                )
            }

            StepKind.Curve -> {
                Action.Curve(
                    listOf(
                        Point(x(HALF), y(CURVE_START)),
                        Point(x(CURVE_BEND), y(CURVE_MID)),
                        Point(x(HALF), y(CURVE_END)),
                    ),
                    DEFAULT_CURVE_MS,
                    DEFAULT_GESTURE_INTERVAL_MS,
                )
            }

            StepKind.Multi -> {
                Action.Multi(
                    listOf(
                        Stroke(Point(x(PINCH_INNER_LEFT), y(HALF)), Point(x(QUARTER), y(HALF))),
                        Stroke(Point(x(PINCH_INNER_RIGHT), y(HALF)), Point(x(THREE_QUARTERS), y(HALF))),
                    ),
                    DEFAULT_SWIPE_MS,
                    DEFAULT_GESTURE_INTERVAL_MS,
                )
            }

            StepKind.Wait -> {
                Action.Wait(DEFAULT_WAIT_MS)
            }
        }
    }

    /** An empty Simple script with the user's defaults. */
    fun newScript(
        name: String,
        screen: ScreenSpec,
        defaults: AppSettings,
    ): Script =
        Script(
            id = "",
            name = name,
            screen = screen,
            run =
                RunConfig(
                    loops = 1,
                    startDelayMs = Limits.clampStartDelay(defaults.defaultStartDelayMs),
                    countdownSec = Limits.clampCountdown(defaults.defaultCountdownSec),
                    searchRate = defaults.searchRate,
                ),
            body = ScriptBody.Simple(emptyList()),
        )
}
