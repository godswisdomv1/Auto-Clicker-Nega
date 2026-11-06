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

        /** Builds a frame from 0xAARRGGBB pixels. Colour is kept only when [keepColor] is true. */
        fun fromArgb(
            w: Int,
            h: Int,
            argb: IntArray,
            keepColor: Boolean = false,
        ): Frame {
            val gray = ByteArray(w * h)
            val color = if (keepColor) IntArray(w * h) else null
            for (i in gray.indices) {
                val p = argb[i]
                val r = (p shr 16) and BYTE_MASK
                val g = (p shr 8) and BYTE_MASK
                val b = p and BYTE_MASK
                gray[i] = ((r * R_WEIGHT + g * G_WEIGHT + b * B_WEIGHT) shr SHIFT).toByte()
                color?.set(i, p and 0xFFFFFF)
            }
            return Frame(w, h, gray, color)
        }
    }
}
