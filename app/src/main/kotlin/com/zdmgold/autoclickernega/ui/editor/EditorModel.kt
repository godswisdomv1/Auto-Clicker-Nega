package com.zdmgold.autoclickernega.ui.editor

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.AppSettings
import com.zdmgold.autoclickernega.core.model.Script

/** A one-off notice in the editor. */
sealed interface EditorMessage {
    data object Saved : EditorMessage

    data class Failed(
        val error: AppError,
    ) : EditorMessage
}

/** What the editor shows. [baseline] is the script as it was loaded or last saved, to tell whether anything changed. */
data class EditorUiState(
    val loaded: Boolean = false,
    val script: Script? = null,
    val baseline: Script? = null,
    val isNew: Boolean = true,
    val problem: AppError? = null,
    val message: EditorMessage? = null,
    val confirmDiscard: Boolean = false,
    val confirmDelete: Boolean = false,
    val finished: Boolean = false,
    val defaults: AppSettings = AppSettings(),
) {
    val dirty: Boolean get() = script != baseline

    val stepCount: Int get() = script?.let { SimpleEditing.steps(it).size } ?: 0

    /** Save needs a name, at least one step, and something to save: a change, or a script not stored yet. */
    val canSave: Boolean get() = script != null && script.name.isNotBlank() && stepCount > 0 && (dirty || isNew)

    /** Leaving would lose something when there are unsaved changes. */
    val needsDiscardCheck: Boolean get() = dirty
}
