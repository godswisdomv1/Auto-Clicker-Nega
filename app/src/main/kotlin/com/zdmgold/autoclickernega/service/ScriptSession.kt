package com.zdmgold.autoclickernega.service

import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.DraftStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The live draft shared by the floating panel and the editors. It lives in the process, so work done over another
 * app is still there when the app opens. The editors reach it through the [DraftStore] interface.
 */
object ScriptSession : DraftStore {
    private val state = MutableStateFlow<Script?>(null)

    override val draft: StateFlow<Script?> = state.asStateFlow()

    override fun begin(script: Script) {
        state.value = script
    }

    override fun update(transform: (Script) -> Script) {
        state.value = state.value?.let(transform)
    }

    override fun clear() {
        state.value = null
    }
}
