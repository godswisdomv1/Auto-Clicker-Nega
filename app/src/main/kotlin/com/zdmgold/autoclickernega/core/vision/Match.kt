package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect

/** Where a target was found, with how well it matched (0 to 1). */
data class Match(
    val score: Float,
    val box: Rect,
) {
    val center: Point get() = box.center
}

/**
 * The outcome of a search. [best] is the strongest candidate even when it is below [threshold], so the HUD can
 * show a live score and explain a miss. [accepted] is set only when the score meets the threshold.
 */
data class MatchResult(
    val best: Match?,
    val threshold: Float,
) {
    val accepted: Match? get() = best?.takeIf { it.score >= threshold }
}
