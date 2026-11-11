package com.zdmgold.autoclickernega.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.Motion
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens
import kotlin.math.roundToInt

/** A filled text field with an optional label above it and an error line below. Focus shows a 1.5 dp accent border. */
@Composable
fun NegaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = NegaTheme.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val isError = errorText != null
    val shape = RoundedCornerShape(Tokens.Radius.input)
    val borderColor =
        when {
            isError -> colors.error
            focused -> colors.borderFocus
            else -> Color.Transparent
        }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
        if (label != null) NegaText(label, style = NegaTheme.typography.footnote, color = colors.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { if (errorText != null) error(errorText) },
            enabled = enabled,
            textStyle =
                NegaTheme.typography.body.copy(
                    color = if (enabled) colors.textPrimary else colors.textTertiary,
                ),
            cursorBrush = SolidColor(colors.accent),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            decorationBox = { inner ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Metrics.fieldMinHeight)
                            .clip(shape)
                            .background(colors.bgTertiary, shape)
                            .border(Metrics.focusBorder, borderColor, shape)
                            .padding(horizontal = Tokens.Spacing.m, vertical = Tokens.Spacing.s),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        NegaText(placeholder, style = NegaTheme.typography.body, color = colors.textTertiary)
                    }
                    inner()
                }
            },
        )
        if (errorText != null) NegaText(errorText, style = NegaTheme.typography.footnote, color = colors.error)
    }
}

/** A number with a minus and a plus button. The buttons disable at the ends of [range]. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    format: (Int) -> String = { it.toString() },
) {
    val colors = NegaTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            description = stringResource(R.string.stepper_decrease),
            enabled = value - step >= range.first,
            onClick = { onValueChange((value - step).coerceAtLeast(range.first)) },
        ) {
            Box(
                Modifier
                    .size(Metrics.minusWidth, Metrics.minusHeight)
                    .background(
                        if (value - step >= range.first) colors.textPrimary else colors.textTertiary,
                        RoundedCornerShape(Tokens.Radius.pill(Metrics.minusHeight)),
                    ),
            )
        }
        NegaText(
            text = format(value),
            modifier = Modifier.widthIn(min = Tokens.touchTarget).padding(horizontal = Tokens.Spacing.s),
            style = NegaTheme.typography.title2,
            textAlign = TextAlign.Center,
        )
        StepButton(
            description = stringResource(R.string.stepper_increase),
            enabled = value + step <= range.last,
            onClick = { onValueChange((value + step).coerceAtMost(range.last)) },
        ) {
            com.zdmgold.autoclickernega.ui.icons.AppIcon(
                icon = AppIcons.Add,
                size = Tokens.IconSize.row,
                tint = if (value + step <= range.last) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}

@Composable
private fun StepButton(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(Tokens.touchTarget)
                .pressable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Metrics.chipIconContainer)
                .background(NegaTheme.colors.bgTertiary, RoundedCornerShape(Tokens.Radius.chip)),
            contentAlignment = Alignment.Center,
        ) { glyph() }
    }
}

/** An iOS-style switch: a 51 by 31 dp track and a round knob that slides, honouring reduced motion and RTL. */
@Composable
fun NegaToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
) {
    val colors = NegaTheme.colors
    val duration = Motion.duration(Motion.PRESS_MS, NegaTheme.reduceMotion)
    val fraction by animateFloatAsState(
        if (checked) 1f else 0f,
        tween(duration, easing = Motion.easeInOut),
        label = "toggle",
    )
    val track by animateColorAsState(
        if (checked) colors.accent else colors.bgTertiary,
        tween(duration, easing = Motion.easeInOut),
        label = "track",
    )
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val travel =
        with(LocalDensity.current) {
            (Metrics.toggleWidth - Metrics.toggleKnob - Metrics.toggleInset * 2).toPx()
        }
    Box(
        modifier =
            modifier
                .size(Metrics.toggleWidth, Metrics.toggleHeight)
                .background(track, CircleShape)
                .border(Tokens.Stroke.hairline, colors.borderSubtle, CircleShape)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onValueChange = onCheckedChange,
                ).semantics { if (description != null) contentDescription = description }
                .padding(Metrics.toggleInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset((travel * fraction * direction).roundToInt(), 0) }
                .size(Metrics.toggleKnob)
                .background(colors.bgSecondary, CircleShape)
                .border(Tokens.Stroke.hairline, colors.textTertiary, CircleShape),
        )
    }
}

/** A row of exclusive options in a pill, like a segmented control. The chosen one is lifted onto a card. */
@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NegaTheme.colors
    val outer = RoundedCornerShape(Tokens.Radius.button)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(outer)
                .background(colors.bgTertiary, outer)
                .padding(Tokens.Spacing.xxs),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val inner = RoundedCornerShape(Tokens.Radius.button - Tokens.Spacing.xxs)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = Metrics.segmentHeight)
                        .clip(inner)
                        .then(
                            if (selected) {
                                Modifier
                                    .background(colors.bgSecondary, inner)
                                    .border(Tokens.Stroke.hairline, colors.borderSubtle, inner)
                            } else {
                                Modifier
                            },
                        ).selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onSelect(index) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                NegaText(
                    text = label,
                    style = NegaTheme.typography.callout,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
