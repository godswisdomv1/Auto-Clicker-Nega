package com.zdmgold.autoclickernega.ui.editor

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleEditingTest {
    private val screen = ScreenSpec(1080, 2400, 420)
    private val defaults = AppSettings()

    private fun script(vararg steps: Action): Script {
        var s = SimpleEditing.newScript("Test", screen, defaults)
        steps.forEach { s = SimpleEditing.add(s, it) }
        return s
    }

    private fun wait(ms: Long) = Action.Wait(ms)

    private fun waits(s: Script) = SimpleEditing.steps(s).map { (it as Action.Wait).ms }

    @Test
    fun aNewScriptIsEmptyAndUsesTheUsersDefaults() {
        val custom = AppSettings(defaultStartDelayMs = 120_000, defaultCountdownSec = 5)
        val s = SimpleEditing.newScript("Mine", screen, custom)
        assertEquals("", s.id)
        assertTrue(SimpleEditing.steps(s).isEmpty())
        assertEquals(120_000L, s.run.startDelayMs)
        assertEquals(5, s.run.countdownSec)
    }

    @Test
    fun addAppendsAndReplaceChangesOneStep() {
        val s = script(wait(1), wait(2))
        assertEquals(listOf(1L, 2L), waits(s))
        assertEquals(listOf(1L, 9L), waits(SimpleEditing.replace(s, 1, wait(9))))
    }

    @Test
    fun anIndexOutsideTheListChangesNothing() {
        val s = script(wait(1))
        assertSame(s, SimpleEditing.replace(s, 5, wait(9)))
        assertSame(s, SimpleEditing.remove(s, -1))
        assertSame(s, SimpleEditing.move(s, 0, 1))
        assertSame(s, SimpleEditing.duplicate(s, 3))
    }

    @Test
    fun removeTakesOutTheStep() {
        assertEquals(listOf(1L, 3L), waits(SimpleEditing.remove(script(wait(1), wait(2), wait(3)), 1)))
    }

    @Test
    fun moveSwapsWithTheNeighbourAndStopsAtTheEdges() {
        val s = script(wait(1), wait(2), wait(3))
        assertEquals(listOf(2L, 1L, 3L), waits(SimpleEditing.move(s, 1, -1)))
        assertEquals(listOf(1L, 3L, 2L), waits(SimpleEditing.move(s, 1, 1)))
        assertSame(s, SimpleEditing.move(s, 0, -1))
        assertSame(s, SimpleEditing.move(s, 2, 1))
    }

    @Test
    fun duplicateInsertsACopyRightAfter() {
        assertEquals(listOf(1L, 2L, 2L, 3L), waits(SimpleEditing.duplicate(script(wait(1), wait(2), wait(3)), 1)))
    }

    @Test
    fun aFullScriptTakesNoMoreSteps() {
        var s = script()
        repeat(Limits.MAX_ITEMS) { s = SimpleEditing.add(s, wait(1)) }
        assertEquals(Limits.MAX_ITEMS, SimpleEditing.steps(s).size)
        assertTrue(!SimpleEditing.canAdd(s))
        assertSame(s, SimpleEditing.add(s, wait(2)))
        assertSame(s, SimpleEditing.duplicate(s, 0))
    }

    @Test
    fun everyBlankStepKindIsValidAndOfTheRightKind() {
        StepKind.entries.forEach { kind ->
            val step = SimpleEditing.blankStep(kind, screen, defaults)
            assertEquals(kind, step.stepKind())
            val s = SimpleEditing.add(SimpleEditing.newScript("Check", screen, defaults), step)
            assertTrue("$kind: ${ScriptValidator.validate(s)}", ScriptValidator.validate(s) is Outcome.Success)
        }
    }

    @Test
    fun aBlankTapIsInTheMiddleOfTheScreenWithTheDefaults() {
        val custom = AppSettings(defaultIntervalMs = 250, defaultJitterPx = 4)
        val tap = SimpleEditing.blankStep(StepKind.Tap, screen, custom) as Action.Tap
        assertEquals(Target.At(Point(540, 1200)), tap.at)
        assertEquals(250L, tap.intervalMs)
        assertEquals(4, tap.jitterPx)
    }

    @Test
    fun aBlankTapNeverGoesBelowTheIntervalFloor() {
        val tap = SimpleEditing.blankStep(StepKind.Tap, screen, AppSettings(defaultIntervalMs = 5)) as Action.Tap
        assertEquals(Limits.MIN_INTERVAL_MS, tap.intervalMs)
    }

    @Test
    fun counterAndScriptActionsAreNotSimpleSteps() {
        assertNull(Action.Stop.stepKind())
        assertNull(Action.RunScript("x").stepKind())
    }
}
