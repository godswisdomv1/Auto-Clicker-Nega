package com.zdmgold.autoclickernega.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.format.formatDuration
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.SearchRate
import com.zdmgold.autoclickernega.core.run.StartWaitPolicy
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.components.NegaSheet
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.Segmented
import com.zdmgold.autoclickernega.ui.components.Stepper
import com.zdmgold.autoclickernega.ui.components.currentLocale
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** Repeat, stop after, countdown, Start in X and search rate for one script. */
@Composable
fun RunSettingsCard(
    run: RunConfig,
    onChange: ((RunConfig) -> RunConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingWait by remember { mutableStateOf(false) }
    val locale = currentLocale()
    NegaCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Tokens.Spacing.m), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.m)) {
            LabelledStepper(
                label = stringResource(R.string.run_loops),
                value = run.loops,
                range = 0..RunSettingsEditing.MAX_LOOPS,
                format = { if (it == 0) "" else stringResource(R.string.run_loops_times, it) },
                zero = stringResource(R.string.run_loops_forever),
                onValue = { v -> onChange { RunSettingsEditing.loops(it, v) } },
            )
            LabelledStepper(
                label = stringResource(R.string.run_timer),
                value = RunSettingsEditing.timerMinutes(run),
                range = 0..RunSettingsEditing.MAX_TIMER_MINUTES,
                format = { stringResource(R.string.run_timer_minutes, it) },
                zero = stringResource(R.string.run_timer_off),
                onValue = { v -> onChange { RunSettingsEditing.timerMinutes(it, v) } },
            )
            LabelledStepper(
                label = stringResource(R.string.run_countdown_label),
                value = run.countdownSec,
                range = 0..com.zdmgold.autoclickernega.core.model.Limits.MAX_COUNTDOWN_SEC,
                format = { stringResource(R.string.run_countdown_seconds, it) },
                zero = stringResource(R.string.run_countdown_seconds, 0),
                onValue = { v -> onChange { RunSettingsEditing.countdown(it, v) } },
            )
            NegaRow(
                title = stringResource(R.string.run_start_in),
                subtitle =
                    if (run.startDelayMs ==
                        0L
                    ) {
                        stringResource(R.string.run_start_now)
                    } else {
                        formatDuration(run.startDelayMs, locale)
                    },
                icon = AppIcons.Timer,
                onClick = { pickingWait = true },
            )
            if (RunSettingsEditing.needsPlugIn(run)) {
                NegaText(
                    stringResource(R.string.run_plug_in),
                    style = NegaTheme.typography.footnote,
                    color = NegaTheme.colors.warning,
                )
            }
            NegaText(
                stringResource(R.string.run_search_rate),
                style = NegaTheme.typography.footnote,
                color = NegaTheme.colors.textSecondary,
            )
            Segmented(
                options =
                    listOf(
                        stringResource(R.string.rate_auto),
                        stringResource(R.string.rate_fast),
                        stringResource(R.string.rate_battery),
                    ),
                selectedIndex = run.searchRate.ordinal,
                onSelect = { i -> onChange { RunSettingsEditing.searchRate(it, SearchRate.entries[i]) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    StartWaitSheet(
        visible = pickingWait,
        run = run,
        onPick = { ms -> onChange { RunSettingsEditing.startWait(it, ms) } },
        onDismiss = { pickingWait = false },
    )
}

@Composable
private fun LabelledStepper(
    label: String,
    value: Int,
    range: IntRange,
    format: @Composable (Int) -> String,
    zero: String,
    onValue: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NegaText(label, style = NegaTheme.typography.title2, modifier = Modifier.weight(1f))
        val text = if (value == 0) zero else format(value)
        Stepper(value = value, onValueChange = onValue, range = range, format = { text })
    }
}

/** The presets from one minute to twelve hours, and a custom entry in minutes up to the cap. */
@Composable
private fun StartWaitSheet(
    visible: Boolean,
    run: RunConfig,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    NegaSheet(visible = visible, onDismiss = onDismiss) {
        Column(
            Modifier.padding(horizontal = Tokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        ) {
            NegaText(stringResource(R.string.run_start_in), style = NegaTheme.typography.title1)
            NegaRow(
                title = stringResource(R.string.run_start_now),
                icon = if (run.startDelayMs == 0L) AppIcons.Check else null,
                onClick = { onPick(0L) },
                showChevron = false,
            )
            StartWaitPolicy.presetsMs.forEach { ms ->
                NegaRow(
                    title = formatDuration(ms, locale),
                    icon = if (run.startDelayMs == ms) AppIcons.Check else null,
                    onClick = { onPick(ms) },
                    showChevron = false,
                )
            }
            LabelledStepper(
                label = stringResource(R.string.run_start_custom),
                value = RunSettingsEditing.startWaitMinutes(run),
                range = 0..RunSettingsEditing.MAX_CUSTOM_WAIT_MINUTES,
                format = { stringResource(R.string.run_timer_minutes, it) },
                zero = stringResource(R.string.run_start_now),
                onValue = { minutes -> onPick(minutes * MS_PER_MINUTE) },
            )
            if (RunSettingsEditing.needsPlugIn(run)) {
                NegaText(
                    stringResource(R.string.run_plug_in),
                    style = NegaTheme.typography.footnote,
                    color = NegaTheme.colors.warning,
                )
            }
        }
    }
}

private const val MS_PER_MINUTE = 60_000L
