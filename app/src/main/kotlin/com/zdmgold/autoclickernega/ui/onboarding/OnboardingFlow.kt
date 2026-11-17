package com.zdmgold.autoclickernega.ui.onboarding

import com.zdmgold.autoclickernega.core.model.ServiceStatus

/** The steps of first launch. Agreeing to the disclosure comes before the link to the system settings. */
enum class OnboardingStep { Welcome, Disclosure, EnableService }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val service: ServiceStatus = ServiceStatus.Off,
) {
    /** The service is on and connected, so nothing is left to do. Only the last step can be done. */
    val done: Boolean get() = step == OnboardingStep.EnableService && service == ServiceStatus.Ready
}

/** The rules of the onboarding steps as plain functions, so they are tested without any screen. */
object OnboardingFlow {
    fun advance(step: OnboardingStep): OnboardingStep =
        when (step) {
            OnboardingStep.Welcome -> OnboardingStep.Disclosure
            OnboardingStep.Disclosure -> OnboardingStep.EnableService
            OnboardingStep.EnableService -> OnboardingStep.EnableService
        }

    /**
     * Where the Back gesture goes from a step, or null when Back is not handled here. The disclosure can be left,
     * and once it has been agreed to there is no way back to it.
     */
    fun back(step: OnboardingStep): OnboardingStep? =
        when (step) {
            OnboardingStep.Disclosure -> OnboardingStep.Welcome
            OnboardingStep.Welcome, OnboardingStep.EnableService -> null
        }
}
