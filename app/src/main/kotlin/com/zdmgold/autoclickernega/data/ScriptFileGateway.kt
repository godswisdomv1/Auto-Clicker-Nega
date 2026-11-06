package com.zdmgold.autoclickernega.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** A file ready to be written and shared. [extension] has no dot. */
class ScriptExportFile(
    val name: String,
    val extension: String,
    val bytes: ByteArray,
)

/** Reads picked files with size caps, writes exports to a private cache folder and builds the share intent. */
class ScriptFileGateway(
    private val context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** The bytes behind a picked file, capped at the largest bundle. */
    suspend fun read(uri: Uri): Outcome<ByteArray> =
        withContext(io) {
            try {
                val stream =
                    context.contentResolver.openInputStream(uri)
                        ?: return@withContext Outcome.Failure(AppError.Storage("the file could not be opened"))
                stream.use { readCapped(it, Limits.MAX_BUNDLE_BYTES) }
            } catch (e: IOException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            } catch (e: SecurityException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            }
        }

    /** Writes an export under cache/exports and returns a shareable content link for it. */
    suspend fun writeExport(file: ScriptExportFile): Outcome<Uri> =
        withContext(io) {
            try {
                val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val target = File(dir, safeName(file.name) + "." + file.extension)
                target.writeBytes(file.bytes)
                Outcome.Success(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target))
            } catch (e: IOException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            } catch (e: IllegalArgumentException) {
                Outcome.Failure(AppError.Storage(e.message.orEmpty()))
            }
        }

    /** The system share sheet for an exported file. */
    fun shareIntent(
        uri: Uri,
        mimeType: String,
        title: String,
    ): Intent {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(send, title)
    }

    private fun safeName(name: String): String =
        name
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .ifEmpty { "script" }
            .take(MAX_NAME)

    private companion object {
        const val EXPORT_DIR = "exports"
        const val MAX_NAME = 60
    }
}
