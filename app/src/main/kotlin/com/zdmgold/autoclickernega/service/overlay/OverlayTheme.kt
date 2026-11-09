package com.zdmgold.autoclickernega.service.overlay

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** The design tokens as plain numbers for the overlay's views, so the floating panel matches the app. */
class OverlayTheme(
    context: Context,
    val dark: Boolean,
) {
    private val colors = if (dark) Tokens.dark else Tokens.light
    private val density = context.resources.displayMetrics.density

    val background = colors.bgSecondary.toArgb()
    val surface = colors.bgTertiary.toArgb()
    val border = colors.borderSubtle.toArgb()
    val accent = colors.accent.toArgb()
    val accentDim = colors.accentDim.toArgb()
    val onAccent = colors.onAccent.toArgb()
    val text = colors.textPrimary.toArgb()
    val textSecondary = colors.textSecondary.toArgb()
    val textTertiary = colors.textTertiary.toArgb()
    val error = colors.error.toArgb()
    val markerTap = colors.markerTap.toArgb()
    val markerSwipe = colors.markerSwipe.toArgb()
    val markerImage = colors.markerImage.toArgb()
    val scrim = colors.scrim.toArgb()
    val shadow = Tokens.PanelShadow.color.toArgb()

    val regular: Typeface = context.resources.getFont(R.font.inter_regular)
    val semibold: Typeface = context.resources.getFont(R.font.inter_semibold)

    fun dp(value: Float): Int = (value * density + HALF).toInt()

    fun dp(value: Int): Int = dp(value.toFloat())

    fun dp(value: Dp): Int = dp(value.value)

    fun dpf(value: Float): Float = value * density

    fun dpf(value: Dp): Float = value.value * density

    private companion object {
        const val HALF = 0.5f
    }
}
