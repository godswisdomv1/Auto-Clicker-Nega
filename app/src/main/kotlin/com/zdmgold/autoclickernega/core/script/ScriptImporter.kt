package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.result.flatMap

/** Turns picked or pasted bytes into a checked script, whether they are JSON or a .nega bundle. */
object ScriptImporter {
    private const val ZIP_MAGIC_0 = 0x50
    private const val ZIP_MAGIC_1 = 0x4B

    /** A script plus the images it uses. [templates] is empty for plain JSON. */
    fun import(
        bytes: ByteArray,
        lookup: (String) -> Script? = { null },
    ): Outcome<Bundle> =
        if (isZip(bytes)) {
            NegaBundle.unpack(bytes).flatMap { bundle -> checked(bundle.script, lookup).mapBundle(bundle) }
        } else {
            ScriptCodec.decode(String(bytes, Charsets.UTF_8)).flatMap { script ->
                checked(script, lookup).mapBundle(Bundle(script, emptyMap()))
            }
        }

    /** Plain JSON for a script with no images, a .nega bundle for one with images. */
    fun export(
        script: Script,
        templates: Map<String, ByteArray>,
        appVersion: String,
    ): Outcome<Export> =
        if (script.templates.isEmpty()) {
            val text = ScriptCodec.encode(script)
            if (text.toByteArray(Charsets.UTF_8).size > Limits.MAX_SCRIPT_JSON_BYTES) {
                Outcome.Failure(AppError.MalformedImport(ImportProblem.TooLarge))
            } else {
                Outcome.Success(Export(text.toByteArray(Charsets.UTF_8), ExportKind.Json))
            }
        } else {
            NegaBundle.pack(script, templates, appVersion).flatMap { Outcome.Success(Export(it, ExportKind.Bundle)) }
        }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0].toInt() == ZIP_MAGIC_0 && bytes[1].toInt() == ZIP_MAGIC_1

    private fun checked(
        script: Script,
        lookup: (String) -> Script?,
    ): Outcome<Script> = ScriptValidator.validate(script, lookup)

    private fun Outcome<Script>.mapBundle(original: Bundle): Outcome<Bundle> =
        when (this) {
            is Outcome.Success -> Outcome.Success(original.copy(script = value))
            is Outcome.Failure -> this
        }
}

enum class ExportKind { Json, Bundle }

class Export(
    val bytes: ByteArray,
    val kind: ExportKind,
) {
    val extension: String get() = if (kind == ExportKind.Json) "json" else "nega"
}
