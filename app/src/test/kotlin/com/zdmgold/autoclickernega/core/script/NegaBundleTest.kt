package com.zdmgold.autoclickernega.core.script

import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.ImportProblem
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.Script
import com.zdmgold.autoclickernega.core.result.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class NegaBundleTest {
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3, 4, 5)
    private val script: Script = TestScripts.smart(TestScripts.event("e", 1, TestScripts.image))

    private fun packed(): ByteArray = (NegaBundle.pack(script, mapOf("t1" to png), "0.1.0") as Outcome.Success).value

    private fun entries(bytes: ByteArray): MutableMap<String, ByteArray> {
        val out = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                out[e.name] = zip.readBytes()
                e = zip.nextEntry
            }
        }
        return out
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            entries.forEach { (name, data) ->
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun refusal(bytes: ByteArray): ImportProblem {
        val r = NegaBundle.unpack(bytes) as? Outcome.Failure ?: throw AssertionError("expected a failure")
        return (r.error as AppError.MalformedImport).reason
    }

    @Test
    fun roundTripsAScriptWithItsImages() {
        val bundle = (NegaBundle.unpack(packed()) as Outcome.Success).value
        assertEquals(script, bundle.script)
        assertTrue(png.contentEquals(bundle.templates.getValue("t1")))
    }

    @Test
    fun packingIsReproducible() {
        assertTrue(packed().contentEquals(packed()))
    }

    @Test
    fun theManifestCarriesTheHashOfEveryImage() {
        val manifest = String(entries(packed()).getValue("manifest.json"), Charsets.UTF_8)
        assertTrue(manifest, manifest.contains(NegaBundle.sha256Hex(png)))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            NegaBundle.sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun aChangedImageIsRefused() {
        val e = entries(packed())
        e["templates/t1.png"] = png.copyOf().also { it[10] = 99 }
        assertEquals(ImportProblem.HashMismatch, refusal(zip(e)))
    }

    @Test
    fun aMissingImageOrManifestIsRefused() {
        val e = entries(packed())
        e.remove("templates/t1.png")
        assertEquals(ImportProblem.BadBundle, refusal(zip(e)))
        val f = entries(packed())
        f.remove("manifest.json")
        assertEquals(ImportProblem.BadBundle, refusal(zip(f)))
        val g = entries(packed())
        g.remove("script.json")
        assertEquals(ImportProblem.BadBundle, refusal(zip(g)))
    }

    @Test
    fun anImageThatIsNotAPngIsRefused() {
        val e = entries(packed())
        val fake = ByteArray(20) { 7 }
        e["templates/t1.png"] = fake
        val manifest =
            String(
                e.getValue("manifest.json"),
                Charsets.UTF_8,
            ).replace(NegaBundle.sha256Hex(png), NegaBundle.sha256Hex(fake))
        e["manifest.json"] = manifest.toByteArray(Charsets.UTF_8)
        assertEquals(ImportProblem.BadBundle, refusal(zip(e)))
    }

    @Test
    fun extraFilesAndOddNamesAreRefused() {
        val extra = entries(packed())
        extra["templates/other.png"] = png
        assertEquals(ImportProblem.BadBundle, refusal(zip(extra)))
        val traversal = entries(packed())
        traversal["../evil.png"] = png
        assertEquals(ImportProblem.BadBundle, refusal(zip(traversal)))
        val nested = entries(packed())
        nested["templates/sub/t.png"] = png
        assertEquals(ImportProblem.BadBundle, refusal(zip(nested)))
        val other = entries(packed())
        other["notes.txt"] = png
        assertEquals(ImportProblem.BadBundle, refusal(zip(other)))
    }

    @Test
    fun garbageAndEmptyInputAreRefused() {
        assertEquals(ImportProblem.BadBundle, refusal(ByteArray(64) { it.toByte() }))
        assertEquals(ImportProblem.BadBundle, refusal(ByteArray(0)))
    }

    @Test
    fun anOversizedBundleIsRefused() {
        assertEquals(ImportProblem.TooLarge, refusal(ByteArray(Limits.MAX_BUNDLE_BYTES + 1)))
    }

    @Test
    fun aZipBombIsRefused() {
        val e = entries(packed())
        e["templates/t1.png"] = png + ByteArray(3 * 1024 * 1024)
        assertEquals(ImportProblem.TooLarge, refusal(zip(e)))
    }

    @Test
    fun aNewerManifestSchemaIsRefused() {
        val e = entries(packed())
        e["manifest.json"] =
            String(e.getValue("manifest.json"), Charsets.UTF_8)
                .replace("\"schema\":1", "\"schema\":2")
                .toByteArray(Charsets.UTF_8)
        assertEquals(ImportProblem.InvalidField, refusal(zip(e)))
    }

    @Test
    fun packingNeedsEveryImageAndASafeFileName() {
        val missing = NegaBundle.pack(script, emptyMap(), "0.1.0") as Outcome.Failure
        assertEquals(ImportProblem.BadBundle, (missing.error as AppError.MalformedImport).reason)
        val unsafe = script.copy(templates = mapOf("t1" to TestScripts.template.copy(file = "../x.png")))
        val r = NegaBundle.pack(unsafe, mapOf("t1" to png), "0.1.0") as Outcome.Failure
        assertEquals(ImportProblem.InvalidField, (r.error as AppError.MalformedImport).reason)
    }

    @Test
    fun aScriptWithoutImagesStillBundles() {
        val plain = TestScripts.simple(com.zdmgold.autoclickernega.core.model.Action.Stop)
        val bundle =
            (
                NegaBundle.unpack(
                    (NegaBundle.pack(plain, emptyMap(), "0.1.0") as Outcome.Success).value,
                ) as Outcome.Success
            ).value
        assertEquals(plain, bundle.script)
        assertTrue(bundle.templates.isEmpty())
    }
}
