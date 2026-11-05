package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Event
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.result.Outcome

/** An event with the condition its "tap on the match" actions anchor to, if it has one. */
data class CompiledEvent(
    val event: Event,
    val anchor: Condition?,
)

/** Events in the order the scheduler tries them: lower priority numbers first. */
data class EventPlan(
    val events: List<CompiledEvent>,
)

/** Smart mode: validates a script, orders its events by priority and resolves match anchors. */
object EventCompiler {
    fun compile(script: Script): Outcome<EventPlan> {
        val body =
            script.body as? ScriptBody.Smart
                ?: return Outcome.Failure(
                    AppError.MalformedImport(ImportProblem.InvalidField, "script is not a Smart script"),
                )
        return when (val checked = ScriptValidator.validate(script)) {
            is Outcome.Failure -> {
                checked
            }

            is Outcome.Success -> {
                val events = (checked.value.body as ScriptBody.Smart).events
                check(events.size == body.events.size)
                Outcome.Success(
                    EventPlan(
                        events
                            .withIndex()
                            .sortedWith(compareBy({ it.value.priority }, { it.index }))
                            .map { CompiledEvent(it.value, anchorOf(it.value)) },
                    ),
                )
            }
        }
    }

    private fun anchorOf(event: Event): Condition? =
        event.whenExpr.items.firstOrNull { !it.negate && (it is Condition.Image || it is Condition.Text) }
}
