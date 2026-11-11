package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The root of every screen: the primary background, with the system bars and the keyboard honoured. Sheets and
 * dialogs are placed inside it so they can cover the screen.
 */
@Composable
fun NegaScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(NegaTheme.colors.bgPrimary)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        content()
    }
}

/** A filled card with a hairline border. No elevation, no ripple. Pass [onClick] to make the whole card tappable. */
@Composable
fun NegaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Tokens.Radius.card)
    val base =
        modifier
            .clip(shape)
            .background(NegaTheme.colors.bgSecondary, shape)
            .border(Tokens.Stroke.hairline, NegaTheme.colors.borderSubtle, shape)
    Column(modifier = if (onClick != null) base.pressable(onClick = onClick) else base, content = content)
}
