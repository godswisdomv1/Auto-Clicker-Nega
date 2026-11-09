package com.zdmgold.autoclickernega.core.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameBandsTest {
    @Test
    fun readingInBandsGivesTheSameFrameAsReadingAllAtOnce() {
        val w = 7
        val h = 23
        val argb = IntArray(w * h) { (0xFF shl 24) or ((it * 37) and 0xFFFFFF) }
        val whole = Frame.fromArgb(w, h, argb, keepColor = true)
        val banded =
            Frame.fromRows(w, h, keepColor = true, rowsPerBand = 5) { band, start, rows ->
                argb.copyInto(band, 0, start * w, (start + rows) * w)
            }
        assertTrue(whole.px.contentEquals(banded.px))
        assertTrue(whole.rgb!!.contentEquals(banded.rgb!!))
    }

    @Test
    fun aBandTallerThanTheFrameStillWorks() {
        val f =
            Frame.fromRows(3, 2, keepColor = false, rowsPerBand = 100) { band, _, rows ->
                for (i in 0 until 3 * rows) band[i] = 0xFFFFFFFF.toInt()
            }
        assertEquals(255, f.gray(2, 1))
    }

    @Test
    fun grayLevelsUseTheStandardWeights() {
        assertEquals(255, Frame.grayOf(0xFFFFFFFF.toInt()))
        assertEquals(0, Frame.grayOf(0xFF000000.toInt()))
        assertEquals(76, Frame.grayOf(0xFFFF0000.toInt()))
    }

    @Test
    fun aFlatFrameIsBlankAndARealOneIsNot() {
        assertTrue(FrameSanity.isBlank(SyntheticFrames.flat(50, 50, 0)))
        assertTrue(FrameSanity.isBlank(SyntheticFrames.flat(50, 50, 200)))
        assertFalse(FrameSanity.isBlank(SyntheticFrames.background(50, 50, 1)))
        val oneBrightPixel =
            SyntheticFrames.flat(50, 50, 0).let { f ->
                Frame(50, 50, f.px.copyOf().also { it[2499] = 9 })
            }
        assertFalse(FrameSanity.isBlank(oneBrightPixel))
    }
}
