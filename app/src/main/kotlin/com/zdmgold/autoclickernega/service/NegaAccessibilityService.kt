package com.zdmgold.autoclickernega.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent

/**
 * The accessibility service. It performs the user's taps and swipes and captures the screen area the engine needs.
 * It asks for no accessibility events and cannot read other apps' content, so everything here is driven by the
 * engine, and the system only ever sees it perform gestures the user set up.
 */
class NegaAccessibilityService : AccessibilityService() {
    private var runtime: ServiceRuntime? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        runtime?.close()
        runtime = ServiceRuntime(this).also { it.start() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runtime?.onConfigurationChanged()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        runtime?.close()
        runtime = null
    }
}
