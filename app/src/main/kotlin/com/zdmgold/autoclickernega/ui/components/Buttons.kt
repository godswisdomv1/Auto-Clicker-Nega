package com.zdmgold.autoclickernega.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.zdmgold.autoclickernega.ui.icons.AppIcon
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** A filled accent button. Never a 1 dp outline for a primary action. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val colors = NegaTheme.colors
    FilledButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = if (enabled) colors.accent else colors.bgTertiary,
        content = if (enabled) colors.onAccent else colors.textTertiary,
        icon = icon,
    )
}

/** A filled neutral button for secondary actions. [destructive] colours the label as a warning. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    val colors = NegaTheme.colors
    FilledButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fill = colors.bgTertiary,
        content =
            when {
                !enabled -> colors.textTertiary
                destructive -> colors.error
                else -> colors.textPrimary
            },
        icon = icon,
    )
}

@Composable
private fun FilledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Color,
    content: Color,
    @DrawableRes icon: Int?,
) {
    val shape = RoundedCornerShape(Tokens.Radius.button)
    Row(
        modifier =
            modifier
                .heightIn(min = Metrics.buttonHeight)
                .clip(shape)
                .background(fill, shape)
                .pressable(enabled = enabled, onClick = onClick)
                .padding(horizontal = Tokens.Spacing.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(icon, size = Tokens.IconSize.row, tint = content)
            Spacer(Modifier.width(Tokens.Spacing.s))
        }
        NegaText(label, style = NegaTheme.typography.title2, color = content, textAlign = TextAlign.Center)
    }
}

/** An icon in a 40 dp container with a 48 dp touch target, so no icon ever floats bare. */
@Composable
fun ChipIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = NegaTheme.colors.textPrimary,
    mirrorInRtl: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .size(Tokens.touchTarget)
                .pressable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(Metrics.chipIconContainer)
                    .background(NegaTheme.colors.bgTertiary, RoundedCornerShape(Tokens.Radius.chip)),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = icon,
                modifier = if (mirrorInRtl) Modifier.mirrorInRtl() else Modifier,
                size = Tokens.IconSize.row,
                tint = if (enabled) tint else NegaTheme.colors.textTertiary,
            )
        }
    }
}
