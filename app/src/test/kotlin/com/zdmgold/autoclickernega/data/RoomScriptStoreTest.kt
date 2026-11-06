package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.ScriptMode
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.script.TestScripts
import com.zdmgold.autoclickernega.data.db.ScriptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomScriptStoreTest {
    private val dao = FakeScriptDao()
    private val clock = FakeWallClock()
    private var next = 0
    private val store = RoomScriptStore(dao, clock) { "id${++next}" }

    private fun ok(outcome: Outcome<Script>): Script =
        (outcome as? Outcome.Success)?.value
            ?: throw AssertionError("expected success but got $outcome")

    @Test
    fun savingAssignsAnIdAndTheScriptComesBackEqual() =
        runTest {
            val saved = ok(store.save(TestScripts.simple(Action.Wait(10), id = "")))
            assertEquals("id1", saved.id)
            val loaded = (store.get("id1") as Outcome.Success).value
            assertEquals(saved, loaded)
        }

    @Test
    fun savingKeepsAnExistingIdAndUpdatesTheRow() =
        runTest {
            store.save(TestScripts.simple(Action.Wait(10), id = "keep"))
            clock.now = 5_000
            store.save(TestScripts.simple(Action.Wait(20), Action.Stop, id = "keep"))
            val rows = store.scripts.first()
            assertEquals(1, rows.size)
            assertEquals(2, rows.single().itemCount)
            assertEquals(5_000L, rows.single().updatedAtMs)
        }

    @Test
    fun anInvalidScriptIsRefusedAndNothingIsStored() =
        runTest {
            val bad = TestScripts.simple(Action.Stop, id = "bad").copy(name = " ")
            assertTrue(store.save(bad) is Outcome.Failure)
            assertTrue(store.scripts.first().isEmpty())
        }

    @Test
    fun theListIsNewestFirstWithSummaryFields() =
        runTest {
            clock.now = 100
            store.save(TestScripts.simple(Action.Stop, id = "old"))
            clock.now = 200
            store.save(TestScripts.smart(TestScripts.event("e", 1, TestScripts.image), id = "new"))
            val rows = store.scripts.first()
            assertEquals(listOf("new", "old"), rows.map { it.id })
            assertEquals(ScriptMode.Smart, rows[0].mode)
            assertEquals(ScriptMode.Simple, rows[1].mode)
            assertEquals(1, rows[0].itemCount)
        }

    @Test
    fun deleteReturnsTheScriptSoItCanBeRestored() =
        runTest {
            val saved = ok(store.save(TestScripts.simple(Action.Wait(1), id = "d")))
            val removed = (store.delete("d") as Outcome.Success).value
            assertEquals(saved, removed)
            assertNull((store.get("d") as Outcome.Success).value)
            assertTrue(store.restore(removed!!) is Outcome.Success)
            assertEquals(saved, (store.get("d") as Outcome.Success).value)
        }

    @Test
    fun deletingAMissingScriptIsNotAnError() =
        runTest {
            assertNull((store.delete("nope") as Outcome.Success).value)
        }

    @Test
    fun renameChangesOnlyTheName() =
        runTest {
            val saved = ok(store.save(TestScripts.simple(Action.Wait(1), id = "r")))
            assertTrue(store.rename("r", "Better name") is Outcome.Success)
            assertEquals(saved.copy(name = "Better name"), (store.get("r") as Outcome.Success).value)
            assertTrue(store.rename("r", " ") is Outcome.Failure)
            assertTrue(store.rename("missing", "x") is Outcome.Failure)
        }

    @Test
    fun duplicateMakesANewScriptWithItsOwnIdAndName() =
        runTest {
            val saved = ok(store.save(TestScripts.simple(Action.Wait(1), id = "src")))
            val copy = ok(store.duplicate("src", "Copy"))
            assertEquals("Copy", copy.name)
            assertTrue(copy.id != saved.id)
            assertEquals(saved.body, copy.body)
            assertEquals(2, store.scripts.first().size)
            assertTrue(store.duplicate("missing", "x") is Outcome.Failure)
        }

    @Test
    fun aLoopAcrossStoredScriptsIsRefusedOnSave() =
        runTest {
            store.save(TestScripts.simple(Action.Wait(1), id = "b"))
            store.save(TestScripts.simple(Action.RunScript("b"), id = "a"))
            val loop = store.save(TestScripts.simple(Action.RunScript("a"), id = "b"))
            val error = (loop as Outcome.Failure).error as AppError.MalformedImport
            assertEquals(ImportProblem.Cycle, error.reason)
        }

    @Test
    fun anUnreadableRowIsReportedNotHidden() =
        runTest {
            dao.upsert(ScriptEntity("broken", "Broken", "simple", 0, 1, "{ nope"))
            val r = store.get("broken")
            assertTrue(r is Outcome.Failure)
            assertTrue((r as Outcome.Failure).error is AppError.Storage)
            assertNotNull(store.scripts.first().single())
        }
}
