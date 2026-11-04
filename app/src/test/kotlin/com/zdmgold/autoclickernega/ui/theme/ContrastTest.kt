package com.zdmgold.autoclickernega.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ContrastTest {
    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
    }

    private fun ratio(
        a: Color,
        b: Color,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun failures(
        name: String,
        c: NegaColors,
    ): List<String> {
        val surfaces = listOf("bgPrimary" to c.bgPrimary, "bgSecondary" to c.bgSecondary, "bgTertiary" to c.bgTertiary)
        val texts =
            listOf(
                "textPrimary" to c.textPrimary,
                "textSecondary" to c.textSecondary,
                "textTertiary" to c.textTertiary,
                "accentText" to c.accentText,
                "error" to c.error,
                "success" to c.success,
                "warning" to c.warning,
            )
        val pairs =
            buildList {
                for ((textName, text) in texts) {
                    for ((surfaceName, surface) in surfaces) {
                        add(Triple("$name $textName on $surfaceName", text, surface))
                    }
                }
                add(Triple("$name onAccent on accent", c.onAccent, c.accent))
                add(Triple("$name onAccent on accentDim", c.onAccent, c.accentDim))
            }
        return pairs.mapNotNull { (label, fg, bg) ->
            val r = ratio(fg, bg)
            if (r >= MIN_RATIO) null else "$label = ${"%.2f".format(r)}"
        }
    }

    @Test
    fun lightTextPairsMeetWcagAa() {
        val bad = failures("light", Tokens.light)
        assertTrue("Contrast below $MIN_RATIO: $bad", bad.isEmpty())
    }

    @Test
    fun darkTextPairsMeetWcagAa() {
        val bad = failures("dark", Tokens.dark)
        assertTrue("Contrast below $MIN_RATIO: $bad", bad.isEmpty())
    }

    private companion object {
        const val MIN_RATIO = 4.5
    }
}
