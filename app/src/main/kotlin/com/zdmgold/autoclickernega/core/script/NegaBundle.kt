package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonNumber
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonParseException
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.encode
import com.zdmgold.autoclickernega.core.json.jsonObject
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.TemplateRef
import com.zdmgold.autoclickernega.core.result.Outcome
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A script together with the reference images its image conditions use, keyed by template id. */
data class Bundle(
    val script: Script,
    val templates: Map<String, ByteArray>,
)

private class BundleException(
    val problem: ImportProblem,
    message: String,
) : Exception(message)

private fun bad(
    problem: ImportProblem,
    message: String,
): Nothing = throw BundleException(problem, message)

/**
 * The .nega file: a zip holding script.json, manifest.json and templates/<file>.png. The manifest carries
 * the SHA-256 of every template, and import checks each one. Studio reads and writes the same format.
 */
object NegaBundle {
    private const val SCRIPT_ENTRY = "script.json"
    private const val MANIFEST_ENTRY = "manifest.json"
    private const val TEMPLATE_DIR = "templates/"
    private const val MANIFEST_LIMIT = 64 * 1024
    private const val TEMPLATE_LIMIT = 2 * 1024 * 1024
    private const val TOTAL_LIMIT = 16 * 1024 * 1024
    private const val COPY_BUFFER = 8192
    private const val FIXED_ENTRY_TIME_MS = 315_532_800_000L
    private const val HEX = "0123456789abcdef"
    private const val NIBBLE_BITS = 4
    private const val NIBBLE_MASK = 0xF
    private val PNG_SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    private val FILE_NAME = Regex("[A-Za-z0-9_-]{1,64}\\.png")

    fun pack(
        script: Script,
        templates: Map<String, ByteArray>,
        appVersion: String,
    ): Outcome<ByteArray> =
        try {
            val files = script.templates.values.sortedBy { it.id }
            files.forEach { ref ->
                if (!FILE_NAME.matches(ref.file)) bad(ImportProblem.InvalidField, "template file name \"${ref.file}\"")
                if (ref.id !in templates) bad(ImportProblem.BadBundle, "missing image for ${ref.id}")
            }
            val bytes = zipBundle(script, files.map { it to templates.getValue(it.id) }, appVersion)
            if (bytes.size > Limits.MAX_BUNDLE_BYTES) bad(ImportProblem.TooLarge, "bundle is larger than 4 MB")
            Outcome.Success(bytes)
        } catch (e: BundleException) {
            Outcome.Failure(AppError.MalformedImport(e.problem, e.message.orEmpty()))
        }

    private fun zipBundle(
        script: Script,
        files: List<Pair<TemplateRef, ByteArray>>,
        appVersion: String,
    ): ByteArray {
        val hashes = files.associate { (ref, data) -> ref.id to JsonString(sha256Hex(data)) }
        val manifest =
            jsonObject(
                "schema" to JsonNumber.of(Script.SCHEMA_VERSION),
                "appVersion" to JsonString(appVersion),
                "templates" to JsonObject(hashes),
            )
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            put(zip, SCRIPT_ENTRY, ScriptCodec.encode(script).toByteArray(Charsets.UTF_8))
            put(zip, MANIFEST_ENTRY, manifest.encode().toByteArray(Charsets.UTF_8))
            files.forEach { (ref, data) -> put(zip, TEMPLATE_DIR + ref.file, data) }
        }
        return out.toByteArray()
    }

    fun unpack(bytes: ByteArray): Outcome<Bundle> =
        try {
            if (bytes.size > Limits.MAX_BUNDLE_BYTES) bad(ImportProblem.TooLarge, "bundle is larger than 4 MB")
            Outcome.Success(read(bytes))
        } catch (e: BundleException) {
            Outcome.Failure(AppError.MalformedImport(e.problem, e.message.orEmpty()))
        } catch (e: IOException) {
            Outcome.Failure(
                AppError.MalformedImport(ImportProblem.BadBundle, "not a valid zip: ${e.message.orEmpty()}"),
            )
        }

    private fun put(
        zip: ZipOutputStream,
        name: String,
        data: ByteArray,
    ) {
        val entry = ZipEntry(name)
        entry.time = FIXED_ENTRY_TIME_MS
        zip.putNextEntry(entry)
        zip.write(data)
        zip.closeEntry()
    }

    private fun read(bytes: ByteArray): Bundle {
        val entries = readEntries(bytes)
        val scriptBytes = entries[SCRIPT_ENTRY] ?: bad(ImportProblem.BadBundle, "script.json is missing")
        val manifestBytes = entries[MANIFEST_ENTRY] ?: bad(ImportProblem.BadBundle, "manifest.json is missing")
        val hashes = readManifest(manifestBytes)
        val script =
            when (val decoded = ScriptCodec.decode(String(scriptBytes, Charsets.UTF_8))) {
                is Outcome.Success -> decoded.value
                is Outcome.Failure -> throw decoded.error.asBundleException()
            }
        val used = HashSet<String>()
        val templates = linkedMapOf<String, ByteArray>()
        for (ref in script.templates.values) {
            val name = TEMPLATE_DIR + ref.file
            val data = entries[name] ?: bad(ImportProblem.BadBundle, "missing image ${ref.file}")
            if (!isPng(data)) bad(ImportProblem.BadBundle, "${ref.file} is not a PNG image")
            val expected = hashes[ref.id] ?: bad(ImportProblem.HashMismatch, "no hash for ${ref.id}")
            if (expected != sha256Hex(data)) bad(ImportProblem.HashMismatch, "hash of ${ref.file} does not match")
            used += name
            templates[ref.id] = data
        }
        val extra = entries.keys.filter { it != SCRIPT_ENTRY && it != MANIFEST_ENTRY && it !in used }
        if (extra.isNotEmpty()) bad(ImportProblem.BadBundle, "unexpected file ${extra.first()}")
        return Bundle(script, templates)
    }

    private fun AppError.asBundleException(): BundleException =
        when (this) {
            is AppError.MalformedImport -> BundleException(reason, detail)
            is AppError.SchemaTooNew -> BundleException(ImportProblem.InvalidField, "made by a newer version")
            else -> BundleException(ImportProblem.BadBundle, code)
        }

    private fun readManifest(bytes: ByteArray): Map<String, String> {
        val root =
            try {
                Json.parse(String(bytes, Charsets.UTF_8))
            } catch (e: JsonParseException) {
                bad(ImportProblem.BadBundle, "manifest.json: ${e.message.orEmpty()}")
            } as? JsonObject ?: bad(ImportProblem.BadBundle, "manifest.json must be an object")
        val schema =
            (root["schema"] as? JsonNumber)?.toLongOrNull() ?: bad(ImportProblem.BadBundle, "manifest has no schema")
        if (schema > Script.SCHEMA_VERSION) bad(ImportProblem.InvalidField, "made by a newer version")
        val templates = root["templates"] as? JsonObject ?: bad(ImportProblem.BadBundle, "manifest has no templates")
        return templates.fields.mapValues { (key, value) ->
            (value as? JsonString)?.value ?: bad(ImportProblem.BadBundle, "manifest hash for $key must be text")
        }
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (entry.isDirectory || !allowedName(name)) bad(ImportProblem.BadBundle, "unexpected entry \"$name\"")
                if (name in entries) bad(ImportProblem.BadBundle, "duplicate entry \"$name\"")
                if (entries.size >= Limits.MAX_TEMPLATES + 2) bad(ImportProblem.TooManyItems, "too many files")
                val limit =
                    when (name) {
                        SCRIPT_ENTRY -> Limits.MAX_SCRIPT_JSON_BYTES
                        MANIFEST_ENTRY -> MANIFEST_LIMIT
                        else -> TEMPLATE_LIMIT
                    }
                val data = readCapped(zip, limit)
                total += data.size
                if (total > TOTAL_LIMIT) bad(ImportProblem.TooLarge, "bundle expands to more than 16 MB")
                entries[name] = data
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun allowedName(name: String): Boolean =
        name == SCRIPT_ENTRY ||
            name == MANIFEST_ENTRY ||
            (name.startsWith(TEMPLATE_DIR) && FILE_NAME.matches(name.removePrefix(TEMPLATE_DIR)))

    private fun readCapped(
        stream: InputStream,
        limit: Int,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(COPY_BUFFER)
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            if (out.size() + n > limit) bad(ImportProblem.TooLarge, "a file in the bundle is too large")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun isPng(data: ByteArray): Boolean =
        data.size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { data[it] == PNG_SIGNATURE[it] }

    /** Lower case hex SHA-256, the same text Studio computes with SubtleCrypto. */
    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val out = StringBuilder(digest.size * 2)
        for (b in digest) {
            out.append(HEX[(b.toInt() shr NIBBLE_BITS) and NIBBLE_MASK]).append(HEX[b.toInt() and NIBBLE_MASK])
        }
        return out.toString()
    }
}
