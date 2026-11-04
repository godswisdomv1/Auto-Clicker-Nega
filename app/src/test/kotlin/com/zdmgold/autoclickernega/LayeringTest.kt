package com.zdmgold.autoclickernega

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Fails the build when the layering rules of the architecture are broken. */
class LayeringTest {
    private val base = "com.zdmgold.autoclickernega"
    private val root = File("src/main/kotlin/com/zdmgold/autoclickernega")

    private fun imports(layer: String): List<Pair<String, String>> {
        val folder = File(root, layer)
        assertTrue("Missing source folder ${folder.path}", folder.isDirectory)
        return folder
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file
                    .readLines()
                    .filter { it.startsWith("import ") }
                    .map { file.name to it.removePrefix("import ").trim() }
            }.toList()
    }

    @Test
    fun coreImportsNothingFromAndroid() {
        val bad =
            imports("core").filter { (_, imp) ->
                imp.startsWith("android.") || imp.startsWith("androidx.") || imp == "$base.R"
            }
        assertTrue("core/ must be pure Kotlin: $bad", bad.isEmpty())
    }

    @Test
    fun coreDoesNotReachIntoOuterLayers() {
        val outer = listOf("ui", "data", "platform", "service").map { "$base.$it." }
        val bad = imports("core").filter { (_, imp) -> outer.any { imp.startsWith(it) } }
        assertTrue("core/ may only depend on core/: $bad", bad.isEmpty())
    }

    @Test
    fun uiReachesServiceOnlyThroughTheBridge() {
        val bad =
            imports("ui").filter { (_, imp) ->
                imp.startsWith("$base.service.") && imp != "$base.service.bridge.EngineBridge"
            }
        assertTrue("ui/ may use service/ only through EngineBridge: $bad", bad.isEmpty())
    }
}
