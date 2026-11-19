package com.zdmgold.autoclickernega.core.lab

import com.zdmgold.autoclickernega.core.vision.Frame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatePickerTest {
    /** A flat gray screen with a checkerboard of 8 px squares at the given place. */
    private fun frameWithPattern(
        w: Int,
        h: Int,
        patternLeft: Int,
        patternTop: Int,
        patternSize: Int,
    ): Frame {
        val px = ByteArray(w * h) { 128.toByte() }
        for (y in patternTop until patternTop + patternSize) {
            for (x in patternLeft until patternLeft + patternSize) {
                val dark = ((x - patternLeft) / 8 + (y - patternTop) / 8) % 2 == 0
                px[y * w + x] = (if (dark) 20 else 235).toByte()
            }
        }
        return Frame(w, h, px)
    }

    @Test
    fun theBusyPatchIsPickedOverFlatScreen() {
        val frame = frameWithPattern(400, 600, 200, 300, 64)
        val picked = TemplatePicker.pick(frame, 48, 32)!!
        assertEquals(48, picked.width)
        assertEquals(32, picked.height)
        assertTrue("left ${picked.l}", picked.l in 188..240)
        assertTrue("top ${picked.t}", picked.t in 288..332)
    }

    @Test
    fun aPatchThatDoesNotFitGivesNull() {
        val frame = Frame(100, 100, ByteArray(100 * 100))
        assertNull(TemplatePicker.pick(frame, 200, 50))
        assertNull(TemplatePicker.pick(frame, 50, 200))
    }

    @Test
    fun theVerticalMarginsAreSkipped() {
        val frame = frameWithPattern(400, 600, 100, 8, 64)
        val skipped = TemplatePicker.pick(frame, 48, 32, verticalMarginPx = 120)!!
        assertTrue("top ${skipped.t}", skipped.t >= 120)
        val allowed = TemplatePicker.pick(frame, 48, 32)!!
        assertTrue("top ${allowed.t}", allowed.t < 80)
    }

    @Test
    fun aMarginLargerThanTheScreenGivesNull() {
        val frame = Frame(400, 200, ByteArray(400 * 200))
        assertNull(TemplatePicker.pick(frame, 48, 32, verticalMarginPx = 120))
    }
}
