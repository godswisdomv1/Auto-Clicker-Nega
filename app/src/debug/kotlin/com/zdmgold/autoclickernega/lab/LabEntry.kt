package com.zdmgold.autoclickernega.lab

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zdmgold.autoclickernega.AppGraph

/** The entry to the Detection lab. It exists in debug builds only. */
object LabEntry {
    val available: Boolean get() = true

    @Composable
    fun Screen(
        graph: AppGraph,
        onBack: () -> Unit,
    ) {
        val context = LocalContext.current
        val version =
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
                ?: "unknown"
        val model: DetectionLabViewModel = viewModel(factory = DetectionLabViewModel.factory(graph, version))
        DetectionLabScreen(model, onBack)
    }
}
