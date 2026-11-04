package com.zdmgold.autoclickernega.core.model

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.WaitCancelCause
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.result.asSuccess
import com.zdmgold.autoclickernega.core.result.fold
import com.zdmgold.autoclickernega.core.result.getOrNull
import com.zdmgold.autoclickernega.core.result.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val screen = ScreenSpec(1080, 2400, 420)

    @Test
    fun valueTypesCompareByContentAndCopy() {
        val tap = Action.Tap(Target.At(Point(540, 1200)))
        assertEquals(tap, Action.Tap(Target.At(Point(540, 1200))))
        assertEquals(60L, tap.copy(holdMs = 60).holdMs)
        assertEquals(Limits.DEFAULT_HOLD_MS, tap.holdMs)
        assertEquals(Limits.DEFAULT_INTERVAL_MS, tap.intervalMs)
    }

    @Test
    fun rectMeasuresAndIntersects() {
        val r = Rect(10, 20, 110, 220)
        assertEquals(100, r.width)
        assertEquals(200, r.height)
        assertEquals(Point(60, 120), r.center)
        assertTrue(r.contains(Point(10, 20)))
        assertFalse(r.contains(Point(110, 220)))
        assertEquals(Rect(50, 50, 110, 100), r.intersect(Rect(50, 50, 500, 100)))
        assertTrue(r.intersect(Rect(300, 300, 400, 400)).isEmpty)
    }

    @Test
    fun rangeClampsStayInsideTheLimits() {
        assertEquals(0.60f, Limits.clampConfidence(0.1f), 0f)
        assertEquals(0.98f, Limits.clampConfidence(1.5f), 0f)
        assertEquals(0.85f, Limits.clampConfidence(0.85f), 0f)
        assertEquals(50L, Limits.clampInterval(10))
        assertEquals(500L, Limits.clampInterval(500))
        assertEquals(0L, Limits.clampStartDelay(-5))
        assertEquals(43_200_000L, Limits.clampStartDelay(99_999_999_999))
        assertEquals(30, Limits.clampCountdown(300))
        assertEquals(255, Limits.clampTolerance(999))
        assertEquals(0.3f, AppSettings.clampOpacity(0f), 0f)
        assertEquals(1.0f, AppSettings.clampOpacity(5f), 0f)
    }

    @Test
    fun startWaitCapIsTwelveHours() {
        assertEquals(12L * 60 * 60 * 1000, Limits.MAX_START_WAIT_MS)
    }

    @Test
    fun comparisonsEvaluateAndRoundTripTheirSymbols() {
        assertTrue(Comparison.Lt.holds(1L, 2L))
        assertTrue(Comparison.Gte.holds(2L, 2L))
        assertFalse(Comparison.Gt.holds(2.0, 2.0))
        assertTrue(Comparison.Neq.holds(1.5, 2.5))
        Comparison.entries.forEach { assertEquals(it, Comparison.fromSymbol(it.symbol)) }
        assertNull(Comparison.fromSymbol("~"))
    }

    @Test
    fun scriptCountsItemsInBothModes() {
        val simple =
            Script(
                id = "s1",
                name = "Tap loop",
                screen = screen,
                body = ScriptBody.Simple(listOf(Action.Wait(1000), Action.Stop)),
            )
        assertEquals(2, simple.itemCount)
        assertEquals(Script.SCHEMA_VERSION, simple.schema)
        val event =
            Event(
                id = "e1",
                name = "Reward",
                priority = 1,
                whenExpr = Expr(BoolOp.And, listOf(Condition.Counter("c1", Comparison.Lt, 50))),
                actions = listOf(Action.Stop),
            )
        val smart = simple.copy(body = ScriptBody.Smart(listOf(event)))
        assertEquals(1, smart.itemCount)
        assertTrue(event.enabled)
    }

    @Test
    fun defaultsMatchTheProductDefaults() {
        val run = RunConfig()
        assertEquals(1, run.loops)
        assertEquals(3, run.countdownSec)
        assertEquals(0L, run.startDelayMs)
        assertEquals(SearchRate.Auto, run.searchRate)
        val image = Condition.Image(template = "t1")
        assertEquals(0.85f, image.confidence, 0f)
        assertEquals(MatchMode.Safe, image.mode)
        assertFalse(image.negate)
    }

    @Test
    fun consentAllowsAdsOnlyWhenPermitted() {
        assertFalse(ConsentState().canRequestAds)
        assertFalse(ConsentState(ConsentDecision.Declined).canRequestAds)
        assertTrue(ConsentState(ConsentDecision.Permitted).canRequestAds)
    }

    @Test
    fun freeEntitlementIsNotPro() {
        assertFalse(Entitlement.Free.isPro)
        assertNull(Entitlement.Free.verifiedAtMs)
    }

    @Test
    fun errorsCarryTheirTreatmentFlags() {
        assertTrue(AppError.ServiceNotEnabled.recoverable)
        assertFalse(AppError.ServiceNotEnabled.retriable)
        assertFalse(AppError.SchemaTooNew(found = 2, supported = 1).recoverable)
        assertTrue(AppError.StartWaitCancelled(WaitCancelCause.ScreenOff).retriable)
        assertEquals("nothing_found", AppError.NothingFound.code)
    }

    @Test
    fun outcomeMapsAndFoldsBothWays() {
        val ok: Outcome<Int> = 2.asSuccess()
        assertEquals(4, ok.map { it * 2 }.getOrNull())
        val failed: Outcome<Int> = Outcome.Failure(AppError.NothingFound)
        assertNull(failed.map { it * 2 }.getOrNull())
        assertEquals("nothing_found", failed.fold(onSuccess = { "ok" }, onFailure = { it.code }))
    }
}
