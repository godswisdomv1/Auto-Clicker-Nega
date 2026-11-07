package com.zdmgold.autoclickernega.core.run

import com.zdmgold.autoclickernega.core.model.SearchRate
import org.junit.Assert.assertEquals
import org.junit.Test

class CadenceControllerTest {
    private fun sequence(
        c: CadenceController,
        changed: List<Boolean>,
    ) = changed.map { c.nextIntervalMs(it) }

    @Test
    fun autoStartsAtTwoHundredAndBacksOffWhenTheScreenIsStill() {
        val c = CadenceController()
        val still = sequence(c, List(12) { false })
        assertEquals(List(4) { 200L } + List(5) { 500L } + List(3) { 1000L }, still)
    }

    @Test
    fun anyChangeStartsOver() {
        val c = CadenceController()
        repeat(11) { c.nextIntervalMs(false) }
        assertEquals(200L, c.nextIntervalMs(true))
        assertEquals(200L, c.nextIntervalMs(false))
    }

    @Test
    fun anActionStartsOver() {
        val c = CadenceController()
        repeat(11) { c.nextIntervalMs(false) }
        c.onAction()
        assertEquals(200L, c.nextIntervalMs(false))
    }

    @Test
    fun theOsLimitRaisesTheBase() {
        val c = CadenceController(osMinIntervalMs = 333)
        assertEquals(333L, c.nextIntervalMs(true))
        assertEquals(333L, c.retryIntervalMs())
        repeat(4) { c.nextIntervalMs(false) }
        assertEquals(500L, c.nextIntervalMs(false))
    }

    @Test
    fun aVerySlowOsLimitIsNeverShortened() {
        val c = CadenceController(osMinIntervalMs = 2500)
        repeat(12) { assertEquals(2500L, c.nextIntervalMs(false)) }
    }

    @Test
    fun fastNeverBacksOff() {
        val c = CadenceController(SearchRate.Fast)
        assertEquals(List(12) { 200L }, sequence(c, List(12) { false }))
        assertEquals(100L, CadenceController(SearchRate.Fast, osMinIntervalMs = 50).nextIntervalMs(true))
    }

    @Test
    fun batterySaverSearchesLessOften() {
        val c = CadenceController(SearchRate.BatterySaver)
        assertEquals(List(4) { 400L } + List(5) { 1000L } + List(3) { 2000L }, sequence(c, List(12) { false }))
    }

    @Test
    fun failedCapturesRetryAtTheBaseRate() {
        val c = CadenceController()
        repeat(12) { c.nextIntervalMs(false) }
        assertEquals(200L, c.retryIntervalMs())
    }
}
