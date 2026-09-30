package org.claudroide.app

import org.claudroide.app.feature.onboarding.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingTest {

    @Test
    fun onboardingSteps_hasExpectedOrderAndCount() {
        val steps = OnboardingStep.entries
        assertEquals(4, steps.size)
        assertEquals(OnboardingStep.WELCOME, steps[0])
        assertEquals(OnboardingStep.LANGUAGE_THEME, steps[1])
        assertEquals(OnboardingStep.PROVIDER_EXPLANATION, steps[2])
        assertEquals(OnboardingStep.FIRST_PROJECT, steps[3])
    }
}
