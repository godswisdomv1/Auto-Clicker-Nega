package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonParseException
import com.zdmgold.autoclickernega.core.json.encode
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome

/** JSON to and from [Script], schema 1, for both Simple and Smart mode. */
object ScriptCodec {
    fun encode(script: Script): String = ScriptJsonWriter.script(script).encode()

    /** Reads the structure. Semantic limits are checked separately by [ScriptValidator]. */
    fun decode(json: String): Outcome<Script> {
        if (json.toByteArray(Charsets.UTF_8).size > Limits.MAX_SCRIPT_JSON_BYTES) {
            return malformed(ImportProblem.TooLarge, "script is larger than ${Limits.MAX_SCRIPT_JSON_BYTES} bytes")
        }
        val root =
            try {
                Json.parse(json)
            } catch (e: JsonParseException) {
                return malformed(ImportProblem.BadJson, e.message.orEmpty())
            }
        val obj = root as? JsonObject ?: return malformed(ImportProblem.BadJson, "the top level must be an object")
        return decodeObject(obj)
    }

    fun decodeObject(raw: JsonObject): Outcome<Script> =
        when (val migrated = ScriptMigrator.migrate(raw)) {
            is Outcome.Failure -> {
                migrated
            }

            is Outcome.Success -> {
                try {
                    Outcome.Success(ScriptJsonReader.script(migrated.value))
                } catch (e: DecodeException) {
                    malformed(ImportProblem.InvalidField, e.message.orEmpty())
                }
            }
        }

    private fun malformed(
        reason: ImportProblem,
        detail: String,
    ): Outcome.Failure = Outcome.Failure(AppError.MalformedImport(reason, detail))
}
