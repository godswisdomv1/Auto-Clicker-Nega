package com.zdmgold.autoclickernega.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrLanguagesTest {
    @Test
    fun theElevenAppLanguagesAreSupported() {
        assertEquals(11, OcrLanguages.all.size)
        assertTrue(OcrLanguages.all.all { OcrLanguages.isSupported(it) })
        assertFalse(OcrLanguages.isSupported("ru"))
    }

    @Test
    fun onlyArabicAndHebrewAreRightToLeft() {
        assertEquals(listOf("ar", "he"), OcrLanguages.all.filter { OcrLanguages.isRightToLeft(it) })
    }

    @Test
    fun tagsMapToTheSupportedCodes() {
        assertEquals("zh", OcrLanguages.normalize("zh-Hans"))
        assertEquals("zh", OcrLanguages.normalize("zh_CN"))
        assertEquals("pt", OcrLanguages.normalize("pt-BR"))
        assertEquals("he", OcrLanguages.normalize("iw"))
        assertEquals("he", OcrLanguages.normalize("he-IL"))
        assertEquals("en", OcrLanguages.normalize("EN"))
        assertNull(OcrLanguages.normalize("ru"))
        assertNull(OcrLanguages.normalize(""))
    }
}
