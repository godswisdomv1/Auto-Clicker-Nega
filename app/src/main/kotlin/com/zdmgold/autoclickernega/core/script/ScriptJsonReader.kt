package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.json.JsonArray
import com.zdmgold.autoclickernega.core.json.JsonBool
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.JsonValue
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.Limits
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

/** Raised for any structural problem while reading a script. The message names the field. */
internal class DecodeException(
    message: String,
) : Exception(message)

private fun fail(
    path: String,
    what: String,
): Nothing = throw DecodeException("$path: $what")

/** A JSON object together with its path, so every error names the field it came from. */
internal class Node(
    val path: String,
    val obj: JsonObject,
) {
    private fun at(key: String) = "$path.$key"

    fun has(key: String): Boolean = obj[key] != null

    fun str(key: String): String = (obj[key] as? JsonString)?.value ?: fail(at(key), "expected a string")

    fun strOr(
        key: String,
        default: String,
    ): String = if (has(key)) str(key) else default

    fun long(key: String): Long = (obj[key] as? JsonNumber)?.toLongOrNull() ?: fail(at(key), "expected an integer")

    fun longOr(
        key: String,
        default: Long,
    ): Long = if (has(key)) long(key) else default

    fun int(key: String): Int {
        val v = long(key)
        if (v < Int.MIN_VALUE || v > Int.MAX_VALUE) fail(at(key), "number out of range")
        return v.toInt()
    }

    fun intOr(
        key: String,
        default: Int,
    ): Int = if (has(key)) int(key) else default

    fun double(key: String): Double = (obj[key] as? JsonNumber)?.toDouble() ?: fail(at(key), "expected a number")

    fun floatOr(
        key: String,
        default: Float,
    ): Float = if (has(key)) double(key).toFloat() else default

    fun boolOr(
        key: String,
        default: Boolean,
    ): Boolean =
        when (val v = obj[key]) {
            null -> default
            is JsonBool -> v.value
            else -> fail(at(key), "expected true or false")
        }

    fun obj(key: String): Node = Node(at(key), obj[key] as? JsonObject ?: fail(at(key), "expected an object"))

    fun objOrNull(key: String): Node? = if (has(key)) obj(key) else null

    fun arr(key: String): List<JsonValue> = (obj[key] as? JsonArray)?.items ?: fail(at(key), "expected a list")

    fun arrOrEmpty(key: String): List<JsonValue> = if (has(key)) arr(key) else emptyList()

    fun items(key: String): List<Node> = arr(key).mapIndexed { i, v -> element("${at(key)}[$i]", v) }

    fun itemsOrEmpty(key: String): List<Node> = if (has(key)) items(key) else emptyList()

    fun point(key: String): Point = pointOf(at(key), obj[key])

    fun pointOrNull(key: String): Point? = if (has(key)) point(key) else null

    fun rectOrNull(key: String): Rect? {
        if (!has(key)) return null
        val list = (obj[key] as? JsonArray)?.items ?: fail(at(key), "expected [left, top, right, bottom]")
        if (list.size != RECT_SIZE) fail(at(key), "expected four numbers")
        val n = list.mapIndexed { i, v -> intOf("${at(key)}[$i]", v) }
        return Rect(l = n.first(), t = n[1], r = n[2], b = n.last())
    }

    private companion object {
        const val RECT_SIZE = 4
    }
}

private fun element(
    path: String,
    v: JsonValue,
): Node = Node(path, v as? JsonObject ?: fail(path, "expected an object"))

private fun intOf(
    path: String,
    v: JsonValue?,
): Int {
    val n = (v as? JsonNumber)?.toLongOrNull() ?: fail(path, "expected an integer")
    if (n < Int.MIN_VALUE || n > Int.MAX_VALUE) fail(path, "number out of range")
    return n.toInt()
}

private fun pointOf(
    path: String,
    v: JsonValue?,
): Point {
    val list = (v as? JsonArray)?.items ?: fail(path, "expected [x, y]")
    if (list.size != 2) fail(path, "expected two numbers")
    return Point(intOf("$path[0]", list[0]), intOf("$path[1]", list[1]))
}

internal object ScriptJsonReader {
    fun script(root: JsonObject): Script {
        val n = Node("script", root)
        val mode = n.str("mode")
        val body =
            when (mode) {
                "simple" -> ScriptBody.Simple(n.itemsOrEmpty("steps").map(::action))
                "smart" -> ScriptBody.Smart(n.itemsOrEmpty("events").map(::event))
                else -> fail("script.mode", "expected \"simple\" or \"smart\"")
            }
        return Script(
            id = n.strOr("id", ""),
            name = n.str("name"),
            screen = screen(n.obj("screen")),
            run = n.objOrNull("run")?.let(::run) ?: RunConfig(),
            templates = templates(n),
            counters = n.itemsOrEmpty("counters").map(::counter),
            body = body,
            schema = n.int("schema"),
        )
    }

    private fun screen(n: Node) = ScreenSpec(n.int("w"), n.int("h"), n.int("density"))

    private fun run(n: Node) =
        RunConfig(
            loops = n.intOr("loops", 1),
            globalTimerMs = n.longOr("globalTimerMs", 0),
            startDelayMs = n.longOr("startDelayMs", 0),
            countdownSec = n.intOr("countdownSec", Limits.DEFAULT_COUNTDOWN_SEC),
            searchRate = searchRate(n.strOr("searchRate", "auto"), n.path),
        )

    private fun searchRate(
        wire: String,
        path: String,
    ): SearchRate =
        when (wire) {
            "auto" -> SearchRate.Auto
            "fast" -> SearchRate.Fast
            "battery" -> SearchRate.BatterySaver
            else -> fail("$path.searchRate", "expected auto, fast or battery")
        }

    private fun templates(n: Node): Map<String, TemplateRef> {
        val node = n.objOrNull("templates") ?: return emptyMap()
        val out = LinkedHashMap<String, TemplateRef>()
        for (key in node.obj.fields.keys) {
            val t = node.obj(key)
            out[key] = TemplateRef(key, t.str("file"), t.int("w"), t.int("h"), t.int("refDensity"))
        }
        return out
    }

    private fun counter(n: Node) = CounterDef(n.str("id"), n.str("name"), n.intOr("start", 0))

    private fun event(n: Node): Event {
        val whenNode = n.obj("when")
        return Event(
            id = n.str("id"),
            name = n.strOr("name", ""),
            priority = n.int("priority"),
            cooldownMs = n.longOr("cooldownMs", 0),
            enabled = n.boolOr("enabled", true),
            whenExpr = Expr(boolOp(whenNode), whenNode.items("items").map(::condition)),
            actions = n.items("do").map(::action),
        )
    }

    private fun boolOp(n: Node): BoolOp =
        when (n.strOr("op", "AND")) {
            "AND" -> BoolOp.And
            "OR" -> BoolOp.Or
            else -> fail("${n.path}.op", "expected AND or OR")
        }

    private fun comparison(n: Node): Comparison =
        Comparison.fromSymbol(n.str("cmp")) ?: fail("${n.path}.cmp", "expected <, <=, ==, !=, >= or >")

    fun condition(n: Node): Condition {
        val negate = n.boolOr("negate", false)
        return when (val type = n.str("type")) {
            "image" -> {
                Condition.Image(
                    template = n.str("template"),
                    confidence = n.floatOr("confidence", Limits.DEFAULT_CONFIDENCE),
                    mode = matchMode(n),
                    region = n.rectOrNull("region"),
                    negate = negate,
                )
            }

            "text" -> {
                Condition.Text(
                    text = n.str("text"),
                    kind = textKind(n),
                    caseSensitive = n.boolOr("caseSensitive", false),
                    lang = n.strOr("lang", "en"),
                    region = n.rectOrNull("region"),
                    negate = negate,
                )
            }

            "color" -> {
                Condition.Color(
                    point = Point(n.int("x"), n.int("y")),
                    rgb = hexColor(n),
                    tolerance = n.intOr("tolerance", Limits.DEFAULT_COLOR_TOLERANCE),
                    region = n.rectOrNull("region"),
                    negate = negate,
                )
            }

            "number" -> {
                Condition.Number(
                    region = n.rectOrNull("region") ?: fail("${n.path}.region", "expected [left, top, right, bottom]"),
                    cmp = comparison(n),
                    value = n.double("value"),
                    lang = n.strOr("lang", "en"),
                    negate = negate,
                )
            }

            "timer" -> {
                Condition.Timer(timerSince(n), comparison(n), n.long("ms"), negate)
            }

            "counter" -> {
                Condition.Counter(n.str("id"), comparison(n), n.int("value"), negate)
            }

            else -> {
                fail("${n.path}.type", "unknown condition \"$type\"")
            }
        }
    }

    private fun matchMode(n: Node): MatchMode =
        when (n.strOr("mode", "safe")) {
            "safe" -> MatchMode.Safe
            "fast" -> MatchMode.Fast
            else -> fail("${n.path}.mode", "expected safe or fast")
        }

    private fun textKind(n: Node): TextMatchKind =
        when (n.strOr("kind", "contains")) {
            "contains" -> TextMatchKind.Contains
            "equals" -> TextMatchKind.Equals
            else -> fail("${n.path}.kind", "expected contains or equals")
        }

    private fun hexColor(n: Node): Int {
        val text = n.str("rgb")
        val digits = text.removePrefix("#")
        if (digits.length != HEX_COLOR_LENGTH) fail("${n.path}.rgb", "expected #RRGGBB")
        return digits.toIntOrNull(HEX_RADIX) ?: fail("${n.path}.rgb", "expected #RRGGBB")
    }

    private fun timerSince(n: Node): TimerSince {
        val wire = n.strOr("since", "run")
        return when {
            wire == "run" -> TimerSince.RunStart

            wire.startsWith(
                "event:",
            ) && wire.length > EVENT_PREFIX.length -> TimerSince.EventFired(wire.removePrefix(EVENT_PREFIX))

            else -> fail("${n.path}.since", "expected run or event:<id>")
        }
    }

    fun action(n: Node): Action =
        when (val type = n.str("type")) {
            "tap" -> {
                tap(n)
            }

            "swipe" -> {
                Action.Swipe(
                    from = n.point("from"),
                    to = n.point("to"),
                    durationMs = n.long("durationMs"),
                    intervalMs = n.longOr("intervalMs", Limits.DEFAULT_INTERVAL_MS),
                    timingJitterMs = n.intOr("timingJitterMs", 0),
                )
            }

            "curve" -> {
                Action.Curve(
                    points = n.arr("points").mapIndexed { i, v -> pointOf("${n.path}.points[$i]", v) },
                    durationMs = n.long("durationMs"),
                    intervalMs = n.longOr("intervalMs", Limits.DEFAULT_INTERVAL_MS),
                    timingJitterMs = n.intOr("timingJitterMs", 0),
                )
            }

            "multi" -> {
                Action.Multi(
                    strokes = n.items("strokes").map { Stroke(it.point("from"), it.point("to")) },
                    durationMs = n.long("durationMs"),
                    intervalMs = n.longOr("intervalMs", Limits.DEFAULT_INTERVAL_MS),
                    timingJitterMs = n.intOr("timingJitterMs", 0),
                )
            }

            "wait" -> {
                Action.Wait(n.long("ms"))
            }

            "counter" -> {
                Action.CounterOp(n.str("id"), counterOp(n), n.int("value"))
            }

            "runScript" -> {
                Action.RunScript(n.str("scriptId"))
            }

            "stop" -> {
                Action.Stop
            }

            else -> {
                fail("${n.path}.type", "unknown action \"$type\"")
            }
        }

    private fun tap(n: Node): Action.Tap {
        val target =
            if (n.has("at")) {
                if (n.str("at") != "match") fail("${n.path}.at", "expected \"match\"")
                Target.AtMatch(n.pointOrNull("offset") ?: Point(0, 0))
            } else {
                Target.At(Point(n.int("x"), n.int("y")))
            }
        return Action.Tap(
            at = target,
            holdMs = n.longOr("holdMs", Limits.DEFAULT_HOLD_MS),
            jitterPx = n.intOr("jitterPx", 0),
            intervalMs = n.longOr("intervalMs", Limits.DEFAULT_INTERVAL_MS),
            timingJitterMs = n.intOr("timingJitterMs", 0),
        )
    }

    private fun counterOp(n: Node): CounterOpKind =
        when (n.str("op")) {
            "set" -> CounterOpKind.Set
            "add" -> CounterOpKind.Add
            "subtract" -> CounterOpKind.Subtract
            else -> fail("${n.path}.op", "expected set, add or subtract")
        }

    private const val HEX_COLOR_LENGTH = 6
    private const val HEX_RADIX = 16
    private const val EVENT_PREFIX = "event:"
}
