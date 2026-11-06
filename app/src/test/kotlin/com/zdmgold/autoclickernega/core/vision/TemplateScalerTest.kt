package com.zdmgold.autoclickernega.core.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateScalerTest {
    private val t = SyntheticFrames.pattern(60, 40, 3)

    @Test
    fun aRatioNearOneChangesNothing() {
        assertSame(t, TemplateScaler.scale(t, 1f))
        assertSame(t, TemplateScaler.scale(t, 1.004f))
        assertSame(t, TemplateScaler.scale(t, 0f))
    }

    @Test
    fun sizesFollowTheRatio() {
        val half = TemplateScaler.scale(t, 0.5f)
        assertEquals(30 to 20, half.w to half.h)
        val double = TemplateScaler.scale(t, 2f)
        assertEquals(120 to 80, double.w to double.h)
        val tiny = TemplateScaler.scale(SyntheticFrames.flat(3, 3, 9), 0.1f)
        assertEquals(1 to 1, tiny.w to tiny.h)
    }

    @Test
    fun shrinkingAverages() {
        val checker = Frame(2, 2, byteArrayOf(0, -1, -1, 0))
        assertEquals(127, TemplateScaler.scale(checker, 0.5f).let { it.gray(0, 0) })
    }

    @Test
    fun flatImagesStayFlatAtAnyScale() {
        listOf(0.4f, 0.7f, 1.5f, 3f).forEach { r ->
            val s = TemplateScaler.scale(SyntheticFrames.flat(20, 20, 123), r)
            assertTrue(s.px.all { (it.toInt() and 0xFF) == 123 })
        }
    }

    @Test
    fun enlargingKeepsTheAverageBrightness() {
        val big = TemplateScaler.scale(t, 1.5f)
        val a = t.px.sumOf { it.toInt() and 0xFF } / t.px.size
        val b = big.px.sumOf { it.toInt() and 0xFF } / big.px.size
        assertTrue(kotlin.math.abs(a - b) <= 3)
    }
}
