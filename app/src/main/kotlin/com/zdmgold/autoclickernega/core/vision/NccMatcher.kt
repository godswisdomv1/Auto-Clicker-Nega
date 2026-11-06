package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.Limits
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Grayscale normalised cross-correlation, searched coarse to fine inside the search region. A coarse scan over a
 * downscaled copy picks a few candidates, and each is refined level by level down to full resolution. The score
 * ignores brightness and contrast changes, so a dark mode or a dimmed screen still matches.
 */
object NccMatcher {
    private const val MIN_COARSE_SIDE = 12
    private const val MAX_LEVELS = 4
    private const val FLAT_STD = 1.5
    private const val SAFE_CANDIDATES = 5
    private const val FAST_CANDIDATES = 2
    private const val SAFE_RADIUS = 2
    private const val FAST_RADIUS = 1
    private const val MASK = 0xFF

    fun find(
        f: Frame,
        t: Frame,
        spec: Condition.Image,
    ): Match? = search(f, t, spec).accepted

    /** Searches for [t] in [f]. The result carries the best candidate even when it misses the confidence. */
    fun search(
        f: Frame,
        t: Frame,
        spec: Condition.Image,
    ): MatchResult {
        val threshold = Limits.clampConfidence(spec.confidence)
        val region = (spec.region ?: f.bounds).intersect(f.bounds)
        if (region.isEmpty || region.width < t.w || region.height < t.h) return MatchResult(null, threshold)
        val levelCount = levelCount(t)
        val patch = Pyramid.levels(f.crop(region), levelCount)
        val templates = Pyramid.levels(t, levelCount).map(::prepare)
        val usable = templates.indexOfLast { !it.flat }.let { if (it < 0) -1 else minOf(it, patch.size - 1) }
        if (usable < 0 || templates[0].flat) return MatchResult(null, threshold)
        val fast = spec.mode == MatchMode.Fast
        val candidates =
            coarseCandidates(patch[usable], templates[usable], if (fast) FAST_CANDIDATES else SAFE_CANDIDATES)
        val radius = if (fast) FAST_RADIUS else SAFE_RADIUS
        var best: Candidate? = null
        for (candidate in candidates) {
            val refined = refine(candidate, patch, templates, usable, radius)
            if (best == null || refined.score > best.score) best = refined
        }
        val match =
            best?.let {
                val box = Rect(region.l + it.x, region.t + it.y, region.l + it.x + t.w, region.t + it.y + t.h)
                Match(it.score.toFloat().coerceIn(0f, 1f), box)
            }
        return MatchResult(match, threshold)
    }

    private class Candidate(
        val x: Int,
        val y: Int,
        val score: Double,
    )

    /** A template with its mean removed, ready for correlation. [flat] templates have no detail to match. */
    private class Prepared(
        val w: Int,
        val h: Int,
        val centered: DoubleArray,
        val norm: Double,
        val flat: Boolean,
    )

    private fun levelCount(t: Frame): Int {
        var n = 1
        while (n < MAX_LEVELS && (min(t.w, t.h) shr n) >= MIN_COARSE_SIDE) n++
        return n
    }

    private fun prepare(t: Frame): Prepared {
        val n = t.w * t.h
        var sum = 0.0
        for (b in t.px) sum += b.toInt() and MASK
        val mean = sum / n
        val centered = DoubleArray(n)
        var sq = 0.0
        for (i in 0 until n) {
            val d = (t.px[i].toInt() and MASK) - mean
            centered[i] = d
            sq += d * d
        }
        val norm = sqrt(sq)
        return Prepared(t.w, t.h, centered, norm, norm / sqrt(n.toDouble()) < FLAT_STD)
    }

    private fun coarseCandidates(
        f: Frame,
        t: Prepared,
        count: Int,
    ): List<Candidate> {
        val pw = f.w - t.w + 1
        val ph = f.h - t.h + 1
        val sums = integral(f, squared = false)
        val squares = integral(f, squared = true)
        val scores = FloatArray(pw * ph)
        val area = (t.w * t.h).toDouble()
        for (y in 0 until ph) {
            for (x in 0 until pw) {
                val s = rect(sums, f.w, x, y, t.w, t.h).toDouble()
                val q = rect(squares, f.w, x, y, t.w, t.h).toDouble()
                val variance = q - s * s / area
                scores[y * pw + x] =
                    if (variance <= 1e-9) 0f else (dot(f, x, y, t) / (sqrt(variance) * t.norm)).toFloat()
            }
        }
        val suppress = max(1, min(t.w, t.h) / 2)
        val picked = ArrayList<Candidate>(count)
        repeat(count) {
            var bestIndex = -1
            var bestScore = -Float.MAX_VALUE
            for (i in scores.indices) {
                if (scores[i] > bestScore) {
                    bestScore = scores[i]
                    bestIndex = i
                }
            }
            if (bestIndex >= 0 && bestScore > -Float.MAX_VALUE) {
                val bx = bestIndex % pw
                val by = bestIndex / pw
                picked += Candidate(bx, by, bestScore.toDouble())
                for (yy in max(0, by - suppress)..min(ph - 1, by + suppress)) {
                    for (xx in max(0, bx - suppress)..min(pw - 1, bx + suppress)) {
                        scores[yy * pw + xx] =
                            -Float.MAX_VALUE
                    }
                }
            }
        }
        return picked
    }

    private fun refine(
        start: Candidate,
        frames: List<Frame>,
        templates: List<Prepared>,
        from: Int,
        radius: Int,
    ): Candidate {
        var current = start
        for (level in from - 1 downTo 0) {
            current = bestAround(current.x * 2, current.y * 2, frames[level], templates[level], radius) ?: current
        }
        return current
    }

    /** The best position within [radius] pixels of ([cx], [cy]) at one pyramid level. */
    private fun bestAround(
        cx: Int,
        cy: Int,
        f: Frame,
        t: Prepared,
        radius: Int,
    ): Candidate? {
        var best: Candidate? = null
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = cx + dx
                val y = cy + dy
                if (!fits(f, x, y, t)) continue
                val s = nccDirect(f, x, y, t)
                if (best == null || s > best.score) best = Candidate(x, y, s)
            }
        }
        return best
    }

    private fun fits(
        f: Frame,
        x: Int,
        y: Int,
        t: Prepared,
    ): Boolean = x >= 0 && y >= 0 && x + t.w <= f.w && y + t.h <= f.h

    private fun nccDirect(
        f: Frame,
        x: Int,
        y: Int,
        t: Prepared,
    ): Double {
        var sum = 0.0
        var sq = 0.0
        var dot = 0.0
        for (j in 0 until t.h) {
            val row = (y + j) * f.w + x
            val trow = j * t.w
            for (i in 0 until t.w) {
                val v = (f.px[row + i].toInt() and MASK).toDouble()
                sum += v
                sq += v * v
                dot += v * t.centered[trow + i]
            }
        }
        val n = (t.w * t.h).toDouble()
        val variance = sq - sum * sum / n
        return if (variance <= 1e-9) 0.0 else dot / (sqrt(variance) * t.norm)
    }

    private fun dot(
        f: Frame,
        x: Int,
        y: Int,
        t: Prepared,
    ): Double {
        var dot = 0.0
        for (j in 0 until t.h) {
            val row = (y + j) * f.w + x
            val trow = j * t.w
            for (i in 0 until t.w) dot += (f.px[row + i].toInt() and MASK) * t.centered[trow + i]
        }
        return dot
    }

    /** A summed-area table with one extra row and column, so any rectangle sum is four lookups. */
    private fun integral(
        f: Frame,
        squared: Boolean,
    ): LongArray {
        val w1 = f.w + 1
        val out = LongArray(w1 * (f.h + 1))
        for (y in 0 until f.h) {
            var rowSum = 0L
            for (x in 0 until f.w) {
                val v = (f.px[y * f.w + x].toInt() and MASK).toLong()
                rowSum += if (squared) v * v else v
                out[(y + 1) * w1 + x + 1] = out[y * w1 + x + 1] + rowSum
            }
        }
        return out
    }

    private fun rect(
        table: LongArray,
        width: Int,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
    ): Long {
        val w1 = width + 1
        return table[(y + h) * w1 + x + w] - table[y * w1 + x + w] - table[(y + h) * w1 + x] + table[y * w1 + x]
    }
}
