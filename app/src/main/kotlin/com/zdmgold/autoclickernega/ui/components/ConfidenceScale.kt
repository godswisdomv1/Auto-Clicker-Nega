package com.zdmgold.autoclickernega.ui.components

import com.zdmgold.autoclickernega.core.model.Limits
import kotlin.math.roundToInt

/** How a confidence value maps onto the slider, and what to tell the user about it. */
object ConfidenceScale {
    const val MIN = Limits.MIN_CONFIDENCE
    const val MAX = Limits.MAX_CONFIDENCE
    private const val LOOSE_BELOW = 0.75f
    private const val BALANCED_BELOW = 0.90f
    private const val HUNDRED = 100f

    enum class Hint { Loose, Balanced, Strict }

    /** The value kept in range and rounded to two decimals, the way it is shown and saved. */
    fun clamp(value: Float): Float = ((value.coerceIn(MIN, MAX) * HUNDRED).roundToInt()) / HUNDRED

    fun toFraction(value: Float): Float = (clamp(value) - MIN) / (MAX - MIN)

    fun fromFraction(fraction: Float): Float = clamp(MIN + fraction.coerceIn(0f, 1f) * (MAX - MIN))

    fun hint(value: Float): Hint =
        when {
            value < LOOSE_BELOW -> Hint.Loose
            value < BALANCED_BELOW -> Hint.Balanced
            else -> Hint.Strict
        }
}
