package com.zdmgold.autoclickernega.data

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.result.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DataWiperTest {
    private fun layout(): Triple<File, File, List<File>> {
        val root = Files.createTempDirectory("nega-wipe").toFile()
        val db = File(root, "nega.db").also { it.writeText("x") }
        File(root, "nega.db-wal").writeText("x")
        File(root, "nega.db-shm").writeText("x")
        val templates = File(root, "templates/s1").also { it.mkdirs() }
        File(templates, "t1.png").writeBytes(byteArrayOf(1))
        val packs = File(root, "packs/ar").also { it.mkdirs() }
        File(packs, "data").writeBytes(byteArrayOf(1))
        return Triple(root, db, listOf(File(root, "templates"), File(root, "packs")))
    }

    @Test
    fun removesEveryKindOfLocalData() =
        runTest {
            val (root, db, dirs) = layout()
            try {
                var closed = false
                var cleared = false
                val wiper =
                    DataWiper({
                        cleared = true
                        Outcome.Success(Unit)
                    }, { closed = true }, listOf(db), dirs)
                assertTrue(wiper.wipeAll() is Outcome.Success)
                assertTrue(closed && cleared)
                assertEquals(emptyList<String>(), root.list()!!.toList())
            } finally {
                root.deleteRecursively()
            }
        }

    @Test
    fun aFailingStepDoesNotStopTheOthersAndIsReported() =
        runTest {
            val (root, db, dirs) = layout()
            try {
                val wiper = DataWiper({ Outcome.Failure(AppError.Storage("disk")) }, {}, listOf(db), dirs)
                val r = wiper.wipeAll()
                assertTrue(r is Outcome.Failure)
                assertTrue(((r as Outcome.Failure).error as AppError.Storage).detail.contains("settings"))
                assertFalse(db.exists())
                assertFalse(File(root, "templates").exists())
            } finally {
                root.deleteRecursively()
            }
        }

    @Test
    fun missingFilesAreNotAnError() =
        runTest {
            val root = Files.createTempDirectory("nega-wipe-empty").toFile()
            try {
                val wiper =
                    DataWiper({ Outcome.Success(Unit) }, {}, listOf(File(root, "nega.db")), listOf(File(root, "none")))
                assertTrue(wiper.wipeAll() is Outcome.Success)
            } finally {
                root.deleteRecursively()
            }
        }
}
