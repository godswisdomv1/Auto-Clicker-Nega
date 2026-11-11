package com.zdmgold.autoclickernega.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Flips an arrow-like icon in right-to-left layouts. Everything else flips through start and end. */
@Composable
fun Modifier.mirrorInRtl(): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return if (rtl) graphicsLayer { scaleX = -1f } else this
}
