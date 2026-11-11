package com.zdmgold.autoclickernega.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfidenceScaleTest {
    @Test
    fun theEndsOfTheSliderAreTheLimits() {
        assertEquals(0.60f, ConfidenceScale.fromFraction(0f), 0f)
        assertEquals(0.98f, ConfidenceScale.fromFraction(1f), 0f)
        assertEquals(0f, ConfidenceScale.toFraction(0.60f), 0f)
        assertEquals(1f, ConfidenceScale.toFraction(0.98f), 0f)
    }

    @Test
    fun valuesOutsideTheRangeAreClamped() {
        assertEquals(0.60f, ConfidenceScale.clamp(0.1f), 0f)
        assertEquals(0.98f, ConfidenceScale.clamp(2f), 0f)
        assertEquals(0.60f, ConfidenceScale.fromFraction(-3f), 0f)
        assertEquals(0.98f, ConfidenceScale.fromFraction(9f), 0f)
    }

    @Test
    fun valuesAreRoundedToTwoDecimals() {
        assertEquals(0.85f, ConfidenceScale.clamp(0.8499f), 0f)
        assertEquals(0.86f, ConfidenceScale.clamp(0.8551f), 0f)
        assertEquals(0.79f, ConfidenceScale.fromFraction(0.5f), 0f)
    }

    @Test
    fun theDefaultSitsInTheBalancedBand() {
        assertEquals(ConfidenceScale.Hint.Balanced, ConfidenceScale.hint(0.85f))
        assertEquals(ConfidenceScale.Hint.Loose, ConfidenceScale.hint(0.60f))
        assertEquals(ConfidenceScale.Hint.Loose, ConfidenceScale.hint(0.749f))
        assertEquals(ConfidenceScale.Hint.Balanced, ConfidenceScale.hint(0.75f))
        assertEquals(ConfidenceScale.Hint.Strict, ConfidenceScale.hint(0.90f))
        assertEquals(ConfidenceScale.Hint.Strict, ConfidenceScale.hint(0.98f))
    }

    @Test
    fun mappingBackAndForthIsStable() {
        for (hundredths in 60..98) {
            val v = hundredths / 100f
            assertEquals(v, ConfidenceScale.fromFraction(ConfidenceScale.toFraction(v)), 0.0001f)
        }
    }
}
