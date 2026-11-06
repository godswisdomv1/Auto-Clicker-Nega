package com.zdmgold.autoclickernega.core.ocr

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackManifestTest {
    private val h1 = "a".repeat(64)
    private val h2 = "0123456789abcdef".repeat(4)

    private fun parsed(text: String): PackManifest = (PackManifest.parse(text) as Outcome.Success).value

    private fun detail(text: String): String =
        ((PackManifest.parse(text) as Outcome.Failure).error as AppError.MalformedImport).detail

    @Test
    fun readsTheBuiltInList() {
        val m = parsed("""{"packs":[{"lang":"ar","sha256":"$h1"},{"lang":"he","sha256":"$h2"}]}""")
        assertEquals(listOf(PackSpec("ar", h1), PackSpec("he", h2)), m.specs)
        assertEquals(PackSpec("he", h2), m.specFor("he"))
        assertNull(m.specFor("fr"))
    }

    @Test
    fun anEmptyListIsValid() {
        assertTrue(parsed("""{"packs":[]}""").specs.isEmpty())
    }

    @Test
    fun refusesBadFiles() {
        assertTrue(PackManifest.parse("not json") is Outcome.Failure)
        assertTrue(PackManifest.parse("[]") is Outcome.Failure)
        assertTrue(detail("""{"nope":1}""").contains("packs"))
    }

    @Test
    fun checksEveryEntry() {
        assertTrue(detail("""{"packs":[{"lang":"xx","sha256":"$h1"}]}""").contains("lang"))
        assertTrue(detail("""{"packs":[{"lang":"ar","sha256":"short"}]}""").contains("sha256"))
        assertTrue(detail("""{"packs":[{"lang":"ar","sha256":"${h1.uppercase()}"}]}""").contains("sha256"))
        assertTrue(detail("""{"packs":["ar"]}""").contains("packs[0]"))
        assertTrue(
            detail("""{"packs":[{"lang":"ar","sha256":"$h1"},{"lang":"ar","sha256":"$h2"}]}""").contains("duplicate"),
        )
    }

    @Test
    fun theFailureIsATypedImportProblem() {
        val e = (PackManifest.parse("{}") as Outcome.Failure).error as AppError.MalformedImport
        assertEquals(ImportProblem.InvalidField, e.reason)
    }
}
