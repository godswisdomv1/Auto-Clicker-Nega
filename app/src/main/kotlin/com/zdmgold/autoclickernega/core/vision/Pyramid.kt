package com.zdmgold.autoclickernega.core.vision

/** Downscaled copies of a frame for a coarse-to-fine search. Level 0 is the frame itself. */
object Pyramid {
    fun levels(
        f: Frame,
        n: Int,
    ): List<Frame> {
        val out = ArrayList<Frame>(n)
        out += f
        while (out.size < n) {
            val last = out.last()
            if (last.w < 2 || last.h < 2) break
            out += half(last)
        }
        return out
    }

    /** Averages each 2 by 2 block of grayscale pixels. */
    fun half(f: Frame): Frame {
        val nw = f.w / 2
        val nh = f.h / 2
        val out = ByteArray(nw * nh)
        for (y in 0 until nh) {
            val r0 = (2 * y) * f.w
            val r1 = r0 + f.w
            for (x in 0 until nw) {
                val a = f.px[r0 + 2 * x].toInt() and MASK
                val b = f.px[r0 + 2 * x + 1].toInt() and MASK
                val c = f.px[r1 + 2 * x].toInt() and MASK
                val d = f.px[r1 + 2 * x + 1].toInt() and MASK
                out[y * nw + x] = ((a + b + c + d + 2) shr 2).toByte()
            }
        }
        return Frame(nw, nh, out)
    }

    private const val MASK = 0xFF
}
