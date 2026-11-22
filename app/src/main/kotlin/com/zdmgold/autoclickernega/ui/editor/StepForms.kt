package com.zdmgold.autoclickernega.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Stroke
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

private const val POINT_STEP_PX = 100

/** The fields of one step. [key] tells the number fields to start again when another step is shown. */
@Composable
fun StepForm(
    step: Action,
    key: Any,
    onChange: (Action) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        when (step) {
            is Action.Tap -> {
                TapForm(step, key, onChange)
            }

            is Action.Swipe -> {
                SwipeForm(step, key, onChange)
            }

            is Action.Curve -> {
                CurveForm(step, key, onChange)
            }

            is Action.Multi -> {
                MultiForm(step, key, onChange)
            }

            is Action.Wait -> {
                NumberField(stringResource(R.string.field_wait), step.ms, {
                    onChange(step.copy(ms = it))
                }, resetKey = key)
            }

            is Action.CounterOp, is Action.RunScript, Action.Stop -> {
                Unit
            }
        }
    }
}

@Composable
private fun TwoFields(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

@Composable
private fun PointFields(
    labelX: String,
    labelY: String,
    point: Point,
    key: Any,
    onChange: (Point) -> Unit,
) {
    TwoFields(
        { NumberField(labelX, point.x, { onChange(point.copy(x = it)) }, it, key) },
        { NumberField(labelY, point.y, { onChange(point.copy(y = it)) }, it, key) },
    )
}

@Composable
private fun TimingFields(
    durationMs: Long,
    intervalMs: Long,
    key: Any,
    onChange: (duration: Long, interval: Long) -> Unit,
) {
    TwoFields(
        { NumberField(stringResource(R.string.field_duration), durationMs, { onChange(it, intervalMs) }, it, key) },
        { NumberField(stringResource(R.string.field_interval), intervalMs, { onChange(durationMs, it) }, it, key) },
    )
}

@Composable
private fun TapForm(
    step: Action.Tap,
    key: Any,
    onChange: (Action) -> Unit,
) {
    val at = step.at
    if (at is Target.At) {
        PointFields(stringResource(R.string.field_x), stringResource(R.string.field_y), at.p, key) {
            onChange(step.copy(at = Target.At(it)))
        }
    }
    TwoFields(
        {
            NumberField(
                stringResource(R.string.field_hold),
                step.holdMs,
                { v -> onChange(step.copy(holdMs = v)) },
                it,
                key,
            )
        },
        {
            NumberField(
                stringResource(R.string.field_interval),
                step.intervalMs,
                { v -> onChange(step.copy(intervalMs = v)) },
                it,
                key,
            )
        },
    )
    TwoFields(
        {
            NumberField(
                stringResource(R.string.field_jitter_px),
                step.jitterPx,
                { v -> onChange(step.copy(jitterPx = v)) },
                it,
                key,
            )
        },
        {
            NumberField(
                stringResource(R.string.field_timing_jitter),
                step.timingJitterMs,
                { v -> onChange(step.copy(timingJitterMs = v)) },
                it,
                key,
            )
        },
    )
}

@Composable
private fun SwipeForm(
    step: Action.Swipe,
    key: Any,
    onChange: (Action) -> Unit,
) {
    PointFields(stringResource(R.string.field_from_x), stringResource(R.string.field_from_y), step.from, key) {
        onChange(step.copy(from = it))
    }
    PointFields(stringResource(R.string.field_to_x), stringResource(R.string.field_to_y), step.to, key) {
        onChange(step.copy(to = it))
    }
    TimingFields(step.durationMs, step.intervalMs, key) { d, i -> onChange(step.copy(durationMs = d, intervalMs = i)) }
    NumberField(stringResource(R.string.field_timing_jitter), step.timingJitterMs, {
        onChange(step.copy(timingJitterMs = it))
    }, resetKey = key)
}

@Composable
private fun CurveForm(
    step: Action.Curve,
    key: Any,
    onChange: (Action) -> Unit,
) {
    step.points.forEachIndexed { i, point ->
        NegaText(
            stringResource(R.string.field_point, i + 1),
            style = NegaTheme.typography.footnote,
            color = NegaTheme.colors.textSecondary,
        )
        PointFields(stringResource(R.string.field_x), stringResource(R.string.field_y), point, key) { moved ->
            onChange(step.copy(points = step.points.toMutableList().also { it[i] = moved }))
        }
    }
    CountButtons(
        count = step.points.size,
        range = Limits.MIN_CURVE_POINTS..Limits.MAX_CURVE_POINTS,
        onAdd = {
            val last = step.points.last()
            onChange(step.copy(points = step.points + Point(last.x, (last.y - POINT_STEP_PX).coerceAtLeast(0))))
        },
        onRemove = { onChange(step.copy(points = step.points.dropLast(1))) },
    )
    TimingFields(step.durationMs, step.intervalMs, key) { d, i -> onChange(step.copy(durationMs = d, intervalMs = i)) }
}

@Composable
private fun MultiForm(
    step: Action.Multi,
    key: Any,
    onChange: (Action) -> Unit,
) {
    step.strokes.forEachIndexed { i, stroke ->
        NegaText(
            stringResource(R.string.field_finger, i + 1),
            style = NegaTheme.typography.footnote,
            color = NegaTheme.colors.textSecondary,
        )
        PointFields(
            stringResource(R.string.field_from_x),
            stringResource(R.string.field_from_y),
            stroke.from,
            key,
        ) { moved ->
            onChange(step.copy(strokes = step.strokes.toMutableList().also { it[i] = stroke.copy(from = moved) }))
        }
        PointFields(stringResource(R.string.field_to_x), stringResource(R.string.field_to_y), stroke.to, key) { moved ->
            onChange(step.copy(strokes = step.strokes.toMutableList().also { it[i] = stroke.copy(to = moved) }))
        }
    }
    CountButtons(
        count = step.strokes.size,
        range = Limits.MIN_MULTI_STROKES..Limits.MAX_MULTI_STROKES,
        onAdd = {
            val last = step.strokes.last()
            val shifted =
                Stroke(
                    Point(last.from.x, last.from.y + POINT_STEP_PX),
                    Point(
                        last.to.x,
                        last.to.y + POINT_STEP_PX,
                    ),
                )
            onChange(step.copy(strokes = step.strokes + shifted))
        },
        onRemove = { onChange(step.copy(strokes = step.strokes.dropLast(1))) },
    )
    TimingFields(step.durationMs, step.intervalMs, key) { d, i -> onChange(step.copy(durationMs = d, intervalMs = i)) }
}

@Composable
private fun CountButtons(
    count: Int,
    range: IntRange,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        SecondaryButton(
            stringResource(R.string.action_add_point),
            onAdd,
            Modifier.weight(1f),
            enabled =
                count < range.last,
        )
        SecondaryButton(
            stringResource(R.string.action_remove_point),
            onRemove,
            Modifier.weight(1f),
            enabled =
                count > range.first,
        )
    }
}
