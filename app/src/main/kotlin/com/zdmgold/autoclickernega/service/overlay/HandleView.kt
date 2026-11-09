package com.zdmgold.autoclickernega.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View

/** A small draggable dot at one point of a marker. The first handle of a marker carries its number. */
@SuppressLint("ViewConstructor")
class HandleView(
    context: Context,
    private val theme: OverlayTheme,
    private val color: Int,
    private val label: String?,
    private val description: String,
) : View(context) {
    private val fill =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = this@HandleView.color
        }
    private val edge =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.dpf(EDGE_DP)
            this.color = theme.background
        }
    private val text =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = theme.onAccent
            textAlign = Paint.Align.CENTER
            typeface = theme.semibold
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SP, resources.displayMetrics)
        }

    init {
        contentDescription = description
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = theme.dpf(if (label != null) NUMBERED_DP else PLAIN_DP)
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, edge)
        if (label != null) canvas.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text)
    }

    private companion object {
        const val EDGE_DP = 1.5f
        const val NUMBERED_DP = 12f
        const val PLAIN_DP = 7f
        const val TEXT_SP = 12f
    }
}
