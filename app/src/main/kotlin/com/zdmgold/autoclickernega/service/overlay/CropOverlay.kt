package com.zdmgold.autoclickernega.service.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.ui.theme.Tokens

/**
 * Dims the screen and lets the user drag a rectangle over what to find. Drag anywhere to draw it, then drag a
 * corner to resize it or its middle to move it. The picture itself was taken before the dimming appeared.
 */
class CropOverlay(
    private val service: AccessibilityService,
    private val theme: OverlayTheme,
    private val metrics: ScreenMetrics,
) {
    private val windowManager: WindowManager = service.getSystemService(WindowManager::class.java)
    private var root: FrameLayout? = null

    val isShown: Boolean get() = root != null

    /** Shows the tool. [onResult] gets the chosen area in screen pixels, or null when the user cancels. */
    fun show(onResult: (Rect?) -> Unit) {
        if (root != null) return
        val bounds = metrics.bounds()
        val view = CropView(service, theme)
        val container = FrameLayout(service)
        container.addView(
            view,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )
        container.addView(hint(), hintParams())
        val confirm =
            actionButton(R.drawable.ic_check, R.string.crop_confirm, theme.accent, theme.onAccent) {
                view.selection?.let { onResult(it) }
            }
        val cancel =
            actionButton(R.drawable.ic_close, R.string.crop_cancel, theme.surface, theme.text) { onResult(null) }
        view.onSelectionChanged = { confirm.alpha = if (view.selection != null) 1f else DISABLED_ALPHA }
        confirm.alpha = DISABLED_ALPHA
        container.addView(cancel, buttonParams(Gravity.BOTTOM or Gravity.START))
        container.addView(confirm, buttonParams(Gravity.BOTTOM or Gravity.END))
        val params =
            WindowManager
                .LayoutParams(
                    bounds.width(),
                    bounds.height(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.START }
        try {
            windowManager.addView(container, params)
            root = container
        } catch (_: WindowManager.BadTokenException) {
            onResult(null)
        } catch (_: IllegalStateException) {
            onResult(null)
        }
    }

    fun hide() {
        root?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                // Already gone.
            }
        }
        root = null
    }

    private fun hint(): TextView =
        TextView(service).apply {
            setText(R.string.crop_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, HINT_SP)
            setTextColor(theme.text)
            typeface = theme.semibold
            background = roundedFill(theme.background, theme.dpf(Tokens.Radius.chip))
            val pad = theme.dp(HINT_PADDING_DP)
            setPadding(pad * 2, pad, pad * 2, pad)
        }

    private fun hintParams() =
        FrameLayout
            .LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply { topMargin = theme.dp(HINT_TOP_DP) }

    private fun buttonParams(gravity: Int) =
        FrameLayout.LayoutParams(theme.dp(BUTTON_DP), theme.dp(BUTTON_DP), gravity).apply {
            val margin = theme.dp(BUTTON_MARGIN_DP)
            setMargins(margin, margin, margin, margin * 2)
        }

    private fun actionButton(
        icon: Int,
        description: Int,
        fill: Int,
        tint: Int,
        onClick: () -> Unit,
    ): ImageView =
        ImageView(service).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(tint)
            val inset = theme.dp((BUTTON_DP - ICON_DP) / 2)
            setPadding(inset, inset, inset, inset)
            background = roundedFill(fill, theme.dpf(Tokens.Radius.button))
            contentDescription = service.getString(description)
            setOnClickListener { onClick() }
            pressFeedback()
        }

    private companion object {
        const val BUTTON_DP = 56
        const val ICON_DP = 24
        const val BUTTON_MARGIN_DP = 24
        const val HINT_SP = 14f
        const val HINT_PADDING_DP = 8
        const val HINT_TOP_DP = 72
        const val DISABLED_ALPHA = 0.4f
    }
}

/** The dimmed surface with the selection rectangle. A selection smaller than a usable target is not reported. */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
private class CropView(
    context: Context,
    private val theme: OverlayTheme,
) : View(context) {
    var onSelectionChanged: () -> Unit = {}

    /** The chosen area, or null while there is none or it is too small to be a target. */
    val selection: Rect?
        get() = rect?.takeIf { it.width >= minSide && it.height >= minSide }

    private var rect: Rect? = null
    private var drag: Drag? = null
    private val minSide = theme.dp(MIN_SIDE_DP)
    private val hit = theme.dpf(HIT_DP)
    private val dim = Paint().apply { color = theme.scrim }
    private val line =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.dpf(LINE_DP)
            color = theme.accent
        }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.accent }

    private class Drag(
        val kind: Int,
        val startX: Float,
        val startY: Float,
        val origin: Rect?,
    )

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = rect
        if (r == null) {
            canvas.drawRect(0f, 0f, w, h, dim)
            return
        }
        canvas.drawRect(0f, 0f, w, r.t.toFloat(), dim)
        canvas.drawRect(0f, r.b.toFloat(), w, h, dim)
        canvas.drawRect(0f, r.t.toFloat(), r.l.toFloat(), r.b.toFloat(), dim)
        canvas.drawRect(r.r.toFloat(), r.t.toFloat(), w, r.b.toFloat(), dim)
        canvas.drawRect(r.l.toFloat(), r.t.toFloat(), r.r.toFloat(), r.b.toFloat(), line)
        for (p in corners(r)) canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), theme.dpf(DOT_DP), dot)
    }

    private fun corners(r: Rect) = listOf(Point(r.l, r.t), Point(r.r, r.t), Point(r.r, r.b), Point(r.l, r.b))

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> drag = begin(x, y)
            MotionEvent.ACTION_MOVE -> drag?.let { move(it, x, y) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> drag = null
        }
        return true
    }

    private fun begin(
        x: Float,
        y: Float,
    ): Drag {
        val r = rect ?: return Drag(NEW, x, y, null)
        val corner = corners(r).indexOfFirst { kotlin.math.hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= hit }
        return when {
            corner >= 0 -> Drag(corner, x, y, r)
            x >= r.l && x <= r.r && y >= r.t && y <= r.b -> Drag(MOVE, x, y, r)
            else -> Drag(NEW, x, y, null)
        }
    }

    private fun move(
        d: Drag,
        x: Float,
        y: Float,
    ) {
        val cx = x.coerceIn(0f, width.toFloat()).toInt()
        val cy = y.coerceIn(0f, height.toFloat()).toInt()
        val origin = d.origin
        rect =
            when {
                d.kind == NEW -> {
                    normalized(d.startX.toInt(), d.startY.toInt(), cx, cy)
                }

                d.kind == MOVE && origin != null -> {
                    shifted(origin, (x - d.startX).toInt(), (y - d.startY).toInt())
                }

                origin != null -> {
                    val opposite = corners(origin)[(d.kind + 2) % CORNER_COUNT]
                    normalized(opposite.x, opposite.y, cx, cy)
                }

                else -> {
                    rect
                }
            }
        invalidate()
        onSelectionChanged()
    }

    private fun normalized(
        ax: Int,
        ay: Int,
        bx: Int,
        by: Int,
    ) = Rect(minOf(ax, bx), minOf(ay, by), maxOf(ax, bx), maxOf(ay, by))

    private fun shifted(
        r: Rect,
        dx: Int,
        dy: Int,
    ): Rect {
        val nx = dx.coerceIn(-r.l, width - r.r)
        val ny = dy.coerceIn(-r.t, height - r.b)
        return Rect(r.l + nx, r.t + ny, r.r + nx, r.b + ny)
    }

    private companion object {
        const val NEW = -1
        const val MOVE = -2
        const val CORNER_COUNT = 4
        const val MIN_SIDE_DP = 24
        const val HIT_DP = 36f
        const val LINE_DP = 2f
        const val DOT_DP = 7f
    }
}
