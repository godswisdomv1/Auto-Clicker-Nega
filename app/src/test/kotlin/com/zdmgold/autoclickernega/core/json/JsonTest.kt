package com.zdmgold.autoclickernega.core.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTest {
    private fun fails(text: String): Boolean =
        try {
            Json.parse(text)
            false
        } catch (e: JsonParseException) {
            e.offset >= 0
        }

    @Test
    fun parsesEveryValueKind() {
        val v = Json.parse("""{"a":[1,-2.5,3e2],"b":"x","c":true,"d":false,"e":null,"f":{}}""") as JsonObject
        assertEquals(JsonBool(true), v["c"])
        assertEquals(JsonNull, v["e"])
        assertEquals(JsonObject(emptyMap()), v["f"])
        val a = v["a"] as JsonArray
        assertEquals(3, a.items.size)
        assertEquals(-2.5, (a.items[1] as JsonNumber).toDouble(), 0.0)
        assertEquals(1L, (a.items[0] as JsonNumber).toLongOrNull())
    }

    @Test
    fun decodesEscapesAndUnicode() {
        val v = Json.parse("\"a\\n\\t\\\"\\\\\\/\\u00e9\\u4e2d\"") as JsonString
        assertEquals("a\n\t\"\\/é中", v.value)
    }

    @Test
    fun stripsAByteOrderMark() {
        assertEquals(JsonNumber("1"), Json.parse("\uFEFF 1 "))
    }

    @Test
    fun rejectsInvalidDocuments() {
        listOf(
            "",
            "{",
            "[1,]",
            "{\"a\":1,}",
            "{\"a\":1 \"b\":2}",
            "{a:1}",
            "'x'",
            "01",
            "1.",
            "-",
            "1e",
            "+1",
            "tru",
            "nul",
            "\"abc",
            "\"a\nb\"",
            "\"\\x\"",
            "\"\\u12\"",
            "1 2",
            "{} {}",
            "{\"a\":1,\"a\":2}",
        ).forEach { assertTrue("should fail: $it", fails(it)) }
    }

    @Test
    fun limitsNestingDepth() {
        val ok = "[".repeat(Json.MAX_DEPTH) + "]".repeat(Json.MAX_DEPTH)
        Json.parse(ok)
        val tooDeep = "[".repeat(Json.MAX_DEPTH + 2) + "]".repeat(Json.MAX_DEPTH + 2)
        assertTrue(fails(tooDeep))
        assertTrue(fails("[".repeat(100_000)))
    }

    @Test
    fun encodesAndRoundTrips() {
        val original =
            jsonObject(
                "name" to JsonString("quote \" backslash \\ newline \n tab \t bell \u0007 é 中"),
                "list" to jsonArray(listOf(JsonNumber.of(1), JsonNumber.of(0.85f), JsonBool(false), JsonNull)),
                "nested" to jsonObject("k" to JsonString("v")),
            )
        val text = original.encode()
        assertEquals(original, Json.parse(text))
        assertTrue(text.contains("\\u0007"))
        assertEquals(text, Json.parse(text).encode())
    }

    @Test
    fun keepsKeyOrder() {
        val text = jsonObject("z" to JsonNumber.of(1), "a" to JsonNumber.of(2)).encode()
        assertEquals("""{"z":1,"a":2}""", text)
    }
}
