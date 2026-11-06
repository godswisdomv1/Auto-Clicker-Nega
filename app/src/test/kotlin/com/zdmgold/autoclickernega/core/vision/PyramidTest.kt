package com.zdmgold.autoclickernega.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class PyramidTest {
    @Test
    fun eachLevelHalvesTheSize() {
        val levels = Pyramid.levels(SyntheticFrames.flat(100, 60, 50), 3)
        assertEquals(listOf(100 to 60, 50 to 30, 25 to 15), levels.map { it.w to it.h })
    }

    @Test
    fun aFlatImageStaysFlat() {
        val levels = Pyramid.levels(SyntheticFrames.flat(64, 64, 200), 4)
        levels.forEach { l -> assertEquals(200, l.gray(l.w / 2, l.h / 2)) }
    }

    @Test
    fun blocksAreAveraged() {
        val f = Frame(2, 2, byteArrayOf(10, 20, 30, 41))
        assertEquals(25, Pyramid.half(f).gray(0, 0))
    }

    @Test
    fun stopsWhenTheImageCannotShrinkFurther() {
        assertEquals(3, Pyramid.levels(SyntheticFrames.flat(4, 4, 1), 10).size)
        assertEquals(1, Pyramid.levels(SyntheticFrames.flat(1, 1, 1), 5).size)
    }

    @Test
    fun oddSizesRoundDown() {
        val l = Pyramid.levels(SyntheticFrames.flat(101, 51, 7), 2)[1]
        assertEquals(50 to 25, l.w to l.h)
    }
}
