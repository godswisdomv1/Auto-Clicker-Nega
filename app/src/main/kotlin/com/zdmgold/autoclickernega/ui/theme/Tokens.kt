package com.zdmgold.autoclickernega.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One complete colour set. Light is the default and dark is first class. */
data class NegaColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val accent: Color,
    val accentDim: Color,
    val accentText: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: Color,
    val borderFocus: Color,
    val error: Color,
    val success: Color,
    val warning: Color,
    val markerTap: Color,
    val markerSwipe: Color,
    val markerImage: Color,
    val scrim: Color,
)

/** The only place raw design values exist. Every other file reads them from here. */
object Tokens {
    val light =
        NegaColors(
            bgPrimary = Color(0xFFF5F6F8),
            bgSecondary = Color(0xFFFFFFFF),
            bgTertiary = Color(0xFFECEEF2),
            accent = Color(0xFF12C79C),
            accentDim = Color(0xFF0FA683),
            accentText = Color(0xFF007A62),
            onAccent = Color(0xFF052B23),
            textPrimary = Color(0xFF0E1116),
            textSecondary = Color(0xFF4A5260),
            textTertiary = Color(0xFF636B78),
            borderSubtle = Color(0xFFE1E4EA),
            borderFocus = Color(0xFF007A62),
            error = Color(0xFFCD2A1E),
            success = Color(0xFF197A48),
            warning = Color(0xFFAE5009),
            markerTap = Color(0xFF12C79C),
            markerSwipe = Color(0xFF4F6DF5),
            markerImage = Color(0xFF9B45D6),
            scrim = Color(0x66000000),
        )

    val dark =
        NegaColors(
            bgPrimary = Color(0xFF0B0D10),
            bgSecondary = Color(0xFF15181D),
            bgTertiary = Color(0xFF1E2229),
            accent = Color(0xFF2EE6B6),
            accentDim = Color(0xFF1FB892),
            accentText = Color(0xFF2EE6B6),
            onAccent = Color(0xFF052B23),
            textPrimary = Color(0xFFF2F4F7),
            textSecondary = Color(0xFFA4ABB6),
            textTertiary = Color(0xFF8A929E),
            borderSubtle = Color(0xFF262B33),
            borderFocus = Color(0xFF2EE6B6),
            error = Color(0xFFFF6B61),
            success = Color(0xFF4ADE9A),
            warning = Color(0xFFF5B04A),
            markerTap = Color(0xFF2EE6B6),
            markerSwipe = Color(0xFF8FA8FF),
            markerImage = Color(0xFFD49BFF),
            scrim = Color(0x99000000),
        )

    /** Spacing scale. Every gap is a decision, and unrelated groups never share the same gap. */
    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val s: Dp = 8.dp
        val m: Dp = 16.dp
        val l: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
        val xxxl: Dp = 64.dp

        val all: List<Dp> = listOf(xxs, xs, s, m, l, xl, xxl, xxxl)
    }

    object Radius {
        val card: Dp = 16.dp
        val hero: Dp = 20.dp
        val chip: Dp = 12.dp
        val button: Dp = 12.dp
        val input: Dp = 12.dp
        val sheet: Dp = 20.dp
        val dialog: Dp = 16.dp

        val all: List<Dp> = listOf(card, hero, chip, button, input, sheet, dialog)

        /** A pill is half its height. */
        fun pill(height: Dp): Dp = height / 2
    }

    object IconSize {
        val nav: Dp = 22.dp
        val row: Dp = 22.dp
        val chevron: Dp = 16.dp
        val hero: Dp = 28.dp
        val panel: Dp = 20.dp

        val all: List<Dp> = listOf(nav, row, chevron, hero, panel)
    }

    object Stroke {
        /** Icon stroke width, used by every vector drawable. */
        val icon: Dp = 2.dp

        /** Card and divider hairline. */
        val hairline: Dp = 0.5.dp
    }

    /** Minimum touch target on both axes. */
    val touchTarget: Dp = 48.dp

    /** Opacity while something is pressed. There is no ripple anywhere. */
    const val PRESSED_ALPHA: Float = 0.6f

    /** The only shadow in the product: the floating panel. */
    object PanelShadow {
        val color: Color = Color(0x2E000000)
        val blur: Dp = 16.dp
        val offsetY: Dp = 4.dp
    }
}
