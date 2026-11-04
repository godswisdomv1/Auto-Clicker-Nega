package com.zdmgold.autoclickernega.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zdmgold.autoclickernega.R

/** Inter, four static weights. Scripts Inter does not cover fall back to the system font. */
val InterFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

/** The ten text roles. Sizes, weights and line heights are fixed by the design system. */
data class NegaTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    /** Uppercasing is applied by the SectionHeader component, since a text style cannot do it. */
    val sectionHeader: TextStyle,
    val mono: TextStyle,
)

private fun role(
    size: Int,
    weight: FontWeight,
    lineHeightMultiple: Float,
): TextStyle =
    TextStyle(
        fontFamily = InterFamily,
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = (size * lineHeightMultiple).sp,
    )

fun negaTypography(): NegaTypography =
    NegaTypography(
        display = role(size = 34, weight = FontWeight.Bold, lineHeightMultiple = 1.1f),
        headline = role(size = 28, weight = FontWeight.SemiBold, lineHeightMultiple = 1.2f),
        title1 = role(size = 22, weight = FontWeight.SemiBold, lineHeightMultiple = 1.3f),
        title2 = role(size = 17, weight = FontWeight.SemiBold, lineHeightMultiple = 1.3f),
        body = role(size = 17, weight = FontWeight.Normal, lineHeightMultiple = 1.5f),
        callout = role(size = 16, weight = FontWeight.Medium, lineHeightMultiple = 1.4f),
        footnote = role(size = 13, weight = FontWeight.Normal, lineHeightMultiple = 1.4f),
        caption = role(size = 12, weight = FontWeight.Normal, lineHeightMultiple = 1.3f),
        sectionHeader =
            role(
                size = 12,
                weight = FontWeight.SemiBold,
                lineHeightMultiple = 1.3f,
            ).copy(letterSpacing = 0.8.sp),
        mono =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = (14 * 1.4f).sp,
            ),
    )
