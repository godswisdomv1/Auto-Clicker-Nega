package com.zdmgold.autoclickernega.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The only icon composable. The drawables are plain strokes, so the tint decides the colour, and the size comes
 * from the design tokens. Pass a [contentDescription] for an icon that stands alone, or null when a label next
 * to it already says what it means.
 */
@Composable
fun AppIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = Tokens.IconSize.row,
    tint: Color = NegaTheme.colors.textPrimary,
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
