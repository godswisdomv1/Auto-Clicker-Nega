package com.zdmgold.autoclickernega.service.overlay

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.zdmgold.autoclickernega.core.model.Point

/**
 * Puts markers on the live screen. The shapes are drawn by one window that ignores touches, and each draggable
 * handle is its own small window, so the rest of the screen, and the app under it, stays usable. Dragging a handle
 * edits its marker, and holding one removes the marker.
 */
class MarkerLayer(
    service: AccessibilityService,
    private val theme: OverlayTheme,
    private val metrics: ScreenMetrics,
) {
    private class Handle(
        val view: HandleView,
        val params: WindowManager.LayoutParams,
        val markerIndex: Int,
        val handleIndex: Int,
    )

    private val context = service
    private val windowManager: WindowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(service).scaledTouchSlop
    private val longPressMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val handleSize = theme.dp(HANDLE_DP)

    private var shapes: MarkerShapesView? = null
    private val handles = mutableListOf<Handle>()
    private var markers: List<Marker> = emptyList()
    private var hidden = false

    /** Called after a marker was moved, resized or removed. */
    var onChanged: (List<Marker>) -> Unit = {}

    val isShown: Boolean get() = shapes != null

    fun show(markers: List<Marker>) {
        this.markers = markers
        if (shapes == null) addShapes()
        shapes?.markers = markers
        rebuildHandles()
    }

    fun hide() {
        removeHandles()
        shapes?.let { removeView(it) }
        shapes = null
    }

    /** Makes everything invisible for a moment, so a screenshot does not include the markers. */
    fun setHidden(hide: Boolean) {
        hidden = hide
        val visibility = if (hide) View.INVISIBLE else View.VISIBLE
        shapes?.visibility = visibility
        handles.forEach { it.view.visibility = visibility }
    }

    private fun addShapes() {
        val view = MarkerShapesView(context, theme)
        val bounds = metrics.bounds()
        val params =
            baseParams(bounds.width(), bounds.height(), touchable = false).apply {
                gravity = Gravity.TOP or Gravity.START
            }
        if (addView(view, params)) shapes = view
    }

    private fun rebuildHandles() {
        removeHandles()
        markers.forEachIndexed { mi, marker ->
            Markers.handlesOf(marker).forEachIndexed { hi, point -> addHandle(marker, mi, hi, point) }
        }
        if (hidden) setHidden(true)
    }

    private fun addHandle(
        marker: Marker,
        mi: Int,
        hi: Int,
        point: Point,
    ) {
        val color =
            when (marker) {
                is Marker.Tap -> theme.markerTap
                is Marker.Image -> theme.markerImage
                else -> theme.markerSwipe
            }
        val label = if (hi == 0) marker.number.toString() else null
        val view = HandleView(context, theme, color, label, "${marker.number}")
        val params =
            baseParams(handleSize, handleSize, touchable = true).apply {
                gravity = Gravity.TOP or Gravity.START
                x = point.x - handleSize / 2
                y = point.y - handleSize / 2
            }
        if (!addView(view, params)) return
        val handle = Handle(view, params, mi, hi)
        handles += handle
        attachDrag(handle)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag(handle: Handle) {
        var offsetX = 0f
        var offsetY = 0f
        var downX = 0f
        var downY = 0f
        var removed = false
        val longPress =
            Runnable {
                removed = true
                removeMarker(handle.markerIndex)
            }
        handle.view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    removed = false
                    downX = event.rawX
                    downY = event.rawY
                    offsetX = handle.params.x + handleSize / 2f - event.rawX
                    offsetY = handle.params.y + handleSize / 2f - event.rawY
                    handler.postDelayed(longPress, longPressMs)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!removed) {
                        if (kotlin.math.abs(event.rawX - downX) > slop || kotlin.math.abs(event.rawY - downY) > slop) {
                            handler.removeCallbacks(longPress)
                        }
                        drag(handle, (event.rawX + offsetX).toInt(), (event.rawY + offsetY).toInt())
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    if (!removed) onChanged(markers)
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun drag(
        handle: Handle,
        x: Int,
        y: Int,
    ) {
        if (handle.markerIndex !in markers.indices) return
        val moved = Markers.moveHandle(markers[handle.markerIndex], handle.handleIndex, Point(x, y))
        markers = markers.toMutableList().also { it[handle.markerIndex] = moved }
        shapes?.markers = markers
        val points = Markers.handlesOf(moved)
        handles.filter { it.markerIndex == handle.markerIndex }.forEach { h ->
            val p = points.getOrNull(h.handleIndex) ?: return@forEach
            h.params.x = p.x - handleSize / 2
            h.params.y = p.y - handleSize / 2
            updateView(h.view, h.params)
        }
    }

    private fun removeMarker(index: Int) {
        if (index !in markers.indices) return
        markers = Markers.renumber(markers.filterIndexed { i, _ -> i != index })
        shapes?.markers = markers
        rebuildHandles()
        onChanged(markers)
    }

    private fun removeHandles() {
        handles.forEach { removeView(it.view) }
        handles.clear()
    }

    private fun baseParams(
        width: Int,
        height: Int,
        touchable: Boolean,
    ): WindowManager.LayoutParams {
        var flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        )
    }

    private fun addView(
        view: View,
        params: WindowManager.LayoutParams,
    ): Boolean =
        try {
            windowManager.addView(view, params)
            true
        } catch (_: WindowManager.BadTokenException) {
            false
        } catch (_: IllegalStateException) {
            false
        }

    private fun updateView(
        view: View,
        params: WindowManager.LayoutParams,
    ) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            // The window is already gone.
        }
    }

    private fun removeView(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The window is already gone.
        }
    }

    private companion object {
        const val HANDLE_DP = 44
    }
}
