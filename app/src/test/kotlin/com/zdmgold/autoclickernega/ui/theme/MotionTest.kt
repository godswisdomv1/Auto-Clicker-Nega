package com.zdmgold.autoclickernega.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun durationsFollowTheDesignSystem() {
        assertEquals(350, Motion.PUSH_MS)
        assertEquals(280, Motion.POP_MS)
        assertEquals(400, Motion.SHEET_MS)
        assertEquals(150, Motion.PRESS_MS)
        assertEquals(300, Motion.REVEAL_MS)
        assertEquals(200, Motion.THEME_TOGGLE_MS)
    }

    @Test
    fun reducedMotionMakesEveryTransitionInstant() {
        assertEquals(0, Motion.duration(Motion.PUSH_MS, reduced = true))
        assertEquals(350, Motion.duration(Motion.PUSH_MS, reduced = false))
    }

    @Test
    fun onlyAnimatorScaleZeroCountsAsReducedMotion() {
        assertTrue(Motion.isReduced(0f))
        assertFalse(Motion.isReduced(0.5f))
        assertFalse(Motion.isReduced(1f))
    }
}
