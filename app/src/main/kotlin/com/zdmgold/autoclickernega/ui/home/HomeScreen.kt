package com.zdmgold.autoclickernega.ui.home

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.format.formatDuration
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import com.zdmgold.autoclickernega.core.port.ScriptMode
import com.zdmgold.autoclickernega.core.port.ScriptSummary
import com.zdmgold.autoclickernega.lab.LabEntry
import com.zdmgold.autoclickernega.platform.access.SettingsIntents
import com.zdmgold.autoclickernega.ui.AppUiState
import com.zdmgold.autoclickernega.ui.Features
import com.zdmgold.autoclickernega.ui.components.ChipIconButton
import com.zdmgold.autoclickernega.ui.components.EmptyState
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.components.NegaTextField
import com.zdmgold.autoclickernega.ui.components.NegaWordmark
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.components.SectionHeader
import com.zdmgold.autoclickernega.ui.components.Segmented
import com.zdmgold.autoclickernega.ui.components.UndoBanner
import com.zdmgold.autoclickernega.ui.components.currentLocale
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.messageRes
import com.zdmgold.autoclickernega.ui.nav.Route
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * Home: the 'Nega' wordmark once, the status of the service, the floating panel, the script library and import.
 * Only the list scrolls. Entries for screens that are not built yet are left out, see [Features].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    app: AppUiState,
    onOpen: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importFrom(uri)
        }
    Column(Modifier.fillMaxSize()) {
        NavBar(
            leading = { NegaWordmark(Modifier.padding(start = Tokens.Spacing.s)) },
            actions = {
                if (Features.SETTINGS) {
                    ChipIconButton(
                        AppIcons.Settings,
                        stringResource(R.string.home_settings),
                        { onOpen(Route.Settings) },
                    )
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Tokens.Spacing.m, vertical = Tokens.Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        ) {
            state.message?.let { message ->
                item(key = "message") { MessageBanner(message, viewModel::dismissMessage) }
            }
            item(key = "status") { StatusCard(app, onOpen) }
            if (state.runActive) item(key = "run") { RunCard(state.engine, viewModel::stop) }
            item(key = "panel") {
                PanelCard(state.panelVisible) {
                    if (state.panelVisible) {
                        viewModel.closePanel()
                    } else if (viewModel.openPanel()) {
                        context.findActivity()?.moveTaskToBack(true)
                    }
                }
            }
            if (Features.QUICK_SCENARIO) {
                item(key = "quick") {
                    NegaCard(Modifier.fillMaxWidth()) {
                        NegaRow(
                            title = stringResource(R.string.home_quick_title),
                            subtitle = stringResource(R.string.home_quick_sub),
                            icon = AppIcons.Crosshair,
                            onClick = { onOpen(Route.QuickScenario) },
                        )
                    }
                }
            }
            if (LabEntry.available) {
                item(key = "lab") {
                    NegaCard(Modifier.fillMaxWidth()) {
                        NegaRow(
                            title = stringResource(R.string.home_lab_title),
                            subtitle = stringResource(R.string.home_lab_sub),
                            icon = AppIcons.Hud,
                            onClick = { onOpen(Route.Lab) },
                        )
                    }
                }
            }
            item(key = "header") { SectionHeader(stringResource(R.string.home_scripts_header)) }
            if (state.showSearch) item(key = "search") { SearchBar(state, viewModel) }
            if (state.scripts.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title =
                            stringResource(
                                if (state.totalCount ==
                                    0
                                ) {
                                    R.string.home_empty_title
                                } else {
                                    R.string.home_no_matches
                                },
                            ),
                        message = if (state.totalCount == 0) stringResource(R.string.home_empty_message) else null,
                        icon = AppIcons.Tap,
                    )
                }
            }
            items(state.scripts, key = { it.id }) { script ->
                ScriptRow(script, onOpen = { onOpen(Route.Editor(script.id)) }, onRun = { viewModel.run(script.id) })
            }
            item(key = "import") {
                NegaCard(Modifier.fillMaxWidth()) {
                    NegaRow(
                        title = stringResource(R.string.home_import_title),
                        subtitle = stringResource(R.string.home_import_sub),
                        icon = AppIcons.Import,
                        onClick = { picker.launch(arrayOf(ANY_FILE)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBanner(
    message: HomeMessage,
    onDismiss: () -> Unit,
) {
    val text =
        when (message) {
            HomeMessage.ServiceNeeded -> stringResource(R.string.msg_service_needed)
            is HomeMessage.Imported -> stringResource(R.string.msg_imported, message.name)
            is HomeMessage.Failed -> stringResource(message.error.messageRes())
        }
    UndoBanner(text, stringResource(R.string.action_ok), onDismiss)
}

@Composable
private fun StatusCard(
    app: AppUiState,
    onOpen: (Route) -> Unit,
) {
    val context = LocalContext.current
    val ready = app.service == ServiceStatus.Ready
    NegaCard(Modifier.fillMaxWidth()) {
        NegaRow(
            title = stringResource(R.string.home_status_title),
            subtitle =
                stringResource(
                    when (app.service) {
                        ServiceStatus.Ready -> R.string.status_service_ready
                        ServiceStatus.Off -> R.string.status_service_off
                        ServiceStatus.Connecting -> R.string.status_service_connecting
                    },
                ),
            icon = if (ready) AppIcons.Check else AppIcons.Warning,
        )
        if (!ready) {
            val label =
                if (app.service ==
                    ServiceStatus.Off
                ) {
                    R.string.home_turn_on
                } else {
                    R.string.action_open_accessibility
                }
            PrimaryButton(
                label = stringResource(label),
                onClick = {
                    if (app.service == ServiceStatus.Off && !app.onboardingDone) {
                        onOpen(Route.Onboarding)
                    } else {
                        SettingsIntents.open(context, SettingsIntents.accessibility())
                    }
                },
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = Tokens.Spacing.m,
                        end = Tokens.Spacing.m,
                        bottom = Tokens.Spacing.m,
                    ),
            )
        }
    }
}

@Composable
private fun RunCard(
    engine: EngineState,
    onStop: () -> Unit,
) {
    val locale = currentLocale()
    val title =
        when (engine) {
            is EngineState.Waiting -> stringResource(R.string.run_waiting, formatDuration(engine.remainingMs, locale))
            is EngineState.Countdown -> stringResource(R.string.run_countdown, engine.secondsLeft)
            is EngineState.Paused -> stringResource(R.string.run_paused)
            else -> stringResource(R.string.run_running)
        }
    NegaCard(Modifier.fillMaxWidth()) {
        NegaRow(
            title = title,
            icon = AppIcons.Play,
            trailing = { SecondaryButton(stringResource(R.string.run_stop), onStop, destructive = true) },
        )
    }
}

@Composable
private fun PanelCard(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    NegaCard(Modifier.fillMaxWidth()) {
        NegaRow(
            title = stringResource(if (visible) R.string.home_panel_close else R.string.home_panel_open),
            subtitle = stringResource(R.string.home_panel_sub),
            icon = AppIcons.Hud,
            onClick = onToggle,
        )
    }
}

@Composable
private fun SearchBar(
    state: HomeUiState,
    viewModel: HomeViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        NegaTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.home_search_hint),
        )
        Segmented(
            options = listOf(stringResource(R.string.home_sort_recent), stringResource(R.string.home_sort_name)),
            selectedIndex = state.sort.ordinal,
            onSelect = { viewModel.setSort(HomeSort.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScriptRow(
    script: ScriptSummary,
    onOpen: () -> Unit,
    onRun: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val subtitle =
        when (script.mode) {
            ScriptMode.Simple -> resources.getQuantityString(R.plurals.script_steps, script.itemCount, script.itemCount)
            ScriptMode.Smart -> resources.getQuantityString(R.plurals.script_events, script.itemCount, script.itemCount)
        }
    NegaCard(Modifier.fillMaxWidth()) {
        NegaRow(
            title = script.name,
            subtitle = subtitle,
            icon = if (script.mode == ScriptMode.Smart) AppIcons.Branch else AppIcons.Tap,
            onClick = if (Features.EDITOR) onOpen else null,
            trailing = {
                ChipIconButton(AppIcons.Play, stringResource(R.string.script_run, script.name), onRun)
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val ANY_FILE = "*/*"
