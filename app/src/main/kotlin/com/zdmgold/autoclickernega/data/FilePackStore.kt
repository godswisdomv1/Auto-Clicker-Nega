package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonParseException
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.encode
import com.zdmgold.autoclickernega.core.json.jsonObject
import com.zdmgold.autoclickernega.core.port.InstalledPack
import com.zdmgold.autoclickernega.core.port.PackStore
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Language packs in their own folder: packs/<lang>/ with a small pack.json recording the verified hash. */
class FilePackStore(
    private val root: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PackStore {
    override fun directoryFor(lang: String): Outcome<File> =
        if (SAFE.matches(lang)) {
            Outcome.Success(File(root, lang))
        } else {
            Outcome.Failure(AppError.Storage("unsafe language code"))
        }

    override suspend fun installed(): Outcome<Map<String, InstalledPack>> =
        guarded {
            val found = linkedMapOf<String, InstalledPack>()
            root.listFiles { f -> f.isDirectory }.orEmpty().sortedBy { it.name }.forEach { dir ->
                readRecord(dir)?.let { found[dir.name] = it }
            }
            found
        }

    override suspend fun markInstalled(pack: InstalledPack): Outcome<Unit> =
        withDir(pack.lang) { dir ->
            dir.mkdirs()
            val record =
                jsonObject(
                    "sha256" to JsonString(pack.sha256),
                    "installedAtMs" to JsonNumber.of(pack.installedAtMs),
                )
            val temp = File(dir, "$RECORD.tmp")
            temp.writeText(record.encode())
            if (!temp.renameTo(File(dir, RECORD))) {
                File(dir, RECORD).delete()
                if (!temp.renameTo(File(dir, RECORD))) throw IOException("could not write the pack record")
            }
        }

    override suspend fun remove(lang: String): Outcome<Unit> =
        withDir(lang) { dir ->
            if (dir.exists() && !dir.deleteRecursively()) throw IOException("could not delete the pack")
        }

    private fun readRecord(dir: File): InstalledPack? {
        val file = File(dir, RECORD)
        val root = if (file.isFile) parse(file.readText()) else null
        val hash = (root?.get("sha256") as? JsonString)?.value
        val at = (root?.get("installedAtMs") as? JsonNumber)?.toLongOrNull()
        return if (hash != null && at != null) InstalledPack(dir.name, hash, at) else null
    }

    private fun parse(text: String): JsonObject? =
        try {
            Json.parse(text) as? JsonObject
        } catch (e: JsonParseException) {
            null.also { e.message }
        }

    private suspend fun withDir(
        lang: String,
        block: (File) -> Unit,
    ): Outcome<Unit> {
        val dir =
            when (val d = directoryFor(lang)) {
                is Outcome.Success -> d.value
                is Outcome.Failure -> return d
            }
        return guarded { block(dir) }
    }

    private suspend fun <T> guarded(block: () -> T): Outcome<T> =
        withContext(io) {
            try {
                Outcome.Success(block())
            } catch (e: IOException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            }
        }

    private companion object {
        const val RECORD = "pack.json"
        val SAFE = Regex("[A-Za-z0-9_-]{2,16}")
    }
}
