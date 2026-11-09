package com.zdmgold.autoclickernega.service.overlay

import android.content.Context
import android.graphics.Rect
import android.view.WindowManager
import com.zdmgold.autoclickernega.core.model.ScreenSpec

/** The screen as it is right now: size in pixels, density and orientation. Rotation changes all of them. */
class ScreenMetrics(
    private val context: Context,
) {
    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)

    /** The full screen in pixels, including the system bars, in the same coordinates a screenshot uses. */
    fun bounds(): Rect = windowManager.currentWindowMetrics.bounds

    fun current(): ScreenSpec {
        val b = bounds()
        return ScreenSpec(b.width(), b.height(), context.resources.displayMetrics.densityDpi)
    }
}
