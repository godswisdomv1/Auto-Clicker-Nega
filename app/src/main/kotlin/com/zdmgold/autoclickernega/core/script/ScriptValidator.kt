package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.model.TimerSince
import com.zdmgold.autoclickernega.core.result.Outcome

private class ValidationException(
    val problem: ImportProblem,
    message: String,
) : Exception(message)

private fun invalid(
    path: String,
    what: String,
): Nothing = throw ValidationException(ImportProblem.InvalidField, "$path: $what")

/**
 * Checks a script against the limits of the schema. Out-of-range numbers are clamped and returned in the
 * sanitised script. Anything that cannot be made valid is refused with the field named.
 */
object ScriptValidator {
    /** [lookup] finds other scripts by id, so Run script chains can be checked for cycles and depth. */
    fun validate(
        script: Script,
        lookup: (String) -> Script? = { null },
    ): Outcome<Script> {
        val clean = sanitize(script)
        return try {
            check(clean)
            checkRunScripts(clean, lookup)
            Outcome.Success(clean)
        } catch (e: ValidationException) {
            Outcome.Failure(AppError.MalformedImport(e.problem, e.message.orEmpty()))
        }
    }

    /** Clamps every range to its allowed values. It never changes the structure of the script. */
    fun sanitize(script: Script): Script {
        val run = script.run
        val body =
            when (val b = script.body) {
                is ScriptBody.Simple -> ScriptBody.Simple(b.steps.map(::sanitizeAction))
                is ScriptBody.Smart -> ScriptBody.Smart(b.events.map(::sanitizeEvent))
            }
        return script.copy(
            run =
                run.copy(
                    loops = maxOf(run.loops, 0),
                    globalTimerMs = maxOf(run.globalTimerMs, 0L),
                    startDelayMs = Limits.clampStartDelay(run.startDelayMs),
                    countdownSec = Limits.clampCountdown(run.countdownSec),
                ),
            body = body,
        )
    }

    private fun sanitizeEvent(e: Event) =
        e.copy(
            cooldownMs = maxOf(e.cooldownMs, 0L),
            whenExpr = e.whenExpr.copy(items = e.whenExpr.items.map(::sanitizeCondition)),
            actions = e.actions.map(::sanitizeAction),
        )

    private fun sanitizeCondition(c: Condition): Condition =
        when (c) {
            is Condition.Image -> c.copy(confidence = Limits.clampConfidence(c.confidence))
            is Condition.Color -> c.copy(tolerance = Limits.clampTolerance(c.tolerance))
            else -> c
        }

    private fun sanitizeAction(a: Action): Action =
        when (a) {
            is Action.Tap -> {
                a.copy(
                    holdMs = maxOf(a.holdMs, 1L),
                    jitterPx = maxOf(a.jitterPx, 0),
                    intervalMs = Limits.clampInterval(a.intervalMs),
                    timingJitterMs = maxOf(a.timingJitterMs, 0),
                )
            }

            is Action.Swipe -> {
                a.copy(
                    durationMs = maxOf(a.durationMs, 1L),
                    intervalMs = Limits.clampInterval(a.intervalMs),
                    timingJitterMs = maxOf(a.timingJitterMs, 0),
                )
            }

            is Action.Curve -> {
                a.copy(
                    durationMs = maxOf(a.durationMs, 1L),
                    intervalMs = Limits.clampInterval(a.intervalMs),
                    timingJitterMs = maxOf(a.timingJitterMs, 0),
                )
            }

            is Action.Multi -> {
                a.copy(
                    durationMs = maxOf(a.durationMs, 1L),
                    intervalMs = Limits.clampInterval(a.intervalMs),
                    timingJitterMs = maxOf(a.timingJitterMs, 0),
                )
            }

            is Action.Wait -> {
                a.copy(ms = maxOf(a.ms, 0L))
            }

            else -> {
                a
            }
        }

    private class Ctx(
        val script: Script,
    ) {
        val counterIds: Set<String> = script.counters.map { it.id }.toSet()
        val eventIds: Set<String> =
            (script.body as? ScriptBody.Smart)?.events?.map { it.id }?.toSet() ?: emptySet()
    }

    private fun check(script: Script) {
        if (script.name.isBlank()) invalid("script.name", "must not be empty")
        if (script.screen.w <= 0 || script.screen.h <= 0 || script.screen.density <= 0) {
            invalid("script.screen", "width, height and density must be positive")
        }
        if (script.templates.size > Limits.MAX_TEMPLATES) {
            throw ValidationException(ImportProblem.TooManyItems, "more than ${Limits.MAX_TEMPLATES} templates")
        }
        if (script.itemCount > Limits.MAX_ITEMS) {
            throw ValidationException(ImportProblem.TooManyItems, "more than ${Limits.MAX_ITEMS} steps or events")
        }
        script.templates.values.forEach {
            if (it.w <= 0 || it.h <= 0) invalid("template ${it.id}", "size must be positive")
        }
        val counterIds = script.counters.map { it.id }
        if (counterIds.any { it.isBlank() } || counterIds.toSet().size != counterIds.size) {
            invalid("counters", "ids must be unique and not empty")
        }
        val ctx = Ctx(script)
        when (val body = script.body) {
            is ScriptBody.Simple -> {
                body.steps.forEachIndexed {
                    i,
                    a,
                    ->
                    checkAction(a, "steps[$i]", ctx, anchor = false)
                }
            }

            is ScriptBody.Smart -> {
                checkEvents(body.events, ctx)
            }
        }
    }

    private fun checkEvents(
        events: List<Event>,
        ctx: Ctx,
    ) {
        val ids = events.map { it.id }
        if (ids.any { it.isBlank() } ||
            ids.toSet().size != ids.size
        ) {
            invalid("events", "ids must be unique and not empty")
        }
        events.forEachIndexed { i, e ->
            val path = "events[$i]"
            val items = e.whenExpr.items
            if (items.isEmpty() || items.size > Limits.MAX_CONDITIONS) {
                invalid("$path.when.items", "an event needs 1 to ${Limits.MAX_CONDITIONS} conditions")
            }
            items.forEachIndexed { j, c -> checkCondition(c, "$path.when.items[$j]", ctx) }
            val anchor = hasAnchor(e.whenExpr)
            e.actions.forEachIndexed { j, a -> checkAction(a, "$path.do[$j]", ctx, anchor) }
        }
    }

    private fun hasAnchor(expr: Expr): Boolean =
        expr.items.any { !it.negate && (it is Condition.Image || it is Condition.Text) }

    private fun checkCondition(
        c: Condition,
        path: String,
        ctx: Ctx,
    ) {
        when (c) {
            is Condition.Image -> {
                if (c.template !in ctx.script.templates) invalid(path, "unknown template \"${c.template}\"")
                checkRegion(c.region, path)
            }

            is Condition.Text -> {
                if (c.text.isEmpty()) invalid(path, "text must not be empty")
                checkRegion(c.region, path)
            }

            is Condition.Color -> {
                checkRegion(c.region, path)
            }

            is Condition.Number -> {
                checkRegion(c.region, path)
            }

            is Condition.Timer -> {
                val since = c.since
                if (since is TimerSince.EventFired && since.eventId !in ctx.eventIds) {
                    invalid(path, "unknown event \"${since.eventId}\"")
                }
            }

            is Condition.Counter -> {
                if (c.id !in ctx.counterIds) invalid(path, "unknown counter \"${c.id}\"")
            }
        }
    }

    private fun checkRegion(
        region: Rect?,
        path: String,
    ) {
        if (region != null && isBadRegion(region)) invalid(path, "region is empty or negative")
    }

    private fun isBadRegion(region: Rect): Boolean = region.isEmpty || region.l < 0 || region.t < 0

    private fun checkAction(
        a: Action,
        path: String,
        ctx: Ctx,
        anchor: Boolean,
    ) {
        when (a) {
            is Action.Tap -> {
                if (a.at is Target.AtMatch && !anchor) {
                    invalid(path, "a tap on a match needs an event with a positive image or text condition")
                }
            }

            is Action.Curve -> {
                if (a.points.size !in Limits.MIN_CURVE_POINTS..Limits.MAX_CURVE_POINTS) {
                    invalid(path, "a curve needs ${Limits.MIN_CURVE_POINTS} to ${Limits.MAX_CURVE_POINTS} points")
                }
            }

            is Action.Multi -> {
                if (a.strokes.size !in Limits.MIN_MULTI_STROKES..Limits.MAX_MULTI_STROKES) {
                    invalid(
                        path,
                        "multi-finger needs ${Limits.MIN_MULTI_STROKES} to ${Limits.MAX_MULTI_STROKES} strokes",
                    )
                }
            }

            is Action.CounterOp -> {
                if (a.id !in ctx.counterIds) invalid(path, "unknown counter \"${a.id}\"")
            }

            is Action.RunScript -> {
                if (a.scriptId.isBlank()) invalid(path, "script id must not be empty")
            }

            else -> {}
        }
    }

    /** The ids of the scripts that [script] runs through Run script actions. */
    fun runTargets(script: Script): List<String> {
        val actions =
            when (val body = script.body) {
                is ScriptBody.Simple -> body.steps
                is ScriptBody.Smart -> body.events.flatMap { it.actions }
            }
        return actions.filterIsInstance<Action.RunScript>().map { it.scriptId }
    }

    private fun checkRunScripts(
        script: Script,
        lookup: (String) -> Script?,
    ) {
        fun walk(
            current: Script,
            chain: List<String>,
            depth: Int,
        ) {
            for (target in runTargets(current)) {
                if (target in chain) {
                    throw ValidationException(ImportProblem.Cycle, "scripts run each other in a loop at \"$target\"")
                }
                if (depth + 1 > Limits.MAX_RUN_SCRIPT_DEPTH) {
                    throw ValidationException(
                        ImportProblem.TooDeep,
                        "scripts run other scripts more than ${Limits.MAX_RUN_SCRIPT_DEPTH} deep",
                    )
                }
                val child = lookup(target) ?: continue
                walk(child, chain + target, depth + 1)
            }
        }
        walk(script, listOf(script.id), 0)
    }
}
