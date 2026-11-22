package com.zdmgold.autoclickernega.ui.home

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.core.port.ScriptSummary

enum class HomeSort { Recent, Name }

/** A one-off notice at the top of Home. */
sealed interface HomeMessage {
    data object ServiceNeeded : HomeMessage

    data class Imported(
        val name: String,
    ) : HomeMessage

    data class Failed(
        val error: AppError,
    ) : HomeMessage
}

/** What Home shows. */
data class HomeUiState(
    val loaded: Boolean = false,
    val scripts: List<ScriptSummary> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val sort: HomeSort = HomeSort.Recent,
    val engine: EngineState = EngineState.ServiceLost,
    val panelVisible: Boolean = false,
    val message: HomeMessage? = null,
    val panelScriptSteps: Int = 0,
    val deletedName: String? = null,
) {
    /** Search and sort appear once the library is long enough to need them. */
    val showSearch: Boolean get() = HomeModel.showSearch(totalCount)

    /** True while a run, a countdown or a Start in X wait is in progress. */
    val runActive: Boolean get() = HomeModel.isActive(engine)
}

/** The list rules of Home as plain functions. */
object HomeModel {
    /** Search and sort show above this many scripts. */
    const val SEARCH_THRESHOLD = 8

    fun showSearch(count: Int): Boolean = count > SEARCH_THRESHOLD

    fun isActive(state: EngineState): Boolean =
        when (state) {
            is EngineState.Waiting,
            is EngineState.Countdown,
            is EngineState.Running,
            is EngineState.Paused,
            -> true

            is EngineState.Blocked, EngineState.Idle, EngineState.ServiceLost -> false
        }

    /** The scripts to show: filtered by name, then ordered. With no search the whole library is shown. */
    fun visible(
        all: List<ScriptSummary>,
        query: String,
        sort: HomeSort,
    ): List<ScriptSummary> {
        val needle = query.trim()
        val matching = if (needle.isEmpty()) all else all.filter { it.name.contains(needle, ignoreCase = true) }
        return when (sort) {
            HomeSort.Recent -> {
                matching.sortedWith(
                    compareByDescending<ScriptSummary> {
                        it.updatedAtMs
                    }.thenBy { it.name.lowercase() },
                )
            }

            HomeSort.Name -> {
                matching.sortedWith(
                    compareBy<ScriptSummary> {
                        it.name.lowercase()
                    }.thenByDescending { it.updatedAtMs },
                )
            }
        }
    }
}
