package com.zdmgold.autoclickernega.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStackTest {
    @Test
    fun theFirstLaunchStartsOnOnboardingAndLaterLaunchesOnHome() {
        assertEquals(listOf(Route.Onboarding), BackStack.start(onboardingDone = false).items)
        assertEquals(listOf(Route.Home), BackStack.start(onboardingDone = true).items)
    }

    @Test
    fun anEmptyStackHasNothingOnTop() {
        assertNull(BackStack().current)
        assertFalse(BackStack().canPop)
    }

    @Test
    fun pushingAddsOnTopAndPoppingGoesBack() {
        val s = BackStack.start(true).push(Route.Settings).push(Route.About)
        assertEquals(Route.About, s.current)
        assertEquals(NavDirection.Push, s.direction)
        val back = s.pop()
        assertEquals(Route.Settings, back.current)
        assertEquals(NavDirection.Pop, back.direction)
    }

    @Test
    fun openingTheScreenAlreadyOnTopDoesNothing() {
        val s = BackStack.start(true).push(Route.Settings)
        assertSame(s, s.push(Route.Settings))
        assertEquals(2, s.push(Route.Settings).items.size)
    }

    @Test
    fun theRootCannotBePopped() {
        val s = BackStack.start(true)
        assertFalse(s.canPop)
        assertSame(s, s.pop())
    }

    @Test
    fun aScreenCanBeOpenedAgainOnceSomethingElseIsOnTop() {
        val s =
            BackStack
                .start(true)
                .push(Route.Settings)
                .push(Route.Help)
                .push(Route.Settings)
        assertEquals(listOf(Route.Home, Route.Settings, Route.Help, Route.Settings), s.items)
    }

    @Test
    fun finishingOnboardingReplacesTheStackSoBackNeverReturnsToIt() {
        val s = BackStack.start(false).reset(Route.Home)
        assertEquals(listOf(Route.Home), s.items)
        assertEquals(NavDirection.Reset, s.direction)
        assertFalse(s.canPop)
    }

    @Test
    fun editorsForDifferentScriptsAreDifferentScreens() {
        val s = BackStack.start(true).push(Route.Editor("a")).push(Route.Editor("b"))
        assertEquals(3, s.items.size)
        assertEquals(Route.Editor("b"), s.current)
        assertTrue(Route.Editor(null) != Route.Editor("a"))
    }
}
