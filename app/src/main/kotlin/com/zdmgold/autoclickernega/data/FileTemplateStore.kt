package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** PNG crops under templates/<scriptId>/<templateId>.png. Ids are checked, so no path can escape the folder. */
class FileTemplateStore(
    private val root: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : TemplateStore {
    override suspend fun save(
        scriptId: String,
        templateId: String,
        png: ByteArray,
    ): Outcome<Unit> =
        run(scriptId, templateId) { target ->
            target.parentFile?.mkdirs()
            val temp = File(target.path + ".tmp")
            temp.writeBytes(png)
            if (!temp.renameTo(target)) {
                target.delete()
                if (!temp.renameTo(target)) throw IOException("could not replace ${target.name}")
            }
        }

    override suspend fun load(
        scriptId: String,
        templateId: String,
    ): Outcome<ByteArray?> = run(scriptId, templateId) { target -> if (target.isFile) target.readBytes() else null }

    override suspend fun delete(
        scriptId: String,
        templateId: String,
    ): Outcome<Unit> = run(scriptId, templateId) { target -> target.delete() }.let { done(it) }

    override suspend fun deleteAll(scriptId: String): Outcome<Unit> =
        guarded {
            val dir = scriptDir(scriptId)
            if (dir.exists() && !dir.deleteRecursively()) throw IOException("could not delete ${dir.name}")
        }

    override suspend fun list(scriptId: String): Outcome<List<String>> =
        guarded {
            val files = scriptDir(scriptId).listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) }.orEmpty()
            files.map { it.name.removeSuffix(EXTENSION) }.sorted()
        }

    private fun done(result: Outcome<Boolean>): Outcome<Unit> =
        when (result) {
            is Outcome.Success -> Outcome.Success(Unit)
            is Outcome.Failure -> result
        }

    private fun scriptDir(scriptId: String): File {
        require(SAFE.matches(scriptId)) { "unsafe script id" }
        return File(root, scriptId)
    }

    private suspend fun <T> run(
        scriptId: String,
        templateId: String,
        block: (File) -> T,
    ): Outcome<T> =
        guarded {
            require(SAFE.matches(templateId)) { "unsafe template id" }
            block(File(scriptDir(scriptId), templateId + EXTENSION))
        }

    private suspend fun <T> guarded(block: () -> T): Outcome<T> =
        withContext(io) {
            try {
                Outcome.Success(block())
            } catch (e: IOException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            } catch (e: IllegalArgumentException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            }
        }

    private companion object {
        const val EXTENSION = ".png"
        val SAFE = Regex("[A-Za-z0-9_-]{1,64}")
    }
}
