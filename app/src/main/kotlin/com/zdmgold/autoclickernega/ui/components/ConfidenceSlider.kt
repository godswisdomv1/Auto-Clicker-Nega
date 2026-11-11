package com.zdmgold.autoclickernega.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens
import java.util.Locale
import kotlin.math.roundToInt

/** A slider from 0.60 to 0.98 that shows its value and says what a low or high setting means. */
@Composable
fun ConfidenceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = String.format(Locale.getDefault(), "%.2f", ConfidenceScale.clamp(value))
    val hint =
        when (ConfidenceScale.hint(value)) {
            ConfidenceScale.Hint.Loose -> R.string.confidence_hint_loose
            ConfidenceScale.Hint.Balanced -> R.string.confidence_hint_balanced
            ConfidenceScale.Hint.Strict -> R.string.confidence_hint_strict
        }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NegaText(
                text = stringResource(R.string.confidence_label),
                modifier = Modifier.weight(1f),
                style = NegaTheme.typography.title2,
            )
            NegaText(shown, style = NegaTheme.typography.title2, color = NegaTheme.colors.accentText)
        }
        NegaSlider(
            fraction = ConfidenceScale.toFraction(value),
            onFractionChange = { onValueChange(ConfidenceScale.fromFraction(it)) },
            description = stringResource(R.string.confidence_label),
            valueText = shown,
        )
        NegaText(stringResource(hint), style = NegaTheme.typography.footnote, color = NegaTheme.colors.textSecondary)
    }
}

/** The slider itself, from 0 to 1. A screen reader can adjust it, and it follows the layout direction. */
@Composable
internal fun NegaSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    description: String,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    val colors = NegaTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var width by remember { mutableIntStateOf(0) }

    fun at(x: Float): Float {
        if (width <= 0) return fraction
        val f = (x / width).coerceIn(0f, 1f)
        return if (rtl) 1f - f else f
    }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Tokens.touchTarget)
                .padding(horizontal = Metrics.sliderThumb / 2)
                .onSizeChanged { width = it.width }
                .pointerInput(width, rtl) { detectTapGestures { onFractionChange(at(it.x)) } }
                .pointerInput(width, rtl) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onFractionChange(at(change.position.x))
                    }
                }.semantics {
                    contentDescription = description
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { target ->
                        onFractionChange(target.coerceIn(0f, 1f))
                        true
                    }
                },
    ) {
        val track = Metrics.sliderTrack.toPx()
        val thumb = Metrics.sliderThumb.toPx() / 2
        val y = size.height / 2
        val x = (if (rtl) 1f - fraction else fraction) * size.width
        val radius = CornerRadius(track / 2, track / 2)
        drawRoundRect(colors.bgTertiary, Offset(0f, y - track / 2), Size(size.width, track), radius)
        val filledFrom = if (rtl) x else 0f
        val filledWidth = if (rtl) size.width - x else x
        drawRoundRect(colors.accent, Offset(filledFrom, y - track / 2), Size(filledWidth, track), radius)
        drawCircle(colors.bgSecondary, thumb, Offset(x, y))
        drawCircle(colors.accent, thumb, Offset(x, y), style = Stroke(width = Tokens.Stroke.icon.toPx()))
    }
}
