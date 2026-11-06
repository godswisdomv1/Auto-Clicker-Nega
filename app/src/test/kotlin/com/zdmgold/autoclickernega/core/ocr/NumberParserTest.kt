package com.zdmgold.autoclickernega.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParserTest {
    private fun p(s: String): Double? = NumberParser.parse(s)

    @Test
    fun plainIntegersAndDecimals() {
        assertEquals(42.0, p("42")!!, 0.0)
        assertEquals(0.0, p("0")!!, 0.0)
        assertEquals(3.14, p("3.14")!!, 0.0)
        assertEquals(0.125, p("0.125")!!, 0.0)
        assertEquals(12.5, p("12,5")!!, 0.0)
    }

    @Test
    fun negativeNumbers() {
        assertEquals(-7.0, p("-7")!!, 0.0)
        assertEquals(-7.5, p("\u22127.5")!!, 0.0)
    }

    @Test
    fun groupingWithEachCommonSeparator() {
        assertEquals(1234.0, p("1,234")!!, 0.0)
        assertEquals(1234567.0, p("1,234,567")!!, 0.0)
        assertEquals(1234567.0, p("1.234.567")!!, 0.0)
        assertEquals(1234.0, p("1 234")!!, 0.0)
        assertEquals(1234.0, p("1\u00A0234")!!, 0.0)
        assertEquals(1234.0, p("1\u202F234")!!, 0.0)
        assertEquals(1234567.0, p("1'234'567")!!, 0.0)
    }

    @Test
    fun whenBothSeparatorsAppearTheLastIsTheDecimalPoint() {
        assertEquals(1234.56, p("1,234.56")!!, 0.0)
        assertEquals(1234.56, p("1.234,56")!!, 0.0)
        assertEquals(1234567.89, p("1,234,567.89")!!, 0.0)
        assertEquals(1234567.89, p("1.234.567,89")!!, 0.0)
    }

    @Test
    fun aSingleDotIsAlwaysDecimal() {
        assertEquals(1.234, p("1.234")!!, 0.0)
        assertEquals(12.5, p("12.5")!!, 0.0)
    }

    @Test
    fun aSingleCommaIsGroupingOnlyBeforeExactlyThreeDigits() {
        assertEquals(1234.0, p("1,234")!!, 0.0)
        assertEquals(1.2345, p("1,2345")!!, 0.0)
        assertEquals(0.125, p("0,125")!!, 0.0)
        assertEquals(12.34, p("12,34")!!, 0.0)
    }

    @Test
    fun digitsOfOtherScriptsAreRead() {
        assertEquals(1234.0, p("١٢٣٤")!!, 0.0)
        assertEquals(1234.0, p("۱۲۳۴")!!, 0.0)
        assertEquals(1234.0, p("१२३४")!!, 0.0)
        assertEquals(1234.0, p("１２３４")!!, 0.0)
        assertEquals(1234.56, p("١٬٢٣٤٫٥٦")!!, 0.0)
    }

    @Test
    fun findsTheFirstNumberInSurroundingText() {
        assertEquals(1500.0, p("Gold: 1,500 coins")!!, 0.0)
        assertEquals(30.0, p("Level 30 / 50")!!, 0.0)
        assertEquals(5.0, p("x5")!!, 0.0)
        assertEquals(99.0, p("99%")!!, 0.0)
    }

    @Test
    fun textWithoutANumberGivesNull() {
        assertNull(p(""))
        assertNull(p("none"))
        assertNull(p("--"))
        assertNull(p(".,"))
    }

    @Test
    fun spacesBetweenShortNumbersDoNotJoinThem() {
        assertEquals(12.0, p("12 34")!!, 0.0)
    }
}
