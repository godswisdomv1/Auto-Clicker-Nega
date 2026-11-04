package com.zdmgold.autoclickernega.core.model

/** The two script models. Simple is an ordered list of actions. Smart is a list of events. */
sealed interface ScriptBody {
    data class Simple(
        val steps: List<Action>,
    ) : ScriptBody

    data class Smart(
        val events: List<Event>,
    ) : ScriptBody
}

data class Script(
    val id: String,
    val name: String,
    val screen: ScreenSpec,
    val run: RunConfig = RunConfig(),
    val templates: Map<String, TemplateRef> = emptyMap(),
    val counters: List<CounterDef> = emptyList(),
    val body: ScriptBody,
    val schema: Int = SCHEMA_VERSION,
) {
    /** Number of steps or events, shown on Home and used for the size limit. */
    val itemCount: Int
        get() =
            when (body) {
                is ScriptBody.Simple -> body.steps.size
                is ScriptBody.Smart -> body.events.size
            }

    companion object {
        /** Nothing has shipped, so the first schema is 1. */
        const val SCHEMA_VERSION = 1
    }
}
