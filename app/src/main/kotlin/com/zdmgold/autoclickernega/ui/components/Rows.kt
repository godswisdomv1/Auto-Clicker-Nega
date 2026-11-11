package com.zdmgold.autoclickernega.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.ui.icons.AppIcon
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * A two-line row: a title, an optional subtitle, an optional leading icon and a trailing chevron. Sits inside a
 * [NegaCard]. The whole row is one tap target and one item for a screen reader.
 */
@Composable
fun NegaRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = onClick != null && trailing == null,
) {
    val base =
        modifier
            .fillMaxWidth()
            .heightIn(min = Metrics.rowMinHeight)
            .semantics(mergeDescendants = true) {}
    Row(
        modifier =
            (if (onClick != null) base.pressable(onClick = onClick) else base)
                .padding(horizontal = Tokens.Spacing.m, vertical = Tokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(icon, size = Tokens.IconSize.row, tint = NegaTheme.colors.accentText)
            Spacer(Modifier.width(Tokens.Spacing.m))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)) {
            NegaText(title, style = NegaTheme.typography.title2)
            if (subtitle != null) {
                NegaText(subtitle, style = NegaTheme.typography.footnote, color = NegaTheme.colors.textSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Tokens.Spacing.s))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(Tokens.Spacing.s))
            AppIcon(
                icon = AppIcons.ChevronRight,
                modifier = Modifier.mirrorInRtl(),
                size = Tokens.IconSize.chevron,
                tint = NegaTheme.colors.textTertiary,
            )
        }
    }
}

/** A small metadata pill, with an optional icon. */
@Composable
fun NegaChip(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    tint: Color = NegaTheme.colors.textSecondary,
) {
    val shape = RoundedCornerShape(Tokens.Radius.pill(Metrics.chipHeight))
    Row(
        modifier =
            modifier
                .heightIn(min = Metrics.chipHeight)
                .clip(shape)
                .background(NegaTheme.colors.bgTertiary, shape)
                .padding(horizontal = Tokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            AppIcon(icon, size = Tokens.IconSize.chevron, tint = tint)
            Spacer(Modifier.width(Tokens.Spacing.xs))
        }
        NegaText(text, style = NegaTheme.typography.footnote, color = tint, maxLines = 1)
    }
}

/** An uppercase group title above a list. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    NegaText(
        text = text.uppercase(),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = Tokens.Spacing.m,
                    end = Tokens.Spacing.m,
                    top = Tokens.Spacing.l,
                    bottom = Tokens.Spacing.s,
                ).semantics { heading() },
        style = NegaTheme.typography.sectionHeader,
        color = NegaTheme.colors.textSecondary,
    )
}

/** A centred message for a list with nothing in it. When there is nothing to say it draws nothing at all. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    @DrawableRes icon: Int? = null,
    action: (@Composable () -> Unit)? = null,
) {
    if (title.isBlank() && message.isNullOrBlank()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(Tokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
    ) {
        if (icon != null) {
            AppIcon(icon, size = Tokens.IconSize.hero, tint = NegaTheme.colors.textTertiary)
        }
        if (title.isNotBlank()) {
            NegaText(title, style = NegaTheme.typography.title1, textAlign = TextAlign.Center)
        }
        if (!message.isNullOrBlank()) {
            NegaText(
                message,
                style = NegaTheme.typography.body,
                color = NegaTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.padding(top = Tokens.Spacing.s))
            action()
        }
    }
}

/** The brand wordmark. It appears once per screen and is the same in every language. */
@Composable
fun NegaWordmark(modifier: Modifier = Modifier) {
    NegaText(
        text = stringResource(R.string.brand_wordmark),
        modifier = modifier,
        style = NegaTheme.typography.headline,
        color = NegaTheme.colors.textPrimary,
    )
}

/** The bar at the top of a screen: the brand or a back button on the leading side, a title, and action chips. */
@Composable
fun NavBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Metrics.navBarHeight)
                .padding(horizontal = Tokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading()
        if (title != null) {
            NegaText(
                text = title,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = Tokens.Spacing.s)
                        .semantics { heading() },
                style = NegaTheme.typography.title2,
                maxLines = 1,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

/** The back button for a screen's nav bar. */
@Composable
fun NavBarBack(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipIconButton(
        icon = AppIcons.ChevronLeft,
        description = stringResource(R.string.action_back),
        onClick = onClick,
        modifier = modifier,
        mirrorInRtl = true,
    )
}
