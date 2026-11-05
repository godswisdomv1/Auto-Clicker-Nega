package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.model.TemplateRef
import com.zdmgold.autoclickernega.core.model.TextMatchKind
import com.zdmgold.autoclickernega.core.model.TimerSince

/** Scripts shared by the script tests. */
object TestScripts {
    val screen = ScreenSpec(1080, 2400, 420)
    val template = TemplateRef("t1", "t1.png", 96, 64, 420)

    fun simple(
        vararg steps: Action,
        id: String = "s1",
    ) = Script(id = id, name = "Simple", screen = screen, body = ScriptBody.Simple(steps.toList()))

    fun smart(
        vararg events: Event,
        id: String = "s1",
    ) = Script(
        id = id,
        name = "Smart",
        screen = screen,
        templates = mapOf(template.id to template),
        counters = listOf(CounterDef("c1", "Collected", 0)),
        body = ScriptBody.Smart(events.toList()),
    )

    fun event(
        id: String,
        priority: Int,
        vararg items: Condition,
        actions: List<Action> = listOf(Action.Stop),
    ) = Event(id, id, priority, 500, true, Expr(BoolOp.And, items.toList()), actions)

    val image = Condition.Image("t1", 0.85f, MatchMode.Safe, Rect(0, 1600, 1080, 2400))
    val counterBelow = Condition.Counter("c1", Comparison.Lt, 50)
    val tapOnMatch = Action.Tap(Target.AtMatch(Point(0, 0)))

    /** A script that uses every kind of action and condition, with non-default values everywhere. */
    fun everything(): Script =
        Script(
            id = "full",
            name = "Everything é 中 \"quoted\"",
            screen = screen,
            run =
                RunConfig(
                    loops = 0,
                    globalTimerMs = 600_000,
                    startDelayMs = 3_600_000,
                    countdownSec = 5,
                    searchRate = SearchRate.BatterySaver,
                ),
            templates = mapOf(template.id to template),
            counters = listOf(CounterDef("c1", "Collected", 2), CounterDef("c2", "Misses", 0)),
            body =
                ScriptBody.Smart(
                    listOf(
                        Event(
                            id = "e1",
                            name = "First",
                            priority = 1,
                            cooldownMs = 750,
                            enabled = false,
                            whenExpr =
                                Expr(
                                    BoolOp.Or,
                                    listOf(
                                        image.copy(negate = true),
                                        Condition.Text("Collect", TextMatchKind.Equals, true, "ar", Rect(1, 2, 3, 4)),
                                        Condition.Color(Point(5, 6), 0x12C79C, 20, Rect(5, 6, 9, 9)),
                                    ),
                                ),
                            actions =
                                listOf(
                                    Action.Tap(Target.AtMatch(Point(4, -4)), 60, 2, 150, 10),
                                    Action.Tap(Target.At(Point(10, 20))),
                                    Action.Swipe(Point(1, 2), Point(3, 4), 300, 400, 5),
                                    Action.Curve(listOf(Point(1, 1), Point(2, 2), Point(3, 3), Point(4, 4)), 500, 400),
                                    Action.Multi(
                                        listOf(Stroke(Point(1, 1), Point(2, 2)), Stroke(Point(3, 3), Point(4, 4))),
                                        300,
                                        400,
                                    ),
                                    Action.Wait(250),
                                    Action.CounterOp("c1", CounterOpKind.Subtract, 3),
                                    Action.RunScript("other"),
                                    Action.Stop,
                                ),
                        ),
                        Event(
                            id = "e2",
                            name = "",
                            priority = 2,
                            whenExpr =
                                Expr(
                                    BoolOp.And,
                                    listOf(
                                        Condition.Number(Rect(0, 0, 10, 10), Comparison.Gte, 10.5, "en", true),
                                        Condition.Timer(TimerSince.EventFired("e1"), Comparison.Gt, 5000),
                                        Condition.Timer(TimerSince.RunStart, Comparison.Neq, 1),
                                    ),
                                ),
                            actions = listOf(Action.CounterOp("c2", CounterOpKind.Set, 7)),
                        ),
                    ),
                ),
        )
}
