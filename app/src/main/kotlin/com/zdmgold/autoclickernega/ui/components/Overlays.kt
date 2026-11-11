package com.zdmgold.autoclickernega.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.zdmgold.autoclickernega.ui.theme.Motion
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** Taps on the scrim close whatever is above it. No indication, since the scrim is not a button. */
@Composable
private fun Scrim(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val duration = Motion.duration(Motion.SHEET_MS, NegaTheme.reduceMotion)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(duration, easing = Motion.easeOut)),
        exit = fadeOut(tween(duration, easing = Motion.easeOut)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(NegaTheme.colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
        )
    }
}

/**
 * A bottom sheet with a scrim, 20 dp top corners and a slide of 400 ms. Place it inside the screen's root box. The
 * system back gesture closes it. With reduced motion it simply appears.
 */
@Composable
fun NegaSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val duration = Motion.duration(Motion.SHEET_MS, NegaTheme.reduceMotion)
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Scrim(visible, onDismiss)
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(duration, easing = Motion.easeOut)) { it },
            exit = slideOutVertically(tween(duration, easing = Motion.easeOut)) { it },
        ) {
            val shape = RoundedCornerShape(topStart = Tokens.Radius.sheet, topEnd = Tokens.Radius.sheet)
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(NegaTheme.colors.bgSecondary, shape)
                        .border(Tokens.Stroke.hairline, NegaTheme.colors.borderSubtle, shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(Tokens.Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(Metrics.sheetHandleWidth, Metrics.sheetHandleHeight)
                        .background(
                            NegaTheme.colors.textTertiary,
                            RoundedCornerShape(Tokens.Radius.pill(Metrics.sheetHandleHeight)),
                        ),
                )
                content()
            }
        }
    }
}

/** A centred dialog with a scrim, a message and one or two buttons. Place it inside the screen's root box. */
@Composable
fun NegaDialog(
    visible: Boolean,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissLabel: String? = null,
    destructive: Boolean = false,
) {
    val duration = Motion.duration(Motion.SHEET_MS, NegaTheme.reduceMotion)
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Scrim(visible, onDismiss)
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(duration, easing = Motion.easeOut)),
            exit = fadeOut(tween(duration, easing = Motion.easeOut)),
        ) {
            val shape = RoundedCornerShape(Tokens.Radius.dialog)
            Column(
                modifier =
                    modifier
                        .padding(Tokens.Spacing.l)
                        .widthIn(max = Metrics.dialogMaxWidth)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(NegaTheme.colors.bgSecondary, shape)
                        .border(Tokens.Stroke.hairline, NegaTheme.colors.borderSubtle, shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(Tokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
            ) {
                NegaText(title, style = NegaTheme.typography.title1)
                if (message != null) {
                    NegaText(message, style = NegaTheme.typography.body, color = NegaTheme.colors.textSecondary)
                }
                Spacer(Modifier.height(Tokens.Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
                    if (dismissLabel != null) {
                        SecondaryButton(dismissLabel, onDismiss, Modifier.weight(1f))
                    }
                    if (destructive) {
                        SecondaryButton(confirmLabel, onConfirm, Modifier.weight(1f), destructive = true)
                    } else {
                        PrimaryButton(confirmLabel, onConfirm, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
