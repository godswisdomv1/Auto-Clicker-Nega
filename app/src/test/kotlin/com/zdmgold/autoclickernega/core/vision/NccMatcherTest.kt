package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NccMatcherTest {
    private val w = 540
    private val h = 1200
    private val target = SyntheticFrames.pattern(48, 32, seed = 7)
    private val spec = Condition.Image(template = "t1", confidence = 0.85f)

    private fun scene(
        x: Int,
        y: Int,
        seed: Long = 1,
    ) = SyntheticFrames.embed(SyntheticFrames.background(w, h, seed), target, x, y)

    @Test
    fun findsATargetAtItsExactPosition() {
        val m = NccMatcher.find(scene(200, 500), target, spec)
        assertNotNull(m)
        assertEquals(Rect(200, 500, 248, 532), m!!.box)
        assertTrue("score ${m.score}", m.score >= 0.99f)
    }

    @Test
    fun scoresSurviveBrightnessContrastAndNoise() {
        val changed = SyntheticFrames.noisy(SyntheticFrames.adjust(scene(300, 700), 0.8, 25), 3.0, seed = 5)
        val m = NccMatcher.find(changed, target, spec)
        assertNotNull(m)
        assertTrue(abs(m!!.box.l - 300) <= 1 && abs(m.box.t - 700) <= 1)
        assertTrue("score ${m.score}", m.score >= 0.9f)
    }

    @Test
    fun darkAndInvertedBrightnessStillMatchesThePositiveCorrelation() {
        val dark = SyntheticFrames.adjust(scene(100, 100), 0.4, 0)
        assertNotNull(NccMatcher.find(dark, target, spec))
    }

    @Test
    fun findsTargetsAtTheCornersOfTheFrame() {
        val topLeft = NccMatcher.find(scene(0, 0), target, spec)
        assertEquals(Rect(0, 0, 48, 32), topLeft!!.box)
        val bottomRight = NccMatcher.find(scene(w - 48, h - 32), target, spec)
        assertEquals(Rect(w - 48, h - 32, w, h), bottomRight!!.box)
    }

    @Test
    fun anAbsentTargetIsNotAcceptedAtTheDefaultConfidence() {
        var accepted = 0
        for (seed in 100L until 160L) {
            if (NccMatcher.find(SyntheticFrames.background(w, h, seed), target, spec) != null) accepted++
        }
        assertEquals(0, accepted)
    }

    @Test
    fun falsePositivesStayLowEvenAtTheMinimumConfidence() {
        val loose = spec.copy(confidence = 0.60f)
        var accepted = 0
        val total = 60
        for (seed in 200L until 200L + total) {
            if (NccMatcher.find(SyntheticFrames.background(w, h, seed), target, loose) != null) accepted++
        }
        assertTrue("accepted $accepted of $total", accepted <= total * 3 / 100)
    }

    @Test
    fun aDifferentTargetOfTheSameSizeIsNotAccepted() {
        val other = SyntheticFrames.pattern(48, 32, seed = 99)
        val m =
            NccMatcher.find(
                SyntheticFrames.embed(SyntheticFrames.background(w, h, 3), other, 200, 500),
                target,
                spec,
            )
        assertNull(m)
    }

    @Test
    fun theSearchRegionRestrictsWhereTheTargetIsLookedFor() {
        val frame = scene(200, 500)
        assertNull(NccMatcher.find(frame, target, spec.copy(region = Rect(0, 0, w, 400))))
        val inside = NccMatcher.find(frame, target, spec.copy(region = Rect(100, 450, 400, 650)))
        assertEquals(Rect(200, 500, 248, 532), inside!!.box)
    }

    @Test
    fun aRegionSmallerThanTheTemplateCannotMatch() {
        val result = NccMatcher.search(scene(200, 500), target, spec.copy(region = Rect(200, 500, 230, 520)))
        assertNull(result.best)
        assertNull(NccMatcher.find(SyntheticFrames.flat(20, 20, 128), target, spec))
    }

    @Test
    fun aFlatTemplateHasNothingToMatch() {
        val flat = SyntheticFrames.flat(40, 30, 200)
        assertNull(NccMatcher.search(scene(10, 10), flat, spec).best)
    }

    @Test
    fun picksTheBetterOfTwoCopies() {
        val bg = SyntheticFrames.background(w, h, 11)
        val withFirst = SyntheticFrames.embed(bg, target, 100, 200)
        val degraded = SyntheticFrames.noisy(target, 28.0, seed = 3)
        val both = SyntheticFrames.embed(withFirst, degraded, 350, 900)
        val m = NccMatcher.find(both, target, spec)
        assertEquals(100, m!!.box.l)
        assertEquals(200, m.box.t)
    }

    @Test
    fun aMissStillReportsTheBestScoreForTheHud() {
        val result = NccMatcher.search(SyntheticFrames.background(w, h, 42), target, spec)
        assertNotNull(result.best)
        assertNull(result.accepted)
        assertTrue(result.best!!.score < 0.85f)
        assertEquals(0.85f, result.threshold, 0f)
    }

    @Test
    fun fastModeFindsAClearTarget() {
        val m = NccMatcher.find(scene(260, 640), target, spec.copy(mode = MatchMode.Fast))
        assertEquals(Rect(260, 640, 308, 672), m!!.box)
    }

    @Test
    fun resultsAreRepeatable() {
        val frame = scene(222, 333, seed = 9)
        assertEquals(NccMatcher.search(frame, target, spec), NccMatcher.search(frame, target, spec))
    }

    @Test
    fun theConfidenceIsClampedToTheAllowedRange() {
        val r = NccMatcher.search(scene(50, 50), target, spec.copy(confidence = 0.1f))
        assertEquals(0.60f, r.threshold, 0f)
    }

    @Test
    fun aTemplateAtAnotherScaleNeedsTheScaler() {
        val smallTarget = TemplateScaler.scale(target, 2f / 3f)
        val frame = SyntheticFrames.embed(SyntheticFrames.background(w, h, 5), smallTarget, 150, 300)
        assertNull(NccMatcher.find(frame, target, spec))
        val m = NccMatcher.find(frame, smallTarget, spec)
        assertEquals(150, m!!.box.l)
    }

    @Test
    fun largeTargetsUseMoreLevelsAndStillLandOnThePixel() {
        val big = SyntheticFrames.pattern(160, 120, seed = 21)
        val frame = SyntheticFrames.embed(SyntheticFrames.background(w, h, 4), big, 123, 457)
        val m = NccMatcher.find(frame, big, spec)
        assertEquals(Rect(123, 457, 283, 577), m!!.box)
    }
}
