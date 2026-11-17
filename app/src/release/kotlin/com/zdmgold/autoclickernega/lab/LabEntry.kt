package com.zdmgold.autoclickernega.lab

import androidx.compose.runtime.Composable
import com.zdmgold.autoclickernega.AppGraph

/** Release builds contain no Detection lab. This stub keeps Home and navigation identical in both builds. */
object LabEntry {
    val available: Boolean get() = false

    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun Screen(
        graph: AppGraph,
        onBack: () -> Unit,
    ) = Unit
}
