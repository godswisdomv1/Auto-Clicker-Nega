package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Comparison
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.TimerSince
import com.zdmgold.autoclickernega.ui.icons.AppIcon
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens
import java.text.NumberFormat
import java.util.Locale

/** A readable summary of one condition with its icon, for the event list. */
@Composable
fun ConditionChip(
    condition: Condition,
    modifier: Modifier = Modifier,
) {
    val (icon, text) =
        when (condition) {
            is Condition.Image -> {
                AppIcons.ImageTarget to
                    stringResource(
                        R.string.condition_image,
                        String.format(currentLocale(), "%.2f", condition.confidence),
                    )
            }

            is Condition.Text -> {
                AppIcons.TextTarget to stringResource(R.string.condition_text, condition.text)
            }

            is Condition.Color -> {
                AppIcons.ColorTarget to
                    stringResource(
                        R.string.condition_color,
                        String.format(
                            Locale.ROOT,
                            "#%06X",
                            condition.rgb and RGB_MASK,
                        ),
                    )
            }

            is Condition.Number -> {
                AppIcons.Counter to
                    stringResource(
                        R.string.condition_number,
                        condition.cmp.symbol,
                        NumberFormat.getInstance(currentLocale()).format(condition.value),
                    )
            }

            is Condition.Timer -> {
                AppIcons.Timer to
                    stringResource(
                        R.string.condition_timer,
                        condition.cmp.symbol,
                        timerValue(condition.since, condition.cmp, condition.ms),
                    )
            }

            is Condition.Counter -> {
                AppIcons.Counter to
                    stringResource(R.string.condition_counter, condition.id, condition.cmp.symbol, condition.value)
            }
        }
    NegaChip(
        text = if (condition.negate) stringResource(R.string.condition_not, text) else text,
        modifier = modifier,
        icon = icon,
    )
}

private fun timerValue(
    since: TimerSince,
    cmp: Comparison,
    ms: Long,
): String {
    val seconds = NumberFormat.getInstance().format(ms / MS_PER_SECOND.toDouble())
    return if (since is TimerSince.EventFired) "$seconds s (${since.eventId}, ${cmp.symbol})" else "$seconds s"
}

/** A banner offering to undo something just done, such as a deletion. The caller decides how long it stays. */
@Composable
fun UndoBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Tokens.Radius.card)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NegaTheme.colors.bgSecondary, shape)
                .border(Tokens.Stroke.hairline, NegaTheme.colors.borderSubtle, shape)
                .padding(start = Tokens.Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NegaText(message, modifier = Modifier.weight(1f), style = NegaTheme.typography.callout, maxLines = 2)
        NegaText(
            text = actionLabel,
            modifier =
                Modifier
                    .heightIn(min = Tokens.touchTarget)
                    .pressable(onClick = onAction)
                    .padding(horizontal = Tokens.Spacing.m, vertical = Tokens.Spacing.s),
            style = NegaTheme.typography.title2,
            color = NegaTheme.colors.accentText,
        )
    }
}

/** The full-screen fallback for a failure the app cannot recover from, with a retry and a report. */
@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.error_title),
    message: String = stringResource(R.string.error_message),
    onRetry: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Tokens.Spacing.l), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m),
        ) {
            AppIcon(AppIcons.Warning, size = Tokens.IconSize.hero, tint = NegaTheme.colors.warning)
            NegaText(title, style = NegaTheme.typography.title1, textAlign = TextAlign.Center)
            NegaText(
                message,
                style = NegaTheme.typography.body,
                color = NegaTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) PrimaryButton(stringResource(R.string.action_retry), onRetry, Modifier.fillMaxWidth())
            if (onReport !=
                null
            ) {
                SecondaryButton(stringResource(R.string.action_report), onReport, Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * The banner ad slot. It takes no space at all until an ad has loaded, and collapses again when consent is absent,
 * Pro is active, the ad fails, the keyboard is up or the screen is not Home. The ad itself is passed in as [content].
 */
@Composable
fun AdSlot(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    if (!visible || keyboardUp) return
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = Tokens.Spacing.m)
                .heightIn(max = Metrics.adMaxHeight),
    ) {
        content()
    }
}

private const val RGB_MASK = 0xFFFFFF
private const val MS_PER_SECOND = 1000
