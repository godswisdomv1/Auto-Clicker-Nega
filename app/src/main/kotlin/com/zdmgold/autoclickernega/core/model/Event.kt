package com.zdmgold.autoclickernega.core.model

/** A Smart-mode rule. Lower [priority] numbers run first. */
data class Event(
    val id: String,
    val name: String,
    val priority: Int,
    val cooldownMs: Long = 0,
    val enabled: Boolean = true,
    val whenExpr: Expr,
    val actions: List<Action>,
)
