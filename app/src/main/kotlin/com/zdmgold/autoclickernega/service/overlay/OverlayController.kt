package com.zdmgold.autoclickernega.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.zdmgold.autoclickernega.core.model.Point
import com.zdmgold.autoclickernega.core.model.Rect
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Owns the overlay windows: the floating panel, the markers and the crop tool. Every window is added through the
 * accessibility service with the accessibility overlay type, so no "draw over other apps" permission is needed.
 */
class OverlayController(
    private val service: AccessibilityService,
    private val theme: OverlayTheme,
    private val metrics: ScreenMetrics,
    private val actions: PanelActions,
    private val onPanelMoved: (Point) -> Unit,
) {
    private val windowManager: WindowManager = service.getSystemService(WindowManager::class.java)
    private var panel: PanelView? = null
    private var params: WindowManager.LayoutParams? = null
    private var opacity = 1f
    private var keepScreenOn = false

    val markers = MarkerLayer(service, theme, metrics)
    private val crop = CropOverlay(service, theme, metrics)

    val isPanelShown: Boolean get() = panel != null

    /** The panel's rectangle on the screen, so detection can hide it from itself. */
    fun panelBounds(): Rect? {
        val view = panel ?: return null
        val at = IntArray(2)
        view.getLocationOnScreen(at)
        return Rect(at[0], at[1], at[0] + view.width, at[1] + view.height)
    }

    fun showPanel(
        position: Point?,
        opacity: Float,
    ) {
        if (panel != null) return
        this.opacity = opacity
        val view = PanelView(service, theme, withDragging())
        view.alpha = opacity
        val bounds = metrics.bounds()
        val p =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    panelFlags(),
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = position?.x ?: (bounds.width() - theme.dp(DEFAULT_RIGHT_DP))
                    y = position?.y ?: theme.dp(DEFAULT_TOP_DP)
                }
        try {
            windowManager.addView(view, p)
            panel = view
            params = p
        } catch (_: WindowManager.BadTokenException) {
            panel = null
        } catch (_: IllegalStateException) {
            panel = null
        }
    }

    fun hidePanel() {
        panel?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                // Already gone.
            }
        }
        panel = null
        params = null
    }

    fun render(model: PanelModel) {
        panel?.render(model)
    }

    fun setOpacity(value: Float) {
        opacity = value
        panel?.alpha = value
    }

    /** Keeps the screen awake while a Start in X wait is counting down. The flag is on the panel window only. */
    fun setKeepScreenOn(on: Boolean) {
        if (on == keepScreenOn) return
        keepScreenOn = on
        val view = panel ?: return
        val p = params ?: return
        p.flags = panelFlags()
        try {
            windowManager.updateViewLayout(view, p)
        } catch (_: IllegalArgumentException) {
            // Already gone.
        }
    }

    /** Hides every overlay for an instant so a screenshot shows the screen as the app under it looks. */
    fun setHiddenForCapture(hidden: Boolean) {
        panel?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
        markers.setHidden(hidden)
    }

    /** Shows the crop tool and waits for the user. Null means they cancelled. */
    suspend fun pickRect(): Rect? =
        suspendCancellableCoroutine { cont ->
            crop.show { result ->
                crop.hide()
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation { crop.hide() }
            if (!crop.isShown && cont.isActive) cont.resume(null)
        }

    fun release() {
        crop.hide()
        markers.hide()
        hidePanel()
    }

    private fun panelFlags(): Int {
        var flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (keepScreenOn) flags = flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        return flags
    }

    private fun withDragging() =
        PanelActions(
            onToggleRun = actions.onToggleRun,
            onStop = actions.onStop,
            onCancelWait = actions.onCancelWait,
            onAddTap = actions.onAddTap,
            onAddSwipe = actions.onAddSwipe,
            onAddCurve = actions.onAddCurve,
            onAddImage = actions.onAddImage,
            onClose = actions.onClose,
            onDrag = ::dragBy,
            onDragEnd = { params?.let { onPanelMoved(Point(it.x, it.y)) } },
        )

    private fun dragBy(
        dx: Int,
        dy: Int,
    ) {
        val view = panel ?: return
        val p = params ?: return
        val bounds = metrics.bounds()
        p.x = (p.x + dx).coerceIn(0, maxOf(bounds.width() - view.width, 0))
        p.y = (p.y + dy).coerceIn(0, maxOf(bounds.height() - view.height, 0))
        try {
            windowManager.updateViewLayout(view, p)
        } catch (_: IllegalArgumentException) {
            // Already gone.
        }
    }

    private companion object {
        const val DEFAULT_RIGHT_DP = 100
        const val DEFAULT_TOP_DP = 100
    }
}
