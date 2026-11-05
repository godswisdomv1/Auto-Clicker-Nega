package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.jsonObject
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptMigratorTest {
    @Test
    fun schemaOneIsTheIdentity() {
        val raw = jsonObject("schema" to JsonNumber.of(1), "name" to JsonString("x"))
        assertEquals(Outcome.Success(raw), ScriptMigrator.migrate(raw))
    }

    @Test
    fun aNewerSchemaIsRefusedWithBothVersions() {
        val r = ScriptMigrator.migrate(jsonObject("schema" to JsonNumber.of(7))) as Outcome.Failure
        assertEquals(AppError.SchemaTooNew(found = 7, supported = 1), r.error)
    }

    @Test
    fun missingZeroAndNonNumericSchemasAreInvalid() {
        listOf(
            JsonObject(emptyMap()),
            jsonObject("schema" to JsonNumber.of(0)),
            jsonObject("schema" to JsonNumber.of(-3)),
            jsonObject("schema" to JsonString("1")),
        ).forEach {
            val r = ScriptMigrator.migrate(it) as Outcome.Failure
            assertEquals(ImportProblem.InvalidField, (r.error as AppError.MalformedImport).reason)
        }
    }
}
