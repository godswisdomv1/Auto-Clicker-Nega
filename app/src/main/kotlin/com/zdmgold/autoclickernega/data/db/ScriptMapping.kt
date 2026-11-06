package com.zdmgold.autoclickernega.data.db

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.port.ScriptMode
import com.zdmgold.autoclickernega.core.port.ScriptSummary
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptCodec

/** Converts between the domain script and its database row. */
object ScriptMapping {
    private const val SIMPLE = "simple"
    private const val SMART = "smart"

    fun toEntity(
        script: Script,
        updatedAtMs: Long,
    ): ScriptEntity =
        ScriptEntity(
            id = script.id,
            name = script.name,
            mode = if (script.body is ScriptBody.Simple) SIMPLE else SMART,
            itemCount = script.itemCount,
            updatedAt = updatedAtMs,
            json = ScriptCodec.encode(script),
        )

    fun toSummary(row: ScriptSummaryRow): ScriptSummary =
        ScriptSummary(
            id = row.id,
            name = row.name,
            mode = if (row.mode == SIMPLE) ScriptMode.Simple else ScriptMode.Smart,
            itemCount = row.itemCount,
            updatedAtMs = row.updatedAt,
        )

    /** Reads the stored JSON. A row that no longer decodes is reported, never silently dropped. */
    fun toScript(entity: ScriptEntity): Outcome<Script> =
        when (val decoded = ScriptCodec.decode(entity.json)) {
            is Outcome.Success -> Outcome.Success(decoded.value.copy(id = entity.id))
            is Outcome.Failure -> Outcome.Failure(AppError.Storage("stored script ${entity.id} is unreadable"))
        }
}
