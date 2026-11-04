package com.zdmgold.autoclickernega.core.model

enum class BoolOp { And, Or }

/** One to three conditions combined with AND or OR. The limit is enforced by the validator. */
data class Expr(
    val op: BoolOp,
    val items: List<Condition>,
)
