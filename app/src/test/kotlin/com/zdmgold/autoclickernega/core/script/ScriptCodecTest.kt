package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.model.ScriptBody
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptCodecTest {
    private fun ok(json: String): Script =
        when (val r = ScriptCodec.decode(json)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> throw AssertionError("expected success but got ${r.error}")
        }

    private fun malformed(json: String): AppError.MalformedImport {
        val r = ScriptCodec.decode(json) as? Outcome.Failure ?: throw AssertionError("expected a failure")
        return r.error as? AppError.MalformedImport ?: throw AssertionError("expected malformed import, got ${r.error}")
    }

    private val header = """"schema":1,"name":"n","screen":{"w":1080,"h":2400,"density":420}"""

    @Test
    fun roundTripsEveryActionAndCondition() {
        val script = TestScripts.everything()
        assertEquals(script, ok(ScriptCodec.encode(script)))
    }

    @Test
    fun roundTripsSimpleMode() {
        val script =
            TestScripts
                .simple(
                    Action.Tap(Target.At(Point(540, 1200)), 40, 3, 100),
                    Action.Wait(1000),
                ).copy(name = "Tap loop")
        assertEquals(script, ok(ScriptCodec.encode(script)))
    }

    @Test
    fun encodingIsReproducible() {
        val script = TestScripts.everything()
        assertEquals(ScriptCodec.encode(script), ScriptCodec.encode(ok(ScriptCodec.encode(script))))
    }

    @Test
    fun appliesDefaultsToMissingFields() {
        val script = ok("""{$header,"mode":"simple","steps":[{"type":"tap","x":1,"y":2}]}""")
        val tap = (script.body as ScriptBody.Simple).steps.single() as Action.Tap
        assertEquals(Limits.DEFAULT_HOLD_MS, tap.holdMs)
        assertEquals(Limits.DEFAULT_INTERVAL_MS, tap.intervalMs)
        assertEquals(0, tap.jitterPx)
        assertEquals("", script.id)
        assertEquals(1, script.run.loops)
    }

    @Test
    fun readsColoursAsHexText() {
        val json = """{$header,"mode":"smart","events":[{"id":"e","priority":1,"when":{"op":"AND","items":
            [{"type":"color","x":1,"y":2,"rgb":"#12C79C"}]},"do":[{"type":"stop"}]}]}"""
        val event = (ok(json).body as ScriptBody.Smart).events.single()
        assertEquals(0x12C79C, (event.whenExpr.items.single() as Condition.Color).rgb)
    }

    @Test
    fun refusesBrokenJsonAndWrongShapes() {
        assertEquals(ImportProblem.BadJson, malformed("{ not json").reason)
        assertEquals(ImportProblem.BadJson, malformed("[]").reason)
        assertEquals(ImportProblem.BadJson, malformed("").reason)
    }

    @Test
    fun refusesOversizedScripts() {
        val big = """{$header,"mode":"simple","steps":[],"pad":"${"x".repeat(Limits.MAX_SCRIPT_JSON_BYTES)}"}"""
        assertEquals(ImportProblem.TooLarge, malformed(big).reason)
    }

    @Test
    fun namesTheFieldThatIsWrong() {
        val e = malformed("""{$header,"mode":"simple","steps":[{"type":"tap","x":"left","y":2}]}""")
        assertEquals(ImportProblem.InvalidField, e.reason)
        assertTrue(e.detail, e.detail.contains("steps[0].x"))
        assertTrue(malformed("""{$header,"mode":"simple","steps":[{"type":"zap"}]}""").detail.contains("zap"))
        assertTrue(malformed("""{"schema":1,"name":"n","mode":"simple"}""").detail.contains("screen"))
    }

    @Test
    fun refusesNumbersOutsideTheIntRange() {
        val e = malformed("""{$header,"mode":"simple","steps":[{"type":"tap","x":99999999999,"y":2}]}""")
        assertEquals(ImportProblem.InvalidField, e.reason)
    }

    @Test
    fun rejectsANewerSchema() {
        val r = ScriptCodec.decode("""{"schema":2,"name":"n"}""") as Outcome.Failure
        assertEquals(AppError.SchemaTooNew(found = 2, supported = 1), r.error)
    }
}
