package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts.event
import com.zdmgold.autoclickernega.core.script.TestScripts.image
import com.zdmgold.autoclickernega.core.script.TestScripts.smart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventCompilerTest {
    private fun plan(script: com.zdmgold.autoclickernega.core.model.Script): EventPlan =
        (EventCompiler.compile(script) as Outcome.Success).value

    @Test
    fun ordersEventsByPriorityKeepingTheAuthoredOrderForTies() {
        val p = plan(smart(event("c", 5, image), event("a", 1, image), event("b", 1, image), event("d", 3, image)))
        assertEquals(listOf("a", "b", "d", "c"), p.events.map { it.event.id })
    }

    @Test
    fun resolvesTheMatchAnchor() {
        val text = Condition.Text("go")
        val p =
            plan(
                smart(
                    event("img", 1, TestScripts.counterBelow, image),
                    event("txt", 2, text),
                    event("none", 3, TestScripts.counterBelow),
                    event("neg", 4, image.copy(negate = true)),
                ),
            )
        val byId = p.events.associateBy { it.event.id }
        assertEquals(image, byId.getValue("img").anchor)
        assertEquals(text, byId.getValue("txt").anchor)
        assertNull(byId.getValue("none").anchor)
        assertNull(byId.getValue("neg").anchor)
    }

    @Test
    fun keepsDisabledEventsInThePlan() {
        val p = plan(smart(event("a", 1, image).copy(enabled = false)))
        assertEquals(
            false,
            p.events
                .single()
                .event.enabled,
        )
    }

    @Test
    fun refusesASimpleScript() {
        assertTrue(EventCompiler.compile(TestScripts.simple(Action.Stop)) is Outcome.Failure)
    }

    @Test
    fun refusesAnInvalidScript() {
        assertTrue(EventCompiler.compile(smart(event("e", 1))) is Outcome.Failure)
    }

    @Test
    fun appliesTheValidatorClampsToTheCompiledEvents() {
        val p = plan(smart(event("e", 1, image.copy(confidence = 0.1f))))
        assertEquals(0.6f, (p.events.single().anchor as Condition.Image).confidence, 0f)
    }
}
