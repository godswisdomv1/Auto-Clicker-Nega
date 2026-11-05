package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickScenarioFactoryTest {
    private val screen = TestScripts.screen

    @Test
    fun tapWhenImageBuildsAValidOneEventScript() {
        val region = Rect(0, 100, 500, 700)
        val s = QuickScenarioFactory.tapWhenImage("q1", "Quick", screen, TestScripts.template, region)
        assertTrue(ScriptValidator.validate(s) is Outcome.Success)
        val event = (s.body as ScriptBody.Smart).events.single()
        val cond = event.whenExpr.items.single() as Condition.Image
        assertEquals("t1", cond.template)
        assertEquals(region, cond.region)
        assertEquals(Limits.DEFAULT_CONFIDENCE, cond.confidence, 0f)
        assertEquals(listOf(TestScripts.tapOnMatch), event.actions)
        assertEquals(mapOf("t1" to TestScripts.template), s.templates)
    }

    @Test
    fun tapWhenTextBuildsAValidOneEventScript() {
        val s = QuickScenarioFactory.tapWhenText("q2", "Quick", screen, "Collect", "ar", null)
        assertTrue(ScriptValidator.validate(s) is Outcome.Success)
        val cond =
            (s.body as ScriptBody.Smart)
                .events
                .single()
                .whenExpr.items
                .single() as Condition.Text
        assertEquals("Collect", cond.text)
        assertEquals("ar", cond.lang)
    }

    @Test
    fun tapEveryBuildsAnEndlessSimpleScript() {
        val s = QuickScenarioFactory.tapEvery("q3", "Quick", screen, Point(540, 1200), 250)
        assertTrue(ScriptValidator.validate(s) is Outcome.Success)
        assertEquals(0, s.run.loops)
        val tap = (s.body as ScriptBody.Simple).steps.single() as Action.Tap
        assertEquals(Target.At(Point(540, 1200)), tap.at)
        assertEquals(250L, tap.intervalMs)
    }

    @Test
    fun tapEveryNeverGoesBelowTheFloor() {
        val s = QuickScenarioFactory.tapEvery("q4", "Quick", screen, Point(1, 1), 5)
        assertEquals(Limits.MIN_INTERVAL_MS, ((s.body as ScriptBody.Simple).steps.single() as Action.Tap).intervalMs)
    }
}
