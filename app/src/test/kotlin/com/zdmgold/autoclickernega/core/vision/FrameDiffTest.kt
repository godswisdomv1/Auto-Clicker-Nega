package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDiffTest {
    private val base = SyntheticFrames.background(200, 400, 1)

    @Test
    fun identicalFramesHaveNotChanged() {
        assertFalse(FrameDiff.changed(base, base))
    }

    @Test
    fun smallNoiseIsNotAChange() {
        assertFalse(FrameDiff.changed(base, SyntheticFrames.noisy(base, 2.0, 4)))
    }

    @Test
    fun aMovedScreenIsAChange() {
        assertTrue(FrameDiff.changed(base, SyntheticFrames.background(200, 400, 2)))
        assertTrue(FrameDiff.changed(base, SyntheticFrames.embed(base, SyntheticFrames.flat(120, 120, 255), 20, 20)))
    }

    @Test
    fun differentSizesAreAChange() {
        assertTrue(FrameDiff.changed(base, SyntheticFrames.background(200, 300, 1)))
    }

    @Test
    fun theRegionLimitsWhatIsCompared() {
        val changed = SyntheticFrames.embed(base, SyntheticFrames.flat(100, 100, 255), 100, 300)
        assertFalse(FrameDiff.changed(base, changed, Rect(0, 0, 200, 200)))
        assertTrue(FrameDiff.changed(base, changed, Rect(100, 300, 200, 400)))
    }
}
