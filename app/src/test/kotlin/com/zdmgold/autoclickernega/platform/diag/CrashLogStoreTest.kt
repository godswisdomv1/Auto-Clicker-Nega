package com.zdmgold.autoclickernega.platform.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CrashLogStoreTest {
    private fun withDir(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("nega-crash").toFile()
        try {
            block(File(dir, "crashes"))
        } finally {
            dir.deleteRecursively()
        }
    }

    private var clock = 1_000L

    private fun store(dir: File) = CrashLogStore(dir, now = { clock++ })

    @Test
    fun aSavedCrashComesBackWithItsDetails() =
        withDir { dir ->
            val s = store(dir)
            s.record(IllegalStateException("boom"), "main")
            val e = s.entries().single()
            assertEquals("java.lang.IllegalStateException", e.type)
            assertEquals("boom", e.message)
            assertEquals("main", e.thread)
            assertTrue(e.stack.isNotEmpty())
        }

    @Test
    fun onlyTheLastFiveAreKeptNewestFirst() =
        withDir { dir ->
            val s = store(dir)
            repeat(8) { s.record(RuntimeException("crash $it"), "t") }
            val entries = s.entries()
            assertEquals(5, entries.size)
            assertEquals(listOf("crash 7", "crash 6", "crash 5", "crash 4", "crash 3"), entries.map { it.message })
        }

    @Test
    fun longMessagesAndStacksAreCutShort() =
        withDir { dir ->
            val s = store(dir)
            s.record(RuntimeException("x".repeat(5_000)), "t")
            val e = s.entries().single()
            assertEquals(200, e.message.length)
            assertTrue(e.stack.size <= 40)
        }

    @Test
    fun aCorruptFileIsSkippedNotFatal() =
        withDir { dir ->
            val s = store(dir)
            s.record(RuntimeException("good"), "t")
            File(dir, "crash-9999-1.json").writeText("{ broken")
            assertEquals(listOf("good"), s.entries().map { it.message })
        }

    @Test
    fun clearRemovesEverything() =
        withDir { dir ->
            val s = store(dir)
            s.record(RuntimeException("a"), "t")
            s.clear()
            assertTrue(s.entries().isEmpty())
        }

    @Test
    fun savingNeverThrowsEvenWhenTheFolderCannotBeCreated() =
        withDir { dir ->
            dir.parentFile!!.resolve("crashes").writeText("a file is in the way")
            store(dir).record(RuntimeException("x"), "t")
            assertTrue(store(dir).entries().isEmpty())
        }

    @Test
    fun anEmptyFolderHasNoEntries() =
        withDir { dir ->
            assertTrue(store(dir).entries().isEmpty())
        }

    @Test
    fun aMissingMessageIsStoredAsEmptyText() =
        withDir { dir ->
            val s = store(dir)
            s.record(NullPointerException(), "t")
            assertEquals("", s.entries().single().message)
        }
}
