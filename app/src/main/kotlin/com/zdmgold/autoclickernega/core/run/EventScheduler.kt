package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.script.CompiledEvent
import com.zdmgold.autoclickernega.core.script.EventPlan

/**
 * Decides which events may be tried on a frame: enabled ones whose cooldown has passed, lowest priority number
 * first. A cooldown stops a fired event from firing again on the same screen.
 */
class EventScheduler(
    private val plan: EventPlan,
) {
    private val nextAllowed = HashMap<String, Long>()
    private val lastFired = HashMap<String, Long>()

    fun eligible(now: Long): List<CompiledEvent> =
        plan.events.filter { it.event.enabled && now >= (nextAllowed[it.event.id] ?: Long.MIN_VALUE) }

    fun markFired(
        event: Event,
        now: Long,
    ) {
        lastFired[event.id] = now
        nextAllowed[event.id] = now + event.cooldownMs
    }

    fun lastFiredAt(eventId: String): Long? = lastFired[eventId]
}
