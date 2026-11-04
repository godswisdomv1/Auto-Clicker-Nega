package com.zdmgold.autoclickernega.core.model

/** Hard limits and defaults shared by the validator, the editors and the engine. */
object Limits {
    const val MAX_SCRIPT_JSON_BYTES = 256 * 1024
    const val MAX_BUNDLE_BYTES = 4 * 1024 * 1024
    const val MAX_ITEMS = 500
    const val MAX_TEMPLATES = 40
    const val MAX_CONDITIONS = 3
    const val MAX_RUN_SCRIPT_DEPTH = 3

    const val MIN_CURVE_POINTS = 3
    const val MAX_CURVE_POINTS = 5
    const val MIN_MULTI_STROKES = 2
    const val MAX_MULTI_STROKES = 3

    /** The fastest interval the engine accepts by default. */
    const val MIN_INTERVAL_MS = 50L
    const val DEFAULT_INTERVAL_MS = 100L
    const val DEFAULT_HOLD_MS = 40L
    const val DEFAULT_COUNTDOWN_SEC = 3
    const val MAX_COUNTDOWN_SEC = 30

    const val MIN_CONFIDENCE = 0.60f
    const val MAX_CONFIDENCE = 0.98f
    const val DEFAULT_CONFIDENCE = 0.85f
    const val DEFAULT_COLOR_TOLERANCE = 12
    const val MAX_COLOR_TOLERANCE = 255

    /** Start in X: 12 hours. Lowered only if the 12-hour acceptance gate fails on a real phone. */
    const val MAX_START_WAIT_MS = 43_200_000L

    fun clampConfidence(value: Float): Float = value.coerceIn(MIN_CONFIDENCE, MAX_CONFIDENCE)

    fun clampInterval(ms: Long): Long = maxOf(ms, MIN_INTERVAL_MS)

    fun clampStartDelay(ms: Long): Long = ms.coerceIn(0L, MAX_START_WAIT_MS)

    fun clampCountdown(sec: Int): Int = sec.coerceIn(0, MAX_COUNTDOWN_SEC)

    fun clampTolerance(value: Int): Int = value.coerceIn(0, MAX_COLOR_TOLERANCE)
}
