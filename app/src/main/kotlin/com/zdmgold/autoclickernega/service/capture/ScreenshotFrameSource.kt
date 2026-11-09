package com.zdmgold.autoclickernega.service.capture

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.port.FrameSource
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.FrameSanity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Captures the screen through the accessibility screenshot API, with no notification and no consent dialog. A
 * frame lives in memory only. A refused or protected capture is a typed failure, and so is a frame of one flat
 * colour, which is how protected content comes back, so nothing is ever matched or tapped on it.
 */
class ScreenshotFrameSource(
    private val service: AccessibilityService,
    private val keepColor: () -> Boolean = { false },
) : FrameSource {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override suspend fun next(): Outcome<Frame> =
        when (val shot = captureBitmap()) {
            is Outcome.Failure -> shot
            is Outcome.Success -> withContext(Dispatchers.Default) { toFrame(shot.value) }
        }

    /** The screen as a software bitmap. The caller owns it and must recycle it. Used by the crop tool. */
    suspend fun captureBitmap(): Outcome<Bitmap> =
        suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val bitmap = copyToSoftware(result)
                        if (!cont.isActive) {
                            bitmap?.recycle()
                            return
                        }
                        cont.resume(
                            if (bitmap != null) Outcome.Success(bitmap) else failure(CaptureCause.Internal),
                        )
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) cont.resume(failure(CaptureErrors.cause(errorCode)))
                    }
                },
            )
        }

    private fun copyToSoftware(result: AccessibilityService.ScreenshotResult): Bitmap? {
        val buffer = result.hardwareBuffer
        return try {
            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
            val soft = hardware?.copy(Bitmap.Config.ARGB_8888, false)
            hardware?.recycle()
            soft
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            buffer.close()
        }
    }

    private fun toFrame(bitmap: Bitmap): Outcome<Frame> {
        val frame =
            try {
                BitmapToFrame.convert(bitmap, keepColor())
            } finally {
                bitmap.recycle()
            }
        return if (FrameSanity.isBlank(frame)) failure(CaptureCause.SecureScreen) else Outcome.Success(frame)
    }

    private fun failure(cause: CaptureCause) = Outcome.Failure(AppError.CaptureUnavailable(cause))

    /** Stops the capture thread. The source cannot be used afterwards. */
    fun close() {
        executor.shutdown()
    }
}
