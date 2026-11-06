package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.TextMatchKind
import com.zdmgold.autoclickernega.core.vision.Match
import java.text.Normalizer
import java.util.Locale

/**
 * Decides whether recognised text satisfies a text condition. Matching ignores layout noise: it normalises
 * Unicode, drops invisible direction marks, collapses spaces and, unless asked otherwise, ignores case and the
 * marks that OCR often misreads in Arabic and Hebrew. A phrase may span up to four neighbouring blocks on one line.
 */
object TextMatcher {
    const val DEFAULT_MIN_CONFIDENCE = 0.5f
    private const val MAX_WINDOW = 4
    private val FORMAT_CHARS = Regex("[\\p{Cf}]")
    private val SPACES = Regex("\\s+")
    private val SOFT_MARKS = Regex("[\\u0640\\u064B-\\u065F\\u0670\\u0591-\\u05C7]")

    fun matches(
        blocks: List<TextBlock>,
        c: Condition.Text,
        minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
    ): Boolean = find(blocks, c, minConfidence) != null

    /** The best match, or null. The box covers the blocks that made the text; the score is the least sure of them. */
    fun find(
        blocks: List<TextBlock>,
        c: Condition.Text,
        minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
    ): Match? {
        val needle = normalize(c.text, c.caseSensitive)
        if (needle.isEmpty()) return null
        val usable = blocks.filter { it.confidence >= minConfidence && it.box.width > 0 && it.box.height > 0 }
        val hits =
            lines(usable, OcrLanguages.isRightToLeft(c.lang))
                .flatMap(::windows)
                .filter { satisfies(it, needle, c) }
                .map { Match(it.minOf { block -> block.confidence }, union(it)) }
        return hits.maxByOrNull { it.score }
    }

    /** Every run of up to four neighbouring blocks on one line, starting from each block. */
    private fun windows(line: List<TextBlock>): List<List<TextBlock>> =
        line.indices.flatMap { start ->
            (start until minOf(line.size, start + MAX_WINDOW)).map { end -> line.subList(start, end + 1) }
        }

    private fun satisfies(
        window: List<TextBlock>,
        needle: String,
        c: Condition.Text,
    ): Boolean {
        val text = normalize(window.joinToString(" ") { it.text }, c.caseSensitive)
        return if (c.kind == TextMatchKind.Equals) text == needle else text.contains(needle)
    }

    /** Lower-level helper shared with number reading: the same normalisation the matcher applies. */
    fun normalize(
        text: String,
        caseSensitive: Boolean,
    ): String {
        var s = Normalizer.normalize(text, Normalizer.Form.NFKC)
        s = FORMAT_CHARS.replace(s, "")
        if (!caseSensitive) {
            s = SOFT_MARKS.replace(s, "").lowercase(Locale.ROOT)
        }
        return SPACES.replace(s, " ").trim()
    }

    /** Groups blocks into lines top to bottom, and orders each line left to right, or right to left for Arabic and Hebrew. */
    private fun lines(
        blocks: List<TextBlock>,
        rtl: Boolean,
    ): List<List<TextBlock>> {
        val result = mutableListOf<MutableList<TextBlock>>()
        for (block in blocks.sortedBy { it.box.t }) {
            val line = result.firstOrNull { sameLine(it.first(), block) }
            if (line != null) line += block else result += mutableListOf(block)
        }
        return result.map { line -> if (rtl) line.sortedByDescending { it.box.l } else line.sortedBy { it.box.l } }
    }

    private fun sameLine(
        a: TextBlock,
        b: TextBlock,
    ): Boolean {
        val overlap = minOf(a.box.b, b.box.b) - maxOf(a.box.t, b.box.t)
        return overlap * 2 >= minOf(a.box.height, b.box.height)
    }

    private fun union(blocks: List<TextBlock>) =
        Rect(
            blocks.minOf { it.box.l },
            blocks.minOf { it.box.t },
            blocks.maxOf { it.box.r },
            blocks.maxOf { it.box.b },
        )
}
