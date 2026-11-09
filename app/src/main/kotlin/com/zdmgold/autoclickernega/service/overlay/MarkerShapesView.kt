package com.zdmgold.autoclickernega.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import com.zdmgold.autoclickernega.core.model.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws every marker over the screen: circles for taps, arrows for swipes and curves, corner brackets for image
 * targets. It never takes touches. The draggable handles are small separate windows, so the rest of the screen
 * stays usable while markers are being placed.
 */
@SuppressLint("ViewConstructor")
class MarkerShapesView(
    context: Context,
    private val theme: OverlayTheme,
) : View(context) {
    var markers: List<Marker> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val ring =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.dpf(STROKE_DP)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        for (m in markers) {
            when (m) {
                is Marker.Tap -> drawTap(canvas, m)
                is Marker.Swipe -> drawLine(canvas, listOf(m.from, m.to), theme.markerSwipe)
                is Marker.Curve -> drawCurve(canvas, m)
                is Marker.Image -> drawBrackets(canvas, m)
            }
        }
    }

    private fun drawTap(
        canvas: Canvas,
        m: Marker.Tap,
    ) {
        val r = theme.dpf(TAP_RADIUS_DP)
        fill.color = withAlpha(theme.markerTap, FILL_ALPHA)
        canvas.drawCircle(m.at.x.toFloat(), m.at.y.toFloat(), r, fill)
        ring.color = theme.markerTap
        canvas.drawCircle(m.at.x.toFloat(), m.at.y.toFloat(), r, ring)
    }

    private fun drawLine(
        canvas: Canvas,
        points: List<Point>,
        color: Int,
    ) {
        ring.color = color
        fill.color = color
        val from = points.first()
        val to = points.last()
        val path =
            if (points.size > 2) {
                smooth(points)
            } else {
                Path().apply {
                    moveTo(from.x.toFloat(), from.y.toFloat())
                    lineTo(to.x.toFloat(), to.y.toFloat())
                }
            }
        canvas.drawPath(path, ring)
        canvas.drawCircle(from.x.toFloat(), from.y.toFloat(), theme.dpf(START_DOT_DP), fill)
        val before = points[points.size - 2]
        arrowHead(canvas, before, to)
    }

    private fun drawCurve(
        canvas: Canvas,
        m: Marker.Curve,
    ) = drawLine(canvas, m.points, theme.markerSwipe)

    private fun smooth(points: List<Point>): Path {
        val path = Path()
        path.moveTo(points.first().x.toFloat(), points.first().y.toFloat())
        for (i in 1 until points.size - 1) {
            val p = points[i]
            val next = points[i + 1]
            path.quadTo(p.x.toFloat(), p.y.toFloat(), (p.x + next.x) / 2f, (p.y + next.y) / 2f)
        }
        path.lineTo(points.last().x.toFloat(), points.last().y.toFloat())
        return path
    }

    private fun arrowHead(
        canvas: Canvas,
        from: Point,
        to: Point,
    ) {
        val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
        val length = theme.dpf(ARROW_DP)
        for (side in listOf(-ARROW_SPREAD, ARROW_SPREAD)) {
            val a = angle + Math.PI + side
            canvas.drawLine(
                to.x.toFloat(),
                to.y.toFloat(),
                to.x + (cos(a) * length).toFloat(),
                to.y + (sin(a) * length).toFloat(),
                ring,
            )
        }
    }

    private fun drawBrackets(
        canvas: Canvas,
        m: Marker.Image,
    ) {
        val r = m.rect
        val arm = minOf(theme.dpf(BRACKET_DP), r.width / MAX_ARM_FRACTION, r.height / MAX_ARM_FRACTION)
        fill.color = withAlpha(theme.markerImage, FAINT_ALPHA)
        canvas.drawRect(r.l.toFloat(), r.t.toFloat(), r.r.toFloat(), r.b.toFloat(), fill)
        ring.color = theme.markerImage
        val l = r.l.toFloat()
        val t = r.t.toFloat()
        val rr = r.r.toFloat()
        val b = r.b.toFloat()
        canvas.drawLine(l, t, l + arm, t, ring)
        canvas.drawLine(l, t, l, t + arm, ring)
        canvas.drawLine(rr, t, rr - arm, t, ring)
        canvas.drawLine(rr, t, rr, t + arm, ring)
        canvas.drawLine(rr, b, rr - arm, b, ring)
        canvas.drawLine(rr, b, rr, b - arm, ring)
        canvas.drawLine(l, b, l + arm, b, ring)
        canvas.drawLine(l, b, l, b - arm, ring)
    }

    private fun withAlpha(
        color: Int,
        alpha: Int,
    ): Int = (color and RGB_MASK) or (alpha shl ALPHA_SHIFT)

    @Suppress("unused")
    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private companion object {
        const val STROKE_DP = 3f
        const val TAP_RADIUS_DP = 22f
        const val START_DOT_DP = 6f
        const val ARROW_DP = 14f
        const val ARROW_SPREAD = 0.45
        const val BRACKET_DP = 20f
        const val MAX_ARM_FRACTION = 3f
        const val FILL_ALPHA = 0x40
        const val FAINT_ALPHA = 0x1A
        const val RGB_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
    }
}
