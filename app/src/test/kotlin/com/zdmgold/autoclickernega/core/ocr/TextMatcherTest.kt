package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.TextMatchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextMatcherTest {
    private fun block(
        text: String,
        l: Int,
        t: Int = 100,
        w: Int = 80,
        conf: Float = 0.9f,
    ) = TextBlock(text, Rect(l, t, l + w, t + 20), conf)

    private fun cond(
        text: String,
        kind: TextMatchKind = TextMatchKind.Contains,
        caseSensitive: Boolean = false,
        lang: String = "en",
    ) = Condition.Text(text, kind, caseSensitive, lang)

    @Test
    fun containsFindsTextInsideABlock() {
        val m = TextMatcher.find(listOf(block("Collect reward now", 10)), cond("reward"))
        assertEquals(Rect(10, 100, 90, 120), m!!.box)
        assertEquals(0.9f, m.score, 0f)
    }

    @Test
    fun equalsNeedsTheWholeBlock() {
        val blocks = listOf(block("Collect reward", 10))
        assertNull(TextMatcher.find(blocks, cond("Collect", TextMatchKind.Equals)))
        assertTrue(TextMatcher.matches(blocks, cond("collect REWARD", TextMatchKind.Equals)))
    }

    @Test
    fun caseSensitivityIsOptional() {
        val blocks = listOf(block("Start", 10))
        assertTrue(TextMatcher.matches(blocks, cond("START")))
        assertFalse(TextMatcher.matches(blocks, cond("START", caseSensitive = true)))
        assertTrue(TextMatcher.matches(blocks, cond("Start", caseSensitive = true)))
    }

    @Test
    fun spacingAndInvisibleMarksAreIgnored() {
        val blocks = listOf(block("  Claim \u200E  all\u00A0 rewards ", 10))
        assertTrue(TextMatcher.matches(blocks, cond("claim all rewards", TextMatchKind.Equals)))
    }

    @Test
    fun fullWidthAndCompatibilityFormsAreNormalised() {
        assertTrue(TextMatcher.matches(listOf(block("ＯＫ", 10)), cond("ok", TextMatchKind.Equals)))
    }

    @Test
    fun aPhraseCanSpanNeighbouringBlocksOnOneLine() {
        val blocks = listOf(block("Tap", 10, w = 40), block("to", 60, w = 30), block("continue", 100, w = 90))
        val m = TextMatcher.find(blocks, cond("tap to continue", TextMatchKind.Equals))
        assertEquals(Rect(10, 100, 190, 120), m!!.box)
    }

    @Test
    fun aPhraseDoesNotJoinBlocksFromDifferentLines() {
        val blocks = listOf(block("Tap", 10, t = 100), block("continue", 10, t = 300))
        assertNull(TextMatcher.find(blocks, cond("tap continue")))
    }

    @Test
    fun lowConfidenceBlocksAreIgnored() {
        val blocks = listOf(block("Play", 10, conf = 0.3f))
        assertNull(TextMatcher.find(blocks, cond("Play")))
        assertTrue(TextMatcher.matches(blocks, cond("Play"), minConfidence = 0.2f))
    }

    @Test
    fun theScoreIsTheLeastSureBlockOfAPhrase() {
        val blocks = listOf(block("Go", 10, w = 30, conf = 0.95f), block("now", 50, w = 40, conf = 0.7f))
        assertEquals(0.7f, TextMatcher.find(blocks, cond("go now", TextMatchKind.Equals))!!.score, 0f)
    }

    @Test
    fun theMostConfidentHitWins() {
        val blocks = listOf(block("Buy", 10, conf = 0.6f), block("Buy", 200, conf = 0.95f))
        assertEquals(200, TextMatcher.find(blocks, cond("buy"))!!.box.l)
    }

    @Test
    fun anEmptyNeedleOrEmptyScreenNeverMatches() {
        assertNull(TextMatcher.find(listOf(block("x", 0)), cond("  ")))
        assertNull(TextMatcher.find(emptyList(), cond("x")))
    }

    @Test
    fun arabicMarksAreIgnoredButLettersAreNot() {
        val blocks = listOf(block("مَرْحَبًا", 10))
        assertTrue(TextMatcher.matches(blocks, cond("مرحبا", TextMatchKind.Equals, lang = "ar")))
        assertFalse(TextMatcher.matches(blocks, cond("مرحبي", TextMatchKind.Equals, lang = "ar")))
        assertTrue(TextMatcher.matches(listOf(block("كـتـاب", 10)), cond("كتاب", lang = "ar")))
    }

    @Test
    fun hebrewPointsAreIgnored() {
        assertTrue(TextMatcher.matches(listOf(block("שָׁלוֹם", 10)), cond("שלום", lang = "he")))
    }

    @Test
    fun rightToLeftLinesJoinFromTheRight() {
        val blocks = listOf(block("עולם", 10, w = 60), block("שלום", 90, w = 60))
        assertTrue(TextMatcher.matches(blocks, cond("שלום עולם", TextMatchKind.Equals, lang = "he")))
        assertFalse(TextMatcher.matches(blocks, cond("שלום עולם", TextMatchKind.Equals, lang = "en")))
    }

    @Test
    fun devanagariMarksAreKept() {
        assertTrue(TextMatcher.matches(listOf(block("नमस्ते", 10)), cond("नमस्ते", TextMatchKind.Equals, lang = "hi")))
        assertFalse(TextMatcher.matches(listOf(block("नमस्ते", 10)), cond("नमसत", TextMatchKind.Equals, lang = "hi")))
    }

    @Test
    fun cjkTextMatches() {
        assertTrue(TextMatcher.matches(listOf(block("確認して開始", 10)), cond("開始", lang = "ja")))
        assertTrue(TextMatcher.matches(listOf(block("시작하기", 10)), cond("시작", lang = "ko")))
    }

    @Test
    fun theFakeRecogniserFiltersByRegionAndLanguage() {
        val fake = FakeTextRecognizer(mapOf("en" to listOf(block("a", 10), block("b", 500))))
        val frame =
            com.zdmgold.autoclickernega.core.vision.SyntheticFrames
                .flat(10, 10, 1)
        kotlinx.coroutines.runBlocking {
            val r = fake.read(frame, Rect(0, 0, 200, 300), "en")
            assertEquals(1, (r as com.zdmgold.autoclickernega.core.result.Outcome.Success).value.size)
        }
        assertEquals(1, fake.calls)
    }
}
