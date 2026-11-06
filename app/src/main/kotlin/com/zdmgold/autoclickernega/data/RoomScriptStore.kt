package com.zdmgold.autoclickernega.data

import android.database.sqlite.SQLiteException
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.ScriptSummary
import com.zdmgold.autoclickernega.core.port.WallClock
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptValidator
import com.zdmgold.autoclickernega.data.db.ScriptDao
import com.zdmgold.autoclickernega.data.db.ScriptMapping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Stores scripts in Room. Every failure becomes a typed storage error, never an exception for the caller. */
class RoomScriptStore(
    private val dao: ScriptDao,
    private val wall: WallClock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ScriptStore {
    override val scripts: Flow<List<ScriptSummary>> =
        dao
            .summaries()
            .map { rows -> rows.map(ScriptMapping::toSummary) }
            .catch { emit(emptyList()) }

    override suspend fun get(id: String): Outcome<Script?> =
        guarded {
            val entity = dao.get(id) ?: return@guarded null
            when (val script = ScriptMapping.toScript(entity)) {
                is Outcome.Success -> script.value
                is Outcome.Failure -> throw StoredScriptException(script.error)
            }
        }

    override suspend fun save(script: Script): Outcome<Script> {
        val withId = if (script.id.isBlank()) script.copy(id = newId()) else script
        val known = knownScripts(withId)
        return when (val checked = ScriptValidator.validate(withId) { known[it] }) {
            is Outcome.Failure -> {
                checked
            }

            is Outcome.Success -> {
                guarded {
                    dao.upsert(ScriptMapping.toEntity(checked.value, wall.epochMs()))
                    checked.value
                }
            }
        }
    }

    override suspend fun delete(id: String): Outcome<Script?> =
        guarded {
            val entity = dao.get(id) ?: return@guarded null
            val script = (ScriptMapping.toScript(entity) as? Outcome.Success)?.value
            dao.delete(id)
            script
        }

    override suspend fun restore(script: Script): Outcome<Unit> =
        guarded {
            dao.upsert(ScriptMapping.toEntity(script, wall.epochMs()))
        }

    override suspend fun rename(
        id: String,
        name: String,
    ): Outcome<Unit> =
        when (val current = get(id)) {
            is Outcome.Failure -> {
                current
            }

            is Outcome.Success -> {
                val script = current.value ?: return missing(id)
                when (val saved = save(script.copy(name = name))) {
                    is Outcome.Success -> Outcome.Success(Unit)
                    is Outcome.Failure -> saved
                }
            }
        }

    override suspend fun duplicate(
        id: String,
        newName: String,
    ): Outcome<Script> =
        when (val current = get(id)) {
            is Outcome.Failure -> {
                current
            }

            is Outcome.Success -> {
                val script = current.value ?: return missing(id)
                save(script.copy(id = newId(), name = newName))
            }
        }

    /** The scripts that [script] runs, and the ones they run, so a loop across scripts is caught on save. */
    private suspend fun knownScripts(script: Script): Map<String, Script> {
        val found = LinkedHashMap<String, Script>()
        val queue = ArrayDeque(ScriptValidator.runTargets(script))
        while (queue.isNotEmpty() && found.size < MAX_LOOKUPS) {
            val id = queue.removeFirst()
            val other = if (id in found || id == script.id) null else (get(id) as? Outcome.Success)?.value
            if (other != null) {
                found[id] = other
                queue.addAll(ScriptValidator.runTargets(other))
            }
        }
        return found
    }

    private fun missing(id: String) = Outcome.Failure(AppError.Storage("no script with id $id"))

    private class StoredScriptException(
        val error: AppError,
    ) : Exception(error.code)

    private suspend fun <T> guarded(block: suspend () -> T): Outcome<T> =
        try {
            Outcome.Success(block())
        } catch (e: SQLiteException) {
            Outcome.Failure(AppError.Storage(e.message.orEmpty()))
        } catch (e: StoredScriptException) {
            Outcome.Failure(e.error)
        }

    private companion object {
        const val MAX_LOOKUPS = 30
    }
}
