package com.zdmgold.autoclickernega.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.ui.components.ChipIconButton
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.Tokens

private const val QUARTER_TURN = 90f

fun StepKind.iconRes(): Int =
    when (this) {
        StepKind.Tap -> AppIcons.Tap
        StepKind.Swipe -> AppIcons.Swipe
        StepKind.Curve -> AppIcons.Curve
        StepKind.Multi -> AppIcons.Multi
        StepKind.Wait -> AppIcons.Timer
    }

/** The one-line name of a step, such as "Tap at 540, 1200". */
@Composable
fun stepTitle(step: Action): String =
    when (step) {
        is Action.Tap -> {
            when (val at = step.at) {
                is Target.At -> stringResource(R.string.step_tap, at.p.x, at.p.y)
                is Target.AtMatch -> stringResource(R.string.step_tap_match)
            }
        }

        is Action.Swipe -> {
            stringResource(R.string.step_swipe, step.from.x, step.from.y, step.to.x, step.to.y)
        }

        is Action.Curve -> {
            stringResource(R.string.step_curve, step.points.size)
        }

        is Action.Multi -> {
            stringResource(R.string.step_multi, step.strokes.size)
        }

        is Action.Wait -> {
            stringResource(R.string.step_wait, spanText(step.ms))
        }

        is Action.CounterOp, is Action.RunScript, Action.Stop -> {
            ""
        }
    }

/** The second line of a step: how long it takes and how often it repeats. */
@Composable
fun stepSubtitle(step: Action): String? =
    when (step) {
        is Action.Tap -> stringResource(R.string.step_timing, step.holdMs, step.intervalMs)
        is Action.Swipe -> stringResource(R.string.step_timing, step.durationMs, step.intervalMs)
        is Action.Curve -> stringResource(R.string.step_timing, step.durationMs, step.intervalMs)
        is Action.Multi -> stringResource(R.string.step_timing, step.durationMs, step.intervalMs)
        is Action.Wait, is Action.CounterOp, is Action.RunScript, Action.Stop -> null
    }

/** One step in the list, with up and down controls. A tap on the row opens the step. */
@Composable
fun StepRow(
    step: Action,
    first: Boolean,
    last: Boolean,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = step.stepKind() ?: return
    NegaCard(modifier.fillMaxWidth()) {
        NegaRow(
            title = stepTitle(step),
            subtitle = stepSubtitle(step),
            icon = kind.iconRes(),
            onClick = onOpen,
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)) {
                    ChipIconButton(
                        AppIcons.ChevronLeft,
                        stringResource(R.string.action_move_up),
                        { onMove(-1) },
                        Modifier.rotate(QUARTER_TURN),
                        enabled = !first,
                    )
                    ChipIconButton(
                        AppIcons.ChevronRight,
                        stringResource(R.string.action_move_down),
                        { onMove(1) },
                        Modifier.rotate(QUARTER_TURN),
                        enabled = !last,
                    )
                }
            },
        )
    }
}
