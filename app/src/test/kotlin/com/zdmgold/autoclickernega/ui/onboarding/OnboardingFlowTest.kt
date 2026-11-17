package com.zdmgold.autoclickernega.ui.onboarding

import com.zdmgold.autoclickernega.core.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun theStepsGoInOrderAndTheLastOneStays() {
        assertEquals(OnboardingStep.Disclosure, OnboardingFlow.advance(OnboardingStep.Welcome))
        assertEquals(OnboardingStep.EnableService, OnboardingFlow.advance(OnboardingStep.Disclosure))
        assertEquals(OnboardingStep.EnableService, OnboardingFlow.advance(OnboardingStep.EnableService))
    }

    @Test
    fun onlyTheDisclosureCanBeLeftWithBack() {
        assertEquals(OnboardingStep.Welcome, OnboardingFlow.back(OnboardingStep.Disclosure))
        assertNull(OnboardingFlow.back(OnboardingStep.Welcome))
        assertNull(OnboardingFlow.back(OnboardingStep.EnableService))
    }

    @Test
    fun theLinkToSettingsComesOnlyAfterTheDisclosure() {
        assertEquals(OnboardingStep.Welcome, OnboardingState().step)
        val order =
            generateSequence(OnboardingStep.Welcome) {
                OnboardingFlow.advance(it).takeIf { next ->
                    next != it
                }
            }.toList()
        assertEquals(listOf(OnboardingStep.Welcome, OnboardingStep.Disclosure, OnboardingStep.EnableService), order)
    }

    @Test
    fun onboardingIsDoneOnlyWhenTheServiceIsReadyOnTheLastStep() {
        assertFalse(OnboardingState(OnboardingStep.EnableService, ServiceStatus.Off).done)
        assertFalse(OnboardingState(OnboardingStep.EnableService, ServiceStatus.Connecting).done)
        assertTrue(OnboardingState(OnboardingStep.EnableService, ServiceStatus.Ready).done)
        assertFalse(OnboardingState(OnboardingStep.Disclosure, ServiceStatus.Ready).done)
    }
}
