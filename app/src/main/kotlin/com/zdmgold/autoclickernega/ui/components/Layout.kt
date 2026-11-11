package com.zdmgold.autoclickernega.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/** Flips an arrow-like icon in right-to-left layouts. Everything else flips through start and end. */
@Composable
fun Modifier.mirrorInRtl(): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return if (rtl) graphicsLayer { scaleX = -1f } else this
}

/** The user's current locale, read in a way that makes the screen update when the language changes. */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
