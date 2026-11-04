package com.zdmgold.autoclickernega.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Durations and easing. Soft motion, no spring and no bounce anywhere. */
object Motion {
    const val PUSH_MS = 350
    const val POP_MS = 280
    const val SHEET_MS = 400
    const val PRESS_MS = 150
    const val REVEAL_MS = 300
    const val THEME_TOGGLE_MS = 200

    /** Screen slide distance, in pixels, paired with a fade. */
    const val SLIDE_PX = 20

    val easeInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val easeOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    /** With reduced motion every transition becomes an instant change. */
    fun duration(
        ms: Int,
        reduced: Boolean,
    ): Int = if (reduced) 0 else ms

    /** Reduced motion means the system animator scale is zero. */
    fun isReduced(animatorScale: Float): Boolean = animatorScale == 0f

    fun readAnimatorScale(resolver: ContentResolver): Float =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}

val LocalReduceMotion = staticCompositionLocalOf { false }

/** Follows the system animator scale live, so changing it takes effect without a restart. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(Motion.isReduced(Motion.readAnimatorScale(resolver))) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = Motion.isReduced(Motion.readAnimatorScale(resolver))
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}
