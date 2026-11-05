package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonBool
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Runs the golden fixtures shared with Studio, so the two implementations cannot drift apart. */
class ContractFixturesTest {
    private val dir = File("../contract/fixtures")

    private fun outcomeCode(json: String): String? {
        val decoded = ScriptCodec.decode(json)
        val checked =
            when (decoded) {
                is Outcome.Success -> ScriptValidator.validate(decoded.value)
                is Outcome.Failure -> decoded
            }
        return when (checked) {
            is Outcome.Success -> {
                null
            }

            is Outcome.Failure -> {
                when (val e = checked.error) {
                    is AppError.MalformedImport -> e.reason.code
                    is AppError.SchemaTooNew -> "schema_too_new"
                    else -> e.code
                }
            }
        }
    }

    @Test
    fun everyFixtureBehavesAsExpected() {
        assertTrue("Missing ${dir.path}", dir.isDirectory)
        val expectations = Json.parse(File(dir, "expectations.json").readText()) as JsonObject
        assertTrue(expectations.fields.size >= 20)
        val files =
            dir
                .walkTopDown()
                .filter { it.isFile && it.name != "expectations.json" }
                .map { it.relativeTo(dir).path.replace('\\', '/') }
                .toSet()
        assertEquals("Every fixture needs an expectation and the other way round", files, expectations.fields.keys)
        for ((path, expected) in expectations.fields) {
            val spec = expected as JsonObject
            val actual = outcomeCode(File(dir, path).readText())
            if ((spec["ok"] as JsonBool).value) {
                assertEquals("$path should be valid", null, actual)
            } else {
                assertEquals("$path", (spec["code"] as JsonString).value, actual)
            }
        }
    }
}
