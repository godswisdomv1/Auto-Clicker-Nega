package com.zdmgold.autoclickernega.platform.access

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityComponentsTest {
    private val mine = "com.zdmgold.autoclickernega/com.zdmgold.autoclickernega.service.NegaAccessibilityService"

    @Test
    fun findsTheServiceInTheList() {
        assertTrue(AccessibilityComponents.isEnabled(mine, mine))
        assertTrue(AccessibilityComponents.isEnabled("other.app/other.app.Svc:$mine:third/third.S", mine))
    }

    @Test
    fun aClassWrittenRelativeToItsPackageIsTheSameService() {
        val relative = "com.zdmgold.autoclickernega/.service.NegaAccessibilityService"
        assertTrue(AccessibilityComponents.isEnabled(relative, mine))
        assertTrue(AccessibilityComponents.isEnabled(mine, relative))
    }

    @Test
    fun caseDoesNotMatter() {
        assertTrue(AccessibilityComponents.isEnabled(mine.uppercase(), mine))
    }

    @Test
    fun theDebugBuildIsAnotherService() {
        val debug = "com.zdmgold.autoclickernega.debug/com.zdmgold.autoclickernega.service.NegaAccessibilityService"
        assertFalse(AccessibilityComponents.isEnabled(debug, mine))
        assertTrue(AccessibilityComponents.isEnabled(debug, debug))
    }

    @Test
    fun anEmptyOrMissingListMeansNotEnabled() {
        assertFalse(AccessibilityComponents.isEnabled(null, mine))
        assertFalse(AccessibilityComponents.isEnabled("", mine))
        assertFalse(AccessibilityComponents.isEnabled("   ", mine))
    }

    @Test
    fun otherServicesDoNotCount() {
        assertFalse(AccessibilityComponents.isEnabled("other.app/other.app.Svc", mine))
        assertFalse(
            AccessibilityComponents.isEnabled("com.zdmgold.autoclickernega/com.zdmgold.autoclickernega.Other", mine),
        )
    }
}
