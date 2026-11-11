package com.zdmgold.autoclickernega.ui.components

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionMathTest {
    private val screen = ScreenSpec(1000, 2000, 420)
    private val r = Rect(100, 200, 300, 500)

    @Test
    fun aDrawnRegionIsNormalisedAndKeptOnScreen() {
        assertEquals(Rect(50, 60, 400, 700), RegionMath.drawn(400, 700, 50, 60, screen))
        assertEquals(Rect(0, 0, 1000, 2000), RegionMath.drawn(-50, -50, 5000, 5000, screen))
    }

    @Test
    fun onlyRegionsOfUsableSizeCount() {
        assertTrue(RegionMath.isUsable(Rect(0, 0, 24, 24)))
        assertFalse(RegionMath.isUsable(Rect(0, 0, 23, 100)))
        assertFalse(RegionMath.isUsable(Rect(0, 0, 100, 23)))
    }

    @Test
    fun cornersAreHitWithinTheRadius() {
        assertEquals(0, RegionMath.hitCorner(r, 104f, 203f, 10f))
        assertEquals(2, RegionMath.hitCorner(r, 298f, 497f, 10f))
        assertEquals(-1, RegionMath.hitCorner(r, 200f, 350f, 10f))
        assertEquals(listOf(Point(100, 200), Point(300, 200), Point(300, 500), Point(100, 500)), RegionMath.corners(r))
    }

    @Test
    fun theInsideIsTheMoveHandle() {
        assertTrue(RegionMath.contains(r, 200f, 350f))
        assertFalse(RegionMath.contains(r, 50f, 350f))
    }

    @Test
    fun movingStopsAtTheScreenEdges() {
        assertEquals(Rect(150, 260, 350, 560), RegionMath.shifted(r, 50, 60, screen))
        assertEquals(Rect(0, 0, 200, 300), RegionMath.shifted(r, -999, -999, screen))
        assertEquals(Rect(800, 1700, 1000, 2000), RegionMath.shifted(r, 9999, 9999, screen))
    }

    @Test
    fun resizingAroundTheOppositeCorner() {
        assertEquals(Rect(50, 60, 300, 500), RegionMath.resized(r, 0, 50, 60, screen))
        assertEquals(Rect(100, 200, 600, 900), RegionMath.resized(r, 2, 600, 900, screen))
        assertEquals(Rect(300, 500, 400, 600), RegionMath.resized(r, 0, 400, 600, screen))
        assertEquals(Rect(100, 200, 1000, 2000), RegionMath.resized(r, 2, 5000, 5000, screen))
    }
}
