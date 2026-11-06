package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.json.Json
import com.zdmgold.autoclickernega.core.json.JsonArray
import com.zdmgold.autoclickernega.core.json.JsonObject
import com.zdmgold.autoclickernega.core.json.JsonParseException
import com.zdmgold.autoclickernega.core.json.JsonString
import com.zdmgold.autoclickernega.core.json.JsonValue
import com.zdmgold.autoclickernega.core.result.Outcome

/**
 * The app's built-in list of language packs and their hashes, read from a bundled file of the form
 * {"packs":[{"lang":"ar","sha256":"..."}]}. Every pack the app downloads is checked against this list.
 */
class PackManifest(
    val specs: List<PackSpec>,
) {
    private val byLang = specs.associateBy { it.lang }

    fun specFor(lang: String): PackSpec? = byLang[lang]

    companion object {
        private val HASH = Regex("[0-9a-f]{64}")

        fun parse(text: String): Outcome<PackManifest> =
            try {
                Outcome.Success(PackManifest(readSpecs(text)))
            } catch (e: ManifestException) {
                bad(e.message.orEmpty())
            }

        private fun readSpecs(text: String): List<PackSpec> {
            val root =
                try {
                    Json.parse(text) as? JsonObject
                } catch (e: JsonParseException) {
                    throw ManifestException(e.message.orEmpty())
                } ?: throw ManifestException("the top level must be an object")
            val packs = (root["packs"] as? JsonArray)?.items ?: throw ManifestException("packs: expected a list")
            val specs = mutableListOf<PackSpec>()
            packs.forEachIndexed { i, item -> specs += readSpec(i, item, specs) }
            return specs
        }

        private fun readSpec(
            i: Int,
            item: JsonValue,
            seen: List<PackSpec>,
        ): PackSpec {
            val o = item as? JsonObject ?: throw ManifestException("packs[$i]: expected an object")
            val lang = (o["lang"] as? JsonString)?.value
            val hash = (o["sha256"] as? JsonString)?.value
            if (lang == null ||
                !OcrLanguages.isSupported(lang)
            ) {
                throw ManifestException("packs[$i].lang: unsupported language")
            }
            if (hash == null || !HASH.matches(hash)) {
                throw ManifestException("packs[$i].sha256: expected 64 lowercase hex digits")
            }
            if (seen.any { it.lang == lang }) throw ManifestException("packs[$i].lang: duplicate language $lang")
            return PackSpec(lang, hash)
        }

        private fun bad(detail: String): Outcome.Failure =
            Outcome.Failure(AppError.MalformedImport(ImportProblem.InvalidField, detail))
    }
}

private class ManifestException(
    message: String,
) : Exception(message)
