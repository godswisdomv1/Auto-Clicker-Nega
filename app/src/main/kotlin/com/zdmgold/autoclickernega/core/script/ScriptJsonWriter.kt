package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.json.JsonBool
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.JsonValue
import com.zdmgold.autoclickernega.core.json.jsonArray
import com.zdmgold.autoclickernega.core.json.jsonObject
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.model.TextMatchKind
import com.zdmgold.autoclickernega.core.model.TimerSince
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF

private fun str(value: String): JsonValue = JsonString(value)

private fun num(value: Int): JsonValue = JsonNumber.of(value)

private fun num(value: Long): JsonValue = JsonNumber.of(value)

private fun point(p: Point): JsonValue = jsonArray(listOf(num(p.x), num(p.y)))

private fun rect(r: Rect): JsonValue = jsonArray(listOf(num(r.l), num(r.t), num(r.r), num(r.b)))

internal object ScriptJsonWriter {
    fun script(s: Script): JsonObject {
        val fields = linkedMapOf<String, JsonValue>()
        fields["schema"] = num(s.schema)
        if (s.id.isNotEmpty()) fields["id"] = str(s.id)
        fields["name"] = str(s.name)
        fields["mode"] = str(if (s.body is ScriptBody.Simple) "simple" else "smart")
        fields["screen"] =
            jsonObject("w" to num(s.screen.w), "h" to num(s.screen.h), "density" to num(s.screen.density))
        fields["run"] = run(s.run)
        fields["templates"] =
            JsonObject(
                s.templates.entries.associate { (id, t) ->
                    id to
                        jsonObject(
                            "file" to str(t.file),
                            "w" to num(t.w),
                            "h" to num(t.h),
                            "refDensity" to num(t.refDensity),
                        )
                },
            )
        fields["counters"] = jsonArray(s.counters.map(::counter))
        when (val body = s.body) {
            is ScriptBody.Simple -> fields["steps"] = jsonArray(body.steps.map(::action))
            is ScriptBody.Smart -> fields["events"] = jsonArray(body.events.map(::event))
        }
        return JsonObject(fields)
    }

    private fun run(r: RunConfig) =
        jsonObject(
            "loops" to num(r.loops),
            "globalTimerMs" to num(r.globalTimerMs),
            "startDelayMs" to num(r.startDelayMs),
            "countdownSec" to num(r.countdownSec),
            "searchRate" to
                str(
                    when (r.searchRate) {
                        SearchRate.Auto -> "auto"
                        SearchRate.Fast -> "fast"
                        SearchRate.BatterySaver -> "battery"
                    },
                ),
        )

    private fun counter(c: CounterDef) = jsonObject("id" to str(c.id), "name" to str(c.name), "start" to num(c.start))

    private fun event(e: Event) =
        jsonObject(
            "id" to str(e.id),
            "name" to str(e.name),
            "priority" to num(e.priority),
            "cooldownMs" to num(e.cooldownMs),
            "enabled" to JsonBool(e.enabled),
            "when" to
                jsonObject(
                    "op" to str(if (e.whenExpr.op == BoolOp.And) "AND" else "OR"),
                    "items" to jsonArray(e.whenExpr.items.map(::condition)),
                ),
            "do" to jsonArray(e.actions.map(::action)),
        )

    private fun withCommon(
        type: String,
        negate: Boolean,
        vararg rest: Pair<String, JsonValue>,
    ): JsonObject {
        val fields = linkedMapOf<String, JsonValue>("type" to str(type))
        rest.forEach { (k, v) -> fields[k] = v }
        fields["negate"] = JsonBool(negate)
        return JsonObject(fields)
    }

    fun condition(c: Condition): JsonValue =
        when (c) {
            is Condition.Image -> {
                withCommon(
                    "image",
                    c.negate,
                    "template" to str(c.template),
                    "confidence" to JsonNumber.of(c.confidence),
                    "mode" to str(if (c.mode == MatchMode.Safe) "safe" else "fast"),
                ).withOptionalRegion(c.region)
            }

            is Condition.Text -> {
                withCommon(
                    "text",
                    c.negate,
                    "text" to str(c.text),
                    "kind" to str(if (c.kind == TextMatchKind.Contains) "contains" else "equals"),
                    "caseSensitive" to JsonBool(c.caseSensitive),
                    "lang" to str(c.lang),
                ).withOptionalRegion(c.region)
            }

            is Condition.Color -> {
                withCommon(
                    "color",
                    c.negate,
                    "x" to num(c.point.x),
                    "y" to num(c.point.y),
                    "rgb" to str("#" + String.format(Locale.ROOT, "%06X", c.rgb and RGB_MASK)),
                    "tolerance" to num(c.tolerance),
                ).withOptionalRegion(c.region)
            }

            is Condition.Number -> {
                withCommon(
                    "number",
                    c.negate,
                    "region" to rect(c.region),
                    "cmp" to str(c.cmp.symbol),
                    "value" to JsonNumber.of(c.value),
                    "lang" to str(c.lang),
                )
            }

            is Condition.Timer -> {
                withCommon(
                    "timer",
                    c.negate,
                    "since" to
                        str(
                            when (val since = c.since) {
                                TimerSince.RunStart -> "run"
                                is TimerSince.EventFired -> "event:${since.eventId}"
                            },
                        ),
                    "cmp" to str(c.cmp.symbol),
                    "ms" to num(c.ms),
                )
            }

            is Condition.Counter -> {
                withCommon(
                    "counter",
                    c.negate,
                    "id" to str(c.id),
                    "cmp" to str(c.cmp.symbol),
                    "value" to num(c.value),
                )
            }
        }

    private fun JsonObject.withOptionalRegion(region: Rect?): JsonObject =
        if (region == null) this else JsonObject(fields + ("region" to rect(region)))

    fun action(a: Action): JsonValue =
        when (a) {
            is Action.Tap -> {
                val fields = linkedMapOf<String, JsonValue>("type" to str("tap"))
                when (val target = a.at) {
                    is Target.At -> {
                        fields["x"] = num(target.p.x)
                        fields["y"] = num(target.p.y)
                    }

                    is Target.AtMatch -> {
                        fields["at"] = str("match")
                        fields["offset"] = point(target.offset)
                    }
                }
                fields["holdMs"] = num(a.holdMs)
                fields["jitterPx"] = num(a.jitterPx)
                fields["intervalMs"] = num(a.intervalMs)
                fields["timingJitterMs"] = num(a.timingJitterMs)
                JsonObject(fields)
            }

            is Action.Swipe -> {
                jsonObject(
                    "type" to str("swipe"),
                    "from" to point(a.from),
                    "to" to point(a.to),
                    "durationMs" to num(a.durationMs),
                    "intervalMs" to num(a.intervalMs),
                    "timingJitterMs" to num(a.timingJitterMs),
                )
            }

            is Action.Curve -> {
                jsonObject(
                    "type" to str("curve"),
                    "points" to jsonArray(a.points.map(::point)),
                    "durationMs" to num(a.durationMs),
                    "intervalMs" to num(a.intervalMs),
                    "timingJitterMs" to num(a.timingJitterMs),
                )
            }

            is Action.Multi -> {
                jsonObject(
                    "type" to str("multi"),
                    "strokes" to
                        jsonArray(
                            a.strokes.map { jsonObject("from" to point(it.from), "to" to point(it.to)) },
                        ),
                    "durationMs" to num(a.durationMs),
                    "intervalMs" to num(a.intervalMs),
                    "timingJitterMs" to num(a.timingJitterMs),
                )
            }

            is Action.Wait -> {
                jsonObject("type" to str("wait"), "ms" to num(a.ms))
            }

            is Action.CounterOp -> {
                jsonObject(
                    "type" to str("counter"),
                    "id" to str(a.id),
                    "op" to
                        str(
                            when (a.op) {
                                CounterOpKind.Set -> "set"
                                CounterOpKind.Add -> "add"
                                CounterOpKind.Subtract -> "subtract"
                            },
                        ),
                    "value" to num(a.value),
                )
            }

            is Action.RunScript -> {
                jsonObject("type" to str("runScript"), "scriptId" to str(a.scriptId))
            }

            Action.Stop -> {
                jsonObject("type" to str("stop"))
            }
        }
}
