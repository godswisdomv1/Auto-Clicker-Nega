package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.CounterDef
import com.zdmgold.autoclickernega.core.model.CounterOpKind

/** Counters of one run. Each starts at its declared value, and none survives the run. */
class CounterStore(
    defs: List<CounterDef> = emptyList(),
    private val onChange: (Map<String, Int>) -> Unit = {},
) {
    private val values = LinkedHashMap<String, Int>()

    init {
        register(defs)
    }

    /** Adds counters that do not exist yet. Used when a Run script action brings in another script's counters. */
    @Synchronized
    fun register(defs: List<CounterDef>) {
        var added = false
        for (d in defs) {
            if (d.id !in values) {
                values[d.id] = d.start
                added = true
            }
        }
        if (added) onChange(values.toMap())
    }

    @Synchronized
    fun get(id: String): Int = values[id] ?: 0

    @Synchronized
    fun snapshot(): Map<String, Int> = values.toMap()

    /** Applies a counter action. A counter that is not registered is ignored, and results saturate instead of wrapping. */
    @Synchronized
    fun apply(op: Action.CounterOp) {
        val current = values[op.id] ?: return
        val next =
            when (op.op) {
                CounterOpKind.Set -> op.value.toLong()
                CounterOpKind.Add -> current.toLong() + op.value
                CounterOpKind.Subtract -> current.toLong() - op.value
            }
        values[op.id] = next.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        onChange(values.toMap())
    }
}
