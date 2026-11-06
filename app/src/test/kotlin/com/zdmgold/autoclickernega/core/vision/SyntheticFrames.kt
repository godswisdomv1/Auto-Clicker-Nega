package com.zdmgold.autoclickernega.core.vision

import java.util.Random

/** Generates frames with and without targets, plus the distortions a real screen goes through. */
object SyntheticFrames {
    private const val MASK = 0xFF

    /** A busy, UI-like background: a soft gradient, many flat rectangles and a little noise. */
    fun background(
        w: Int,
        h: Int,
        seed: Long,
    ): Frame {
        val rnd = Random(seed)
        val px = IntArray(w * h) { i -> 90 + ((i % w) * 40 / w) + ((i / w) * 40 / h) }
        repeat(60) {
            val rw = 16 + rnd.nextInt(w / 3)
            val rh = 16 + rnd.nextInt(h / 8)
            val rx = rnd.nextInt(w - rw)
            val ry = rnd.nextInt(h - rh)
            val gray = 20 + rnd.nextInt(215)
            for (y in ry until ry + rh) for (x in rx until rx + rw) px[y * w + x] = gray
        }
        for (i in px.indices) px[i] = (px[i] + rnd.nextInt(13) - 6).coerceIn(0, 255)
        return Frame(w, h, ByteArray(px.size) { px[it].toByte() })
    }

    /** A distinctive target: random high contrast blocks with fine noise. Different seeds give different targets. */
    fun pattern(
        w: Int,
        h: Int,
        seed: Long,
    ): Frame {
        val rnd = Random(seed)
        val block = 6
        val bw = (w + block - 1) / block
        val bh = (h + block - 1) / block
        val levels = IntArray(bw * bh) { if (rnd.nextBoolean()) 30 + rnd.nextInt(60) else 170 + rnd.nextInt(70) }
        val px =
            ByteArray(w * h) { i ->
                val v = levels[(i / w / block) * bw + (i % w) / block] + rnd.nextInt(7) - 3
                v.coerceIn(0, 255).toByte()
            }
        return Frame(w, h, px)
    }

    fun embed(
        f: Frame,
        t: Frame,
        x: Int,
        y: Int,
    ): Frame {
        val out = f.px.copyOf()
        for (j in 0 until t.h) t.px.copyInto(out, (y + j) * f.w + x, j * t.w, (j + 1) * t.w)
        return Frame(f.w, f.h, out)
    }

    fun adjust(
        f: Frame,
        gain: Double,
        delta: Int,
    ): Frame =
        Frame(
            f.w,
            f.h,
            ByteArray(f.px.size) {
                (((f.px[it].toInt() and MASK) * gain) + delta).toInt().coerceIn(0, 255).toByte()
            },
        )

    fun noisy(
        f: Frame,
        sigma: Double,
        seed: Long,
    ): Frame {
        val rnd = Random(seed)
        return Frame(
            f.w,
            f.h,
            ByteArray(
                f.px.size,
            ) { ((f.px[it].toInt() and MASK) + (rnd.nextGaussian() * sigma).toInt()).coerceIn(0, 255).toByte() },
        )
    }

    fun flat(
        w: Int,
        h: Int,
        gray: Int,
    ): Frame = Frame(w, h, ByteArray(w * h) { gray.toByte() })

    fun colored(
        w: Int,
        h: Int,
        rgb: Int,
    ): Frame {
        val argb = IntArray(w * h) { 0xFF000000.toInt() or rgb }
        return Frame.fromArgb(w, h, argb, keepColor = true)
    }
}
