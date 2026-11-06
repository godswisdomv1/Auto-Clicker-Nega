package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FileTemplateStoreTest {
    private fun withStore(block: suspend (FileTemplateStore, java.io.File) -> Unit) =
        runTest {
            val dir = Files.createTempDirectory("nega-tpl").toFile()
            try {
                block(FileTemplateStore(dir), dir)
            } finally {
                dir.deleteRecursively()
            }
        }

    private val png = byteArrayOf(1, 2, 3, 4)

    @Test
    fun savesLoadsAndListsPerScript() =
        withStore { store, _ ->
            assertTrue(store.save("s1", "t2", png) is Outcome.Success)
            assertTrue(store.save("s1", "t1", png + 9) is Outcome.Success)
            assertTrue(store.save("s2", "t1", png) is Outcome.Success)
            assertTrue(png.contentEquals((store.load("s1", "t2") as Outcome.Success).value))
            assertEquals(listOf("t1", "t2"), (store.list("s1") as Outcome.Success).value)
            assertEquals(listOf("t1"), (store.list("s2") as Outcome.Success).value)
        }

    @Test
    fun overwritingReplacesTheImageAndLeavesNoTempFile() =
        withStore { store, root ->
            store.save("s1", "t1", png)
            store.save("s1", "t1", byteArrayOf(7, 7))
            assertEquals(2, ((store.load("s1", "t1") as Outcome.Success).value)!!.size)
            assertEquals(listOf("t1.png"), root.resolve("s1").list()!!.toList())
        }

    @Test
    fun missingImagesLoadAsNullAndListAsEmpty() =
        withStore { store, _ ->
            assertNull((store.load("s1", "t1") as Outcome.Success).value)
            assertTrue((store.list("none") as Outcome.Success).value.isEmpty())
        }

    @Test
    fun deleteRemovesOneImageAndDeleteAllRemovesTheFolder() =
        withStore { store, root ->
            store.save("s1", "t1", png)
            store.save("s1", "t2", png)
            assertTrue(store.delete("s1", "t1") is Outcome.Success)
            assertEquals(listOf("t2"), (store.list("s1") as Outcome.Success).value)
            assertTrue(store.deleteAll("s1") is Outcome.Success)
            assertTrue(!root.resolve("s1").exists())
            assertTrue(store.deleteAll("s1") is Outcome.Success)
        }

    @Test
    fun unsafeIdsAreRefusedAndNothingEscapesTheFolder() =
        withStore { store, root ->
            assertTrue(store.save("../evil", "t1", png) is Outcome.Failure)
            assertTrue(store.save("s1", "../evil", png) is Outcome.Failure)
            assertTrue(store.save("", "t1", png) is Outcome.Failure)
            assertTrue(store.load("s1/x", "t1") is Outcome.Failure)
            assertTrue(
                root.parentFile!!
                    .resolve("evil.png")
                    .exists()
                    .not(),
            )
        }
}
