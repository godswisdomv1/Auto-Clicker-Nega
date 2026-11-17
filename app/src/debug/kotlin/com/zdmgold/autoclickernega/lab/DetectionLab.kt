package com.zdmgold.autoclickernega.lab

import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.zdmgold.autoclickernega.core.error.AppError
import com.zdmgold.autoclickernega.core.error.CaptureCause
import com.zdmgold.autoclickernega.core.lab.ConfidenceRow
import com.zdmgold.autoclickernega.core.lab.LabReport
import com.zdmgold.autoclickernega.core.lab.LabStats
import com.zdmgold.autoclickernega.core.lab.MatchRunResult
import com.zdmgold.autoclickernega.core.lab.RateResult
import com.zdmgold.autoclickernega.core.lab.RepeatResult
import com.zdmgold.autoclickernega.core.model.Condition
import com.zdmgold.autoclickernega.core.model.MatchMode
import com.zdmgold.autoclickernega.core.model.Rect
import com.zdmgold.autoclickernega.core.port.CaptureProbe
import com.zdmgold.autoclickernega.core.port.OverlayPort
import com.zdmgold.autoclickernega.core.result.Outcome
import com.zdmgold.autoclickernega.core.run.CadenceController
import com.zdmgold.autoclickernega.core.vision.Frame
import com.zdmgold.autoclickernega.core.vision.NccMatcher
import com.zdmgold.autoclickernega.service.capture.BitmapToFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The reference image: the pixels to look for, and where on the screen it was cut. */
class LabReference(
    val frame: Frame,
    val box: Rect,
) {
    val text: String get() = "${frame.w}x${frame.h} px, cut at (${box.l}, ${box.t})"
}

data class LabState(
    val busy: String? = null,
    val countdown: Int? = null,
    val progress: String? = null,
    val reference: LabReference? = null,
    val frameText: String? = null,
    val match: MatchRunResult? = null,
    val repeat: RepeatResult? = null,
    val rate: RateResult? = null,
    val message: String? = null,
)

/**
 * Measures what this phone can really do: capture a reference image and a test frame from another app, match one
 * against the other at several confidences, repeat that many times, and probe how fast the system hands out
 * screenshots. Every capture starts after a countdown, so there is time to switch to the app being measured.
 * Debug builds only. Numbers go into a report to share, never pictures.
 */
class DetectionLab(
    private val probe: CaptureProbe,
    private val overlay: OverlayPort,
    private val appVersion: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutable = MutableStateFlow(LabState())
    val state: StateFlow<LabState> = mutable.asStateFlow()

    private var frame: Frame? = null
    private var job: Job? = null

    fun captureReference(wait: Int = COUNTDOWN_SECONDS) =
        launchAction("Capture reference") {
            countdown(wait)
            when (val crop = overlay.captureCrop()) {
                is Outcome.Failure -> {
                    say(describe(crop.error))
                }

                is Outcome.Success -> {
                    crop.value?.let { result ->
                        val bitmap = BitmapFactory.decodeByteArray(result.png, 0, result.png.size)
                        if (bitmap == null) {
                            say("The cropped image could not be read.")
                        } else {
                            val template = BitmapToFrame.convert(bitmap, keepColor = false)
                            bitmap.recycle()
                            mutable.update {
                                it.copy(
                                    reference = LabReference(template, result.box),
                                    match = null,
                                    repeat = null,
                                )
                            }
                            say("Reference captured.")
                        }
                    } ?: say("Crop cancelled.")
                }
            }
        }

    /** Headless variant for the emulator harness: the reference is a region cut from a fresh capture. */
    fun captureReferenceBox(
        box: Rect,
        wait: Int = 0,
    ) = launchAction("Capture reference region") {
        countdown(wait)
        when (val captured = probe.captureFrame()) {
            is Outcome.Failure -> {
                say(describe(captured.error))
            }

            is Outcome.Success -> {
                val cut = captured.value.crop(box)
                mutable.update { it.copy(reference = LabReference(cut, box), match = null, repeat = null) }
                say("Reference captured.")
            }
        }
    }

    fun captureFrame(wait: Int = COUNTDOWN_SECONDS) =
        launchAction("Capture test frame") {
            countdown(wait)
            when (val captured = probe.captureFrame()) {
                is Outcome.Failure -> {
                    say(describe(captured.error))
                }

                is Outcome.Success -> {
                    frame = captured.value
                    mutable.update { it.copy(frameText = "${captured.value.w}x${captured.value.h} px", match = null) }
                    say("Test frame captured.")
                }
            }
        }

    fun runMatch() =
        launchAction("Match") {
            val ref = mutable.value.reference ?: return@launchAction say("Capture a reference image first.")
            val test = frame ?: return@launchAction say("Capture a test frame first.")
            val result =
                withContext(Dispatchers.Default) {
                    val safe =
                        timed {
                            NccMatcher.search(
                                test,
                                ref.frame,
                                Condition.Image("lab", LOWEST_CONFIDENCE, MatchMode.Safe),
                            )
                        }
                    val fast =
                        timed {
                            NccMatcher.search(
                                test,
                                ref.frame,
                                Condition.Image("lab", LOWEST_CONFIDENCE, MatchMode.Fast),
                            )
                        }
                    val best = safe.second.best
                    val score = best?.score ?: 0f
                    MatchRunResult(
                        score = score,
                        boxText = best?.box?.let { "(${it.l}, ${it.t}, ${it.r}, ${it.b})" } ?: "no candidate",
                        safeMedianMs = safe.first,
                        fastMedianMs = fast.first,
                        rows = CONFIDENCES.map { ConfidenceRow(it, best != null && score >= it) },
                    )
                }
            mutable.update { it.copy(match = result) }
            say("Match finished.")
        }

    fun runRepeat(wait: Int = COUNTDOWN_SECONDS) =
        launchAction("Repeat test") {
            val ref = mutable.value.reference ?: return@launchAction say("Capture a reference image first.")
            countdown(wait)
            val cadence = CadenceController()
            val matchMs = mutableListOf<Long>()
            val totalMs = mutableListOf<Long>()
            var captured = 0
            var refused = 0
            var other = 0
            var detections = 0
            val startedAt = System.nanoTime()
            repeat(REPEAT_ATTEMPTS) { attempt ->
                progress("Repeat ${attempt + 1} of $REPEAT_ATTEMPTS")
                val began = System.nanoTime()
                when (val result = probe.captureFrame()) {
                    is Outcome.Failure -> {
                        if (isTooFrequent(result.error)) {
                            refused++
                            cadence.onCaptureTooFrequent()
                        } else {
                            other++
                        }
                        delay(cadence.retryIntervalMs())
                    }

                    is Outcome.Success -> {
                        captured++
                        val found =
                            withContext(Dispatchers.Default) {
                                timed {
                                    NccMatcher.search(
                                        result.value,
                                        ref.frame,
                                        Condition.Image("lab", CONFIDENCE, MatchMode.Safe),
                                    )
                                }
                            }
                        matchMs += found.first
                        totalMs += (System.nanoTime() - began) / NANOS_PER_MS
                        if (found.second.accepted != null) detections++
                        delay(cadence.nextIntervalMs(changed = false))
                    }
                }
            }
            val elapsed = (System.nanoTime() - startedAt) / NANOS_PER_MS
            val result =
                RepeatResult(
                    attempts = REPEAT_ATTEMPTS,
                    captured = captured,
                    refused = refused,
                    otherFailures = other,
                    detections = detections,
                    confidence = CONFIDENCE,
                    matchMs = matchMs,
                    captureToMatchMs = totalMs,
                    elapsedMs = elapsed,
                    learnedMinGapMs = cadence.osMinIntervalMs,
                )
            mutable.update { it.copy(repeat = result, progress = null) }
            say("Repeat test finished.")
        }

    fun runRateProbe(wait: Int = COUNTDOWN_SECONDS) =
        launchAction("Capture rate probe") {
            countdown(wait)
            val acceptedAt = mutableListOf<Long>()
            var refused = 0
            val startedAt = System.nanoTime()
            repeat(RATE_ATTEMPTS) { attempt ->
                progress("Request ${attempt + 1} of $RATE_ATTEMPTS")
                when (probe.captureFrame()) {
                    is Outcome.Success -> acceptedAt += (System.nanoTime() - startedAt) / NANOS_PER_MS
                    is Outcome.Failure -> refused++
                }
                delay(RATE_SPACING_MS)
            }
            val result = RateResult(RATE_ATTEMPTS, acceptedAt.size, refused, LabStats.gaps(acceptedAt), RATE_SPACING_MS)
            mutable.update { it.copy(rate = result, progress = null) }
            say("Rate probe finished.")
        }

    fun cancel() {
        job?.cancel()
        mutable.update { it.copy(busy = null, countdown = null, progress = null, message = "Cancelled.") }
    }

    fun report(): String {
        val s = mutable.value
        return LabReport.build(
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = appVersion,
            reference = s.reference?.text,
            match = s.match,
            repeat = s.repeat,
            rate = s.rate,
        )
    }

    private fun launchAction(
        name: String,
        block: suspend () -> Unit,
    ) {
        if (mutable.value.busy != null) return
        mutable.update { it.copy(busy = name, message = null) }
        job =
            scope.launch {
                try {
                    block()
                } finally {
                    mutable.update { it.copy(busy = null, countdown = null, progress = null) }
                    log(name)
                }
            }
    }

    private suspend fun countdown(seconds: Int) {
        for (left in seconds downTo 1) {
            mutable.update { it.copy(countdown = left) }
            delay(MS_PER_SECOND)
        }
        mutable.update { it.copy(countdown = null) }
    }

    /** Writes the finished action and the report to logcat, so a harness without a screen can read the numbers. */
    private fun log(name: String) {
        Log.i(TAG, "done: $name | ${mutable.value.message ?: ""}")
        report().lines().forEach { Log.i(TAG, it) }
    }

    private fun say(text: String) = mutable.update { it.copy(message = text) }

    private fun progress(text: String) = mutable.update { it.copy(progress = text) }

    private fun describe(error: AppError): String =
        when (error) {
            AppError.ServiceNotEnabled -> "Turn on the accessibility service first."
            is AppError.CaptureUnavailable -> "The screen could not be captured (${error.cause})."
            else -> "Failed: $error"
        }

    private fun isTooFrequent(error: AppError) =
        (error as? AppError.CaptureUnavailable)?.cause == CaptureCause.TooFrequent

    /** Runs [block] a few times and returns the median time in ms with the last result. */
    private fun <T> timed(block: () -> T): Pair<Long, T> {
        val times = mutableListOf<Long>()
        var last: T? = null
        repeat(TIMING_RUNS) {
            val began = System.nanoTime()
            last = block()
            times += (System.nanoTime() - began) / NANOS_PER_MS
        }
        @Suppress("UNCHECKED_CAST")
        return LabStats.median(times) to (last as T)
    }

    companion object {
        const val TAG = "NegaLab"
        private val CONFIDENCES = listOf(0.60f, 0.70f, 0.80f, 0.85f, 0.90f, 0.95f)
        private const val CONFIDENCE = 0.85f
        private const val LOWEST_CONFIDENCE = 0.60f
        private const val REPEAT_ATTEMPTS = 100
        private const val RATE_ATTEMPTS = 100
        private const val RATE_SPACING_MS = 100L
        private const val TIMING_RUNS = 5
        private const val COUNTDOWN_SECONDS = 5
        private const val MS_PER_SECOND = 1000L
        private const val NANOS_PER_MS = 1_000_000L
    }
}
