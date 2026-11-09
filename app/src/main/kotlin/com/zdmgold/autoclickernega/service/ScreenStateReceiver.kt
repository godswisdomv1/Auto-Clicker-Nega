package com.zdmgold.autoclickernega.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Tells the engine when the screen turns off, which ends a run and cancels a Start in X wait. */
class ScreenStateReceiver(
    private val onScreenOff: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) onScreenOff()
    }

    fun register(context: Context) {
        ContextCompat.registerReceiver(
            context,
            this,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: IllegalArgumentException) {
            // Already unregistered, which is fine.
        }
    }
}
