package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateScalerTest {
    private val big = ScreenSpec(1080, 2400, 420)
    private val small = ScreenSpec(720, 1600, 280)

    @Test
    fun sameScreenChangesNothing() {
        assertEquals(Point(540, 1200), CoordinateScaler.scale(Point(540, 1200), big, big))
    }

    @Test
    fun scalesPointsAndRegionsByScreenSize() {
        assertEquals(Point(360, 800), CoordinateScaler.scale(Point(540, 1200), big, small))
        assertEquals(Rect(0, 1067, 720, 1600), CoordinateScaler.scale(Rect(0, 1600, 1080, 2400), big, small))
        assertEquals(Point(2160, 4800), CoordinateScaler.scale(Point(1080, 2400), big, ScreenSpec(2160, 4800, 560)))
    }

    @Test
    fun offsetsAndTemplatesFollowTheDensityRatio() {
        assertEquals(Point(4, -4), CoordinateScaler.scaleOffset(Point(6, -6), big, small))
        assertEquals(2f / 3f, CoordinateScaler.densityRatio(big, small), 0.0001f)
        assertEquals(1f, CoordinateScaler.densityRatio(big, big.copy(density = 0)), 0f)
    }

    @Test
    fun warnsWhenTheOrientationDiffers() {
        assertFalse(CoordinateScaler.orientationMismatch(big, small))
        assertTrue(CoordinateScaler.orientationMismatch(big, ScreenSpec(2400, 1080, 420)))
    }

    @Test
    fun aBrokenReferenceScreenLeavesPointsAlone() {
        assertEquals(Point(5, 6), CoordinateScaler.scale(Point(5, 6), ScreenSpec(0, 0, 0), small))
    }
}
