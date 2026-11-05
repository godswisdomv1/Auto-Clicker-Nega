package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome

/**
 * Brings a script up to the current schema. At schema 1 it is the identity, and it exists from day
 * one so a future schema only has to add a step here.
 */
object ScriptMigrator {
    fun migrate(raw: JsonObject): Outcome<JsonObject> {
        val schema =
            (raw["schema"] as? JsonNumber)?.toLongOrNull()
                ?: return Outcome.Failure(
                    AppError.MalformedImport(ImportProblem.InvalidField, "schema: expected an integer"),
                )
        return when {
            schema > Script.SCHEMA_VERSION -> {
                Outcome.Failure(
                    AppError.SchemaTooNew(
                        found = schema.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        supported = Script.SCHEMA_VERSION,
                    ),
                )
            }

            schema < 1 -> {
                Outcome.Failure(AppError.MalformedImport(ImportProblem.InvalidField, "schema: must be 1 or more"))
            }

            else -> {
                Outcome.Success(raw)
            }
        }
    }
}
