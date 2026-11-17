package com.zdmgold.autoclickernega.platform.display

import android.content.Context
import android.view.WindowManager
import com.zdmgold.autoclickernega.core.model.ScreenSpec

/** The screen the app is on, in the same terms the accessibility service uses, so a run is rescaled correctly. */
object DisplayInfo {
    fun screenSpec(context: Context): ScreenSpec {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return ScreenSpec(bounds.width(), bounds.height(), context.resources.displayMetrics.densityDpi)
    }
}
