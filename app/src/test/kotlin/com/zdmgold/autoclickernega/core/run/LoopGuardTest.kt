package com.zdmgold.autoclickernega.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopGuardTest {
    @Test
    fun theRootAndThreeLevelsOfChildrenAreAllowed() {
        val g = LoopGuard()
        assertTrue(g.enter("root"))
        assertTrue(g.enter("a"))
        assertTrue(g.enter("b"))
        assertTrue(g.enter("c"))
        assertEquals(3, g.depth)
        assertFalse(g.enter("d"))
    }

    @Test
    fun aScriptAlreadyRunningCannotEnterAgain() {
        val g = LoopGuard()
        g.enter("root")
        g.enter("a")
        assertFalse(g.enter("root"))
        assertFalse(g.enter("a"))
    }

    @Test
    fun leavingFreesTheSlot() {
        val g = LoopGuard()
        g.enter("root")
        g.enter("a")
        g.exit("a")
        assertEquals(0, g.depth)
        assertTrue(g.enter("a"))
        g.exit("something-else")
        assertEquals(1, g.depth)
    }

    @Test
    fun theDepthLimitIsConfigurable() {
        val g = LoopGuard(maxDepth = 1)
        assertTrue(g.enter("root"))
        assertTrue(g.enter("a"))
        assertFalse(g.enter("b"))
    }
}
