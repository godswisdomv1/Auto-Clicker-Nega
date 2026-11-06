package com.zdmgold.autoclickernega.core.vision

import com.zdmgold.autoclickernega.core.model.Rect

/** A cheap measure of whether the screen changed, used to slow detection down on a still screen. */
object FrameDiff {
    private const val STEP = 8
    private const val PIXEL_DELTA = 12
    private const val CHANGED_FRACTION = 0.01
    private const val MASK = 0xFF

    /** True when the frames differ in size, or when more than 1 percent of sampled pixels moved noticeably. */
    fun changed(
        a: Frame,
        b: Frame,
        region: Rect? = null,
    ): Boolean {
        if (a.w != b.w || a.h != b.h) return true
        val area = (region ?: a.bounds).intersect(a.bounds)
        if (area.isEmpty) return false
        var sampled = 0
        var moved = 0
        var y = area.t
        while (y < area.b) {
            var x = area.l
            while (x < area.r) {
                val i = y * a.w + x
                val d = (a.px[i].toInt() and MASK) - (b.px[i].toInt() and MASK)
                if (d > PIXEL_DELTA || d < -PIXEL_DELTA) moved++
                sampled++
                x += STEP
            }
            y += STEP
        }
        return moved > sampled * CHANGED_FRACTION
    }
}
