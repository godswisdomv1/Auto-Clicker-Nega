package com.zdmgold.autoclickernega.ui.theme

import com.zdmgold.autoclickernega.core.model.ThemeMode

object ThemeResolver {
    /** Light, dark, or whatever the system is using. */
    fun isDark(
        mode: ThemeMode,
        systemDark: Boolean,
    ): Boolean =
        when (mode) {
            ThemeMode.Dark -> true
            ThemeMode.Light -> false
            ThemeMode.System -> systemDark
        }
}
