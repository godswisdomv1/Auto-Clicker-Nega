package com.zdmgold.autoclickernega.core.model

/** A named counter. Counters are per run and each starts at [start]. */
data class CounterDef(
    val id: String,
    val name: String,
    val start: Int = 0,
)
