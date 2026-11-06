package com.zdmgold.autoclickernega.core.ocr

import java.util.Locale

/** The languages text detection supports, named by the same short codes scripts use. */
object OcrLanguages {
    val all: List<String> = listOf("en", "es", "fr", "de", "pt", "ar", "hi", "ja", "ko", "zh", "he")

    private val rtl = setOf("ar", "he")

    fun isSupported(code: String): Boolean = code in all

    fun isRightToLeft(code: String): Boolean = code in rtl

    /**
     * Maps a language tag such as zh-Hans, pt-BR or the legacy iw to one of [all], or null when the language
     * is not supported.
     */
    fun normalize(tag: String): String? {
        val base = Locale.forLanguageTag(tag.replace('_', '-')).language.lowercase(Locale.ROOT)
        val code = if (base == "iw") "he" else base
        return code.takeIf { it in all }
    }
}
