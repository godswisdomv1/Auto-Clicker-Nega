package com.zdmgold.autoclickernega.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zdmgold.autoclickernega.AppGraph
import com.zdmgold.autoclickernega.core.model.ServiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the step of first launch and the live state of the service. The rules themselves are in [OnboardingFlow]. */
class OnboardingViewModel(
    service: Flow<ServiceStatus>,
    private val saveAgreement: suspend () -> Unit,
) : ViewModel() {
    private val step = MutableStateFlow(OnboardingStep.Welcome)

    val state: StateFlow<OnboardingState> =
        combine(step, service) { s, status -> OnboardingState(s, status) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OnboardingState())

    fun onContinue() = step.update { OnboardingFlow.advance(it) }

    /** Remembers that the disclosure was agreed to, then moves on to the link to the system settings. */
    fun onAgree() {
        viewModelScope.launch {
            saveAgreement()
            step.update { OnboardingFlow.advance(it) }
        }
    }

    /** Handles the Back gesture inside onboarding. Returns false when the step does not use it. */
    fun onBack(): Boolean {
        val previous = OnboardingFlow.back(step.value) ?: return false
        step.value = previous
        return true
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(
            graph: AppGraph,
            saveAgreement: suspend () -> Unit,
        ) = viewModelFactory { initializer { OnboardingViewModel(graph.accessibility.status, saveAgreement) } }
    }
}
