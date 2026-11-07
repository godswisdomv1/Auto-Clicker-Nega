package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.BoolOp
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind
import com.zdmgold.autoclickernega.core.model.Expr
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.TextMatchKind
import com.zdmgold.autoclickernega.core.model.TimerSince
import com.zdmgold.autoclickernega.core.ocr.FakeTextRecognizer
import com.zdmgold.autoclickernega.core.ocr.TextBlock
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.MissReason
import com.zdmgold.autoclickernega.core.vision.SyntheticFrames
import com.zdmgold.autoclickernega.service.engine.FakeClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionEvaluatorTest {
    private val clock = FakeClock(1_000)
    private val counters = CounterStore(listOf(CounterDef("c1", "C", 0)))
    private val fired = HashMap<String, Long>()
    private val ctx = RunContext(clock, counters, runStartMs = 1_000) { fired[it] }
    private val target = SyntheticFrames.pattern(48, 32, 7)
    private val scene = SyntheticFrames.embed(SyntheticFrames.background(540, 1200, 1), target, 200, 500)
    private val empty = SyntheticFrames.background(540, 1200, 2)
    private val image = Condition.Image("t1", mode = MatchMode.Fast)
    private val templates = mapOf("t1" to target)

    private fun eval(
        expr: Expr,
        frame: Frame = scene,
        recognizer: FakeTextRecognizer? = null,
        evaluator: ConditionEvaluator = ConditionEvaluator(templates, recognizer),
    ) = runBlocking { evaluator.evaluate(expr, frame, ctx, "e") }

    private fun and(vararg c: Condition) = Expr(BoolOp.And, c.toList())

    private fun or(vararg c: Condition) = Expr(BoolOp.Or, c.toList())

    private fun block(
        text: String,
        l: Int = 10,
        t: Int = 100,
    ) = TextBlock(text, Rect(l, t, l + 120, t + 24), 0.9f)

    @Test
    fun counterConditionsCompareWithTheLiveValue() {
        val below = Condition.Counter("c1", Comparison.Lt, 2)
        assertTrue(eval(and(below)).passed)
        counters.apply(Action.CounterOp("c1", CounterOpKind.Add, 2))
        assertFalse(eval(and(below)).passed)
    }

    @Test
    fun timerSinceTheRunStartedAndSinceAnEventFired() {
        runBlocking { clock.advanceBy(5_000) }
        assertTrue(eval(and(Condition.Timer(TimerSince.RunStart, Comparison.Gte, 5_000))).passed)
        assertFalse(eval(and(Condition.Timer(TimerSince.RunStart, Comparison.Gt, 5_000))).passed)
        val sinceEvent = Condition.Timer(TimerSince.EventFired("e1"), Comparison.Gte, 1_000)
        assertFalse("never fired", eval(and(sinceEvent)).passed)
        fired["e1"] = 5_000
        assertTrue(eval(and(sinceEvent)).passed)
    }

    @Test
    fun colourNeedsAFrameWithColourData() {
        val c = Condition.Color(Point(5, 5), 0x12C79C, 0)
        assertTrue(eval(and(c), SyntheticFrames.colored(20, 20, 0x12C79C)).passed)
        val grayOnly = Frame.fromArgb(20, 20, IntArray(400) { 0xFF12C79C.toInt() })
        val r = eval(and(c), grayOnly)
        assertFalse(r.passed)
        assertEquals(MissReason.NoColorData, r.miss)
    }

    @Test
    fun anImageIsFoundWithItsAnchorAndScore() {
        val r = eval(and(image))
        assertTrue(r.passed)
        assertEquals(Rect(200, 500, 248, 532), r.anchor!!.box)
        assertTrue(r.score!! >= 0.99f)
    }

    @Test
    fun aMissedImageReportsTheScoreAndReason() {
        val r = eval(and(image), empty)
        assertFalse(r.passed)
        assertNull(r.anchor)
        assertNotNull(r.score)
        assertEquals(MissReason.BelowConfidence, r.miss)
    }

    @Test
    fun imageProblemsHaveSpecificReasons() {
        assertEquals(MissReason.TemplateMissing, eval(and(image.copy(template = "gone"))).miss)
        assertEquals(MissReason.RegionTooSmall, eval(and(image.copy(region = Rect(0, 0, 10, 10)))).miss)
        val flat = ConditionEvaluator(mapOf("t1" to SyntheticFrames.flat(30, 30, 99)), null)
        assertEquals(MissReason.TemplateTooFlat, eval(and(image), evaluator = flat).miss)
    }

    @Test
    fun safeModeNeedsTheSameMatchOnTwoConsecutiveFrames() {
        val evaluator = ConditionEvaluator(templates, null)
        val safe = and(image.copy(mode = MatchMode.Safe))
        ctx.frameIndex = 1
        val first = eval(safe, evaluator = evaluator)
        assertFalse(first.passed)
        assertEquals(MissReason.NotStable, first.miss)
        ctx.frameIndex = 2
        assertTrue(eval(safe, evaluator = evaluator).passed)
    }

    @Test
    fun safeModeStartsOverWhenAFrameWasSkipped() {
        val evaluator = ConditionEvaluator(templates, null)
        val safe = and(image.copy(mode = MatchMode.Safe))
        ctx.frameIndex = 1
        eval(safe, evaluator = evaluator)
        ctx.frameIndex = 3
        assertFalse(eval(safe, evaluator = evaluator).passed)
        ctx.frameIndex = 4
        assertTrue(eval(safe, evaluator = evaluator).passed)
    }

    @Test
    fun textIsReadFromTheRegionAndMatched() {
        val rec = FakeTextRecognizer(mapOf("en" to listOf(block("Collect reward"))))
        val r = eval(and(Condition.Text("reward")), recognizer = rec)
        assertTrue(r.passed)
        assertEquals(Rect(10, 100, 130, 124), r.anchor!!.box)
        assertFalse(eval(and(Condition.Text("missing")), recognizer = rec).passed)
        val equalsMiss = Condition.Text("Collect", kind = TextMatchKind.Equals)
        assertFalse(eval(and(equalsMiss), recognizer = rec).passed)
    }

    @Test
    fun textIsReadAtMostOncePerThreeHundredMilliseconds() {
        val rec = FakeTextRecognizer(mapOf("en" to listOf(block("Play"))))
        val evaluator = ConditionEvaluator(templates, rec)
        val expr = and(Condition.Text("Play"))
        eval(expr, evaluator = evaluator)
        eval(expr, evaluator = evaluator)
        assertEquals(1, rec.calls)
        runBlocking { clock.advanceBy(300) }
        eval(expr, evaluator = evaluator)
        assertEquals(2, rec.calls)
    }

    @Test
    fun aMissingLanguagePackIsReportedNotTreatedAsNoText() {
        val rec = FakeTextRecognizer(missingPacks = setOf("ar"))
        val r = eval(and(Condition.Text("x", lang = "ar")), recognizer = rec)
        assertFalse(r.passed)
        assertEquals(MissReason.LanguagePackMissing, r.miss)
        assertTrue(r.error is AppError.LanguagePack)
        val noEngine = eval(and(Condition.Text("x")), recognizer = null)
        assertEquals(MissReason.LanguagePackMissing, noEngine.miss)
    }

    @Test
    fun numbersAreReadAndCompared() {
        val rec = FakeTextRecognizer(mapOf("en" to listOf(block("Gold: 1,500", 10, 100))))
        val region = Rect(0, 0, 400, 300)
        assertTrue(eval(and(Condition.Number(region, Comparison.Gte, 1_500.0)), recognizer = rec).passed)
        assertFalse(eval(and(Condition.Number(region, Comparison.Gt, 1_500.0)), recognizer = rec).passed)
        val none = FakeTextRecognizer(mapOf("en" to listOf(block("no digits"))))
        val r = eval(and(Condition.Number(region, Comparison.Gt, 1.0)), recognizer = none)
        assertEquals(MissReason.NoNumber, r.miss)
    }

    @Test
    fun negatedConditionsPassWhenTheRawResultFails() {
        assertTrue(eval(and(image.copy(negate = true)), empty).passed)
        assertFalse(eval(and(image.copy(negate = true))).passed)
        assertTrue(eval(and(Condition.Counter("c1", Comparison.Gt, 5, negate = true))).passed)
    }

    @Test
    fun aNegatedConditionNeverProvidesAnAnchor() {
        val r = eval(and(image.copy(negate = true), Condition.Counter("c1", Comparison.Eq, 0)), empty)
        assertTrue(r.passed)
        assertNull(r.anchor)
    }

    @Test
    fun andNeedsEveryConditionAndOrNeedsOne() {
        val yes = Condition.Counter("c1", Comparison.Eq, 0)
        val no = Condition.Counter("c1", Comparison.Eq, 9)
        assertTrue(eval(and(yes, yes)).passed)
        assertFalse(eval(and(yes, no)).passed)
        assertTrue(eval(or(no, yes)).passed)
        assertFalse(eval(or(no, no)).passed)
    }

    @Test
    fun cheapConditionsShortCircuitTheExpensiveOnes() {
        val rec = FakeTextRecognizer(mapOf("en" to listOf(block("Play"))))
        val no = Condition.Counter("c1", Comparison.Eq, 9)
        eval(and(Condition.Text("Play"), no), recognizer = rec)
        assertEquals("the counter fails first, so no text is read", 0, rec.calls)
        val yes = Condition.Counter("c1", Comparison.Eq, 0)
        eval(or(Condition.Text("Play"), yes), recognizer = rec)
        assertEquals("the counter passes first, so no text is read", 0, rec.calls)
    }

    @Test
    fun anOrThatPassesByCounterHasNoAnchor() {
        val r = eval(or(image, Condition.Counter("c1", Comparison.Eq, 0)), empty)
        assertTrue(r.passed)
        assertNull(r.anchor)
    }

    @Test
    fun anAndWithAnImageAndACounterKeepsTheImageAnchor() {
        val r = eval(and(Condition.Counter("c1", Comparison.Eq, 0), image))
        assertTrue(r.passed)
        assertEquals(Rect(200, 500, 248, 532), r.anchor!!.box)
    }
}
