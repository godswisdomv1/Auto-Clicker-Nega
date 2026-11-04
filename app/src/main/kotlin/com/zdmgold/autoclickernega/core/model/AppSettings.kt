package com.zdmgold.autoclickernega.core.model

enum class ThemeMode { System, Light, Dark }

/** How often the engine looks at the screen. */
enum class SearchRate { Auto, Fast, BatterySaver }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** A BCP 47 tag, or null to follow the system language. */
    val languageTag: String? = null,
    val defaultIntervalMs: Long = Limits.DEFAULT_INTERVAL_MS,
    val defaultJitterPx: Int = 0,
    val defaultCountdownSec: Int = Limits.DEFAULT_COUNTDOWN_SEC,
    val defaultStartDelayMs: Long = 0,
    val searchRate: SearchRate = SearchRate.Auto,
    /** Floating panel opacity, 0.3 to 1.0. */
    val panelOpacity: Float = DEFAULT_PANEL_OPACITY,
) {
    companion object {
        const val MIN_PANEL_OPACITY = 0.3f
        const val MAX_PANEL_OPACITY = 1.0f
        const val DEFAULT_PANEL_OPACITY = 0.9f

        fun clampOpacity(value: Float): Float = value.coerceIn(MIN_PANEL_OPACITY, MAX_PANEL_OPACITY)
    }
}
