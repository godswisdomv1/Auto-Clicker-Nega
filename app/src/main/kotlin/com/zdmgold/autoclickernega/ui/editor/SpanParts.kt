package com.zdmgold.autoclickernega.ui.editor

/** One unit of a spoken-style duration, such as the "30 min" in "1 h 30 min". */
enum class SpanUnit { Hours, Minutes, Seconds, Millis }

data class SpanPart(
    val unit: SpanUnit,
    val value: Long,
)

/**
 * Splits a duration into the parts a person would say: "1 h 30 min", "45 s", or "1500 ms" when a second does not
 * divide it evenly. The screens turn each part into text from the string resources.
 */
object SpanParts {
    private const val MS_PER_SECOND = 1000L
    private const val MS_PER_MINUTE = 60_000L
    private const val MS_PER_HOUR = 3_600_000L

    fun of(ms: Long): List<SpanPart> {
        val total = ms.coerceAtLeast(0)
        val partialSecond = total < MS_PER_MINUTE && total % MS_PER_SECOND != 0L
        if (total < MS_PER_SECOND || partialSecond) return listOf(SpanPart(SpanUnit.Millis, total))
        val hours = total / MS_PER_HOUR
        val minutes = total % MS_PER_HOUR / MS_PER_MINUTE
        val seconds = total % MS_PER_MINUTE / MS_PER_SECOND
        return buildList {
            if (hours > 0) add(SpanPart(SpanUnit.Hours, hours))
            if (minutes > 0) add(SpanPart(SpanUnit.Minutes, minutes))
            if (seconds > 0) add(SpanPart(SpanUnit.Seconds, seconds))
        }
    }
}
