package com.zdmgold.autoclickernega.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.ScreenSpec
import com.zdmgold.autoclickernega.core.port.OverlayPort
import com.zdmgold.autoclickernega.core.port.RunRequest
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.ServiceBridge
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.LibraryImporter
import com.zdmgold.autoclickernega.data.ScriptFileGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home: the library, the state of the run, and the floating panel. The list rules are in [HomeModel]. A run
 * started here goes straight to the engine. The run gate joins this path with the ads batch.
 */
class HomeViewModel(
    private val scripts: ScriptStore,
    private val templates: TemplateStore,
    private val files: ScriptFileGateway,
    private val engine: ServiceBridge,
    private val overlay: OverlayPort,
    private val screen: () -> ScreenSpec,
) : ViewModel() {
    private data class Controls(
        val query: String = "",
        val sort: HomeSort = HomeSort.Recent,
        val message: HomeMessage? = null,
    )

    private val controls = MutableStateFlow(Controls())

    val state: StateFlow<HomeUiState> =
        combine(scripts.scripts, controls, engine.state, overlay.panelVisible) { all, c, engineState, panel ->
            HomeUiState(
                loaded = true,
                scripts = HomeModel.visible(all, c.query, c.sort),
                totalCount = all.size,
                query = c.query,
                sort = c.sort,
                engine = engineState,
                panelVisible = panel,
                message = c.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun setSort(sort: HomeSort) = controls.update { it.copy(sort = sort) }

    fun dismissMessage() = controls.update { it.copy(message = null) }

    /** Opens the floating panel. Returns true when it opened, so the screen can step aside and show the app below. */
    fun openPanel(): Boolean =
        when (val result = overlay.showPanel()) {
            is Outcome.Success -> true
            is Outcome.Failure -> fail(result.error).let { false }
        }

    fun closePanel() {
        val result = overlay.hidePanel()
        if (result is Outcome.Failure) fail(result.error)
    }

    fun run(id: String) {
        viewModelScope.launch {
            val script =
                when (val loaded = scripts.get(id)) {
                    is Outcome.Failure -> {
                        return@launch fail(loaded.error)
                    }

                    is Outcome.Success -> {
                        loaded.value
                            ?: return@launch fail(AppError.Storage("the script no longer exists"))
                    }
                }
            val started = engine.start(RunRequest(script, screen()))
            if (started is Outcome.Failure) fail(started.error)
        }
    }

    fun stop() {
        val result = engine.stop()
        if (result is Outcome.Failure) fail(result.error)
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val bytes =
                when (val read = files.read(uri)) {
                    is Outcome.Failure -> return@launch fail(read.error)
                    is Outcome.Success -> read.value
                }
            when (val imported = LibraryImporter.importInto(bytes, scripts, templates)) {
                is Outcome.Failure -> fail(imported.error)
                is Outcome.Success -> controls.update { it.copy(message = HomeMessage.Imported(imported.value.name)) }
            }
        }
    }

    private fun fail(error: AppError) {
        val message = if (error == AppError.ServiceNotEnabled) HomeMessage.ServiceNeeded else HomeMessage.Failed(error)
        controls.update { it.copy(message = message) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(
            graph: AppGraph,
            screen: () -> ScreenSpec,
        ) = viewModelFactory {
            initializer {
                HomeViewModel(graph.scripts, graph.templates, graph.files, graph.engine, graph.overlay, screen)
            }
        }
    }
}
