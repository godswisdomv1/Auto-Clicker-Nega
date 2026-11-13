package com.zdmgold.autoclickernega.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.ui.nav.BackStack
import com.zdmgold.autoclickernega.ui.nav.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The state shared by every screen, and the navigation. Each screen has its own view model for everything else. */
class AppViewModel(
    private val graph: AppGraph,
) : ViewModel() {
    val state: StateFlow<AppUiState> =
        combine(
            graph.settings.settings,
            graph.settings.entitlement,
            graph.settings.consent,
            graph.settings.onboardingDone,
            graph.accessibility.status,
        ) { settings, entitlement, consent, onboarding, service ->
            AppUiState.from(settings, entitlement, consent, onboarding, service)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    private val mutableStack = MutableStateFlow(BackStack())
    val stack: StateFlow<BackStack> = mutableStack.asStateFlow()

    init {
        viewModelScope.launch {
            mutableStack.value = BackStack.start(graph.settings.onboardingDone.first())
        }
    }

    fun navigate(route: Route) = mutableStack.update { it.push(route) }

    fun back() = mutableStack.update { it.pop() }

    /** Onboarding is done. Home replaces it, so Back never returns to onboarding. */
    fun finishOnboarding() {
        viewModelScope.launch {
            graph.settings.setOnboardingDone(true)
            mutableStack.update { it.reset(Route.Home) }
        }
    }

    companion object {
        fun factory(graph: AppGraph) = viewModelFactory { initializer { AppViewModel(graph) } }
    }
}
