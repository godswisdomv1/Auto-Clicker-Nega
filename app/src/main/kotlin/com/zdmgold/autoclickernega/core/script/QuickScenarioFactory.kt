package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.model.TemplateRef

/** Builds the scripts of the Quick Scenario wizard. Each is a valid script that needs no further editing. */
object QuickScenarioFactory {
    private const val EVENT_COOLDOWN_MS = 500L

    /** 'Tap when this appears': one event that taps the centre of the match. */
    fun tapWhenImage(
        id: String,
        name: String,
        screen: ScreenSpec,
        template: TemplateRef,
        region: Rect?,
    ): Script =
        oneEventScript(id, name, screen, Condition.Image(template = template.id, region = region)).copy(
            templates = mapOf(template.id to template),
        )

    /** 'Tap when this text appears'. */
    fun tapWhenText(
        id: String,
        name: String,
        screen: ScreenSpec,
        text: String,
        lang: String,
        region: Rect?,
    ): Script = oneEventScript(id, name, screen, Condition.Text(text = text, lang = lang, region = region))

    /** 'Tap every N milliseconds': a Simple script that taps one point forever until stopped. */
    fun tapEvery(
        id: String,
        name: String,
        screen: ScreenSpec,
        at: Point,
        intervalMs: Long,
    ): Script =
        Script(
            id = id,
            name = name,
            screen = screen,
            run = RunConfig(loops = 0),
            body = ScriptBody.Simple(listOf(Action.Tap(Target.At(at), intervalMs = Limits.clampInterval(intervalMs)))),
        )

    private fun oneEventScript(
        id: String,
        name: String,
        screen: ScreenSpec,
        condition: Condition,
    ): Script =
        Script(
            id = id,
            name = name,
            screen = screen,
            body =
                ScriptBody.Smart(
                    listOf(
                        Event(
                            id = "e1",
                            name = name,
                            priority = 1,
                            cooldownMs = EVENT_COOLDOWN_MS,
                            whenExpr = Expr(BoolOp.And, listOf(condition)),
                            actions = listOf(Action.Tap(Target.AtMatch(Point(0, 0)))),
                        ),
                    ),
                ),
        )
}
