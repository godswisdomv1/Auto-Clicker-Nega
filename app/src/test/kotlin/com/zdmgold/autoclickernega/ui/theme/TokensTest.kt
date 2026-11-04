package com.zdmgold.autoclickernega.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class TokensTest {
    @Test
    fun spacingStaysOnTheAllowedScale() {
        assertEquals(listOf(2f, 4f, 8f, 16f, 24f, 32f, 48f, 64f), Tokens.Spacing.all.map { it.value })
    }

    @Test
    fun radiiStayOnTheAllowedList() {
        assertEquals(listOf(16f, 20f, 12f, 12f, 12f, 20f, 16f), Tokens.Radius.all.map { it.value })
    }

    @Test
    fun iconSizesStayOnTheAllowedList() {
        assertEquals(listOf(22f, 22f, 16f, 28f, 20f), Tokens.IconSize.all.map { it.value })
    }

    @Test
    fun pillIsHalfItsHeight() {
        assertEquals(24f, Tokens.Radius.pill(Tokens.touchTarget).value, 0f)
    }

    @Test
    fun touchTargetAndHairlineAreFixed() {
        assertEquals(48f, Tokens.touchTarget.value, 0f)
        assertEquals(0.5f, Tokens.Stroke.hairline.value, 0f)
        assertEquals(2f, Tokens.Stroke.icon.value, 0f)
    }

    @Test
    fun windowBackgroundResourcesMatchTheTokens() {
        assertEquals(rgb("src/main/res/values/colors.xml"), Tokens.light.bgPrimary.toArgb() and 0xFFFFFF)
        assertEquals(rgb("src/main/res/values-night/colors.xml"), Tokens.dark.bgPrimary.toArgb() and 0xFFFFFF)
    }

    private fun rgb(path: String): Int {
        val file = File(path)
        assertNotNull("Missing $path", file.takeIf { it.exists() })
        val hex = Regex("""name="nega_bg_primary">#([0-9A-Fa-f]{6})<""").find(file.readText())?.groupValues?.get(1)
        assertNotNull("nega_bg_primary not found in $path", hex)
        return hex!!.toInt(16)
    }
}
