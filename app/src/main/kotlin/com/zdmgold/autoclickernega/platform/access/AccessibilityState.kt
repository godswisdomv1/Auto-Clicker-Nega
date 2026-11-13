package com.zdmgold.autoclickernega.platform.access

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.service.NegaAccessibilityService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Whether the accessibility service is switched on in the system settings, and whether it is actually connected. The
 * two differ when the system stops the service in the background, and then the user needs the one-tap fix.
 */
class AccessibilityState(
    private val context: Context,
    private val engineState: StateFlow<EngineState>,
) {
    private val component = ComponentName(context, NegaAccessibilityService::class.java).flattenToString()

    private val enabled: Flow<Boolean> =
        callbackFlow {
            val resolver = context.contentResolver
            val observer =
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        trySend(read())
                    }
                }
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                observer,
            )
            trySend(read())
            awaitClose { resolver.unregisterContentObserver(observer) }
        }.distinctUntilChanged()

    val status: Flow<ServiceStatus> =
        combine(
            enabled,
            engineState.map { it !is EngineState.ServiceLost }.distinctUntilChanged(),
            ServiceStatus::of,
        ).distinctUntilChanged()

    private fun read(): Boolean =
        AccessibilityComponents.isEnabled(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            component,
        )
}
