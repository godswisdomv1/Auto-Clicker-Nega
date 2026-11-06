package com.zdmgold.autoclickernega.core.ocr

/**
 * Reads a number out of recognised text, whatever digits and separators the screen uses.
 *
 * Digits of any script are accepted (Arabic-Indic, Devanagari, full width and others). Separator rules, in order:
 * spaces, apostrophes and the Arabic thousands mark are always grouping; the Arabic decimal mark is always decimal;
 * when both "," and "." appear the last one is the decimal point; a repeated separator is grouping; a single "."
 * is decimal; a single "," followed by exactly three digits is grouping ("1,234" is 1234), otherwise decimal.
 */
object NumberParser {
    private const val ARABIC_THOUSANDS = '\u066C'
    private const val ARABIC_DECIMAL = '\u066B'
    private const val MINUS_SIGN = '\u2212'
    private const val GROUP_SIZE = 3
    private const val RADIX = 10
    private val TOKEN =
        Regex(
            "-?\\d{1,3}(?:[ \\u00A0\\u202F',.\\u066C]\\d{3})+(?!\\d)(?:[.,\\u066B]\\d+)?|-?\\d+(?:[.,\\u066B]\\d+)?",
        )

    /** The first number found in [s], or null when there is none. */
    fun parse(s: String): Double? {
        val text = toAscii(s)
        val token = TOKEN.find(text)?.value ?: return null
        val negative = token.startsWith("-")
        val body = token.removePrefix("-")
        val cleaned =
            body
                .filterNot {
                    it == ' ' || it == '\u00A0' || it == '\u202F' || it == '\'' ||
                        it == ARABIC_THOUSANDS
                }.replace(ARABIC_DECIMAL, '.')
        val value = normalizeSeparators(cleaned)?.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    private fun toAscii(s: String): String =
        buildString(s.length) {
            for (c in s) {
                when {
                    c == MINUS_SIGN -> append('-')
                    Character.isDigit(c) -> append('0' + Character.digit(c, RADIX))
                    else -> append(c)
                }
            }
        }

    private fun normalizeSeparators(body: String): String? {
        val dots = body.count { it == '.' }
        val commas = body.count { it == ',' }
        return when {
            dots > 0 && commas > 0 -> {
                val decimalAt = maxOf(body.lastIndexOf('.'), body.lastIndexOf(','))
                val whole = body.substring(0, decimalAt).filter { it.isDigit() }
                whole + "." + body.substring(decimalAt + 1)
            }

            commas > 1 -> {
                body.replace(",", "")
            }

            dots > 1 -> {
                body.replace(".", "")
            }

            commas == 1 -> {
                val after = body.substringAfter(',')
                val before = body.substringBefore(',')
                if (after.length == GROUP_SIZE && before != "0") before + after else "$before.$after"
            }

            else -> {
                body
            }
        }
    }
}
