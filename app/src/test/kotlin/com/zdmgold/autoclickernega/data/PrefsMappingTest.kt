package com.zdmgold.autoclickernega.data

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentDecision
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.RatingState
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PrefsMappingTest {
    @Test
    fun emptyPreferencesGiveTheDefaults() {
        val p = emptyPreferences()
        assertEquals(AppSettings(), PrefsMapping.settings(p))
        assertEquals(ConsentState(), PrefsMapping.consent(p))
        assertEquals(Entitlement.Free, PrefsMapping.entitlement(p))
        assertEquals(RatingState(), PrefsMapping.rating(p))
        assertFalse(PrefsMapping.onboardingDone(p))
    }

    @Test
    fun settingsRoundTrip() {
        val s =
            AppSettings(
                themeMode = ThemeMode.Dark,
                languageTag = "ar",
                defaultIntervalMs = 250,
                defaultJitterPx = 4,
                defaultCountdownSec = 7,
                defaultStartDelayMs = 60_000,
                searchRate = SearchRate.BatterySaver,
                panelOpacity = 0.5f,
                panelPosition = Point(12, 34),
            )
        val p = mutablePreferencesOf()
        PrefsMapping.write(p, s)
        assertEquals(s, PrefsMapping.settings(p))
    }

    @Test
    fun clearingTheLanguageAndPositionRemovesThem() {
        val p = mutablePreferencesOf()
        PrefsMapping.write(p, AppSettings(languageTag = "fr", panelPosition = Point(1, 2)))
        PrefsMapping.write(p, AppSettings())
        assertNull(PrefsMapping.settings(p).languageTag)
        assertNull(PrefsMapping.settings(p).panelPosition)
    }

    @Test
    fun outOfRangeStoredValuesAreClamped() {
        val p = mutablePreferencesOf()
        PrefsMapping.write(
            p,
            AppSettings(defaultIntervalMs = 1, panelOpacity = 5f, defaultStartDelayMs = Long.MAX_VALUE),
        )
        val s = PrefsMapping.settings(p)
        assertEquals(50L, s.defaultIntervalMs)
        assertEquals(1.0f, s.panelOpacity, 0f)
        assertEquals(43_200_000L, s.defaultStartDelayMs)
    }

    @Test
    fun unknownEnumNamesFallBackToTheDefault() {
        val p =
            mutablePreferencesOf(
                stringPreferencesKey("theme_mode") to "Sepia",
                stringPreferencesKey("consent_decision") to "Maybe",
            )
        assertEquals(ThemeMode.System, PrefsMapping.settings(p).themeMode)
        assertEquals(ConsentDecision.Unknown, PrefsMapping.consent(p).decision)
    }

    @Test
    fun consentEntitlementRatingAndOnboardingRoundTrip() {
        val p = mutablePreferencesOf()
        PrefsMapping.writeConsent(p, ConsentState(ConsentDecision.Declined, privacyOptionsRequired = true))
        PrefsMapping.writeEntitlement(p, Entitlement(isPro = true, verifiedAtMs = 99))
        PrefsMapping.writeRating(p, RatingState(3, true))
        PrefsMapping.writeOnboardingDone(p, true)
        assertEquals(ConsentState(ConsentDecision.Declined, true), PrefsMapping.consent(p))
        assertEquals(Entitlement(true, 99), PrefsMapping.entitlement(p))
        assertEquals(RatingState(3, true), PrefsMapping.rating(p))
        assertEquals(true, PrefsMapping.onboardingDone(p))
        PrefsMapping.writeEntitlement(p, Entitlement.Free)
        assertNull(PrefsMapping.entitlement(p).verifiedAtMs)
    }
}
