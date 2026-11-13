package com.zdmgold.autoclickernega.ui

import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentDecision
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.core.model.ThemeMode
import com.zdmgold.autoclickernega.ui.theme.ThemeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUiStateTest {
    private val permitted = ConsentState(ConsentDecision.Permitted, privacyOptionsRequired = true)

    @Test
    fun theStateStartsNotLoadedSoNothingFlashes() {
        assertFalse(AppUiState().loaded)
    }

    @Test
    fun combinesEverySourceIntoOneState() {
        val s =
            AppUiState.from(
                AppSettings(themeMode = ThemeMode.Dark, languageTag = "ar"),
                Entitlement.Free,
                permitted,
                onboardingDone = true,
                service = ServiceStatus.Ready,
            )
        assertTrue(s.loaded)
        assertEquals(ThemeMode.Dark, s.themeMode)
        assertEquals("ar", s.languageTag)
        assertTrue(s.canRequestAds)
        assertTrue(s.privacyOptionsRequired)
        assertTrue(s.onboardingDone)
        assertEquals(ServiceStatus.Ready, s.service)
    }

    @Test
    fun proTurnsEveryAdOff() {
        val s = AppUiState.from(AppSettings(), Entitlement(isPro = true), permitted, true, ServiceStatus.Off)
        assertTrue(s.isPro)
        assertFalse(s.canRequestAds)
    }

    @Test
    fun adsNeedConsent() {
        val s =
            AppUiState.from(
                AppSettings(),
                Entitlement.Free,
                ConsentState(ConsentDecision.Declined),
                true,
                ServiceStatus.Off,
            )
        assertFalse(s.canRequestAds)
    }

    @Test
    fun themeFollowsTheChoiceOrTheSystem() {
        assertTrue(ThemeResolver.isDark(ThemeMode.Dark, systemDark = false))
        assertFalse(ThemeResolver.isDark(ThemeMode.Light, systemDark = true))
        assertTrue(ThemeResolver.isDark(ThemeMode.System, systemDark = true))
        assertFalse(ThemeResolver.isDark(ThemeMode.System, systemDark = false))
    }

    @Test
    fun theServiceIsReadyOnlyWhenEnabledAndConnected() {
        assertEquals(ServiceStatus.Off, ServiceStatus.of(enabledInSettings = false, connected = false))
        assertEquals(ServiceStatus.Off, ServiceStatus.of(enabledInSettings = false, connected = true))
        assertEquals(ServiceStatus.Connecting, ServiceStatus.of(enabledInSettings = true, connected = false))
        assertEquals(ServiceStatus.Ready, ServiceStatus.of(enabledInSettings = true, connected = true))
    }
}
