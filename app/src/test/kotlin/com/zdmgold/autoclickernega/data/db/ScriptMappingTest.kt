package com.zdmgold.autoclickernega.data.db

import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.port.ScriptMode
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptMappingTest {
    @Test
    fun entityCarriesTheIndexedColumnsAndTheJson() {
        val script = TestScripts.smart(TestScripts.event("e", 1, TestScripts.image))
        val e = ScriptMapping.toEntity(script, 42)
        assertEquals("s1", e.id)
        assertEquals("smart", e.mode)
        assertEquals(1, e.itemCount)
        assertEquals(42L, e.updatedAt)
        assertEquals(script, (ScriptMapping.toScript(e) as Outcome.Success).value)
    }

    @Test
    fun simpleScriptsAreMarkedSimple() {
        val e = ScriptMapping.toEntity(TestScripts.simple(Action.Stop), 1)
        assertEquals("simple", e.mode)
        val summary = ScriptMapping.toSummary(ScriptSummaryRow(e.id, e.name, e.mode, e.itemCount, e.updatedAt))
        assertEquals(ScriptMode.Simple, summary.mode)
    }

    @Test
    fun theRowIdWinsOverAnIdInsideTheJson() {
        val e = ScriptMapping.toEntity(TestScripts.simple(Action.Stop, id = "inner"), 1).copy(id = "outer")
        assertEquals("outer", (ScriptMapping.toScript(e) as Outcome.Success).value.id)
    }

    @Test
    fun brokenJsonBecomesAStorageError() {
        val e = ScriptMapping.toEntity(TestScripts.simple(Action.Stop), 1).copy(json = "[]")
        assertTrue(ScriptMapping.toScript(e) is Outcome.Failure)
    }
}
