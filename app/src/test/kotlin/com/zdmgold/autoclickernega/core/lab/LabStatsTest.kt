package com.zdmgold.autoclickernega.core.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabStatsTest {
    @Test
    fun medianOfAnOddAndAnEvenList() {
        assertEquals(3L, LabStats.median(listOf(5, 1, 3)))
        assertEquals(2L, LabStats.median(listOf(4, 1, 2, 3)))
    }

    @Test
    fun anEmptyListGivesZero() {
        assertEquals(0L, LabStats.median(emptyList()))
        assertEquals(0L, LabStats.percentile(emptyList(), 95))
    }

    @Test
    fun percentilesUseTheNearestRank() {
        val values = (1L..100L).toList()
        assertEquals(95L, LabStats.percentile(values, 95))
        assertEquals(100L, LabStats.percentile(values, 100))
        assertEquals(1L, LabStats.percentile(values, 1))
        assertEquals(7L, LabStats.percentile(listOf(7), 95))
    }

    @Test
    fun gapsAreTheDifferencesBetweenNeighbours() {
        assertEquals(listOf(100L, 250L), LabStats.gaps(listOf(0, 100, 350)))
        assertTrue(LabStats.gaps(listOf(5)).isEmpty())
    }
}
