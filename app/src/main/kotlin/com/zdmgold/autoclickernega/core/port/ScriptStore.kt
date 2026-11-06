package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.Flow

enum class ScriptMode { Simple, Smart }

/** What Home needs to list a script, without loading its whole body. */
data class ScriptSummary(
    val id: String,
    val name: String,
    val mode: ScriptMode,
    val itemCount: Int,
    val updatedAtMs: Long,
)

interface ScriptStore {
    /** All scripts, newest first. Emits again whenever anything changes. */
    val scripts: Flow<List<ScriptSummary>>

    /** The script, or null when no script has this id. */
    suspend fun get(id: String): Outcome<Script?>

    /** Validates and stores a script. A blank id gets a new one. Returns the stored script. */
    suspend fun save(script: Script): Outcome<Script>

    /** Removes a script and returns it, so the caller can offer undo. */
    suspend fun delete(id: String): Outcome<Script?>

    /** Puts back a script removed by [delete], with its original id. */
    suspend fun restore(script: Script): Outcome<Unit>

    suspend fun rename(
        id: String,
        name: String,
    ): Outcome<Unit>

    suspend fun duplicate(
        id: String,
        newName: String,
    ): Outcome<Script>
}
