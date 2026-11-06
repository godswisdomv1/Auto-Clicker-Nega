package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Delete all local data: settings, the database, reference crops, language packs and the crash log.
 * Every step runs even if an earlier one fails, and the result names what could not be removed.
 */
class DataWiper(
    private val clearSettings: suspend () -> Outcome<Unit>,
    private val closeDatabase: () -> Unit,
    private val databaseFiles: List<File>,
    private val directories: List<File>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun wipeAll(): Outcome<Unit> {
        val problems = mutableListOf<String>()
        (clearSettings() as? Outcome.Failure)?.let { problems += "settings" }
        withContext(io) {
            runCatching(closeDatabase).onFailure { problems += "database connection" }
            databaseFiles.forEach { base ->
                listOf(base, File(base.path + "-wal"), File(base.path + "-shm"), File(base.path + "-journal"))
                    .filter { it.exists() }
                    .forEach { if (!it.delete()) problems += it.name }
            }
            directories.filter { it.exists() }.forEach { if (!it.deleteRecursively()) problems += it.name }
        }
        return if (problems.isEmpty()) {
            Outcome.Success(Unit)
        } else {
            Outcome.Failure(AppError.Storage("could not delete: ${problems.joinToString()}"))
        }
    }
}
