package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptImporterTest {
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 9, 9, 9)
    private val withImage = TestScripts.smart(TestScripts.event("e", 1, TestScripts.image))

    private fun imported(bytes: ByteArray): Bundle =
        (ScriptImporter.import(bytes) as? Outcome.Success)?.value ?: throw AssertionError("expected success")

    private fun exported(
        script: com.zdmgold.autoclickernega.core.model.Script,
        images: Map<String, ByteArray>,
    ): Export =
        (ScriptImporter.export(script, images, "0.1.0") as? Outcome.Success)?.value
            ?: throw AssertionError("export failed")

    @Test
    fun aScriptWithoutImagesExportsAsJsonAndImportsBack() {
        val script = TestScripts.simple(Action.Wait(500), Action.Stop)
        val out = exported(script, emptyMap())
        assertEquals(ExportKind.Json, out.kind)
        assertEquals("json", out.extension)
        val back = imported(out.bytes)
        assertEquals(script, back.script)
        assertTrue(back.templates.isEmpty())
    }

    @Test
    fun aScriptWithImagesExportsAsABundleAndImportsBack() {
        val out = exported(withImage, mapOf("t1" to png))
        assertEquals(ExportKind.Bundle, out.kind)
        assertEquals("nega", out.extension)
        val back = imported(out.bytes)
        assertEquals(withImage, back.script)
        assertTrue(png.contentEquals(back.templates.getValue("t1")))
    }

    @Test
    fun importingValidatesAndSanitisesTheScript() {
        val sloppy =
            ScriptCodec.encode(
                TestScripts.simple(
                    Action.Tap(
                        com.zdmgold.autoclickernega.core.model.Target.At(
                            com.zdmgold.autoclickernega.core.model
                                .Point(1, 1),
                        ),
                        intervalMs = 5,
                    ),
                ),
            )
        val tap =
            (
                imported(
                    sloppy.toByteArray(),
                ).script.body as com.zdmgold.autoclickernega.core.model.ScriptBody.Simple
            ).steps.single() as Action.Tap
        assertEquals(50L, tap.intervalMs)
    }

    @Test
    fun badInputIsRefusedWithATypedError() {
        fun reason(bytes: ByteArray): ImportProblem =
            ((ScriptImporter.import(bytes) as Outcome.Failure).error as AppError.MalformedImport).reason
        assertEquals(ImportProblem.BadJson, reason("not json".toByteArray()))
        assertEquals(ImportProblem.BadBundle, reason(byteArrayOf(0x50, 0x4B, 1, 2, 3, 4)))
        val tooMany =
            ScriptCodec.encode(
                TestScripts.smart(
                    TestScripts.event(
                        "e",
                        1,
                        TestScripts.image,
                        TestScripts.image,
                        TestScripts.image,
                        TestScripts.image,
                    ),
                ),
            )
        assertEquals(ImportProblem.InvalidField, reason(tooMany.toByteArray()))
    }

    @Test
    fun aLoopAcrossScriptsIsCaughtWhenTheLibraryIsKnown() {
        val b = TestScripts.simple(Action.RunScript("a"), id = "b")
        val a = TestScripts.simple(Action.RunScript("b"), id = "a")
        val r = ScriptImporter.import(ScriptCodec.encode(a).toByteArray()) { if (it == "b") b else null }
        assertEquals(ImportProblem.Cycle, ((r as Outcome.Failure).error as AppError.MalformedImport).reason)
    }
}
