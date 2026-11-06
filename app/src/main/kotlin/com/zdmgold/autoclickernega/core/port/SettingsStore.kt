package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.RatingState
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.Flow

/** Everything small and per-device: settings, and the cached consent, entitlement and rating state. */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings): Outcome<Unit>

    val consent: Flow<ConsentState>

    suspend fun saveConsent(state: ConsentState): Outcome<Unit>

    val entitlement: Flow<Entitlement>

    suspend fun saveEntitlement(entitlement: Entitlement): Outcome<Unit>

    val rating: Flow<RatingState>

    suspend fun updateRating(transform: (RatingState) -> RatingState): Outcome<Unit>

    val onboardingDone: Flow<Boolean>

    suspend fun setOnboardingDone(done: Boolean): Outcome<Unit>

    /** Deletes everything this store holds. Used by Delete all local data. */
    suspend fun clearAll(): Outcome<Unit>
}
