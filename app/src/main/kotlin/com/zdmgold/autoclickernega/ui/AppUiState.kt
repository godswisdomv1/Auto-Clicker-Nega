package com.zdmgold.autoclickernega.ui

import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.core.model.ThemeMode

/** The state every screen shares: look and language, Pro, consent, whether onboarding is done, and the service. */
data class AppUiState(
    val loaded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val languageTag: String? = null,
    val isPro: Boolean = false,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val onboardingDone: Boolean = false,
    val service: ServiceStatus = ServiceStatus.Off,
) {
    companion object {
        fun from(
            settings: AppSettings,
            entitlement: Entitlement,
            consent: ConsentState,
            onboardingDone: Boolean,
            service: ServiceStatus,
        ) = AppUiState(
            loaded = true,
            themeMode = settings.themeMode,
            languageTag = settings.languageTag,
            isPro = entitlement.isPro,
            canRequestAds = consent.canRequestAds && !entitlement.isPro,
            privacyOptionsRequired = consent.privacyOptionsRequired,
            onboardingDone = onboardingDone,
            service = service,
        )
    }
}
