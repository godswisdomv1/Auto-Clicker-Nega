package com.zdmgold.autoclickernega.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNegaColors = staticCompositionLocalOf { Tokens.light }
val LocalNegaTypography = staticCompositionLocalOf { negaTypography() }

/** Provides the tokens to every composable below it. The caller resolves light, dark or system. */
@Composable
fun NegaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) Tokens.dark else Tokens.light
    val typography = remember { negaTypography() }
    val reduceMotion = rememberReduceMotion()
    CompositionLocalProvider(
        LocalNegaColors provides colors,
        LocalNegaTypography provides typography,
        LocalReduceMotion provides reduceMotion,
        content = content,
    )
}

/** Read access to the current theme from any composable. */
object NegaTheme {
    val colors: NegaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNegaColors.current

    val typography: NegaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNegaTypography.current

    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current
}
