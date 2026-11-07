package com.zdmgold.autoclickernega.ui.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IconSetTest {
    private val drawables = File("src/main/res/drawable")
    private val icons =
        drawables
            .listFiles { f ->
                f.name.startsWith("ic_") && f.extension == "xml"
            }.orEmpty()
            .sortedBy { it.name }

    @Test
    fun thereAreExactlyThirtySixIcons() {
        assertEquals(icons.joinToString { it.name }, 36, icons.size)
    }

    @Test
    fun everyIconIsA24dpRoundedStrokeWithAWidthOfTwo() {
        for (f in icons) {
            val text = f.readText()
            assertTrue(
                "${f.name} size",
                text.contains("android:width=\"24dp\"") && text.contains("android:height=\"24dp\""),
            )
            assertTrue(
                "${f.name} viewport",
                text.contains("android:viewportWidth=\"24\"") && text.contains("android:viewportHeight=\"24\""),
            )
            assertTrue("${f.name} stroke width", text.contains("android:strokeWidth=\"2\""))
            assertTrue("${f.name} caps", text.contains("android:strokeLineCap=\"round\""))
            assertTrue("${f.name} joins", text.contains("android:strokeLineJoin=\"round\""))
            assertTrue("${f.name} unfilled", text.contains("android:fillColor=\"#00000000\""))
            assertTrue("${f.name} tintable", text.contains("android:strokeColor=\"#FF000000\""))
        }
    }

    @Test
    fun everyPathUsesOnlyPathCommandsAndNumbers() {
        val allowed = Regex("^[MmLlHhVvCcSsQqTtAaZz0-9eE.,\\s-]+$")
        for (f in icons) {
            val d = Regex("android:pathData=\"([^\"]+)\"").find(f.readText())?.groupValues?.get(1)
            assertTrue("${f.name} has no path", d != null)
            assertTrue("${f.name} path: $d", allowed.matches(d!!))
            assertTrue("${f.name} must start with a move", d.startsWith("M"))
        }
    }

    @Test
    fun theSemanticAliasesCoverEveryDrawableAndNothingElse() {
        val source = File("src/main/kotlin/com/zdmgold/autoclickernega/ui/icons/AppIcons.kt").readText()
        val aliased = Regex("R\\.drawable\\.(ic_[a-z_]+)").findAll(source).map { it.groupValues[1] }.toList()
        assertEquals(icons.map { it.nameWithoutExtension }, aliased.sorted())
        assertEquals(aliased.size, aliased.toSet().size)
    }

    @Test
    fun theNamedSetMatchesTheDesignSystem() {
        val expected =
            listOf(
                "play",
                "pause",
                "stop",
                "add",
                "close",
                "chevron_right",
                "chevron_left",
                "settings",
                "trash",
                "copy",
                "edit",
                "import",
                "export",
                "paste",
                "tap",
                "swipe",
                "curve",
                "multi",
                "image_target",
                "text_target",
                "color_target",
                "branch",
                "timer",
                "counter",
                "repeat",
                "shield",
                "check",
                "info",
                "warning",
                "help",
                "drag",
                "crosshair",
                "search",
                "record",
                "hud",
                "download",
            )
        assertEquals(expected.map { "ic_$it" }.sorted(), icons.map { it.nameWithoutExtension })
    }
}
