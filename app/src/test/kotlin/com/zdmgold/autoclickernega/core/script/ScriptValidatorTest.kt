package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.model.TemplateRef
import com.zdmgold.autoclickernega.core.model.TimerSince
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts.event
import com.zdmgold.autoclickernega.core.script.TestScripts.image
import com.zdmgold.autoclickernega.core.script.TestScripts.simple
import com.zdmgold.autoclickernega.core.script.TestScripts.smart
import com.zdmgold.autoclickernega.core.script.TestScripts.tapOnMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptValidatorTest {
    private fun problem(
        script: Script,
        lookup: (String) -> Script? = { null },
    ): ImportProblem {
        val r =
            ScriptValidator.validate(script, lookup) as? Outcome.Failure ?: throw AssertionError("expected a failure")
        return (r.error as AppError.MalformedImport).reason
    }

    private fun valid(script: Script): Script =
        when (val r = ScriptValidator.validate(script)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> throw AssertionError("expected success but got ${r.error}")
        }

    private fun runs(
        id: String,
        vararg targets: String,
    ) = simple(*targets.map { Action.RunScript(it) }.toTypedArray(), id = id)

    @Test
    fun acceptsEverythingTheCodecTestsUse() {
        valid(TestScripts.everything().let { it.copy(counters = it.counters + CounterDef("c3", "x")) })
    }

    @Test
    fun clampsRangesInsteadOfRefusing() {
        val script =
            Script(
                id = "s",
                name = "Clamp",
                screen = TestScripts.screen,
                run = RunConfig(loops = -4, globalTimerMs = -1, startDelayMs = 999_999_999_999, countdownSec = 999),
                templates = mapOf("t1" to TestScripts.template),
                body =
                    ScriptBody.Smart(
                        listOf(
                            event(
                                "e",
                                1,
                                image.copy(confidence = 0.1f),
                                Condition.Color(Point(1, 1), 0, tolerance = 900),
                                actions =
                                    listOf(
                                        Action.Tap(
                                            Target.At(Point(1, 1)),
                                            holdMs = 0,
                                            jitterPx = -3,
                                            intervalMs = 10,
                                            timingJitterMs = -9,
                                        ),
                                        Action.Swipe(Point(1, 1), Point(2, 2), durationMs = 0, intervalMs = 1),
                                        Action.Wait(-5),
                                    ),
                            ).copy(cooldownMs = -10),
                        ),
                    ),
            )
        val clean = valid(script)
        assertEquals(0, clean.run.loops)
        assertEquals(0L, clean.run.globalTimerMs)
        assertEquals(Limits.MAX_START_WAIT_MS, clean.run.startDelayMs)
        assertEquals(Limits.MAX_COUNTDOWN_SEC, clean.run.countdownSec)
        val e = (clean.body as ScriptBody.Smart).events.single()
        assertEquals(0L, e.cooldownMs)
        assertEquals(Limits.MIN_CONFIDENCE, (e.whenExpr.items[0] as Condition.Image).confidence, 0f)
        assertEquals(255, (e.whenExpr.items[1] as Condition.Color).tolerance)
        val tap = e.actions[0] as Action.Tap
        assertEquals(1L, tap.holdMs)
        assertEquals(0, tap.jitterPx)
        assertEquals(Limits.MIN_INTERVAL_MS, tap.intervalMs)
        assertEquals(0, tap.timingJitterMs)
        assertEquals(1L, (e.actions[1] as Action.Swipe).durationMs)
        assertEquals(Limits.MIN_INTERVAL_MS, (e.actions[1] as Action.Swipe).intervalMs)
        assertEquals(0L, (e.actions[2] as Action.Wait).ms)
    }

    @Test
    fun enforcesOneToThreeConditions() {
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1, image, image, image, image))))
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1))))
        valid(smart(event("e", 1, image, image, image)))
    }

    @Test
    fun aTapOnAMatchNeedsAPositiveImageOrTextCondition() {
        valid(smart(event("e", 1, image, actions = listOf(tapOnMatch))))
        valid(smart(event("e", 1, Condition.Text("go"), actions = listOf(tapOnMatch))))
        assertEquals(
            ImportProblem.InvalidField,
            problem(smart(event("e", 1, image.copy(negate = true), actions = listOf(tapOnMatch)))),
        )
        assertEquals(
            ImportProblem.InvalidField,
            problem(smart(event("e", 1, TestScripts.counterBelow, actions = listOf(tapOnMatch)))),
        )
        assertEquals(ImportProblem.InvalidField, problem(simple(tapOnMatch)))
    }

    @Test
    fun referencesMustExist() {
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1, image.copy(template = "nope")))))
        assertEquals(
            ImportProblem.InvalidField,
            problem(smart(event("e", 1, Condition.Counter("zzz", Comparison.Lt, 1)))),
        )
        assertEquals(
            ImportProblem.InvalidField,
            problem(
                smart(
                    event(
                        "e",
                        1,
                        TestScripts.counterBelow,
                        actions = listOf(Action.CounterOp("zzz", CounterOpKind.Add, 1)),
                    ),
                ),
            ),
        )
        assertEquals(
            ImportProblem.InvalidField,
            problem(smart(event("e", 1, Condition.Timer(TimerSince.EventFired("ghost"), Comparison.Gt, 1)))),
        )
        valid(smart(event("e", 1, Condition.Timer(TimerSince.EventFired("e"), Comparison.Gt, 1))))
    }

    @Test
    fun eventIdsAndCounterIdsMustBeUnique() {
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1, image), event("e", 2, image))))
        assertEquals(ImportProblem.InvalidField, problem(smart(event("", 1, image))))
        val dup = smart(event("e", 1, image)).copy(counters = listOf(CounterDef("c1", "a"), CounterDef("c1", "b")))
        assertEquals(ImportProblem.InvalidField, problem(dup))
    }

    @Test
    fun gestureShapesAreChecked() {
        val two = listOf(Point(1, 1), Point(2, 2))
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.Curve(two, 300, 100))))
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.Curve(two + two + two, 300, 100))))
        valid(simple(Action.Curve(two + Point(3, 3), 300, 100)))
        val stroke = Stroke(Point(1, 1), Point(2, 2))
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.Multi(listOf(stroke), 300, 100))))
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.Multi(List(4) { stroke }, 300, 100))))
        valid(simple(Action.Multi(listOf(stroke, stroke), 300, 100)))
    }

    @Test
    fun regionsMustBeUsable() {
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1, image.copy(region = Rect(5, 5, 5, 9))))))
        assertEquals(ImportProblem.InvalidField, problem(smart(event("e", 1, image.copy(region = Rect(-1, 0, 5, 9))))))
        assertEquals(
            ImportProblem.InvalidField,
            problem(smart(event("e", 1, Condition.Number(Rect(0, 0, 0, 0), Comparison.Gt, 1.0)))),
        )
    }

    @Test
    fun basicFieldsAreChecked() {
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.Stop).copy(name = " ")))
        assertEquals(
            ImportProblem.InvalidField,
            problem(simple(Action.Stop).copy(screen = TestScripts.screen.copy(w = 0))),
        )
        assertEquals(ImportProblem.InvalidField, problem(simple(Action.RunScript(" "))))
        assertEquals(
            ImportProblem.InvalidField,
            problem(
                smart(event("e", 1, image)).copy(templates = mapOf("t1" to TemplateRef("t1", "t1.png", 0, 5, 420))),
            ),
        )
    }

    @Test
    fun limitsTheNumberOfItemsAndTemplates() {
        val many = simple(*Array(Limits.MAX_ITEMS + 1) { Action.Wait(1) })
        assertEquals(ImportProblem.TooManyItems, problem(many))
        valid(simple(*Array(Limits.MAX_ITEMS) { Action.Wait(1) }))
        val templates = (0..Limits.MAX_TEMPLATES).associate { "t$it" to TemplateRef("t$it", "t$it.png", 4, 4, 420) }
        assertEquals(ImportProblem.TooManyItems, problem(smart(event("e", 1, image)).copy(templates = templates)))
    }

    @Test
    fun scriptsThatRunEachOtherAreCheckedForCyclesAndDepth() {
        assertEquals(ImportProblem.Cycle, problem(runs("a", "a")))
        val b = runs("b", "a")
        assertEquals(ImportProblem.Cycle, problem(runs("a", "b")) { if (it == "b") b else null })
        val c = runs("c")
        val d = runs("d", "c")
        val e = runs("e", "d")
        val lookup = { id: String -> mapOf("c" to c, "d" to d, "e" to e)[id] }
        valid(runs("a", "d"))
        assertEquals(Outcome.Success(runs("x", "e")), ScriptValidator.validate(runs("x", "e"), lookup))
        val deep = runs("f", "e")
        assertEquals(ImportProblem.TooDeep, problem(runs("g", "f")) { if (it == "f") deep else lookup(it) })
    }

    @Test
    fun anOrExpressionKeepsItsOperator() {
        val s = smart(event("e", 1, image).copy(whenExpr = Expr(BoolOp.Or, listOf(image, TestScripts.counterBelow))))
        assertEquals(BoolOp.Or, ((valid(s).body as ScriptBody.Smart).events.single()).whenExpr.op)
    }
}
