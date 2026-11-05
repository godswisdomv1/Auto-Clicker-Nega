package com.zdmgold.autoclickernega.core.json

class JsonParseException(
    message: String,
    val offset: Int,
) : Exception("$message at offset $offset")

/** Strict RFC 8259 parsing with a nesting limit. Duplicate keys, trailing commas and trailing text are errors. */
object Json {
    const val MAX_DEPTH = 32

    fun parse(text: String): JsonValue = Parser(text.removePrefix("\uFEFF")).parseDocument()
}

private class Parser(
    private val s: String,
) {
    private var i = 0

    fun parseDocument(): JsonValue {
        skipWhitespace()
        val value = parseValue(0)
        skipWhitespace()
        if (i != s.length) fail("Unexpected text after the value")
        return value
    }

    private fun fail(message: String): Nothing = throw JsonParseException(message, i)

    private fun skipWhitespace() {
        while (i < s.length && s[i] in WHITESPACE) i++
    }

    private fun peek(): Char = if (i < s.length) s[i] else fail("Unexpected end of input")

    private fun expect(c: Char) {
        if (peek() != c) fail("Expected '$c'")
        i++
    }

    private fun parseValue(depth: Int): JsonValue {
        if (depth > Json.MAX_DEPTH) fail("Nesting is too deep")
        return when (val c = peek()) {
            '{' -> parseObject(depth)
            '[' -> parseArray(depth)
            '"' -> JsonString(parseString())
            't' -> literal("true", JsonBool(true))
            'f' -> literal("false", JsonBool(false))
            'n' -> literal("null", JsonNull)
            else -> if (c == '-' || c in '0'..'9') parseNumber() else fail("Unexpected character '$c'")
        }
    }

    private fun literal(
        word: String,
        value: JsonValue,
    ): JsonValue {
        if (!s.startsWith(word, i)) fail("Invalid literal")
        i += word.length
        return value
    }

    private fun parseObject(depth: Int): JsonObject {
        expect('{')
        val fields = LinkedHashMap<String, JsonValue>()
        skipWhitespace()
        if (peek() == '}') {
            i++
            return JsonObject(fields)
        }
        while (true) {
            skipWhitespace()
            if (peek() != '"') fail("Expected a string key")
            val key = parseString()
            if (fields.containsKey(key)) fail("Duplicate key \"$key\"")
            skipWhitespace()
            expect(':')
            skipWhitespace()
            fields[key] = parseValue(depth + 1)
            skipWhitespace()
            if (peek() == ',') {
                i++
            } else {
                expect('}')
                return JsonObject(fields)
            }
        }
    }

    private fun parseArray(depth: Int): JsonArray {
        expect('[')
        val items = ArrayList<JsonValue>()
        skipWhitespace()
        if (peek() == ']') {
            i++
            return JsonArray(items)
        }
        while (true) {
            skipWhitespace()
            items += parseValue(depth + 1)
            skipWhitespace()
            if (peek() == ',') {
                i++
            } else {
                expect(']')
                return JsonArray(items)
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val out = StringBuilder()
        while (true) {
            val c = peek()
            i++
            when {
                c == '"' -> return out.toString()
                c == '\\' -> out.append(parseEscape())
                c < ' ' -> fail("Control character in string")
                else -> out.append(c)
            }
        }
    }

    private fun parseEscape(): Char {
        val c = peek()
        i++
        return when (c) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicode()
            else -> fail("Invalid escape")
        }
    }

    private fun parseUnicode(): Char {
        if (i + HEX_DIGITS > s.length) fail("Truncated unicode escape")
        val code = s.substring(i, i + HEX_DIGITS).toIntOrNull(HEX_RADIX) ?: fail("Invalid unicode escape")
        i += HEX_DIGITS
        return code.toChar()
    }

    private fun parseNumber(): JsonNumber {
        val start = i
        if (peek() == '-') i++
        when (peek()) {
            '0' -> i++
            in '1'..'9' -> digits()
            else -> fail("Invalid number")
        }
        if (i < s.length && s[i] == '.') {
            i++
            if (digits() == 0) fail("Invalid number")
        }
        if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
            i++
            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
            if (digits() == 0) fail("Invalid number")
        }
        return JsonNumber(s.substring(start, i))
    }

    private fun digits(): Int {
        val start = i
        while (i < s.length && s[i] in '0'..'9') i++
        return i - start
    }

    private companion object {
        const val WHITESPACE = " \t\n\r"
        const val HEX_DIGITS = 4
        const val HEX_RADIX = 16
    }
}
