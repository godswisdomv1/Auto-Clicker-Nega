package com.zdmgold.autoclickernega.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentDecision
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.RatingState
import com.zdmgold.autoclickernega.core.model.ThemeMode
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DataStoreSettingsTest {
    private fun withStore(block: suspend TestScope.(DataStoreSettings) -> Unit) =
        runTest {
            val dir = Files.createTempDirectory("nega-ds").toFile()
            try {
                val store =
                    PreferenceDataStoreFactory.create(
                        scope = backgroundScope,
                        produceFile = { File(dir, "test.preferences_pb") },
                    )
                block(DataStoreSettings(store))
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test
    fun startsWithDefaultsAndPersistsUpdates() =
        withStore { s ->
            assertEquals(AppSettings(), s.settings.first())
            assertTrue(
                s.updateSettings { it.copy(themeMode = ThemeMode.Dark, defaultIntervalMs = 300) } is Outcome.Success,
            )
            assertEquals(ThemeMode.Dark, s.settings.first().themeMode)
            assertEquals(300L, s.settings.first().defaultIntervalMs)
        }

    @Test
    fun consentEntitlementRatingAndOnboardingPersist() =
        withStore { s ->
            s.saveConsent(ConsentState(ConsentDecision.Permitted))
            s.saveEntitlement(Entitlement(true, 5))
            s.updateRating { it.copy(successfulRuns = it.successfulRuns + 1) }
            s.setOnboardingDone(true)
            assertTrue(s.consent.first().canRequestAds)
            assertEquals(Entitlement(true, 5), s.entitlement.first())
            assertEquals(RatingState(1, false), s.rating.first())
            assertTrue(s.onboardingDone.first())
        }

    @Test
    fun clearAllRestoresEverythingToDefaults() =
        withStore { s ->
            s.updateSettings { it.copy(themeMode = ThemeMode.Light) }
            s.saveEntitlement(Entitlement(true, 5))
            s.setOnboardingDone(true)
            assertTrue(s.clearAll() is Outcome.Success)
            assertEquals(AppSettings(), s.settings.first())
            assertEquals(Entitlement.Free, s.entitlement.first())
            assertFalse(s.onboardingDone.first())
        }
}
