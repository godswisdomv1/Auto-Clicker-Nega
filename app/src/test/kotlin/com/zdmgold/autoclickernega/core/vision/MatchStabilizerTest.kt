package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchStabilizerTest {
    private fun at(
        x: Int,
        y: Int,
    ) = Match(0.9f, Rect(x, y, x + 20, y + 20))

    @Test
    fun aMatchNeedsTwoConsecutiveFrames() {
        val s = MatchStabilizer()
        assertNull(s.accept(at(100, 100)))
        assertEquals(at(105, 108), s.accept(at(105, 108)))
    }

    @Test
    fun theTwelvePixelLimitIsInclusive() {
        val s = MatchStabilizer()
        s.accept(at(100, 100))
        assertEquals(at(112, 88), s.accept(at(112, 88)))
        val t = MatchStabilizer()
        t.accept(at(100, 100))
        assertNull(t.accept(at(113, 100)))
    }

    @Test
    fun aMissResetsTheRun() {
        val s = MatchStabilizer()
        s.accept(at(10, 10))
        assertNull(s.accept(null))
        assertNull(s.accept(at(10, 10)))
        assertEquals(at(10, 10), s.accept(at(10, 10)))
    }

    @Test
    fun aJumpRestartsTheCountFromTheNewPlace() {
        val s = MatchStabilizer()
        s.accept(at(0, 0))
        assertNull(s.accept(at(300, 300)))
        assertEquals(at(302, 301), s.accept(at(302, 301)))
    }

    @Test
    fun resetForgetsThePreviousFrame() {
        val s = MatchStabilizer()
        s.accept(at(5, 5))
        s.reset()
        assertNull(s.accept(at(5, 5)))
    }

    @Test
    fun theToleranceIsConfigurable() {
        val s = MatchStabilizer(tolerancePx = 2)
        s.accept(at(0, 0))
        assertNull(s.accept(at(3, 0)))
    }
}
