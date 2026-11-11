package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.zdmgold.autoclickernega.ui.theme.NegaTheme

/** The one text composable. Text always takes its style and colour from the tokens, so nothing drifts. */
@Composable
fun NegaText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NegaTheme.typography.body,
    color: Color = NegaTheme.colors.textPrimary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
