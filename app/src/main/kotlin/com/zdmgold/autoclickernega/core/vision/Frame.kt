package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect

/**
 * One captured screen. [px] is grayscale, one byte per pixel in row order, and drives image matching.
 * [rgb] holds full colour (0xRRGGBB per pixel) only when a script needs a colour condition. A frame is
 * processed in memory and never stored or sent.
 */
class Frame(
    val w: Int,
    val h: Int,
    val px: ByteArray,
    val rgb: IntArray? = null,
) {
    init {
        require(w > 0 && h > 0) { "a frame needs a positive size" }
        require(px.size == w * h) { "pixel data does not match the size" }
        require(rgb == null || rgb.size == w * h) { "colour data does not match the size" }
    }

    val bounds: Rect get() = Rect(0, 0, w, h)

    fun gray(
        x: Int,
        y: Int,
    ): Int = px[y * w + x].toInt() and BYTE_MASK

    /** The colour at a point as 0xRRGGBB, or null when this frame carries no colour or the point is off screen. */
    fun colorAt(p: Point): Int? =
        if (rgb == null || p.x !in 0 until w || p.y !in 0 until h) null else rgb[p.y * w + p.x]

    /** A copy of [r], limited to the frame. */
    fun crop(r: Rect): Frame {
        val c = r.intersect(bounds)
        require(!c.isEmpty) { "nothing to crop" }
        val out = ByteArray(c.width * c.height)
        val outRgb = rgb?.let { IntArray(out.size) }
        for (y in 0 until c.height) {
            val from = (c.t + y) * w + c.l
            px.copyInto(out, y * c.width, from, from + c.width)
            if (rgb != null && outRgb != null) rgb.copyInto(outRgb, y * c.width, from, from + c.width)
        }
        return Frame(c.width, c.height, out, outRgb)
    }

    /** Overwrites [r] in place with a flat value. Used to hide the app's own panel before matching. */
    fun fill(
        r: Rect,
        gray: Int = 0,
        color: Int = 0,
    ) {
        val c = r.intersect(bounds)
        for (y in c.t until c.b) {
            for (x in c.l until c.r) {
                px[y * w + x] = gray.toByte()
                rgb?.set(y * w + x, color)
            }
        }
    }

    companion object {
        private const val BYTE_MASK = 0xFF
        private const val R_WEIGHT = 77
        private const val G_WEIGHT = 151
        private const val B_WEIGHT = 28
        private const val SHIFT = 8
        private const val RGB_MASK = 0xFFFFFF

        /** The gray level of one 0xAARRGGBB pixel, from 0 to 255. */
        fun grayOf(argb: Int): Int {
            val r = (argb shr 16) and BYTE_MASK
            val g = (argb shr 8) and BYTE_MASK
            val b = argb and BYTE_MASK
            return (r * R_WEIGHT + g * G_WEIGHT + b * B_WEIGHT) shr SHIFT
        }

        /** Builds a frame from 0xAARRGGBB pixels. Colour is kept only when [keepColor] is true. */
        fun fromArgb(
            w: Int,
            h: Int,
            argb: IntArray,
            keepColor: Boolean = false,
        ): Frame =
            fromRows(w, h, keepColor, ROWS_PER_BAND) { band, startRow, rows ->
                argb.copyInto(band, 0, startRow * w, (startRow + rows) * w)
            }

        private const val ROWS_PER_BAND = 64

        /**
         * Builds a frame by reading the picture a band of rows at a time, so a large screenshot never needs a second
         * full-size copy of its pixels. [readRows] must fill the first rows * w entries of the band it is given.
         */
        fun fromRows(
            w: Int,
            h: Int,
            keepColor: Boolean,
            rowsPerBand: Int,
            readRows: (band: IntArray, startRow: Int, rows: Int) -> Unit,
        ): Frame {
            val gray = ByteArray(w * h)
            val color = if (keepColor) IntArray(w * h) else null
            val band = IntArray(w * minOf(rowsPerBand, h))
            var y = 0
            while (y < h) {
                val rows = minOf(rowsPerBand, h - y)
                readRows(band, y, rows)
                for (i in 0 until w * rows) {
                    gray[y * w + i] = grayOf(band[i]).toByte()
                    color?.set(y * w + i, band[i] and RGB_MASK)
                }
                y += rows
            }
            return Frame(w, h, gray, color)
        }
    }
}
