package com.zdmgold.autoclickernega.core.json

import java.util.Locale

/** A small, strict JSON model. Scripts are untrusted input, so parsing has a nesting limit. */
sealed interface JsonValue

data class JsonObject(
    val fields: Map<String, JsonValue>,
) : JsonValue {
    operator fun get(key: String): JsonValue? = fields[key]
}

data class JsonArray(
    val items: List<JsonValue>,
) : JsonValue

data class JsonString(
    val value: String,
) : JsonValue

/** A number kept as its source text, so integers never pass through a floating point value. */
data class JsonNumber(
    val raw: String,
) : JsonValue {
    fun toLongOrNull(): Long? = raw.toLongOrNull()

    fun toDouble(): Double = raw.toDouble()

    companion object {
        fun of(value: Long): JsonNumber = JsonNumber(value.toString())

        fun of(value: Int): JsonNumber = JsonNumber(value.toString())

        fun of(value: Float): JsonNumber {
            require(value.isFinite()) { "JSON numbers must be finite" }
            return JsonNumber(value.toString())
        }

        fun of(value: Double): JsonNumber {
            require(value.isFinite()) { "JSON numbers must be finite" }
            return JsonNumber(value.toString())
        }
    }
}

data class JsonBool(
    val value: Boolean,
) : JsonValue

data object JsonNull : JsonValue

fun jsonObject(vararg fields: Pair<String, JsonValue>): JsonObject = JsonObject(linkedMapOf(*fields))

fun jsonArray(items: List<JsonValue>): JsonArray = JsonArray(items)

/** Compact JSON text. Keys keep their insertion order, so output is reproducible. */
fun JsonValue.encode(): String = StringBuilder().also { appendJson(this, it) }.toString()

private fun appendJson(
    value: JsonValue,
    out: StringBuilder,
) {
    when (value) {
        is JsonObject -> {
            out.append('{')
            value.fields.entries.forEachIndexed { index, (key, item) ->
                if (index > 0) out.append(',')
                appendString(key, out)
                out.append(':')
                appendJson(item, out)
            }
            out.append('}')
        }

        is JsonArray -> {
            out.append('[')
            value.items.forEachIndexed { index, item ->
                if (index > 0) out.append(',')
                appendJson(item, out)
            }
            out.append(']')
        }

        is JsonString -> {
            appendString(value.value, out)
        }

        is JsonNumber -> {
            out.append(value.raw)
        }

        is JsonBool -> {
            out.append(value.value)
        }

        JsonNull -> {
            out.append("null")
        }
    }
}

private fun appendString(
    text: String,
    out: StringBuilder,
) {
    out.append('"')
    for (c in text) {
        when {
            c == '"' -> out.append("\\\"")
            c == '\\' -> out.append("\\\\")
            c == '\n' -> out.append("\\n")
            c == '\r' -> out.append("\\r")
            c == '\t' -> out.append("\\t")
            c < ' ' -> out.append(String.format(Locale.ROOT, "\\u%04x", c.code))
            else -> out.append(c)
        }
    }
    out.append('"')
}
