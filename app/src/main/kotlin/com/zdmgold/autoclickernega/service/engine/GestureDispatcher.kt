package com.zdmgold.autoclickernega.service.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import com.zdmgold.autoclickernega.core.model.Action
import com.zdmgold.autoclickernega.core.model.Target
import com.zdmgold.autoclickernega.core.port.GesturePort
import com.zdmgold.autoclickernega.core.vision.GestureOutcome
import com.zdmgold.autoclickernega.service.ShapePaths
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Performs gestures through the accessibility service. A gesture the system will not take is Rejected, and ten of
 * those in a row block the run. One the system starts and then cancels, for example because the user touched the
 * screen, is Cancelled and does not count against the run.
 */
class GestureDispatcher(
    private val service: AccessibilityService,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : GesturePort {
    override suspend fun dispatch(g: Action.Gesture): GestureOutcome {
        val description = GestureShapes.describe(g) ?: return GestureOutcome.Rejected
        return suspendCancellableCoroutine { cont ->
            val callback =
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(GestureOutcome.Completed)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(GestureOutcome.Cancelled)
                    }
                }
            val accepted = service.dispatchGesture(description, callback, handler)
            if (!accepted && cont.isActive) cont.resume(GestureOutcome.Rejected)
        }
    }
}

/** Builds the system description of each gesture, clamped to what the platform allows. */
internal object GestureShapes {
    fun describe(g: Action.Gesture): GestureDescription? =
        try {
            when (g) {
                is Action.Tap -> tap(g)
                is Action.Swipe -> single(ShapePaths.line(g.from, g.to), g.durationMs)
                is Action.Curve -> single(ShapePaths.curve(g.points), g.durationMs)
                is Action.Multi -> multi(g)
            }
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun tap(g: Action.Tap): GestureDescription? {
        val target = g.at as? Target.At ?: return null
        val path = Path().apply { moveTo(target.p.x.toFloat(), target.p.y.toFloat()) }
        return single(path, g.holdMs)
    }

    private fun multi(g: Action.Multi): GestureDescription? {
        val builder = GestureDescription.Builder()
        val strokes = g.strokes.take(GestureDescription.getMaxStrokeCount())
        if (strokes.isEmpty()) return null
        for (s in strokes) {
            builder.addStroke(
                GestureDescription.StrokeDescription(ShapePaths.line(s.from, s.to), 0, clamp(g.durationMs)),
            )
        }
        return builder.build()
    }

    private fun single(
        path: Path,
        durationMs: Long,
    ): GestureDescription =
        GestureDescription
            .Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, clamp(durationMs)))
            .build()

    private fun clamp(ms: Long): Long = ms.coerceIn(1L, GestureDescription.getMaxGestureDuration())
}
