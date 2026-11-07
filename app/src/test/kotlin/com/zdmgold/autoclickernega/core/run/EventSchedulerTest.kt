package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.EventCompiler
import com.zdmgold.autoclickernega.core.script.EventPlan
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.core.script.TestScripts.event
import com.zdmgold.autoclickernega.core.script.TestScripts.image
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventSchedulerTest {
    private fun plan(vararg events: com.zdmgold.autoclickernega.core.model.Event): EventPlan =
        (EventCompiler.compile(TestScripts.smart(*events)) as Outcome.Success).value

    private fun ids(
        s: EventScheduler,
        now: Long,
    ) = s.eligible(now).map { it.event.id }

    @Test
    fun eligibleEventsComeInPriorityOrder() {
        val s = EventScheduler(plan(event("c", 3, image), event("a", 1, image), event("b", 2, image)))
        assertEquals(listOf("a", "b", "c"), ids(s, 0))
    }

    @Test
    fun disabledEventsAreSkipped() {
        val s = EventScheduler(plan(event("a", 1, image).copy(enabled = false), event("b", 2, image)))
        assertEquals(listOf("b"), ids(s, 0))
    }

    @Test
    fun aFiredEventWaitsOutItsCooldown() {
        val a = event("a", 1, image).copy(cooldownMs = 500)
        val s = EventScheduler(plan(a, event("b", 2, image)))
        s.markFired(a, now = 1_000)
        assertEquals(listOf("b"), ids(s, 1_499))
        assertEquals(listOf("a", "b"), ids(s, 1_500))
    }

    @Test
    fun zeroCooldownMeansAlwaysEligible() {
        val a = event("a", 1, image).copy(cooldownMs = 0)
        val s = EventScheduler(plan(a))
        s.markFired(a, 100)
        assertEquals(listOf("a"), ids(s, 100))
    }

    @Test
    fun remembersWhenEachEventLastFired() {
        val a = event("a", 1, image)
        val s = EventScheduler(plan(a))
        assertNull(s.lastFiredAt("a"))
        s.markFired(a, 777)
        assertEquals(777L, s.lastFiredAt("a"))
    }
}
