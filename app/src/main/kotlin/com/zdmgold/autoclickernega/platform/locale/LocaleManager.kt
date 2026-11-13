package com.zdmgold.autoclickernega.platform.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The app's own language, independent of the phone's. It uses the platform's per-app language where it exists and
 * AppCompat's equivalent on older versions, so a change applies at once and is remembered.
 */
class LocaleManager {
    /** Sets the language, or follows the system language when [tag] is null. */
    fun apply(tag: String?) {
        val list = if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(list)
    }

    /** The language chosen for the app, or null when it follows the system. */
    fun current(): String? = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null }
}
