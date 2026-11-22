package com.zdmgold.autoclickernega.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.RunConfig
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.DraftStore
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.SettingsStore
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.ScriptValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Edits one Simple script: an existing one by id, the script placed with the floating panel, or a new empty one.
 * The editing rules are in [SimpleEditing] and [RunSettingsEditing]. Saving goes through the validator.
 */
class EditorViewModel(
    private val scripts: ScriptStore,
    private val drafts: DraftStore,
    private val settings: SettingsStore,
    private val deleted: MutableStateFlow<Script?>,
    private val screen: ScreenSpec,
    private val scriptId: String?,
    private val defaultName: String,
) : ViewModel() {
    private val mutable = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = mutable.asStateFlow()

    private var fromPanel = false

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val defaults = settings.settings.first()
        mutable.update { it.copy(defaults = defaults) }
        if (scriptId != null) {
            when (val loaded = scripts.get(scriptId)) {
                is Outcome.Failure -> {
                    mutable.update {
                        it.copy(
                            loaded = true,
                            message = EditorMessage.Failed(loaded.error),
                            finished = true,
                        )
                    }
                }

                is Outcome.Success -> {
                    val script = loaded.value
                    if (script == null) {
                        mutable.update { it.copy(loaded = true, finished = true) }
                    } else {
                        mutable.update { it.copy(loaded = true, script = script, baseline = script, isNew = false) }
                    }
                }
            }
            return
        }
        val panel = drafts.draft.value
        val script =
            if (panel != null && SimpleEditing.steps(panel).isNotEmpty()) {
                fromPanel = true
                panel.copy(name = panel.name.ifBlank { defaultName })
            } else {
                SimpleEditing.newScript(defaultName, screen, defaults)
            }
        mutable.update { it.copy(loaded = true, script = script, baseline = script, isNew = true) }
    }

    private fun edit(transform: (Script) -> Script) =
        mutable.update { state ->
            state.script?.let { state.copy(script = transform(it), problem = null, message = null) } ?: state
        }

    fun setName(name: String) = edit { it.copy(name = name) }

    fun setRun(transform: (RunConfig) -> RunConfig) = edit { it.copy(run = transform(it.run)) }

    fun addStep(step: Action) = edit { SimpleEditing.add(it, step) }

    fun replaceStep(
        index: Int,
        step: Action,
    ) = edit { SimpleEditing.replace(it, index, step) }

    fun removeStep(index: Int) = edit { SimpleEditing.remove(it, index) }

    fun moveStep(
        index: Int,
        delta: Int,
    ) = edit { SimpleEditing.move(it, index, delta) }

    fun duplicateStep(index: Int) = edit { SimpleEditing.duplicate(it, index) }

    fun dismissMessage() = mutable.update { it.copy(message = null) }

    fun save() {
        val current = mutable.value.script ?: return
        viewModelScope.launch {
            when (val checked = ScriptValidator.validate(current)) {
                is Outcome.Failure -> {
                    mutable.update { it.copy(problem = checked.error) }
                }

                is Outcome.Success -> {
                    when (val saved = scripts.save(checked.value)) {
                        is Outcome.Failure -> {
                            mutable.update { it.copy(message = EditorMessage.Failed(saved.error)) }
                        }

                        is Outcome.Success -> {
                            if (fromPanel) drafts.clear()
                            fromPanel = false
                            mutable.update {
                                it.copy(
                                    script = saved.value,
                                    baseline = saved.value,
                                    isNew = false,
                                    problem = null,
                                    message = EditorMessage.Saved,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Back: leaves at once when nothing changed, otherwise asks first. */
    fun onBack() =
        mutable.update { if (it.needsDiscardCheck) it.copy(confirmDiscard = true) else it.copy(finished = true) }

    fun keepEditing() = mutable.update { it.copy(confirmDiscard = false) }

    fun discard() = mutable.update { it.copy(confirmDiscard = false, finished = true) }

    fun askDelete() = mutable.update { it.copy(confirmDelete = true) }

    fun cancelDelete() = mutable.update { it.copy(confirmDelete = false) }

    /** Deletes the stored script and leaves a copy for Home to offer an undo. A script never stored is just dropped. */
    fun confirmDelete() {
        val script = mutable.value.script ?: return
        viewModelScope.launch {
            if (script.id.isBlank()) {
                mutable.update { it.copy(confirmDelete = false, finished = true) }
                return@launch
            }
            when (val result = scripts.delete(script.id)) {
                is Outcome.Failure -> {
                    mutable.update { it.copy(confirmDelete = false, message = EditorMessage.Failed(result.error)) }
                }

                is Outcome.Success -> {
                    deleted.value = result.value ?: script
                    mutable.update { it.copy(confirmDelete = false, finished = true) }
                }
            }
        }
    }

    companion object {
        fun factory(
            graph: AppGraph,
            screen: ScreenSpec,
            scriptId: String?,
            defaultName: String,
        ) = viewModelFactory {
            initializer {
                EditorViewModel(
                    graph.scripts,
                    graph.drafts,
                    graph.settings,
                    graph.deletedScript,
                    screen,
                    scriptId,
                    defaultName,
                )
            }
        }
    }
}
