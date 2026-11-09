package com.zdmgold.autoclickernega.core.port

import com.zdmgold.autoclickernega.core.model.Script
import kotlinx.coroutines.flow.StateFlow

/**
 * The script being edited right now. The floating panel writes into it as the user places markers on the live
 * screen, and the editors read it back, so work done over another app is not lost.
 */
interface DraftStore {
    val draft: StateFlow<Script?>

    fun begin(script: Script)

    fun update(transform: (Script) -> Script)

    fun clear()
}
