package com.zdmgold.autoclickernega.platform.diag

import com.zdmgold.autoclickernega.core.vision.DetectionEntry
import java.util.Locale

/** What the report says about the device. */
data class DeviceInfo(
    val appVersion: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
)

/**
 * The shareable report: version, device model, Android version, error classes, the last detections and the last
 * crashes. It never contains a script, its name, its events, or any picture. Detections carry only a score, a reason
 * and an outcome, and crashes carry only a short message and a stack.
 */
object DiagnosticReport {
    fun build(
        info: DeviceInfo,
        errors: Map<String, Int>,
        detections: List<DetectionEntry>,
        crashes: List<CrashEntry>,
    ): String =
        buildString {
            appendLine("Auto Clicker Nega diagnostic report")
            appendLine("App version: ${info.appVersion}")
            appendLine("Device: ${info.model}")
            appendLine("Android: ${info.androidVersion} (API ${info.sdk})")
            appendLine()
            appendLine("Errors seen:")
            if (errors.isEmpty()) {
                appendLine("  none")
            } else {
                errors.toSortedMap().forEach { (code, n) ->
                    appendLine("  $code x$n")
                }
            }
            appendLine()
            appendLine("Recent detections (${detections.size}):")
            detections.takeLast(MAX_DETECTIONS).forEach { d ->
                val score = d.score?.let { String.format(Locale.ROOT, "%.2f", it) } ?: "-"
                appendLine(
                    "  t=${d.frameTimeMs} score=$score miss=${d.miss} gesture=${d.gesture} search=${d.searchMs}ms",
                )
            }
            appendLine()
            appendLine("Crashes (${crashes.size}):")
            crashes.forEach { c ->
                appendLine("  t=${c.timeMs} thread=${c.thread}")
                appendLine("  ${c.type}: ${c.message}")
                c.stack.take(MAX_STACK_LINES).forEach { appendLine("    at $it") }
            }
        }

    private const val MAX_DETECTIONS = 30
    private const val MAX_STACK_LINES = 12
}
