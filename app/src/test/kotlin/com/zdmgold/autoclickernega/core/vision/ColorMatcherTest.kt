package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMatcherTest {
    private val teal = 0x12C79C
    private val frame = SyntheticFrames.colored(100, 100, teal)

    private fun cond(
        rgb: Int,
        tolerance: Int = 12,
        point: Point = Point(50, 50),
        region: Rect? = null,
    ) = Condition.Color(point, rgb, tolerance, region)

    @Test
    fun matchesTheColourAtAPoint() {
        assertTrue(ColorMatcher.matches(frame, cond(teal)))
        assertFalse(ColorMatcher.matches(frame, cond(0xFF0000)))
    }

    @Test
    fun toleranceAppliesToEveryChannel() {
        assertTrue(ColorMatcher.matches(frame, cond(0x12C79C + 0x0C0000, tolerance = 12)))
        assertFalse(ColorMatcher.matches(frame, cond(0x12C79C + 0x0D0000, tolerance = 12)))
        assertTrue(ColorMatcher.matches(frame, cond(0x000000, tolerance = 255)))
    }

    @Test
    fun aPointOffTheScreenNeverMatches() {
        assertFalse(ColorMatcher.matches(frame, cond(teal, point = Point(500, 5))))
        assertFalse(ColorMatcher.matches(frame, cond(teal, point = Point(-1, 5))))
    }

    @Test
    fun aRegionMatchesWhenAnyPixelInsideIsClose() {
        val argb = IntArray(100 * 100) { 0xFF000000.toInt() }
        argb[40 * 100 + 70] = 0xFF000000.toInt() or teal
        val f = Frame.fromArgb(100, 100, argb, keepColor = true)
        assertEquals(Point(70, 40), ColorMatcher.find(f, cond(teal, tolerance = 0, region = Rect(60, 30, 90, 50))))
        assertNull(ColorMatcher.find(f, cond(teal, tolerance = 0, region = Rect(0, 0, 60, 30))))
    }

    @Test
    fun aFrameWithoutColourDataNeverMatches() {
        val grayOnly = Frame.fromArgb(10, 10, IntArray(100) { 0xFF000000.toInt() or teal })
        assertFalse(ColorMatcher.matches(grayOnly, cond(teal)))
        assertNull(ColorMatcher.find(grayOnly, cond(teal, region = Rect(0, 0, 10, 10))))
    }

    @Test
    fun aRegionPartlyOffScreenIsClipped() {
        assertTrue(ColorMatcher.matches(frame, cond(teal, region = Rect(90, 90, 500, 500))))
    }
}
