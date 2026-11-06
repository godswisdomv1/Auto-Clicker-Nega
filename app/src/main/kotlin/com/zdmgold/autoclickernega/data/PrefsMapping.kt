package com.zdmgold.autoclickernega.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentDecision
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RatingState
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.model.ThemeMode

/** Reads and writes the app's small state as DataStore preferences. Unknown or invalid values fall back to defaults. */
internal object PrefsMapping {
    private val themeMode = stringPreferencesKey("theme_mode")
    private val languageTag = stringPreferencesKey("language_tag")
    private val defaultIntervalMs = longPreferencesKey("default_interval_ms")
    private val defaultJitterPx = intPreferencesKey("default_jitter_px")
    private val defaultCountdownSec = intPreferencesKey("default_countdown_sec")
    private val defaultStartDelayMs = longPreferencesKey("default_start_delay_ms")
    private val searchRate = stringPreferencesKey("search_rate")
    private val panelOpacity = floatPreferencesKey("panel_opacity")
    private val panelX = intPreferencesKey("panel_x")
    private val panelY = intPreferencesKey("panel_y")
    private val consentDecision = stringPreferencesKey("consent_decision")
    private val consentPrivacyRequired = booleanPreferencesKey("consent_privacy_required")
    private val entitlementPro = booleanPreferencesKey("entitlement_pro")
    private val entitlementVerifiedAt = longPreferencesKey("entitlement_verified_at")
    private val ratingRuns = intPreferencesKey("rating_successful_runs")
    private val ratingPrompted = booleanPreferencesKey("rating_prompted")
    private val onboardingDone = booleanPreferencesKey("onboarding_done")

    private inline fun <reified E : Enum<E>> Preferences.enumOr(
        key: Preferences.Key<String>,
        default: E,
    ): E = enumValues<E>().firstOrNull { it.name == this[key] } ?: default

    fun settings(p: Preferences): AppSettings {
        val defaults = AppSettings()
        val x = p[panelX]
        val y = p[panelY]
        return AppSettings(
            themeMode = p.enumOr(themeMode, defaults.themeMode),
            languageTag = p[languageTag],
            defaultIntervalMs = Limits.clampInterval(p[defaultIntervalMs] ?: defaults.defaultIntervalMs),
            defaultJitterPx = maxOf(p[defaultJitterPx] ?: defaults.defaultJitterPx, 0),
            defaultCountdownSec = Limits.clampCountdown(p[defaultCountdownSec] ?: defaults.defaultCountdownSec),
            defaultStartDelayMs = Limits.clampStartDelay(p[defaultStartDelayMs] ?: defaults.defaultStartDelayMs),
            searchRate = p.enumOr(searchRate, defaults.searchRate),
            panelOpacity = AppSettings.clampOpacity(p[panelOpacity] ?: defaults.panelOpacity),
            panelPosition = if (x != null && y != null) Point(x, y) else null,
        )
    }

    fun write(
        p: MutablePreferences,
        s: AppSettings,
    ) {
        p[themeMode] = s.themeMode.name
        setOrRemove(p, languageTag, s.languageTag)
        p[defaultIntervalMs] = s.defaultIntervalMs
        p[defaultJitterPx] = s.defaultJitterPx
        p[defaultCountdownSec] = s.defaultCountdownSec
        p[defaultStartDelayMs] = s.defaultStartDelayMs
        p[searchRate] = s.searchRate.name
        p[panelOpacity] = s.panelOpacity
        setOrRemove(p, panelX, s.panelPosition?.x)
        setOrRemove(p, panelY, s.panelPosition?.y)
    }

    fun consent(p: Preferences) =
        ConsentState(
            decision = p.enumOr(consentDecision, ConsentDecision.Unknown),
            privacyOptionsRequired = p[consentPrivacyRequired] ?: false,
        )

    fun writeConsent(
        p: MutablePreferences,
        c: ConsentState,
    ) {
        p[consentDecision] = c.decision.name
        p[consentPrivacyRequired] = c.privacyOptionsRequired
    }

    fun entitlement(p: Preferences) =
        Entitlement(
            isPro = p[entitlementPro] ?: false,
            verifiedAtMs = p[entitlementVerifiedAt],
        )

    fun writeEntitlement(
        p: MutablePreferences,
        e: Entitlement,
    ) {
        p[entitlementPro] = e.isPro
        setOrRemove(p, entitlementVerifiedAt, e.verifiedAtMs)
    }

    fun rating(p: Preferences) =
        RatingState(
            successfulRuns = maxOf(p[ratingRuns] ?: 0, 0),
            prompted = p[ratingPrompted] ?: false,
        )

    fun writeRating(
        p: MutablePreferences,
        r: RatingState,
    ) {
        p[ratingRuns] = r.successfulRuns
        p[ratingPrompted] = r.prompted
    }

    fun onboardingDone(p: Preferences): Boolean = p[onboardingDone] ?: false

    fun writeOnboardingDone(
        p: MutablePreferences,
        done: Boolean,
    ) {
        p[onboardingDone] = done
    }

    private fun <T> setOrRemove(
        p: MutablePreferences,
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) p.remove(key) else p[key] = value
    }
}
