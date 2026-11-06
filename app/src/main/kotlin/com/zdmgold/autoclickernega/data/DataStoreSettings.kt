package com.zdmgold.autoclickernega.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.ConsentState
import com.zdmgold.autoclickernega.core.model.Entitlement
import com.zdmgold.autoclickernega.core.model.RatingState
import com.zdmgold.autoclickernega.core.port.SettingsStore
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Settings, and the cached consent, entitlement and rating state, kept in one Preferences DataStore. */
class DataStoreSettings(
    private val store: DataStore<Preferences>,
) : SettingsStore {
    private val data: Flow<Preferences> =
        store.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    override val settings: Flow<AppSettings> = data.map(PrefsMapping::settings)
    override val consent: Flow<ConsentState> = data.map(PrefsMapping::consent)
    override val entitlement: Flow<Entitlement> = data.map(PrefsMapping::entitlement)
    override val rating: Flow<RatingState> = data.map(PrefsMapping::rating)
    override val onboardingDone: Flow<Boolean> = data.map(PrefsMapping::onboardingDone)

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings): Outcome<Unit> =
        write { PrefsMapping.write(it, transform(PrefsMapping.settings(it))) }

    override suspend fun saveConsent(state: ConsentState): Outcome<Unit> =
        write { PrefsMapping.writeConsent(it, state) }

    override suspend fun saveEntitlement(entitlement: Entitlement): Outcome<Unit> =
        write { PrefsMapping.writeEntitlement(it, entitlement) }

    override suspend fun updateRating(transform: (RatingState) -> RatingState): Outcome<Unit> =
        write { PrefsMapping.writeRating(it, transform(PrefsMapping.rating(it))) }

    override suspend fun setOnboardingDone(done: Boolean): Outcome<Unit> =
        write { PrefsMapping.writeOnboardingDone(it, done) }

    override suspend fun clearAll(): Outcome<Unit> = write { it.clear() }

    private suspend fun write(change: (MutablePreferences) -> Unit): Outcome<Unit> =
        try {
            store.edit { change(it) }
            Outcome.Success(Unit)
        } catch (e: IOException) {
            Outcome.Failure(AppError.Storage(e.message.orEmpty()))
        }
}
