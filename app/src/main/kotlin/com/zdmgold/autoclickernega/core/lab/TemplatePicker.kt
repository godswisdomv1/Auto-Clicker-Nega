package com.zdmgold.autoclickernega.core.lab

import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.vision.Frame

/**
 * Picks the most detailed patch of a screen to use as a reference image, so the quick test needs no cropping.
 * Detail is the spread of gray levels inside the patch: a flat area cannot be matched, a busy one can.
 */
object TemplatePicker {
    private const val STEP = 4
    private const val WINDOW_STRIDE = 2

    /**
     * The [width] by [height] patch with the most detail, or null when the patch does not fit. The top and bottom
     * [verticalMarginPx] of the screen are skipped, because the clock and the navigation bar sit there.
     */
    fun pick(
        frame: Frame,
        width: Int,
        height: Int,
        verticalMarginPx: Int = 0,
    ): Rect? {
        val sw = frame.w / STEP
        val sh = frame.h / STEP
        val ww = width / STEP
        val wh = height / STEP
        val margin = verticalMarginPx / STEP
        val fits = ww in 1..sw && wh in 1..(sh - 2 * margin)
        if (!fits) return null

        // Sums over a grid of every fourth pixel, so any patch can be scored in constant time.
        val stride = sw + 1
        val sum = LongArray(stride * (sh + 1))
        val squares = LongArray(stride * (sh + 1))
        for (y in 0 until sh) {
            var rowSum = 0L
            var rowSquares = 0L
            for (x in 0 until sw) {
                val v = frame.gray(x * STEP, y * STEP).toLong()
                rowSum += v
                rowSquares += v * v
                sum[(y + 1) * stride + x + 1] = sum[y * stride + x + 1] + rowSum
                squares[(y + 1) * stride + x + 1] = squares[y * stride + x + 1] + rowSquares
            }
        }

        val n = (ww * wh).toDouble()
        var best = -1.0
        var bestX = 0
        var bestY = 0
        for (y in margin..(sh - wh - margin) step WINDOW_STRIDE) {
            for (x in 0..(sw - ww) step WINDOW_STRIDE) {
                val s = area(sum, stride, x, y, ww, wh)
                val q = area(squares, stride, x, y, ww, wh)
                val variance = q / n - (s / n) * (s / n)
                if (variance > best) {
                    best = variance
                    bestX = x
                    bestY = y
                }
            }
        }
        return Rect(bestX * STEP, bestY * STEP, (bestX + ww) * STEP, (bestY + wh) * STEP)
    }

    private fun area(
        table: LongArray,
        stride: Int,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
    ): Long =
        table[(y + h) * stride + x + w] - table[y * stride + x + w] - table[(y + h) * stride + x] +
            table[y * stride + x]
}
