package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameTest {
    @Test
    fun convertsArgbToGrayWithTheStandardWeights() {
        val f = Frame.fromArgb(3, 1, intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFF0000.toInt()))
        assertEquals(255, f.gray(0, 0))
        assertEquals(0, f.gray(1, 0))
        assertEquals(76, f.gray(2, 0))
    }

    @Test
    fun colourIsKeptOnlyWhenAsked() {
        val argb = intArrayOf(0xFF12C79C.toInt())
        assertNull(Frame.fromArgb(1, 1, argb).colorAt(Point(0, 0)))
        assertEquals(0x12C79C, Frame.fromArgb(1, 1, argb, keepColor = true).colorAt(Point(0, 0)))
        assertNull(Frame.fromArgb(1, 1, argb, keepColor = true).colorAt(Point(1, 0)))
    }

    @Test
    fun cropCopiesTheRegionAndClipsToTheFrame() {
        val f = Frame(4, 3, ByteArray(12) { it.toByte() })
        val c = f.crop(Rect(1, 1, 3, 3))
        assertEquals(2 to 2, c.w to c.h)
        assertEquals(5, c.gray(0, 0))
        assertEquals(10, c.gray(1, 1))
        val clipped = f.crop(Rect(2, 1, 99, 99))
        assertEquals(2 to 2, clipped.w to clipped.h)
    }

    @Test
    fun cropKeepsColourInStep() {
        val f = SyntheticFrames.colored(6, 6, 0xABCDEF)
        assertEquals(0xABCDEF, f.crop(Rect(1, 1, 3, 3)).colorAt(Point(0, 0)))
    }

    @Test
    fun fillHidesARegionInPlace() {
        val f = SyntheticFrames.colored(10, 10, 0xFFFFFF)
        f.fill(Rect(2, 2, 5, 5), gray = 0, color = 0)
        assertEquals(0, f.gray(3, 3))
        assertEquals(0, f.colorAt(Point(3, 3)))
        assertEquals(255, f.gray(6, 6))
        f.fill(Rect(8, 8, 50, 50))
        assertEquals(0, f.gray(9, 9))
    }

    @Test
    fun rejectsInconsistentData() {
        assertTrue(runCatching { Frame(2, 2, ByteArray(3)) }.isFailure)
        assertTrue(runCatching { Frame(0, 2, ByteArray(0)) }.isFailure)
        assertTrue(runCatching { Frame(2, 2, ByteArray(4), IntArray(3)) }.isFailure)
    }
}
