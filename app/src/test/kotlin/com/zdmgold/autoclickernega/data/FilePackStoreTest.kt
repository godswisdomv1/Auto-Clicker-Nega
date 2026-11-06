package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.port.InstalledPack
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FilePackStoreTest {
    private fun withStore(block: suspend (FilePackStore, File) -> Unit) =
        runTest {
            val dir = Files.createTempDirectory("nega-packs").toFile()
            try {
                block(FilePackStore(dir), dir)
            } finally {
                dir.deleteRecursively()
            }
        }

    @Test
    fun recordsInstalledPacksWithTheirHash() =
        withStore { store, _ ->
            assertTrue((store.installed() as Outcome.Success).value.isEmpty())
            assertTrue(store.markInstalled(InstalledPack("ar", "abc123", 1_000)) is Outcome.Success)
            assertTrue(store.markInstalled(InstalledPack("he", "def456", 2_000)) is Outcome.Success)
            val installed = (store.installed() as Outcome.Success).value
            assertEquals(listOf("ar", "he"), installed.keys.toList())
            assertEquals(InstalledPack("ar", "abc123", 1_000), installed.getValue("ar"))
        }

    @Test
    fun removeDeletesTheFilesAndTheRecord() =
        withStore { store, root ->
            store.markInstalled(InstalledPack("ar", "abc", 1))
            File(root, "ar/data.bin").writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(store.remove("ar") is Outcome.Success)
            assertTrue(!File(root, "ar").exists())
            assertTrue((store.installed() as Outcome.Success).value.isEmpty())
            assertTrue(store.remove("ar") is Outcome.Success)
        }

    @Test
    fun aFolderWithAnUnreadableRecordCountsAsNotInstalled() =
        withStore { store, root ->
            File(root, "ja").mkdirs()
            File(root, "ja/pack.json").writeText("{ broken")
            File(root, "ko").mkdirs()
            assertTrue((store.installed() as Outcome.Success).value.isEmpty())
        }

    @Test
    fun directoriesAreResolvedUnderTheRootOnly() =
        withStore { store, root ->
            assertEquals(File(root, "zh"), (store.directoryFor("zh") as Outcome.Success).value)
            assertTrue(store.directoryFor("../x") is Outcome.Failure)
            assertTrue(store.directoryFor("a") is Outcome.Failure)
            assertTrue(store.markInstalled(InstalledPack("../x", "h", 1)) is Outcome.Failure)
        }
}
