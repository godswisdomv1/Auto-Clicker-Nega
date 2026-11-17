package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.port.ScriptStore
import com.zdmgold.autoclickernega.core.port.ScriptSummary
import com.zdmgold.autoclickernega.core.port.TemplateStore
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemoryScripts : ScriptStore {
    val saved = linkedMapOf<String, Script>()
    private var next = 1
    override val scripts: Flow<List<ScriptSummary>> = MutableStateFlow(emptyList())

    override suspend fun get(id: String) = Outcome.Success(saved[id])

    override suspend fun save(script: Script): Outcome<Script> {
        val withId = if (script.id.isBlank()) script.copy(id = "new-${next++}") else script
        saved[withId.id] = withId
        return Outcome.Success(withId)
    }

    override suspend fun delete(id: String): Outcome<Script?> = Outcome.Success(saved.remove(id))

    override suspend fun restore(script: Script): Outcome<Unit> = Outcome.Success(Unit)

    override suspend fun rename(
        id: String,
        name: String,
    ): Outcome<Unit> = Outcome.Success(Unit)

    override suspend fun duplicate(
        id: String,
        newName: String,
    ): Outcome<Script> = Outcome.Failure(AppError.Storage("unused"))
}

private class MemoryTemplates(
    private val failOn: String? = null,
) : TemplateStore {
    val files = linkedMapOf<Pair<String, String>, ByteArray>()
    var deletedAllFor: String? = null

    override suspend fun save(
        scriptId: String,
        templateId: String,
        png: ByteArray,
    ): Outcome<Unit> {
        if (templateId == failOn) return Outcome.Failure(AppError.Storage("disk full"))
        files[scriptId to templateId] = png
        return Outcome.Success(Unit)
    }

    override suspend fun load(
        scriptId: String,
        templateId: String,
    ) = Outcome.Success(files[scriptId to templateId])

    override suspend fun delete(
        scriptId: String,
        templateId: String,
    ): Outcome<Unit> = Outcome.Success(Unit)

    override suspend fun deleteAll(scriptId: String): Outcome<Unit> {
        deletedAllFor = scriptId
        files.keys.removeAll { it.first == scriptId }
        return Outcome.Success(Unit)
    }

    override suspend fun list(scriptId: String) =
        Outcome.Success(
            files.keys
                .filter {
                    it.first == scriptId
                }.map { it.second },
        )
}

class LibraryImporterTest {
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3, 4, 5)

    private fun json(script: Script) = ScriptCodec.encode(script).toByteArray(Charsets.UTF_8)

    @Test
    fun aJsonScriptIsStoredUnderANewId() =
        runBlocking {
            val scripts = MemoryScripts()
            val script = TestScripts.simple(Action.Wait(100), id = "from-the-file")
            val result = LibraryImporter.importInto(json(script), scripts, MemoryTemplates())
            val stored = (result as Outcome.Success).value
            assertEquals("new-1", stored.id)
            assertEquals(script.copy(id = "new-1"), stored)
            assertEquals(listOf("new-1"), scripts.saved.keys.toList())
        }

    @Test
    fun theSameFileTwiceMakesTwoScripts() =
        runBlocking {
            val scripts = MemoryScripts()
            val bytes = json(TestScripts.simple(Action.Wait(100)))
            LibraryImporter.importInto(bytes, scripts, MemoryTemplates())
            LibraryImporter.importInto(bytes, scripts, MemoryTemplates())
            assertEquals(2, scripts.saved.size)
        }

    @Test
    fun aBundleStoresItsImagesUnderTheNewId() =
        runBlocking {
            val scripts = MemoryScripts()
            val templates = MemoryTemplates()
            val script = TestScripts.smart(TestScripts.event("e", 1, TestScripts.image))
            val bytes = (NegaBundle.pack(script, mapOf("t1" to png), "0.1.0") as Outcome.Success).value
            val stored = (LibraryImporter.importInto(bytes, scripts, templates) as Outcome.Success).value
            assertTrue(png.contentEquals(templates.files.getValue(stored.id to "t1")))
        }

    @Test
    fun aBadFileChangesNothing() =
        runBlocking {
            val scripts = MemoryScripts()
            val result = LibraryImporter.importInto("not a script".toByteArray(), scripts, MemoryTemplates())
            val error = (result as Outcome.Failure).error
            assertEquals(ImportProblem.BadJson, (error as AppError.MalformedImport).reason)
            assertTrue(scripts.saved.isEmpty())
        }

    @Test
    fun anImageThatCannotBeStoredTakesTheScriptBackOut() =
        runBlocking {
            val scripts = MemoryScripts()
            val templates = MemoryTemplates(failOn = "t1")
            val script = TestScripts.smart(TestScripts.event("e", 1, TestScripts.image))
            val bytes = (NegaBundle.pack(script, mapOf("t1" to png), "0.1.0") as Outcome.Success).value
            val result = LibraryImporter.importInto(bytes, scripts, templates)
            assertTrue(result is Outcome.Failure)
            assertTrue(scripts.saved.isEmpty())
            assertEquals("new-1", templates.deletedAllFor)
        }
}
