package com.zdmgold.autoclickernega.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.ui.components.EmptyState
import com.zdmgold.autoclickernega.ui.components.Metrics
import com.zdmgold.autoclickernega.ui.components.NavBar
import com.zdmgold.autoclickernega.ui.components.NavBarBack
import com.zdmgold.autoclickernega.ui.components.NegaCard
import com.zdmgold.autoclickernega.ui.components.NegaDialog
import com.zdmgold.autoclickernega.ui.components.NegaRow
import com.zdmgold.autoclickernega.ui.components.NegaSheet
import com.zdmgold.autoclickernega.ui.components.NegaText
import com.zdmgold.autoclickernega.ui.components.NegaTextField
import com.zdmgold.autoclickernega.ui.components.PrimaryButton
import com.zdmgold.autoclickernega.ui.components.SecondaryButton
import com.zdmgold.autoclickernega.ui.components.SectionHeader
import com.zdmgold.autoclickernega.ui.components.UndoBanner
import com.zdmgold.autoclickernega.ui.icons.AppIcons
import com.zdmgold.autoclickernega.ui.messageRes
import com.zdmgold.autoclickernega.ui.theme.NegaTheme
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * The Simple script editor: name, run settings, the list of steps with up and down controls, and a Save button that
 * stays at the bottom. There is no ad anywhere in the editor.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    screen: ScreenSpec,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    BackHandler { viewModel.onBack() }
    var openStep by remember { mutableStateOf<Int?>(null) }
    var choosingKind by remember { mutableStateOf(false) }
    val script = state.script

    Column(Modifier.fillMaxSize()) {
        NavBar(
            title = if (state.isNew) stringResource(R.string.editor_title_new) else script?.name.orEmpty(),
            leading = { NavBarBack(viewModel::onBack) },
        )
        if (script == null) return@Column
        if (script.body !is ScriptBody.Simple) {
            EmptyState(
                title = stringResource(R.string.pending_title),
                message = stringResource(R.string.editor_smart_unavailable),
                icon = AppIcons.Info,
            )
            return@Column
        }
        val steps = SimpleEditing.steps(script)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Tokens.Spacing.m, vertical = Tokens.Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        ) {
            state.message?.let { message ->
                item(key = "message") { EditorBanner(message, viewModel::dismissMessage) }
            }
            item(key = "name") {
                NegaTextField(
                    value = script.name,
                    onValueChange = viewModel::setName,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.editor_name),
                )
            }
            item(key = "run") { RunSettingsCard(script.run, viewModel::setRun) }
            item(key = "header") { SectionHeader(stringResource(R.string.editor_steps_header)) }
            if (steps.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = stringResource(R.string.editor_no_steps), icon = AppIcons.Tap)
                }
            }
            itemsIndexed(steps, key = { index, _ -> "step-$index" }) { index, step ->
                StepRow(
                    step = step,
                    first = index == 0,
                    last = index == steps.lastIndex,
                    onOpen = { openStep = index },
                    onMove = { delta -> viewModel.moveStep(index, delta) },
                )
            }
            item(key = "add") {
                SecondaryButton(
                    label = stringResource(R.string.editor_add_step),
                    onClick = { choosingKind = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = SimpleEditing.canAdd(script),
                    icon = AppIcons.Add,
                )
            }
            if (!SimpleEditing.canAdd(script)) {
                item(key = "full") {
                    NegaText(
                        stringResource(R.string.editor_full),
                        style = NegaTheme.typography.footnote,
                        color = NegaTheme.colors.textSecondary,
                    )
                }
            }
            if (!state.isNew) {
                item(key = "delete") {
                    SecondaryButton(
                        label = stringResource(R.string.action_delete),
                        onClick = viewModel::askDelete,
                        modifier = Modifier.fillMaxWidth(),
                        destructive = true,
                        icon = AppIcons.Trash,
                    )
                }
            }
        }
        SaveBar(state, onSave = viewModel::save)
    }

    KindSheet(
        visible = choosingKind,
        onPick = { kind ->
            choosingKind = false
            viewModel.addStep(SimpleEditing.blankStep(kind, screen, state.defaults))
        },
        onDismiss = { choosingKind = false },
    )
    val current = openStep?.let { index -> script?.let { SimpleEditing.steps(it).getOrNull(index) } }
    StepSheet(
        step = current,
        index = openStep ?: 0,
        onChange = { changed -> openStep?.let { viewModel.replaceStep(it, changed) } },
        onDuplicate = {
            openStep?.let { viewModel.duplicateStep(it) }
            openStep = null
        },
        onDelete = {
            openStep?.let { viewModel.removeStep(it) }
            openStep = null
        },
        onDismiss = { openStep = null },
    )
    NegaDialog(
        visible = state.confirmDiscard,
        title = stringResource(R.string.editor_discard_title),
        message = stringResource(R.string.editor_discard_message),
        confirmLabel = stringResource(R.string.action_discard),
        dismissLabel = stringResource(R.string.action_keep_editing),
        onConfirm = viewModel::discard,
        onDismiss = viewModel::keepEditing,
        destructive = true,
    )
    NegaDialog(
        visible = state.confirmDelete,
        title = stringResource(R.string.editor_delete_title),
        message = stringResource(R.string.editor_delete_message),
        confirmLabel = stringResource(R.string.action_delete),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = viewModel::confirmDelete,
        onDismiss = viewModel::cancelDelete,
        destructive = true,
    )
}

@Composable
private fun EditorBanner(
    message: EditorMessage,
    onDismiss: () -> Unit,
) {
    val text =
        when (message) {
            EditorMessage.Saved -> stringResource(R.string.msg_saved)
            is EditorMessage.Failed -> stringResource(message.error.messageRes())
        }
    UndoBanner(text, stringResource(R.string.action_ok), onDismiss)
}

/** The fixed bottom area: why Save is refused, if it is, and the Save button. */
@Composable
private fun SaveBar(
    state: EditorUiState,
    onSave: () -> Unit,
) {
    Column(Modifier.padding(Tokens.Spacing.m), verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s)) {
        state.problem?.let { problem ->
            val detail = (problem as? com.zdmgold.autoclickernega.core.error.AppError.MalformedImport)?.detail.orEmpty()
            NegaText(
                stringResource(
                    R.string.editor_problem,
                    if (detail.isBlank()) stringResource(problem.messageRes()) else detail,
                ),
                style = NegaTheme.typography.footnote,
                color = NegaTheme.colors.error,
            )
        }
        PrimaryButton(stringResource(R.string.action_save), onSave, Modifier.fillMaxWidth(), enabled = state.canSave)
    }
}

@Composable
private fun KindSheet(
    visible: Boolean,
    onPick: (StepKind) -> Unit,
    onDismiss: () -> Unit,
) {
    NegaSheet(visible = visible, onDismiss = onDismiss) {
        Column(
            Modifier.padding(horizontal = Tokens.Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
        ) {
            NegaText(stringResource(R.string.editor_add_step_title), style = NegaTheme.typography.title1)
            StepKind.entries.forEach { kind ->
                NegaCard(Modifier.fillMaxWidth()) {
                    NegaRow(
                        title = stringResource(kind.titleRes()),
                        subtitle = stringResource(kind.subtitleRes()),
                        icon = kind.iconRes(),
                        onClick = { onPick(kind) },
                    )
                }
            }
        }
    }
}

private fun StepKind.titleRes(): Int =
    when (this) {
        StepKind.Tap -> R.string.kind_tap
        StepKind.Swipe -> R.string.kind_swipe
        StepKind.Curve -> R.string.kind_curve
        StepKind.Multi -> R.string.kind_multi
        StepKind.Wait -> R.string.kind_wait
    }

private fun StepKind.subtitleRes(): Int =
    when (this) {
        StepKind.Tap -> R.string.kind_tap_sub
        StepKind.Swipe -> R.string.kind_swipe_sub
        StepKind.Curve -> R.string.kind_curve_sub
        StepKind.Multi -> R.string.kind_multi_sub
        StepKind.Wait -> R.string.kind_wait_sub
    }

/** The fields of one step, with Duplicate and Delete. Changes apply as they are typed. */
@Composable
private fun StepSheet(
    step: Action?,
    index: Int,
    onChange: (Action) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    NegaSheet(visible = step != null, onDismiss = onDismiss) {
        if (step != null) {
            Column(
                Modifier
                    .padding(horizontal = Tokens.Spacing.m)
                    .heightIn(max = Metrics.sheetMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.s),
            ) {
                NegaText(stepTitle(step), style = NegaTheme.typography.title1)
                StepForm(step, index, onChange)
                SecondaryButton(
                    stringResource(R.string.action_duplicate),
                    onDuplicate,
                    Modifier.fillMaxWidth(),
                    icon = AppIcons.Copy,
                )
                SecondaryButton(
                    stringResource(R.string.action_delete),
                    onDelete,
                    Modifier.fillMaxWidth(),
                    destructive = true,
                    icon = AppIcons.Trash,
                )
                PrimaryButton(stringResource(R.string.action_done), onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}
