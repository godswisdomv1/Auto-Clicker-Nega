package com.zdmgold.autoclickernega.core.vision

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Resizes a reference image by the density ratio between the running screen and the authoring screen. */
object TemplateScaler {
    private const val NEAR_ONE = 0.01f
    private const val MASK = 0xFF

    fun scale(
        t: Frame,
        ratio: Float,
    ): Frame {
        if (ratio <= 0f || kotlin.math.abs(ratio - 1f) < NEAR_ONE) return t
        val nw = max(1, (t.w * ratio).roundToInt())
        val nh = max(1, (t.h * ratio).roundToInt())
        if (nw == t.w && nh == t.h) return t
        return if (ratio < 1f) areaAverage(t, nw, nh) else bilinear(t, nw, nh)
    }

    /** Shrinking averages every source pixel that falls inside a destination pixel, so detail is not lost to aliasing. */
    private fun areaAverage(
        t: Frame,
        nw: Int,
        nh: Int,
    ): Frame {
        val out = ByteArray(nw * nh)
        val sx = t.w.toDouble() / nw
        val sy = t.h.toDouble() / nh
        for (y in 0 until nh) {
            val y0 = floor(y * sy).toInt()
            val y1 = max(y0 + 1, floor((y + 1) * sy).toInt()).coerceAtMost(t.h)
            for (x in 0 until nw) {
                val x0 = floor(x * sx).toInt()
                val x1 = max(x0 + 1, floor((x + 1) * sx).toInt()).coerceAtMost(t.w)
                out[y * nw + x] = (blockSum(t, x0, x1, y0, y1) / ((y1 - y0) * (x1 - x0))).toByte()
            }
        }
        return Frame(nw, nh, out)
    }

    private fun blockSum(
        t: Frame,
        x0: Int,
        x1: Int,
        y0: Int,
        y1: Int,
    ): Int {
        var sum = 0
        for (yy in y0 until y1) {
            for (xx in x0 until x1) sum += t.px[yy * t.w + xx].toInt() and MASK
        }
        return sum
    }

    private fun bilinear(
        t: Frame,
        nw: Int,
        nh: Int,
    ): Frame {
        val out = ByteArray(nw * nh)
        for (y in 0 until nh) {
            val fy = ((y + 0.5) * t.h / nh - 0.5).coerceIn(0.0, (t.h - 1).toDouble())
            val y0 = floor(fy).toInt()
            val y1 = (y0 + 1).coerceAtMost(t.h - 1)
            val wy = fy - y0
            for (x in 0 until nw) {
                val fx = ((x + 0.5) * t.w / nw - 0.5).coerceIn(0.0, (t.w - 1).toDouble())
                val x0 = floor(fx).toInt()
                val x1 = (x0 + 1).coerceAtMost(t.w - 1)
                val wx = fx - x0
                val top = t.gray(x0, y0) * (1 - wx) + t.gray(x1, y0) * wx
                val bottom = t.gray(x0, y1) * (1 - wx) + t.gray(x1, y1) * wx
                out[y * nw + x] = (top * (1 - wy) + bottom * wy).roundToInt().toByte()
            }
        }
        return Frame(nw, nh, out)
    }
}
