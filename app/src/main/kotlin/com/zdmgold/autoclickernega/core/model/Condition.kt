package com.zdmgold.autoclickernega.core.model

import com.zdmgold.autoclickernega.core.model.Limits.DEFAULT_COLOR_TOLERANCE
import com.zdmgold.autoclickernega.core.model.Limits.DEFAULT_CONFIDENCE

enum class MatchMode { Safe, Fast }

enum class TextMatchKind { Contains, Equals }

enum class Comparison(
    val symbol: String,
) {
    Lt("<"),
    Lte("<="),
    Eq("=="),
    Neq("!="),
    Gte(">="),
    Gt(">"),
    ;

    fun holds(
        left: Long,
        right: Long,
    ): Boolean =
        when (this) {
            Lt -> left < right
            Lte -> left <= right
            Eq -> left == right
            Neq -> left != right
            Gte -> left >= right
            Gt -> left > right
        }

    fun holds(
        left: Double,
        right: Double,
    ): Boolean =
        when (this) {
            Lt -> left < right
            Lte -> left <= right
            Eq -> left == right
            Neq -> left != right
            Gte -> left >= right
            Gt -> left > right
        }

    companion object {
        fun fromSymbol(symbol: String): Comparison? = entries.firstOrNull { it.symbol == symbol }
    }
}

/** What a timer condition measures from. */
sealed interface TimerSince {
    data object RunStart : TimerSince

    data class EventFired(
        val eventId: String,
    ) : TimerSince
}

sealed interface Condition {
    val negate: Boolean

    data class Image(
        val template: String,
        val confidence: Float = DEFAULT_CONFIDENCE,
        val mode: MatchMode = MatchMode.Safe,
        val region: Rect? = null,
        override val negate: Boolean = false,
    ) : Condition

    data class Text(
        val text: String,
        val kind: TextMatchKind = TextMatchKind.Contains,
        val caseSensitive: Boolean = false,
        val lang: String = "en",
        val region: Rect? = null,
        override val negate: Boolean = false,
    ) : Condition

    /** A colour at [point], or anywhere in [region] when it is set, within [tolerance] per channel. */
    data class Color(
        val point: Point,
        val rgb: Int,
        val tolerance: Int = DEFAULT_COLOR_TOLERANCE,
        val region: Rect? = null,
        override val negate: Boolean = false,
    ) : Condition

    /** A number read from [region] and compared with [value]. */
    data class Number(
        val region: Rect,
        val cmp: Comparison,
        val value: Double,
        val lang: String = "en",
        override val negate: Boolean = false,
    ) : Condition

    data class Timer(
        val since: TimerSince,
        val cmp: Comparison,
        val ms: Long,
        override val negate: Boolean = false,
    ) : Condition

    data class Counter(
        val id: String,
        val cmp: Comparison,
        val value: Int,
        override val negate: Boolean = false,
    ) : Condition
}
