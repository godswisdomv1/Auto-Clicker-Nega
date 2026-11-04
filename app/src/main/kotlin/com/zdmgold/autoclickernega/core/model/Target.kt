package com.zdmgold.autoclickernega.core.model

/** Where a tap lands: a fixed point, or relative to the match that made its event pass. */
sealed interface Target {
    data class At(
        val p: Point,
    ) : Target

    data class AtMatch(
        val offset: Point,
    ) : Target
}
