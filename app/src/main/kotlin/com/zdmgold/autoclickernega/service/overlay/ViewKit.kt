package com.zdmgold.autoclickernega.service.overlay

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** Opacity-only press feedback, with no ripple, like the rest of the product. Clicks still reach the listener. */
@SuppressLint("ClickableViewAccessibility")
internal fun View.pressFeedback() {
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.alpha = Tokens.PRESSED_ALPHA
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
        }
        false
    }
}

internal fun roundedFill(
    color: Int,
    radiusPx: Float,
): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPx
    }
